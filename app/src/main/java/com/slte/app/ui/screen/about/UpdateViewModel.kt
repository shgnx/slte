package com.slte.app.ui.screen.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.BuildConfig
import com.slte.app.R
import com.slte.app.kernel.KernelProxy
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import com.slte.app.data.remote.config.RemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 更新弹窗展示判定（与常规软件一致）：
 * - 手动检查（About 页"检查更新"）永远展示结果；
 * - "稍后提醒/关闭"仅抑制会话内的自动检查，下次打开软件会再次自动提醒；
 * - 强制更新永远展示。
 */
internal fun shouldShowUpdateDialog(
    updateVersion: String,
    currentVersion: String,
    force: Boolean,
    dismissedInSession: Boolean,
    manual: Boolean
): Boolean {
    if (updateVersion.isBlank() || compareVersions(updateVersion, currentVersion) <= 0) return false
    if (force) return true
    return manual || !dismissedInSession
}

/** 版本号比较：a > b 返回正数；忽略 v 前缀与 -后缀（如 1.0.0-debug 视为 1.0.0） */
internal fun compareVersions(a: String, b: String): Int {
    val pa = a.trimStart('v').split('.', '-').map { it.toIntOrNull() ?: 0 }
    val pb = b.trimStart('v').split('.', '-').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x - y
    }
    return 0
}

/** 检查更新弹窗状态 */
sealed interface UpdateUiState {
    /** 未检查 / 弹窗已关闭 */
    data object Idle : UpdateUiState

    /** 正在检查更新 */
    data object Checking : UpdateUiState

    /**
     * 发现新版本。
     * @param versionName 新版本号
     * @param changelogTitle 更新日志标题（远程下发；空则渲染时回退本地文案）
     * @param changelog 更新日志内容（远程下发；空则渲染时回退本地文案）
     * @param force 强制更新：true 时无"稍后提醒"且弹窗不可关闭
     */
    data class Available(
        val versionName: String,
        val changelogTitle: String?,
        val changelog: String?,
        val force: Boolean
    ) : UpdateUiState

    /** 已是最新版本（提示后回到 Idle） */
    data object Latest : UpdateUiState

    /** 检查失败（提示后回到 Idle） */
    data object Error : UpdateUiState

    /** 打开下载页失败（提示后回到 Idle） */
    data class Failed(val messageRes: Int) : UpdateUiState
}

/**
 * 检查更新 ViewModel。
 *
 * 版本/更新日志/强制标记/APK 地址均来自远程配置（RemoteConfig 下发）；
 * 点击"立即更新"跳转系统浏览器下载安装包（传统下载方式）。
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val remoteConfig: RemoteConfig,
    private val kernelProxy: KernelProxy,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val _kernelVersion = MutableStateFlow<String?>(null)
    val kernelVersion: StateFlow<String?> = _kernelVersion.asStateFlow()

    /** 会话内用户已点过"稍后提醒/关闭"：自动检查不再打扰，下次启动重新提醒 */
    private var dismissedInSession = false

    init {
        viewModelScope.launch {
            repeat(10) {
                _kernelVersion.value = kernelProxy.coreVersion()
                if (_kernelVersion.value != null) return@launch
                delay(1000)
            }
        }
        // 响应式自动检查：启动即用缓存配置检查一次，远程配置拉取/刷新完成后自动复查。
        // 用户点过"稍后提醒/关闭"后会话内不自动弹窗（force 更新除外）。
        viewModelScope.launch {
            remoteConfig.dataFlow.collect {
                checkUpdate()
            }
        }
    }

    /**
     * 检查更新。
     * @param manual true = 用户主动点击（无新版时提示"已是最新"）；false = 自动检查（静默）。手动检测绕过短时间缓存，拉取失败且无缓存时明确提示失败。
     */
    fun checkUpdate(manual: Boolean = false) {
        if (_state.value is UpdateUiState.Checking || _state.value is UpdateUiState.Available) return
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            if (manual) {
                withTimeoutOrNull(REFRESH_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) { remoteConfig.refresh(force = true) }
                }
            }
            val cfg = remoteConfig.data
            val show = shouldShowUpdateDialog(
                    updateVersion = cfg.updateVersion,
                    currentVersion = BuildConfig.VERSION_NAME,
                    force = cfg.updateForce,
                    dismissedInSession = dismissedInSession,
                    manual = manual
                )
            if (!show) {
                _state.value = when {
                    manual && cfg.updateVersion.isBlank() -> UpdateUiState.Error
                    manual -> UpdateUiState.Latest
                    else -> UpdateUiState.Idle
                }
                return@launch
            }
            AppLog.i("SLTE-Update", "发现新版 ${cfg.updateVersion} force=${cfg.updateForce} manual=$manual")
            _state.value = UpdateUiState.Available(
                versionName = cfg.updateVersion,
                changelogTitle = cfg.updateChangelogTitle.ifBlank { null },
                changelog = cfg.updateChangelog.ifBlank { null },
                force = cfg.updateForce
            )
        }
    }

    /** 立即更新：跳转系统浏览器下载安装包 */
    fun updateNow() {
        val current = _state.value as? UpdateUiState.Available ?: return
        val url = remoteConfig.data.updateApkUrl
        if (!url.startsWith("https://")) {
            _state.value = UpdateUiState.Failed(R.string.update_apk_missing)
            return
        }

        AppLog.i("SLTE-Update", "跳转浏览器下载 ${remoteConfig.data.updateVersion}")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLog.w("SLTE-Update", "打开下载页失败: ${sanitizeLog(e.message ?: "Unknown")}")
            _state.value = UpdateUiState.Failed(R.string.update_download_failed)
        }
    }

    /** 非强制更新：稍后提醒（关闭弹窗，下次进入再检查） */
    fun later() {
        dismissedInSession = true
        _state.value = UpdateUiState.Idle
    }

    /** 关闭弹窗（非强制更新时允许） */
    fun dismiss() {
        dismissedInSession = true
        _state.value = UpdateUiState.Idle
    }

    /** 消费"最新版/失败"提示后回到 Idle */
    fun consumeTip() {
        _state.value = UpdateUiState.Idle
    }

    private companion object {
        /** 手动检查时等待远程配置拉取的最长时间（超过则回退缓存） */
        const val REFRESH_TIMEOUT_MS = 6_000L
    }
}
