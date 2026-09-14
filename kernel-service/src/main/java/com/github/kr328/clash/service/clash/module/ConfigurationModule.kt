package com.github.kr328.clash.service.clash.module

import android.app.Service
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.StatusProvider
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.importedDir
import com.github.kr328.clash.service.util.sendProfileLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*

class ConfigurationModule(service: Service) : Module<ConfigurationModule.LoadException>(service) {
    data class LoadException(val message: String)

    private val store = ServiceStore(service)
    private val reload = Channel<Unit>(Channel.CONFLATED)

    override suspend fun run() {
        val broadcasts = receiveBroadcast {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
        }
        Log.d("ConfigurationModule: listening")

        var loaded: UUID? = null

        reload.trySend(Unit)

        while (true) {
            val changed: UUID? = select {
                broadcasts.onReceive {
                    if (it.action == Intents.ACTION_PROFILE_CHANGED)
                        UUID.fromString(it.getStringExtra(Intents.EXTRA_UUID))
                    else
                        null
                }
                reload.onReceive {
                        null
                }
            }
            Log.d("ConfigurationModule: event received, changed=$changed")

            try {
                val current = store.activeProfile
                if (current == null) {
                    // 尚未选择配置（首次启动、登出后）：保持等待，别让服务因"还没就绪"直接退出
                    Log.w("ConfigurationModule: no active profile, skip reload")
                    continue
                }

                if (current == loaded && changed != null && changed != loaded)
                    continue

                loaded = current

                val active = ImportedDao().queryByUUID(current)
                if (active == null) {
                    // 激活的配置记录已不存在（账号登出/切换 API 地址时的清理，或删除与重载竞态）。
                    // 这里**不能**抛异常：抛出会走 LoadException → TunService 退出 → 用户侧表现为
                    // VPN 无声断开。跳过本次重载，等 App 选定新配置后再广播即可。
                    Log.w("ConfigurationModule: active profile $current not found, skip reload")
                    // 允许同名 uuid 之后被重新创建时再次触发加载
                    loaded = null
                    continue
                }

                Clash.setAgeSecretKey(active.ageSecretKey?.takeIf { it.isNotBlank() })

                Clash.load(service.importedDir.resolve(active.uuid.toString())).await()

                val remove = SelectionDao().querySelections(active.uuid)
                    .filterNot { Clash.patchSelector(it.proxy, it.selected) }
                    .map { it.proxy }

                SelectionDao().removeSelections(active.uuid, remove)

                StatusProvider.currentProfile = active.name

                service.sendProfileLoaded(current)

                Log.d("ConfigurationModule: reload done")
                Log.d("Profile ${active.name} loaded")
            } catch (e: Exception) {
                return enqueueEvent(LoadException(e.message ?: "Unknown"))
            }
        }
    }
}
