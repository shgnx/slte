package com.slte.app.kernel

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.util.sendBroadcastSelf
import com.slte.app.BuildConfig
import com.slte.app.utils.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 内核配置账号标记：按邮箱哈希生成，跨会话稳定；无邮箱时回退通用名 */
internal fun profileNameFor(email: String?): String {
    if (email.isNullOrBlank()) return "SLTE"
    val digest = MessageDigest.getInstance("SHA-256").digest(email.toByteArray(Charsets.UTF_8))
    return "SLTE-" + digest.joinToString("") { "%02x".format(it) }
}

/** 订阅更新结果：未变更时跳过内核重载与节点缓存失效，避免每次启动全量重放 */
enum class ProfileUpdateResult {
    UPDATED,
    UNCHANGED,
    FAILED,
}

/** 内核订阅配置管理：导入、更新、直连规则注入 */
@Singleton
class KernelConfig
@Inject
constructor(
    private val manager: KernelManager,
    private val subscribeSource: SubscribeSource,
    private val remoteConfig: AppRemoteConfig,
    @ApplicationContext private val context: Context,
) {
    /** 配置导入/更新互斥：串行化并发下载与文件写入 */
    private val profileMutex = Mutex()

    private suspend fun <T> safe(
        default: T,
        block: suspend () -> T,
    ): T = try {
        withContext(Dispatchers.IO) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.logAsFault()
        default
    }

    /** 确保订阅已导入内核配置，返回激活的配置 UUID */
    suspend fun ensureProfile(): UUID? = profileMutex.withLock { ensureProfileLocked() }

    private suspend fun ensureProfileLocked(): UUID? = safe(null) {
        cleanupStalePending()
        val profiles = manager.profile() ?: return@safe null
        val subscribeUrl = subscribeUrl() ?: return@safe null
        val expectedName = profileName()

        // 账号身份 = 名称（邮箱哈希）：同名不同 URL 是 API 主地址切换后的残留，一并清理；
        // 同 URL 不同名是其他账号残留，删掉避免把别人的配置带进本账号
        val byName = profiles.queryAll().filter { it.name == expectedName }
        val current = byName.firstOrNull { it.source == subscribeUrl }
        byName.filter { it != current }.forEach { profiles.delete(it.uuid) }
        profiles
            .queryAll()
            .filter { it.source == subscribeUrl && it.name != expectedName }
            .forEach { profiles.delete(it.uuid) }

        val uuid = current?.uuid ?: profiles.create(Profile.Type.Url, expectedName, subscribeUrl)

        if (current == null || !current.imported) {
            // 预下载清洗 YAML 到 pending 目录：commit 时内核不再用 source 抓取（source 无 token）
            if (!downloadSubscribeToPending(uuid)) return@safe null
            profiles.commit(uuid)
        }
        val profile = profiles.queryByUUID(uuid) ?: return@safe null
        val activeChanged = profiles.queryActive()?.uuid != uuid
        if (activeChanged) {
            profiles.setActive(profile)
        }
        // 只有「激活配置变了」或「配置文件内容真的变了」才广播。
        // 无条件广播会让已连接状态下的 warmUp/测速触发整段内核热重载、连接被重置。
        if (activeChanged || injectDirectRule(uuid)) {
            context.sendBroadcastSelf(
                Intent(Intents.ACTION_PROFILE_CHANGED)
                    .putExtra(Intents.EXTRA_UUID, uuid.toString()),
            )
        }
        uuid
    }

    /** 首次导入预下载：用内存 token 限流下载并校验、清洗 YAML，写入 pending 目录供 commit 使用 */
    private suspend fun downloadSubscribeToPending(uuid: UUID): Boolean {
        val yaml = readSubscribeYaml() ?: return false
        val domains = directDomains().ifEmpty { return false }
        val file = context.filesDir.resolve("pending/$uuid/config.yaml")
        file.parentFile?.mkdirs()
        atomicWrite(file, SubscriptionSanitizer.sanitize(yaml, domains))
        return true
    }

    /** 手动更新订阅：下载 YAML 并尝试热重载；内容未变更时跳过写盘与重载广播 */
    suspend fun updateProfile(): ProfileUpdateResult = safe(ProfileUpdateResult.FAILED) {
        profileMutex.withLock {
            val profiles = manager.profile() ?: return@withLock ProfileUpdateResult.FAILED
            val subscribeUrl = subscribeUrl() ?: return@withLock ProfileUpdateResult.FAILED
            val expectedName = profileName()
            val profile =
                profiles.queryAll().firstOrNull {
                    it.name == expectedName && it.source == subscribeUrl && it.imported
                }
            if (profile == null) {
                // 登出重登后内核配置已被删除：先走首次导入（下载+清洗+commit），无需二次下载
                AppLog.i("SLTE-Kernel", "updateProfile: 配置不存在，先重新导入")
                val uuid = ensureProfileLocked() ?: return@withLock ProfileUpdateResult.FAILED
                subscribeSource.saveSubscriptionUpdatedAt()
                return@withLock ProfileUpdateResult.UPDATED
            }

            AppLog.d("SLTE-Kernel", "updateProfile: downloading subscription")
            val yaml = readSubscribeYaml() ?: return@withLock ProfileUpdateResult.FAILED
            AppLog.d("SLTE-Kernel", "updateProfile: yaml size=${yaml.length}")

            val domains = directDomains().ifEmpty { return@withLock ProfileUpdateResult.FAILED }
            val cleaned = SubscriptionSanitizer.sanitize(yaml, domains)
            val file = context.filesDir.resolve("imported/${profile.uuid}/config.yaml")
            if (file.exists() && file.readText() == cleaned) {
                AppLog.d("SLTE-Kernel", "updateProfile: 订阅内容未变化，跳过内核重载")
                subscribeSource.saveSubscriptionUpdatedAt()
                return@withLock ProfileUpdateResult.UNCHANGED
            }
            file.parentFile?.mkdirs()
            atomicWrite(file, cleaned)
            context.sendBroadcastSelf(
                Intent(Intents.ACTION_PROFILE_CHANGED)
                    .putExtra(Intents.EXTRA_UUID, profile.uuid.toString()),
            )
            subscribeSource.saveSubscriptionUpdatedAt()
            ProfileUpdateResult.UPDATED
        }
    }

    /**
     * 下载订阅 YAML：限流读取（超限拒绝）+ 内容校验（非 Clash 订阅拒绝）。
     */
    private suspend fun readSubscribeYaml(): String? {
        val body = subscribeSource.fetchSubscribeYaml()
        if (body == null) {
            AppLog.w("SLTE-Kernel", "readSubscribeYaml: 订阅响应体为空，拒绝写入")
            return null
        }
        val text = body.byteStream().use { readLimited(it, MAX_SUBSCRIPTION_BYTES) }
        if (text == null) {
            AppLog.w("SLTE-Kernel", "readSubscribeYaml: 订阅超过大小上限 ${MAX_SUBSCRIPTION_BYTES / 1024 / 1024}MB，拒绝写入")
            return null
        }
        if (!SubscriptionSanitizer.isValidSubscribeYaml(text)) {
            AppLog.w("SLTE-Kernel", "readSubscribeYaml: 响应不是有效 Clash 订阅，拒绝写入")
            return null
        }
        return text
    }

    /** 限流读取响应体：超过上限返回 null（防御异常/恶意服务端 OOM） */
    private fun readLimited(
        input: java.io.InputStream,
        maxBytes: Int,
    ): String? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > maxBytes) return null
            buffer.write(chunk, 0, n)
        }
        // 不用 ByteArrayOutputStream.toString(Charset)：该 API 是 Java 10+，Android 12 及以下不存在
        return String(buffer.toByteArray(), Charsets.UTF_8)
    }

    /**
     * 对已落盘的订阅配置执行安全清洗：端口清零、pattern 清空、
     * 缩进感知注入 App 自身域名直连规则与 fake-ip 豁免
     * （域名跟随远程配置 + 实际 API 地址，保证业务 API 不依赖节点）。
     *
     * @return 内容是否真的发生了变化。此前恒返回 true（文件存在即重写），已连接状态下
     * 每次 warmUp/测速都会广播 PROFILE_CHANGED、触发内核整体热重载、连接被重置；
     * 现与 [updateProfile] 的 no-op 短路一致：未变化不写盘、不广播。
     */
    fun injectDirectRule(uuid: UUID): Boolean {
        val domains = directDomains().ifEmpty { return false }
        val file = context.filesDir.resolve("imported/$uuid/config.yaml")
        if (!file.exists()) return false
        val current = file.readText()
        val cleaned = SubscriptionSanitizer.sanitize(current, domains)
        if (current == cleaned) return false
        atomicWrite(file, cleaned)
        return true
    }

    private fun profileName(): String = profileNameFor(subscribeSource.getEmail())

    /**
     * 删除指定账号在内核中的全部订阅配置（登出时调用）。
     * 按账号标记删除，覆盖 failover 切换产生的多 URL 孤儿；标记缺失时按当前订阅 URL 兜底。
     */
    suspend fun deleteAccountProfiles(email: String?): Boolean = safe(false) {
        val profiles = manager.profile() ?: return@safe false
        val expectedName = email?.let { profileNameFor(it) }
        val url = subscribeUrl()
        profiles
            .queryAll()
            .filter { profile ->
                profile.name == expectedName ||
                    (
                        expectedName == null &&
                            url != null &&
                            (profile.source == url || profile.source.startsWith(url))
                        )
            }.forEach { profiles.delete(it.uuid) }
        true
    }

    /** 直连域名列表：远程配置下发 + 当前 API 域名，合并去重（全部在自有域名白名单内） */
    private fun directDomains(): List<String> {
        val configured = remoteConfig.directDomains
        val current = apiDomain() ?: return configured
        return buildList {
            if (current !in this) add(current)
            configured.filter { it != current }.forEach { if (it !in this) add(it) }
        }
    }

    private fun apiDomain(): String? {
        val host = remoteConfig.apiBaseUrl.toUri().host ?: return null
        val labels = host.split(".")
        return if (labels.size >= 2) labels.takeLast(2).joinToString(".") else host
    }

    /** 同目录 tmp + rename 原子替换；fsync 落盘，写失败清理 tmp 不留垃圾 */
    private fun atomicWrite(
        file: java.io.File,
        text: String,
    ) {
        val tmp = java.io.File(file.parentFile, "${file.name}.tmp")
        try {
            java.io.FileOutputStream(tmp).use { out ->
                out.write(text.toByteArray())
                out.fd.sync()
            }
            if (!tmp.renameTo(file)) {
                throw java.io.IOException("rename failed: ${file.name}")
            }
        } finally {
            tmp.delete()
        }
    }

    /** 清理 pending 目录下超过 [PENDING_DIR_MAX_AGE_MS] 的残留（下载中断/commit 失败的孤儿） */
    private fun cleanupStalePending() {
        runCatching {
            val cutoff = System.currentTimeMillis() - PENDING_DIR_MAX_AGE_MS
            context.filesDir
                .resolve("pending")
                .listFiles()
                ?.filter { it.isDirectory && it.lastModified() < cutoff }
                ?.forEach { it.deleteRecursively() }
        }
    }

    companion object {
        /** 订阅 YAML 大小上限（20MB）：防御异常/恶意服务端超大响应 */
        const val MAX_SUBSCRIPTION_BYTES = 20 * 1024 * 1024

        /** pending 目录残留上限：超过此时长的下载目录视为失败残留，兜底清理 */
        private const val PENDING_DIR_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }

    private fun subscribeUrl(): String? {
        // 不含 token：Room source 列为明文数据库，订阅 token 不落库
        // （下载统一走内存 token，见 updateProfile / downloadSubscribeToPending）
        return remoteConfig.apiBaseUrl.trimEnd('/') + BuildConfig.SUBSCRIBE_PATH
    }
}
