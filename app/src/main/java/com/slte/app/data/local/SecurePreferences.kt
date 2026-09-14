package com.slte.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.slte.app.utils.AppLog
import java.security.KeyStore

/**
 * 加密偏好存储工厂，会话 / 凭证 / 远程配置缓存共用。
 *
 * AndroidKeyStore 与加密文件可能损坏（系统升级、Keystore 异常、异常中断写入），
 * 此前实现在构建 [MasterKey] 时直接抛异常导致启动崩溃，此处提供三级降级：
 *
 * 1. 正常创建 [EncryptedSharedPreferences]；
 * 2. 失败则删除损坏的偏好文件并重建 Keystore 密钥后重试（自愈）；
 * 3. 仍失败则降级为内存存储：进程内可用但不落盘，宁可丢会话也不将 JWT/订阅
 *    token 明文写入磁盘，并在日志中留痕。
 *
 * `@Suppress("DEPRECATION")` 原因：security-crypto 已将 EncryptedSharedPreferences /
 * MasterKey 标记废弃，但官方尚无等价替代（1.1.0 仍为最新稳定版）。用法收敛于本文件
 * 并显式抑制告警，迁移到自管 Keystore + DataStore 时只需替换本文件。
 */
@Suppress("DEPRECATION")
internal object SecurePreferences {
    /** 创建加密偏好；任何情况下都返回可用实例（最差为内存实现） */
    fun create(
        context: Context,
        fileName: String,
        keyAlias: String,
    ): SharedPreferences {
        // 全新安装：直接以新别名创建
        createEncrypted(context, fileName, keyAlias)?.let { return it }

        // 存量升级兼容：旧版本用默认别名加密。新别名打不开但旧别名可打开时说明是旧数据，
        // 迁移到新别名，避免把存量数据当作"损坏"静默清除（否则升级后会话/记住密码/配置
        // 缓存全部丢失且无用户可见提示）。
        migrateLegacy(context, fileName, keyAlias)?.let { return it }

        AppLog.w(TAG, "加密存储创建失败，尝试自愈（删除损坏的偏好文件与 Keystore 密钥）: $fileName")
        runCatching { context.deleteSharedPreferences(fileName) }
        runCatching { deleteKeyStoreEntry(keyAlias) }
        createEncrypted(context, fileName, keyAlias)?.let { return it }

        AppLog.e(
            TAG,
            "加密存储不可用，已降级为内存存储（本次运行的数据不会持久化）: $fileName",
        )
        return InMemoryPreferences()
    }

    /**
     * 存量迁移：旧版本 MasterKey 默认别名能打开时，读出全部数据、用新别名重建并写回。
     * 旧版本三个存储（会话/凭证/配置缓存）共用同一默认别名，故共用这一条迁移路径。
     */
    private fun migrateLegacy(
        context: Context,
        fileName: String,
        keyAlias: String,
    ): SharedPreferences? {
        val legacy = createEncrypted(context, fileName, LEGACY_KEY_ALIAS) ?: return null
        AppLog.w(TAG, "检测到旧版本加密数据，迁移到新别名: $fileName")
        val snapshot = legacy.all
        runCatching { context.deleteSharedPreferences(fileName) }
        val fresh = createEncrypted(context, fileName, keyAlias) ?: return null
        if (snapshot.isNotEmpty()) {
            val editor = fresh.edit()
            snapshot.forEach { (k, v) ->
                when (v) {
                    is String -> editor.putString(k, v)
                    is Int -> editor.putInt(k, v)
                    is Long -> editor.putLong(k, v)
                    is Float -> editor.putFloat(k, v)
                    is Boolean -> editor.putBoolean(k, v)
                    is Set<*> ->
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(k, v as MutableSet<String>)
                }
            }
            if (!editor.commit()) {
                AppLog.w(TAG, "迁移旧数据写回失败（数据可能丢失）: $fileName")
            }
        }
        return fresh
    }

    private fun createEncrypted(
        context: Context,
        fileName: String,
        keyAlias: String,
    ): SharedPreferences? = try {
        val masterKey =
            MasterKey
                .Builder(context, keyAlias)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        AppLog.w(TAG, "创建加密存储失败（$fileName）: ${e.javaClass.simpleName}")
        null
    }

    private fun deleteKeyStoreEntry(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private const val TAG = "SLTE-SecurePrefs"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /** 旧版本 MasterKey 默认别名（MasterKey.Builder 不带别名参数时的取值） */
    private const val LEGACY_KEY_ALIAS = "_androidx_security_master_key_"
}

/**
 * 内存版 [SharedPreferences]：加密存储不可用时的降级实现，也用作单元测试替身
 * （让 SessionStore / CredentialStore 无需 Android 框架即可测试）。
 * 进程内读写语义一致，进程结束即丢弃。
 */
internal class InMemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, Any?> = synchronized(values) { values.toMutableMap() }

    override fun getString(
        key: String,
        defValue: String?,
    ): String? = synchronized(values) { values[key] as? String ?: defValue }

    override fun getStringSet(
        key: String,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? = synchronized(values) {
        @Suppress("UNCHECKED_CAST")
        (values[key] as? MutableSet<String>)
            ?: defValues
    }

    override fun getInt(
        key: String,
        defValue: Int,
    ): Int = synchronized(values) { values[key] as? Int ?: defValue }

    override fun getLong(
        key: String,
        defValue: Long,
    ): Long = synchronized(values) { values[key] as? Long ?: defValue }

    override fun getFloat(
        key: String,
        defValue: Float,
    ): Float = synchronized(values) { values[key] as? Float ?: defValue }

    override fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean = synchronized(values) { values[key] as? Boolean ?: defValue }

    override fun contains(key: String): Boolean = synchronized(values) { values.containsKey(key) }

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        if (listener != null) synchronized(listeners) { listeners.add(listener) }
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        if (listener != null) synchronized(listeners) { listeners.remove(listener) }
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(
            key: String,
            value: String?,
        ): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putStringSet(
            key: String,
            value: MutableSet<String>?,
        ): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putInt(
            key: String,
            value: Int,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putLong(
            key: String,
            value: Long,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putFloat(
            key: String,
            value: Float,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putBoolean(
            key: String,
            value: Boolean,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun remove(key: String): SharedPreferences.Editor = apply { removals.add(key) }

        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() = applyChanges()

        private fun applyChanges() {
            val changed: Set<String>
            synchronized(values) {
                if (clearAll) values.clear()
                removals.forEach { values.remove(it) }
                pending.forEach { (k, v) -> values[k] = v }
                changed = (removals + pending.keys).toSet()
            }
            if (changed.isEmpty()) return
            val snapshot = synchronized(listeners) { listeners.toList() }
            snapshot.forEach { it.onSharedPreferenceChanged(this@InMemoryPreferences, changed.first()) }
        }
    }
}
