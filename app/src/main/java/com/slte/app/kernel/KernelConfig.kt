package com.slte.app.kernel

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun profileNameFor(email: String?): String {
    if (email.isNullOrBlank()) return "SLTE"
    val digest = MessageDigest.getInstance("SHA-256").digest(email.toByteArray(Charsets.UTF_8))
    return "SLTE-" + digest.joinToString("") { "%02x".format(it) }
}

enum class ProfileUpdateResult {
    UPDATED,
    UNCHANGED,
    FAILED,
}

@Singleton
class KernelConfig
@Inject
constructor(
    private val faultReporter: KernelFaultReporter,
    private val manager: KernelManager,
    private val subscribeSource: SubscribeSource,
    private val remoteConfig: AppRemoteConfig,
    @ApplicationContext private val context: Context,
) {

    private val profileMutex = Mutex()

    @Volatile
    private var healAttemptedFor: UUID? = null

    private suspend fun <T> safe(
        default: T,
        operation: String,
        tag: String = DEFAULT_FAULT_TAG,
        block: suspend () -> T,
    ): T = faultReporter.guard(default, operation, tag, block)

    suspend fun ensureProfile(): UUID? = profileMutex.withLock { ensureProfileLocked() }

    private suspend fun ensureProfileLocked(): UUID? = safe(null, "ensureProfileLocked") {
        cleanupStalePending()
        val profiles = manager.profile() ?: return@safe null
        val subscribeUrl = subscribeUrl() ?: return@safe null
        val expectedName = profileName()

        val byName = profiles.queryAll().filter { it.name == expectedName }
        val current = byName.firstOrNull { it.source == subscribeUrl }
        byName.filter { it != current }.forEach { profiles.delete(it.uuid) }
        profiles
            .queryAll()
            .filter { it.source == subscribeUrl && it.name != expectedName }
            .forEach { profiles.delete(it.uuid) }

        val uuid = current?.uuid ?: profiles.create(Profile.Type.Url, expectedName, subscribeUrl)

        if (current == null || !current.imported) {
            if (!downloadSubscribeToPending(uuid)) return@safe null
            profiles.commit(uuid)
        } else if (healIfDefective(uuid)) {
            AppLog.w("SLTE-Kernel", "ensureProfile: 现有配置存在重名等致命问题，已用订阅重新写入")
            profiles.commit(uuid)
        }
        val profile = profiles.queryByUUID(uuid) ?: return@safe null
        val activeChanged = profiles.queryActive()?.uuid != uuid
        if (activeChanged) {
            profiles.setActive(profile)
        }

        if (activeChanged || injectDirectRule(uuid)) {
            context.sendBroadcastSelf(
                Intent(Intents.ACTION_PROFILE_CHANGED)
                    .putExtra(Intents.EXTRA_UUID, uuid.toString()),
            )
        }
        uuid
    }

    private suspend fun downloadSubscribeToPending(uuid: UUID): Boolean {
        val yaml = readSubscribeYaml() ?: return false
        val domains = directDomains().ifEmpty { return false }
        val cleaned = sanitizeOrNull(yaml, domains) ?: return false
        if (!SubscriptionSanitizer.isKernelLoadable(cleaned)) {
            AppLog.w("SLTE-Kernel", "downloadSubscribeToPending: 订阅存在重名等致命问题，拒绝写入")
            return false
        }
        val file = context.filesDir.resolve("pending/$uuid/config.yaml")
        file.parentFile?.mkdirs()
        atomicWrite(file, cleaned)
        return true
    }

    private fun importedConfigDefective(uuid: UUID): Boolean {
        val file = context.filesDir.resolve("imported/$uuid/config.yaml")
        if (!file.exists()) return true
        val text = runCatching { file.readText() }.getOrNull() ?: return false
        return !SubscriptionSanitizer.isKernelLoadable(text)
    }

    private suspend fun healIfDefective(uuid: UUID): Boolean {
        if (!importedConfigDefective(uuid)) return false
        if (healAttemptedFor == uuid) return false
        healAttemptedFor = uuid
        return downloadSubscribeToPending(uuid)
    }

    suspend fun updateProfile(): ProfileUpdateResult = safe(ProfileUpdateResult.FAILED, "updateProfile") {
        profileMutex.withLock {
            val profiles = manager.profile() ?: return@withLock ProfileUpdateResult.FAILED
            val subscribeUrl = subscribeUrl() ?: return@withLock ProfileUpdateResult.FAILED
            val expectedName = profileName()
            val profile =
                profiles.queryAll().firstOrNull {
                    it.name == expectedName && it.source == subscribeUrl && it.imported
                }
            if (profile == null) {
                AppLog.i("SLTE-Kernel", "updateProfile: 配置不存在，先重新导入")
                val uuid = ensureProfileLocked() ?: return@withLock ProfileUpdateResult.FAILED
                subscribeSource.saveSubscriptionUpdatedAt()
                return@withLock ProfileUpdateResult.UPDATED
            }

            AppLog.d("SLTE-Kernel", "updateProfile: downloading subscription")
            val yaml = readSubscribeYaml() ?: return@withLock ProfileUpdateResult.FAILED
            AppLog.d("SLTE-Kernel", "updateProfile: yaml size=${yaml.length}")

            val domains = directDomains().ifEmpty { return@withLock ProfileUpdateResult.FAILED }
            val cleaned = sanitizeOrNull(yaml, domains) ?: return@withLock ProfileUpdateResult.FAILED
            if (!SubscriptionSanitizer.isKernelLoadable(cleaned)) {
                AppLog.w("SLTE-Kernel", "updateProfile: 订阅存在重名等致命问题，保留现有配置")
                return@withLock ProfileUpdateResult.FAILED
            }
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

        return String(buffer.toByteArray(), Charsets.UTF_8)
    }

    fun injectDirectRule(uuid: UUID): Boolean {
        val domains = directDomains().ifEmpty { return false }
        val file = context.filesDir.resolve("imported/$uuid/config.yaml")
        if (!file.exists()) return false
        val current = file.readText()
        val cleaned = sanitizeOrNull(current, domains) ?: return false
        if (current == cleaned) return false
        atomicWrite(file, cleaned)
        return true
    }

    private fun profileName(): String = profileNameFor(subscribeSource.getEmail())

    suspend fun deleteAccountProfiles(email: String?): Boolean = safe(false, "deleteAccountProfiles") {
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

    private fun directDomains(): List<String> {
        val configured = remoteConfig.directDomains
        val current = apiDomain() ?: return configured
        return buildList {
            if (current !in this) add(current)
            configured.filter { it != current }.forEach { if (it !in this) add(it) }
        }
    }

    private fun sanitizeOrNull(
        yaml: String,
        domains: List<String>,
    ): String? = SubscriptionSanitizer.sanitize(yaml, domains).takeIf { it.isNotBlank() }

    private fun apiDomain(): String? {
        val host = runCatching { java.net.URI(remoteConfig.apiBaseUrl).host }.getOrNull() ?: return null
        val labels = host.split(".")
        return if (labels.size >= 2) labels.takeLast(2).joinToString(".") else host
    }

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

        const val MAX_SUBSCRIPTION_BYTES = 20 * 1024 * 1024

        private const val PENDING_DIR_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }

    private fun subscribeUrl(): String? = remoteConfig.apiBaseUrl.trimEnd('/') + BuildConfig.SUBSCRIBE_PATH
}
