package com.slte.app.data.remote.config

import android.content.Context
import android.content.SharedPreferences
import com.slte.app.BuildConfig
import com.slte.app.kernel.AppRemoteConfig
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog

/** OSS 远程下发的运行时可调配置 */
@Serializable
data class RemoteConfigData(
    /** 竞速选出的主 API 地址 */
    val apiBaseUrl: String = BuildConfig.API_BASE_URL,
    /** 全部可用 API 候选（含主地址），运行时 failover 轮询用 */
    val apiBaseUrls: List<String> = emptyList(),
    /** 直连域名列表（清洗注入 + 内核兜底用），为空时回退 apiBaseUrl 域名 */
    val directDomains: List<String> = emptyList(),
    val apiType: String = BuildConfig.API_TYPE,
    val crispWebsiteId: String = BuildConfig.CRISP_WEBSITE_ID,
    val crispEnabled: Boolean = BuildConfig.CRISP_ENABLED,
    /** 远程更新：新版本号（空 = 无更新） */
    val updateVersion: String = "",
    /** 远程更新：更新日志标题（如"更新日志"） */
    val updateChangelogTitle: String = "",
    /** 远程更新：更新日志内容 */
    val updateChangelog: String = "",
    /** 远程更新：是否强制更新 */
    val updateForce: Boolean = false,
    /** 远程更新：APK 下载地址 */
    val updateApkUrl: String = ""
)

/** OSS 配置文件原始 JSON（缺失字段回退 BuildConfig 默认值） */
@Serializable
private data class RemoteConfigDto(
    @SerialName("api_base_url") val apiBaseUrl: String? = null,
    /** 单值或数组均可：字符串按单地址解析，数组按多地址竞速解析 */
    @SerialName("api_base_urls") val apiBaseUrls: JsonElement? = null,
    /** 兼容第三方托管格式的 API 地址列表（单值/数组，支持 Base64 混用，与 api_base_urls 等价） */
    @SerialName("api") val api: JsonElement? = null,
    @SerialName("direct_domains") val directDomains: JsonElement? = null,
    @SerialName("api_type") val apiType: String? = null,
    @SerialName("crisp_website_id") val crispWebsiteId: String? = null,
    @SerialName("crisp_enabled") val crispEnabled: Boolean? = null,
    /** 配置版本号：多镜像源同时可用时选版本最高者 */
    @SerialName("config_version") val configVersion: String? = null,
    @SerialName("update_version") val updateVersion: String? = null,
    @SerialName("update_changelog_title") val updateChangelogTitle: String? = null,
    @SerialName("update_changelog") val updateChangelog: String? = null,
    @SerialName("update_force") val updateForce: Boolean? = null,
    @SerialName("update_apk_url") val updateApkUrl: String? = null
)

/** 配置缓存条目：配置本体 + meta（版本/时间戳/来源/ETag），整体原子写入 */
@Serializable
internal data class CachedConfig(
    val config: RemoteConfigData,
    /** 配置版本号（多源择优依据） */
    val version: String = "",
    /** 拉取成功时间戳（毫秒），用于短缓存与过期判定 */
    val fetchedAt: Long = 0L,
    /** 上次成功配置源地址 */
    val sourceUrl: String = "",
    /** 上次成功响应的 ETag（下次请求带 If-None-Match 避免重复下载） */
    val etag: String = ""
)

/**
 * 远程配置：多 OSS 源并发竞速拉取，按版本择优；API 地址经 EndpointSelector 粘滞选主。
 *
 * 容错层级：
 * 1. 多配置源并发竞速（上次成功源优先），首个合法结果用于择优，全部失败回退缓存/BuildConfig；
 * 2. 配置中的多个 API 地址并发探测，主地址粘滞（新地址明显更快或当前不健康才切换），
 *    运行期连接失败由 ApiFailoverInterceptor 自动 failover；
 * 3. 短时间缓存（5 分钟）内不重复请求；ETag/304 命中复用缓存；失败保留最后一次成功缓存
 *    （stale-while-revalidate），无缓存时回退 BuildConfig 默认值。
 */
@Singleton
class RemoteConfig @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRemoteConfig, FailoverConfig {

    override val apiBaseUrl: String get() = data.apiBaseUrl

    override val directDomains: List<String> get() = data.directDomains

    /** 端点选择器：候选健康状态、熔断与主地址粘滞（与 failover 拦截器共享） */
    private val selector = EndpointSelector()

    /** 对外暴露的选择器（failover 拦截器等共享健康状态） */
    val endpointSelector: EndpointSelector get() = selector

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 各配置源的 ETag（进程内；丢失后回退完整下载，由短缓存兜底） */
    private val etagByUrl = ConcurrentHashMap<String, String>()

