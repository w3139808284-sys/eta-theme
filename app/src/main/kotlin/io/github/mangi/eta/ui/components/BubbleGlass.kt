package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.data.model.BubbleMaterial

/**
 * 轻量玻璃拟态：半透明填充 + 柔和描边 + 阴影 + 顶部高光。
 *
 * 完全基于标准 Compose API 实现，无需 RenderEffect/特殊硬件，任何 API 33+ 设备表现一致。
 * 不引入 Operit 使用的 kyant/liquid 等第三方玻璃库，避免增加依赖面与兼容风险。
 */
@Composable
fun Modifier.bubbleSurface(
    material: BubbleMaterial,
    shape: RoundedCornerShape,
    baseColor: Color,
): Modifier {
    if (material != BubbleMaterial.GLASS) {
        return this
    }
    val isLight = baseColor.luminance() >= 0.5f
    val tint = if (isLight) {
        baseColor.copy(alpha = 0.60f)
    } else {
        baseColor.copy(alpha = 0.66f)
    }
    val borderColor = if (isLight) {
        Color.White.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }
    val gloss = if (isLight) {
        Color.White.copy(alpha = 0.30f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }
    return this
        .shadow(
            elevation = 3.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.12f),
        )
        .clip(shape)
        .background(tint)
        .border(width = 0.6.dp, color = borderColor, shape = shape)
        .drawWithContent {
            drawContent()
            // 顶部高光：沿上缘渐隐，模拟玻璃反光。
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(gloss, Color.Transparent),
                ),
                size = size.copy(height = size.height * 0.5f),
            )
        }
}
