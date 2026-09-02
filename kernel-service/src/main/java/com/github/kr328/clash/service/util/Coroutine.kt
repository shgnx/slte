package com.github.kr328.clash.service.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking

fun CoroutineScope.cancelAndJoinBlocking() {
    val scope = this

    runBlocking {
        scope.coroutineContext.job.cancel()
        scope.coroutineContext.job.join()
    }
}

fun cancelAndJoinBlockingAsync(vararg scopes: CoroutineScope) {
    val targets = scopes.distinct()
    if (targets.isEmpty()) return

    Thread {
        targets.forEach { it.cancelAndJoinBlocking() }
    }.apply {
        isDaemon = true
        start()
    }
}