package com.slte.app.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerDrawProbeTest {
    private fun stickersDir(): File {
        var dir = File("src/main/assets/stickers")
        if (dir.isDirectory) return dir
        dir = File("app/src/main/assets/stickers")
        if (dir.isDirectory) return dir
        var probe: File? = File(System.getProperty("user.dir"))
        while (probe != null) {
            val candidate = File(probe, "app/src/main/assets/stickers")
            if (candidate.isDirectory) return candidate
            probe = probe.parentFile
        }
        error("找不到 stickers 目录")
    }

    private fun drawsWithin(file: File, seconds: Long): Boolean {
        val json = GZIPInputStream(FileInputStream(file)).readBytes().decodeToString()
        val composition = LottieCompositionFactory.fromJsonStringSync(json, file.name).value
        val drawable = LottieDrawable()
        drawable.composition = composition
        drawable.setBounds(0, 0, 80, 80)
        val canvas = Canvas(Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888))
        val done = CountDownLatch(1)
        Thread {
            runCatching { drawable.draw(canvas) }
            done.countDown()
        }.apply { isDaemon = true }.start()
        return done.await(seconds, TimeUnit.SECONDS)
    }

    @Test
    fun `所有内置贴纸都能在一帧内画完`() {
        val failed =
            stickersDir()
                .listFiles { f -> f.extension == "tgs" }
                .orEmpty()
                .sortedBy { it.name }
                .filterNot { drawsWithin(it, 5) }
                .map { it.name }

        assertTrue("以下贴纸在一帧内画不完（多为 ind/parent 造成 Lottie 追父级链死循环）: $failed", failed.isEmpty())
    }
}
