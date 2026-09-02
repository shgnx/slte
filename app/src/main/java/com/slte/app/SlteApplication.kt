package com.slte.app

import android.app.Application
import com.github.kr328.clash.common.Global
import com.slte.app.data.local.LocaleStore
import com.slte.app.data.remote.config.CrispConfig
import com.slte.app.data.remote.config.CrispManager
import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.kernel.KernelManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * 应用入口：双进程架构下仅在主进程执行远程配置拉取、半开探测与内核服务绑定，
 * 后台进程（:background）无界面、不消费配置，避免双进程各自写缓存不一致；
 * GeoIP/GeoSite 数据解压放后台线程，保证内核连接前就绪。
 */
@HiltAndroidApp
class SlteApplication : Application() {

    @Inject
    lateinit var crispManager: CrispManager

    @Inject
    lateinit var kernelManager: KernelManager

    @Inject
    lateinit var remoteConfig: RemoteConfig

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun attachBaseContext(base: android.content.Context?) {
        val wrapped = if (base != null && getProcessName() == base.packageName) {
            LocaleStore.wrapBase(base)
        } else {
            base
        }
        super.attachBaseContext(wrapped)
        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        Thread { extractGeoFiles() }.start()
        if (getProcessName() == packageName) {
            remoteConfig.startFetch()
            remoteConfig.startProbeLoop()
            observeCrispConfig()
            kernelManager.bind()
        }
    }

    private fun observeCrispConfig() {
        scope.launch {
            remoteConfig.dataFlow.collect { cfg ->
                crispManager.init(
                    this@SlteApplication,
                    CrispConfig(cfg.crispWebsiteId, cfg.crispEnabled)
                )
            }
        }
    }

    /** 把 GeoIP/GeoSite 数据从 assets 解压到内核 home 目录 */
    private fun extractGeoFiles() {
        val clashDir = File(filesDir, "clash").apply { mkdirs() }
        val updateDate = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        listOf("geoip.metadb", "geosite.dat", "ASN.mmdb").forEach { name ->
            val target = File(clashDir, name)
            if (target.exists() && target.lastModified() < updateDate) {
                target.delete()
            }
            if (!target.exists()) {
                assets.open(name).use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
