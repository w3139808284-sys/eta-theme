package io.github.mangi.eta.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur

/**
 * Liquid Glass 气泡材质：**真实采样气泡背后的图层**并模糊、透射色彩、加边缘高光。
 *
 * 实现要点：
 * 1. 由调用方传入 [backdrop]（通过 `Modifier.layerBackdrop(backdrop)` 在背景层建立采样源），
 *    气泡用 `textureBlur` 对该图层的**当前可见像素**做实时采样，而非假透明填充。
 * 2. `BlurColors` 负责色彩透射：低饱和 + 轻微提亮 + 提高对比，得到 iOS 那种"透出背景但被压平"的观感。
 * 3. `Highlight` 只在边缘绘制一圈柔光，制造玻璃厚度感，不覆盖正文区域。
 * 4. 设备不支持 RuntimeShader 或没有可用 backdrop 时，返回 null 由调用方降级到 GLASS 分支。
 *
 * 性能：单个气泡只创建一次 `textureBlur` 绘制节点；backdrop 是全页共享的一份，
 * 因此同时可见的多个气泡复用同一采样源，成本随**可见气泡数**增长而非消息总数。
 */
@Composable
internal fun Modifier.liquidGlassSurface(
    backdrop: LayerBackdrop?,
    shape: RoundedCornerShape,
    containerColor: Color,
    blurRadius: Float = LiquidGlassBlurRadius,
): Modifier? {
    if (backdrop == null || !isRuntimeShaderSupported()) return null

    val isLightGlass = containerColor.luminance() >= 0.5f
    // 玻璃基色：向主题色轻微偏移，避免纯灰。
    val tintAlpha = if (isLightGlass) LiquidGlassTintAlphaLight else LiquidGlassTintAlphaDark
    val blendColors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(containerColor.copy(alpha = tintAlpha)),
        ),
        // 轻微提亮 + 提高对比：让背景纹理"看得出但退到后面"。
        brightness = if (isLightGlass) 1.06f else 1.12f,
        contrast = if (isLightGlass) 1.04f else 1.08f,
        saturation = if (isLightGlass) 0.92f else 0.86f,
    )
    val highlight = Highlight(
        width = LiquidGlassEdgeWidth,
        alpha = if (isLightGlass) LiquidGlassEdgeAlphaLight else LiquidGlassEdgeAlphaDark,
    )
    return this.textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = blurRadius,
        colors = blendColors,
        highlight = highlight,
    )
}

/** 供调用方判断是否具备 Liquid Glass 能力（避免重复判断散落各组件）。 */
@Composable
internal fun isLiquidGlassAvailable(backdrop: LayerBackdrop?): Boolean =
    backdrop != null && isRuntimeShaderSupported()

/** 供 Shape 参数转换使用。 */
internal fun liquidGlassShape(corner: androidx.compose.ui.unit.Dp): Shape = RoundedCornerShape(corner)

// 中等偏强的玻璃强度：模糊足够遮蔽背景细节，但不至于糊成色块。
private const val LiquidGlassBlurRadius = 26f
private const val LiquidGlassTintAlphaLight = 0.30f
private const val LiquidGlassTintAlphaDark = 0.38f
private val LiquidGlassEdgeWidth = 1.2.dp
private const val LiquidGlassEdgeAlphaLight = 0.72f
private const val LiquidGlassEdgeAlphaDark = 0.58f
