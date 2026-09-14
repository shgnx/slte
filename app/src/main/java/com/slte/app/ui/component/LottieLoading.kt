package com.slte.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.slte.app.R

/** 全局 Lottie Loading 动画（用于按钮内/小尺寸场景） */
@Composable
fun LottieLoadingIcon(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading),
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier,
    )
}
