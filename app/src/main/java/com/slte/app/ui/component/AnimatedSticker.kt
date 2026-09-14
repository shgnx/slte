package com.slte.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/** 贴纸播放帧率上限：Telegram 的 TGS 规范即 30fps，装饰性贴纸无需更高 */
private const val STICKER_FPS = 30

/**
 * TGS 动态贴纸组件：渲染 assets 目录下的本地 TGS 贴纸（gzip 压缩的 Lottie JSON），无需网络。
 *
 * 源文件按 60fps 导出，真机逐帧重绘数百条矢量路径会掉帧（模拟器因宿主机性能察觉不到），
 * 因此把进度量化到 [STICKER_FPS]：每秒重绘次数减半，观感与 Telegram 播放一致；
 * 源文件本身 ≤30fps 时量化为逐帧，无副作用。
 *
 * @param assetPath assets 中的文件路径，如 "stickers/login.tgs"
 * @param modifier 尺寸约束，建议用 Modifier.size(96.dp)
 * @param iterations 循环次数，默认无限循环
 */
@Composable
fun AnimatedSticker(
    assetPath: String,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset(assetPath),
    )
    val rawProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )
    val progress = remember {
        derivedStateOf {
            val composition = composition ?: return@derivedStateOf rawProgress
            val steps =
                (composition.durationFrames * STICKER_FPS / composition.frameRate).coerceAtLeast(1f)
            (rawProgress * steps).toInt() / steps
        }
    }
    LottieAnimation(
        composition = composition,
        progress = { progress.value },
        modifier = modifier,
    )
}