    /** 配置源拉取客户端（复用连接池；含整体超时防止慢源拖死竞速） */
    private val configClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** API 竞速探测客户端（复用连接池） */
    private val speedClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    /** 配置流：启动即发射缓存值，远程拉取完成后发射新值（更新检查/UI 响应式跟随） */
    private val _dataFlow = MutableStateFlow(loadCached()?.config ?: RemoteConfigData())
    val dataFlow: StateFlow<RemoteConfigData> = _dataFlow.asStateFlow()
    val data: RemoteConfigData get() = _dataFlow.value

    /**
     * 运行期 failover 候选列表：主地址在前，其余按健康度排列（熔断的排后）。
     * 委托 candidateOrder 统一保证主地址去重：同一地址在候选列表中只出现一次，
     * 否则拦截器会对同一地址二次重试——OkHttp 要求同一请求内前一个响应
     * 关闭后才能再次 proceed，未关闭直接重试会抛 IllegalStateException。
     */
    override fun apiCandidates(primary: String): List<String> =
        selector.candidateOrder(primary, dataFlow.value.apiBaseUrls)

    /** 启动自动拉取：由主进程 Application 调用（后台进程不重复拉取，避免双写） */
    fun startFetch() {
        scope.launch { refresh() }
    }

    /**
     * 启动半开恢复探测循环：定期对退避期已过的熔断地址探活，
     * 成功即恢复、失败重新熔断，使故障地址恢复不依赖下一次配置刷新。
     * 与配置拉取共用同一协程作用域，随进程退出自动取消。
     */
    fun startProbeLoop() {
        scope.launch {
            while (true) {
                probeHalfOpen()
                delay(PROBE_LOOP_INTERVAL_MS)
            }
        }
    }

    /** 对半开候选逐个探活：成功 recordProbe（恢复健康），失败 recordFailure（重新熔断） */
    private suspend fun probeHalfOpen() {
        val candidates = selector.halfOpenCandidates()
        if (candidates.isEmpty()) return
        candidates.forEach { url ->
            val latency = probeOne(url)
            if (latency != null) {
                selector.recordProbe(url, latency)
                AppLog.i("SLTE-Config", "RemoteConfig: 半开探测恢复 latency=$latency")
            } else {
                selector.recordFailure(url)
                AppLog.w("SLTE-Config", "RemoteConfig: 半开探测仍失败")
            }
        }
    }

