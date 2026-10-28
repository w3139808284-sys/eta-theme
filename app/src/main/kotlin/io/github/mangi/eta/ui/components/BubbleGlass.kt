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
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 气泡材质统一入口。
 *
 * - SOLID：不附加视觉（由调用方画纯色表面）
 * - GLASS：轻量玻璃拟态（半透明 + 描边 + 阴影 + 顶部高光）
 * - LIQUID_GLASS：真实背景采样玻璃；当 [backdrop] 缺失或设备不支持 RuntimeShader 时，
 *   **自动降级为 GLASS**，保证视觉可用且不崩。
 *
 * [backdrop] 必须是「气泡背后的背景层」，不能是包含气泡自身的消息列表，
 * 否则会自采样成模糊套模糊。
 */
@Composable
fun Modifier.bubbleSurface(
    material: BubbleMaterial,
    shape: RoundedCornerShape,
    baseColor: Color,
    backdrop: LayerBackdrop? = null,
): Modifier {
    if (material == BubbleMaterial.LIQUID_GLASS) {
        val liquid = liquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            containerColor = baseColor,
        )
        if (liquid != null) return liquid
        // 降级：无 backdrop / 不支持 shader 时退回 GLASS 观感。
    }
    if (material != BubbleMaterial.GLASS && material != BubbleMaterial.LIQUID_GLASS) {
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
