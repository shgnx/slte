package com.slte.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 全局圆角系统：小元素 4dp、卡片/输入框 8dp、Card/BottomSheet 10dp、大型容器 14dp。 */
val SlteShapes =
    Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(14.dp),
    )