    /**
     * 拉取并应用远程配置：成功返回 true；全部配置源失败返回 false（保留现有缓存）。
     * 手动"检测更新"应传 force=true 绕过短时间缓存。短时间缓存内非强制刷新直接复用；多源并发竞速择优；304 命中沿用缓存；API 候选并发探测后粘滞选主（当前主健康则保持，明显更快才切换）；后端类型只认构建期内置值。
     */
    suspend fun refresh(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        val cached = loadCached()
        if (!force && cached != null && ConfigValidation.isCacheFresh(cached.fetchedAt, now, CONFIG_CACHE_TTL_MS)) {
            _dataFlow.value = cached.config
            return true
        }

        val urls = orderedConfigUrls()
        if (urls.isEmpty()) return false
        val result = try {
            withTimeout(CONFIG_FETCH_TIMEOUT_MS) {
                ConfigRace.race(urls) { url -> fetchOne(url, cached) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w("SLTE-Config", "RemoteConfig: 配置竞速失败: ${sanitize(e.message)}")
            return false
        }
        val chosen = result.chosen ?: return false

        if (chosen.notModified && cached != null) {
            writeCache(cached.copy(fetchedAt = now, sourceUrl = chosen.url))
            _dataFlow.value = cached.config
            AppLog.i("SLTE-Config", "RemoteConfig: 304 命中，复用缓存")
            return true
        }

        val dto = try {
            json.decodeFromString<RemoteConfigDto>(chosen.raw)
        } catch (e: Exception) {
            AppLog.w("SLTE-Config", "RemoteConfig: 配置解析失败: ${e.message}")
            return false
        }

        val candidates = resolveApiCandidates(dto)
        val probes = probeAll(candidates)
        probes.forEach { (url, latency) -> selector.recordProbe(url, latency) }
        val primary = selector.pickPrimary(
            candidates = candidates,
            probes = probes,
            currentPrimary = data.apiBaseUrl.takeIf { it in candidates }
        ) ?: BuildConfig.API_BASE_URL
        selector.updatePrimary(primary)

        val merged = RemoteConfigData(
            apiBaseUrl = primary,
            apiBaseUrls = candidates.ifEmpty { listOf(BuildConfig.API_BASE_URL) },
            directDomains = resolveDirectDomains(dto.directDomains),
            apiType = dto.apiType?.trim()?.takeIf { it == BuildConfig.API_TYPE } ?: BuildConfig.API_TYPE,
            crispWebsiteId = dto.crispWebsiteId?.trim()?.takeIf { it.isNotBlank() }
                ?: BuildConfig.CRISP_WEBSITE_ID,
            crispEnabled = dto.crispEnabled ?: BuildConfig.CRISP_ENABLED,
            updateVersion = dto.updateVersion?.trim() ?: "",
            updateChangelogTitle = dto.updateChangelogTitle?.trim() ?: "",
            updateChangelog = dto.updateChangelog ?: "",
            updateForce = dto.updateForce ?: false,
            updateApkUrl = dto.updateApkUrl?.trim()?.let { takeIfAllowed(it) } ?: ""
        )
        writeCache(
            CachedConfig(
                config = merged,
                version = chosen.version,
                fetchedAt = now,
                sourceUrl = chosen.url,
                etag = etagByUrl[chosen.url] ?: ""
            )
        )
        _dataFlow.value = merged
        AppLog.i(
            "SLTE-Config",
            "RemoteConfig: 已更新 version=${chosen.version} candidates=${candidates.size} crisp=${merged.crispEnabled}"
        )
        return true
    }

    /** 单源拉取：带 If-None-Match（ETag），304 视为未变更成功；200 校验结构与版本 */
    private suspend fun fetchOne(url: String, cached: CachedConfig?): FetchedConfig? {
        val start = System.currentTimeMillis()
        return try {
            val builder = Request.Builder().url(url)
            etagByUrl[url]?.takeIf { it.isNotBlank() }?.let { builder.header("If-None-Match", it) }
            configClient.newCall(builder.build()).execute().use { resp ->
                val elapsed = System.currentTimeMillis() - start
                when {
                    resp.code == 304 -> {
                        // 仅当本地缓存与 304 同源同 ETag 时视为成功（复用缓存内容）
                        if (cached != null && cached.sourceUrl == url && cached.etag == etagByUrl[url]) {
                            FetchedConfig(url, "", cached.version, elapsed, notModified = true)
                        } else {
                            null
                        }
                    }
                    resp.isSuccessful -> {
                        val body = resp.body?.byteStream()?.use { readLimited(it, MAX_CONFIG_BYTES) }
                            ?: return@use null
                        val raw = body.toString(Charsets.UTF_8)
                        val dto = try {
                            json.decodeFromString<RemoteConfigDto>(raw)
                        } catch (_: Exception) {
                            null
                        } ?: return@use null
                        if (!validateDto(dto)) return@use null
                        etagByUrl[url] = resp.header("ETag") ?: ""
                        FetchedConfig(url, raw, dto.configVersion ?: "", elapsed)
                    }
                    else -> null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w("SLTE-Config", "RemoteConfig: 配置源不可用: ${sanitize(e.message)}")
            null
        }
    }

    /** 配置字段范围校验：版本号字符集限制，其余字段在合并阶段做白名单过滤 */
    private fun validateDto(dto: RemoteConfigDto): Boolean {
        val version = dto.configVersion ?: return true
        // 版本仅允许数字/字母/点/横线，防止异常字符进入版本比较与日志
        return version.matches(Regex("[0-9a-zA-Z.\\-]+"))
    }

    /** 配置源排序：上次成功地址优先（避免重复拉取后可用源顺序漂移） */
    private fun orderedConfigUrls(): List<String> {
        val urls = BuildConfig.REMOTE_CONFIG_URLS.split(',').map { it.trim() }
            .filter { it.startsWith("https://") }
        val last = prefs.getString(KEY_LAST_URL, null)
        if (last != null && last in urls) {
            return listOf(last) + urls.filter { it != last }
        }
        return urls
    }

    private fun resolveApiCandidates(dto: RemoteConfigDto): List<String> {
        val fromArray = dto.apiBaseUrls?.let { el -> jsonElementToList(el) } ?: emptyList()
        val fromApi = dto.api?.let { el -> jsonElementToList(el) } ?: emptyList()
        val list = buildList {
            dto.apiBaseUrl?.let { takeIfAllowed(it) }?.let { add(it) }
            (fromArray + fromApi).mapNotNull { takeIfAllowed(it) }.forEach { if (it !in this) add(it) }
            if (isEmpty()) add(BuildConfig.API_BASE_URL)
        }
        return list
    }

    private fun jsonElementToList(element: JsonElement): List<String> = when (element) {
        is JsonPrimitive -> listOf(element.content)
        is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }
        else -> emptyList()
    }

    /** 仅接受 https 且主机在自有域名白名单内的 API 地址（Base64 编码先解码；凭据只发往受信域） */
    private fun takeIfAllowed(value: String): String? {
        val candidate = ConfigValidation.decodeApiCandidate(value)
        return if (ConfigValidation.isValidApiUrl(candidate, ALLOWED_HOST_SUFFIXES)) candidate.trim() else null
    }

    /** 直连域名：单值或数组均可，白名单校验后去重 */
    private fun resolveDirectDomains(element: JsonElement?): List<String> = buildList {
        val values = when (element) {
            null -> emptyList()
            is JsonPrimitive -> listOf(element.content)
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }
            else -> emptyList()
        }
        values.mapNotNull { takeDomain(it) }.forEach { if (it !in this) add(it) }
    }

    /** 直连域名校验：注册域名格式（两段以上 label），且在自有域名白名单内 */
    private fun takeDomain(value: String): String? =
        if (ConfigValidation.isValidDomain(value, ALLOWED_HOST_SUFFIXES)) {
            value.trim().lowercase().trimEnd('.')
        } else {
            null
        }

    /** 限流读取响应体：超过上限返回 null（防御恶意/异常配置源 OOM） */
    private fun readLimited(input: java.io.InputStream, max: Int): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > max) return null
            buffer.write(chunk, 0, n)
        }
        return buffer.toByteArray()
    }

    /** 多 API 并发探测可达性与延迟（延迟最小者参与粘滞选主；退避期内的地址不探测） */
    private suspend fun probeAll(candidates: List<String>): Map<String, Long> {
        if (candidates.isEmpty()) return emptyMap()
        return coroutineScope {
            candidates.map { url ->
                async(Dispatchers.IO) {
                    // 退避期内不探测：退避期结束后自然进入半开，由探活循环或下次刷新恢复
                    if (selector.isOpen(url)) return@async null
                    val latency = probeOne(url)
                    if (latency != null) url to latency else null
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    /** 单地址探活：探活接口可达返回延迟（毫秒），否则返回 null */
    private suspend fun probeOne(url: String): Long? {
        val start = System.currentTimeMillis()
        return try {
            speedClient.newCall(
                Request.Builder().url(url.trimEnd('/') + PROBE_PATH).build()
            ).execute().use { resp ->
                if (resp.code in 200..499) {
                    System.currentTimeMillis() - start
                } else {
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** 读取加密缓存；损坏/缺失返回 null（回退 BuildConfig 默认） */
    private fun loadCached(): CachedConfig? {
        val raw = prefs.getString(KEY_CACHE, null) ?: return null
        return try {
            json.decodeFromString<CachedConfig>(raw)
        } catch (e: Exception) {
            AppLog.w("SLTE-Config", "RemoteConfig: 缓存解析失败，回退默认配置")
            null
        }
    }

    /** 配置与来源地址同一次 edit 原子写入，避免写一半 */
    private fun writeCache(cached: CachedConfig) {
        prefs.edit()
            .putString(KEY_CACHE, json.encodeToString(cached))
            .putString(KEY_LAST_URL, cached.sourceUrl)
            .apply()
    }

    private fun sanitize(message: String?): String =
        message?.let { sanitizeLog(it) } ?: "Unknown"

    private companion object {
        const val PREFS_NAME = "slte_remote_config"
        const val KEY_CACHE = "cached_config"
        const val KEY_LAST_URL = "last_url"
        const val MAX_CONFIG_BYTES = 256 * 1024
        const val CONFIG_TIMEOUT_SECONDS = 3L
        const val CONFIG_FETCH_TIMEOUT_MS = 5_000L
        /** 短时间缓存：该窗口内非强制刷新直接复用缓存 */
        const val CONFIG_CACHE_TTL_MS = 5 * 60_000L
        /** 半开恢复探测间隔：退避期最小 5s，探测周期取 15s 平衡及时性与开销 */
        const val PROBE_LOOP_INTERVAL_MS = 15_000L
        /** API 探活路径（与 XiaoV2b 面板 guest 接口契约一致） */
        const val PROBE_PATH = "/api/v1/guest/comm/config"
        /** API 域名白名单：内置自有域 + 构建期 SLTE_ALLOWED_DOMAINS 追加；配置只能在这些域内切换 */
        val ALLOWED_HOST_SUFFIXES: List<String> = buildList {
            // 占位域：自有 API 域名后缀在此配置（与 kernel-core process.go directDomains 保持同步）
            add("example.com")
            BuildConfig.ALLOWED_DOMAINS.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .forEach { if (it !in this) add(it) }
        }
    }
}
