package com.slte.app.kernel

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.net.VpnService
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.github.kr328.clash.common.constants.Authorities
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.service.RemoteService
import com.github.kr328.clash.service.StatusProvider
import com.github.kr328.clash.service.TunService
import com.github.kr328.clash.service.remote.IClashManager
import com.github.kr328.clash.service.remote.ILogObserver
import com.github.kr328.clash.service.remote.IProfileManager
import com.github.kr328.clash.service.remote.IRemoteService
import com.github.kr328.clash.service.remote.unwrap
import com.github.kr328.clash.service.util.sendBroadcastSelf
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 内核生命周期管理：绑定后台服务、连接状态、VPN 启停 */
@Singleton
class KernelManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * 绑定状态的唯一写线程：ServiceConnection 回调（主线程）与重绑协程全部收敛到
     * Main.immediate 串行执行，杜绝 remote/bound/rebindJob 的跨线程竞态与双重绑定。
     */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 远程调用与状态查询在 IO 线程执行（不阻塞主线程） */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var remote: IRemoteService? = null

    /** 绑定状态：主线程写（ServiceConnection/重绑协程），IO 线程读（ensureBound），需 volatile 保证可见性 */
    @Volatile
    private var bound = false
    private var receiverRegistered = false
    private var rebindJob: Job? = null
    private var rebindAttempts = 0

    /** 状态查询序号：只有最新一次查询允许写回，避免慢查询覆盖广播刚给出的新状态 */
    @Volatile
    private var syncSeq = 0

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _profileLoaded = MutableStateFlow(0)

    /** 内核配置每次（重新）加载完成时 +1，用于驱动 UI 重新同步 */
    val profileLoaded: StateFlow<Int> = _profileLoaded.asStateFlow()

    /** 内核（mihomo）日志观察者：桥接到 AppLog 缓冲区，供日志导出排查问题 */
    private val kernelLogObserver =
        object : ILogObserver {
            override fun newItem(log: LogMessage) {
                AppLog.kernel(log)
            }
        }

    private val connection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder,
            ) {
                AppLog.d("SLTE-Kernel", "onServiceConnected: $name")
                // 有活连接即持有绑定注册：复位 bound，覆盖「框架自动重连但 bound 已被置 false」的窗口
                bound = true
                rebindJob?.cancel()
                rebindJob = null
                rebindAttempts = 0
                remote = service.unwrap(IRemoteService::class)
                // 订阅内核日志（mihomo），失败不影响连接
                scope.launch {
                    runCatching { remote?.clash()?.setLogObserver(kernelLogObserver) }
                        .onFailure { AppLog.w("SLTE-Kernel", "setLogObserver failed: ${sanitizeLog(it.message ?: "Unknown")}") }
                }
                syncConnectedState()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                handleBindingLost("onServiceDisconnected: $name")
            }

            override fun onBindingDied(name: ComponentName?) {
                handleBindingLost("onBindingDied: $name")
            }

            override fun onNullBinding(name: ComponentName?) {
                // onBind 返回 null：服务不可用，解绑后交给退避重绑
                handleBindingLost("onNullBinding: $name")
            }
        }

    /**
     * 统一的「绑定已失效」处理。
     *
     * 必须复位 [bound]：此前只清 [remote] 而保留 bound=true，退避重绑与按需 bind()
     * 双双短路，内核服务被回收后再也无法重连（clash()/profile() 恒为 null）。
     */
    private fun handleBindingLost(reason: String) {
        AppLog.w("SLTE-Kernel", "$reason → 复位绑定状态并进入退避重绑")
        remote = null
        bound = false
        rebindAttempts = 0
        _connected.value = false
        unbindQuietly()
        scheduleRebind()
    }

    /** 解绑旧注册，避免重绑期间叠加注册；未绑定时系统会抛 IllegalArgumentException，忽略即可 */
    private fun unbindQuietly() {
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            AppLog.d("SLTE-Kernel", "unbindService: ${sanitizeLog(e.message ?: "Unknown")}")
        }
    }

    private val statusReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intents.ACTION_CLASH_STARTED -> _connected.value = true
                    Intents.ACTION_CLASH_STOPPED -> _connected.value = false
                    Intents.ACTION_PROFILE_LOADED -> _profileLoaded.value += 1
                }
            }
        }

    /** 绑定后台内核服务并监听连接状态广播（应用主进程调用一次即可） */
    fun bind() {
        mainScope.launch { bindLocked() }
    }

    /** 主线程串行执行：绑定成功置位并注册广播；失败进入退避重绑 */
    private fun bindLocked() {
        if (bound) return
        if (doBind()) {
            bound = true
            rebindAttempts = 0
            registerStatusReceiver()
            syncConnectedState()
        } else {
            scheduleRebind()
        }
    }

    /** 绑定内核服务；同步失败/异常统一返回 false（不向上抛） */
    private fun doBind(): Boolean = try {
        context.bindService(
            Intent(context, RemoteService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
    } catch (e: Exception) {
        AppLog.w("SLTE-Kernel", "bindService failed: ${sanitizeLog(e.message ?: "Unknown")}")
        false
    }

    /** 状态广播只注册一次（进程生命周期内不注销，与单例同寿命） */
    private fun registerStatusReceiver() {
        if (receiverRegistered) return
        receiverRegistered = true
        ContextCompat.registerReceiver(
            context,
            statusReceiver,
            IntentFilter().apply {
                addAction(Intents.ACTION_CLASH_STARTED)
                addAction(Intents.ACTION_CLASH_STOPPED)
                addAction(Intents.ACTION_PROFILE_LOADED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * 断线指数退避重绑：1s / 2s / 4s … 封顶 [MAX_REBIND_DELAY_MS]，
     * 最多 [MAX_REBIND_ATTEMPTS] 次；每次绑定失败都会继续下一轮（链自续），
     * 耗尽后保持未绑定状态，由 [ensureBound] 按需重新拉起。
     */
    private fun scheduleRebind() {
        if (bound || rebindJob?.isActive == true) return
        rebindJob =
            mainScope.launch {
                while (!bound && remote == null && rebindAttempts < MAX_REBIND_ATTEMPTS) {
                    val backoffMs = minOf(INITIAL_REBIND_DELAY_MS shl rebindAttempts, MAX_REBIND_DELAY_MS)
                    AppLog.d("SLTE-Kernel", "rebind #$rebindAttempts in ${backoffMs}ms")
                    delay(backoffMs)
                    // 等待期间若已恢复连接则终止本轮
                    if (bound || remote != null) return@launch
                    rebindAttempts++
                    if (doBind()) {
                        bound = true
                        registerStatusReceiver()
                        // onServiceConnected 到达后复位计数
                        return@launch
                    }
                }
                if (!bound && remote == null) {
                    AppLog.w("SLTE-Kernel", "rebind 已重试 $MAX_REBIND_ATTEMPTS 次仍失败，等待下次按需 bind()")
                }
            }
    }

    /** 未绑定（含重绑耗尽后）时按需重新绑定，供内核访问入口复用 */
    private fun ensureBound() {
        if (!bound) bind()
    }

    /** 查询后台 StatusProvider：内核服务正在运行且已加载配置时返回 true */
    private fun syncConnectedState() {
        val seq = ++syncSeq
        scope.launch {
            val result =
                try {
                    context.contentResolver.call(
                        Uri
                            .Builder()
                            .scheme("content")
                            .authority(Authorities.STATUS_PROVIDER)
                            .build(),
                        StatusProvider.METHOD_CURRENT_PROFILE,
                        null,
                        null,
                    ) != null
                } catch (e: Exception) {
                    AppLog.w("SLTE-Kernel", "syncConnectedState 查询失败: ${sanitizeLog(e.message ?: "Unknown")}")
                    false
                }
            // 慢查询返回时若已有更新的状态写入（广播/新查询），放弃本次结果
            if (seq == syncSeq) _connected.value = result
        }
    }

    /** 需要先弹 VPN 授权时返回授权 Intent，否则返回 null */
    fun vpnRequestIntent(): Intent? = VpnService.prepare(context)

    /** 启动 TUN 模式内核服务（需先通过 VPN 授权） */
    fun startVpn() {
        AppLog.i("SLTE-Kernel", "startVpn: 请求启动 TUN")
        context.startForegroundService(Intent(context, TunService::class.java))
    }

    fun stopVpn() {
        AppLog.i("SLTE-Kernel", "stopVpn: 请求停止 TUN")
        context.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
    }

    internal fun clash(): IClashManager? {
        ensureBound()
        return remote?.clash()
    }

    internal fun profile(): IProfileManager? {
        ensureBound()
        return remote?.profile()
    }

    companion object {
        /** 第一次重绑前等待（毫秒）；后续按 2 的指数递增，封顶见下 */
        private const val INITIAL_REBIND_DELAY_MS = 1_000L

        /** 重绑退避封顶（30s），避免长期间断时无限拉长 */
        private const val MAX_REBIND_DELAY_MS = 30_000L

        /** 单组重绑尝试上限：超过后视为服务持久异常，保持未绑定交由按需 bind() 恢复 */
        private const val MAX_REBIND_ATTEMPTS = 5
    }
}
