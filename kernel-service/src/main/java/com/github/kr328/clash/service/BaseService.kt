package com.github.kr328.clash.service

import android.app.Service
import com.github.kr328.clash.service.util.cancelAndJoinBlockingAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    override fun onDestroy() {
        super.onDestroy()

        cancelAndJoinBlockingAsync(this)
    }
}