package com.slte.app.kernel

import android.content.Context
import android.content.Intent
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.service.util.sendBroadcastSelf
import com.slte.app.utils.Constants
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import com.slte.app.utils.AppLog

/** 首页服务器行的真实状态：当前策略 + 生效节点 */
data class KernelServerInfo(
    val selection: String?,
    val node: String?
)

/** 出口 IP（IPv4 保底，优先 IPv6）与对应国家 ISO 码（小写），countryCode 可能为 null */
data class IpGeoInfo(
    val ip: String,
    val ipv6: String? = null,
    val countryCode: String?
)

/**
 * 内核代理门面：策略组/节点选择、模式切换、测速、出口 IP。
 *
 * 分组选择与测速逻辑在同包扩展文件（KernelProxyGroup/Speed/Ip）中，
 * 本类保持门面入口与模式/TUN 管理。
 */
@Singleton
class KernelProxy @Inject constructor(
    internal val manager: KernelManager,
    internal val config: KernelConfig,
    internal val speedResultStore: SpeedResultStore,
    internal val geoIpResolver: GeoIpResolver,
    @ApplicationContext internal val context: Context
) {

    internal val modePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    internal suspend fun <T> safe(default: T, block: suspend () -> T): T = try {
        withContext(Dispatchers.IO) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLog.w("SLTE-Kernel", "${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}")
        default
    }

    /** 内核编译版本号（真实内核，非手动维护），内核不可用时返回 null */
    suspend fun coreVersion(): String? = safe(null) {
        manager.clash()?.coreVersion()
    }

    /** 当前内核代理模式（规则/全局/直连/脚本），未连接返回 null */
    suspend fun proxyMode(): String? = safe(null) {
        val clash = manager.clash() ?: return@safe null
        // 持久化 override 是模式选择的权威来源：内核刚初始化（reset 后默认 rule）
        // 或尚未加载配置时，tunnel state 无法反映用户上次的选择
        val mode = clash.queryOverride(Clash.OverrideSlot.Persist).mode
            ?: clash.queryTunnelState().mode
        when (mode) {
            TunnelState.Mode.Global -> Constants.PROXY_MODE_GLOBAL
            TunnelState.Mode.Rule -> Constants.DEFAULT_PROXY_MODE
            TunnelState.Mode.Direct -> "直连"
            TunnelState.Mode.Script -> "脚本"
        }
    }

    /** 切换代理模式：写入内核持久化 override 并热重载配置 */
    suspend fun setProxyMode(mode: String) = safe(Unit) {
        AppLog.d("SLTE-Kernel", "setProxyMode: $mode")
        // 先本地持久化，保证内核不可用时（未连接）选择不丢失
        modePrefs.edit().putString(KEY_PROXY_MODE, mode).apply()
        val clash = manager.clash()
        if (clash == null) {
            AppLog.d("SLTE-Kernel", "setProxyMode: clash=null，已本地保存，待内核就绪后同步")
            return@safe
        }
        val override = clash.queryOverride(Clash.OverrideSlot.Persist).apply {
            this.mode = if (mode == Constants.PROXY_MODE_GLOBAL) {
                TunnelState.Mode.Global
            } else {
                TunnelState.Mode.Rule
            }
        }
        clash.patchOverride(Clash.OverrideSlot.Persist, override)
        AppLog.d("SLTE-Kernel", "setProxyMode: override written, sending broadcast")
        context.sendBroadcastSelf(Intent(Intents.ACTION_OVERRIDE_CHANGED))
    }

    /** 内核就绪后把本地保存的模式选择同步到内核 override（连接/回到首页时调用） */
    suspend fun ensurePersistedMode() = safe(Unit) {
        val clash = manager.clash() ?: return@safe
        val saved = modePrefs.getString(KEY_PROXY_MODE, null) ?: return@safe
        val target = if (saved == Constants.PROXY_MODE_GLOBAL) {
            TunnelState.Mode.Global
        } else {
            TunnelState.Mode.Rule
        }
        val current = clash.queryOverride(Clash.OverrideSlot.Persist).mode
        if (current != target) {
            val override = clash.queryOverride(Clash.OverrideSlot.Persist).apply {
                this.mode = target
            }
            clash.patchOverride(Clash.OverrideSlot.Persist, override)
            context.sendBroadcastSelf(Intent(Intents.ACTION_OVERRIDE_CHANGED))
            AppLog.d("SLTE-Kernel", "ensurePersistedMode: synced $saved")
        }
    }

    /**
     * 当前 TUN 堆栈模式（system/gvisor/mixed）。
     * 以内核服务（后台进程 ServiceStore）为准；内核不可用时返回本地镜像，默认 system。
     */
    suspend fun tunStackMode(): String = safe(DEFAULT_TUN_STACK) {
        val clash = manager.clash()
        if (clash != null) {
            val current = clash.tunStackMode()
            modePrefs.edit().putString(KEY_TUN_STACK, current).apply()
            return@safe current
        }
        modePrefs.getString(KEY_TUN_STACK, null) ?: DEFAULT_TUN_STACK
    }

    /**
     * 切换 TUN 堆栈模式：本地镜像 + 后台进程持久化（ServiceStore）。
     * VPN 运行中会原地重启 TUN 立即生效；断开时设置保存，下次连接生效。
     */
    suspend fun setTunStack(mode: String) = safe(Unit) {
        val normalized = if (mode in TUN_STACK_VALUES) mode else DEFAULT_TUN_STACK
        modePrefs.edit().putString(KEY_TUN_STACK, normalized).apply()
        val clash = manager.clash()
        if (clash == null) {
            AppLog.w("SLTE-Kernel", "setTunStack: clash=null，已本地保存，待内核就绪后同步")
            return@safe
        }
        clash.setTunStackMode(normalized)
        AppLog.d("SLTE-Kernel", "setTunStack: $normalized")
    }

    /** 出口 IP 探测客户端（复用连接池） */
    internal val ipClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    companion object {
        private const val PREFS_NAME = "slte_kernel_mode"
        private const val KEY_PROXY_MODE = "proxy_mode"
        private const val KEY_TUN_STACK = "tun_stack"

        /** 与内核 ServiceStore.tunStackMode 取值一致（mihomo TUNStack 小写枚举） */
        private const val DEFAULT_TUN_STACK = "system"
        private val TUN_STACK_VALUES = setOf("system", "gvisor", "mixed")

        /** 出口 IP 探测端点：v4/v6 分开（ipify 单端点只返回对应地址族） */
        internal const val IPIFY_V4_URL = "https://api.ipify.org"
        internal const val IPIFY_V6_URL = "https://api6.ipify.org"
    }
}
