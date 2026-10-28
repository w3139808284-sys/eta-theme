package io.github.mangi.eta.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect
import top.yukonga.miuix.kmp.blur.textureBlur

/**
 * Liquid Glass 气泡材质：真实采样气泡背后的图层并模糊，可选叠加边缘折射。
 *
 * ## 实现要点
 * 1. 由调用方传入 [backdrop]（通过 `Modifier.layerBackdrop(backdrop)` 在背景层建立采样源），
 *    气泡对该图层的**当前可见像素**做实时采样。
 * 2. `BlurColors` 负责色彩透射：轻压暗 + 略提对比 + 提饱和，保留壁纸色相。
 * 3. `Highlight` 只在边缘绘制一圈柔光，制造玻璃厚度感。
 * 4. 设备不支持 RuntimeShader 或无可用 backdrop 时返回 null，由调用方降级到 GLASS 分支。
 *
 * ## 边缘折射（[refractionAmountPx] > 0 时启用）
 * 在 `blur` 之后追加 `runtimeShaderEffect`（Miuix 是 chain，追加而非替换），
 * 因此折射的是**已模糊的背景**。AGSL 内部按圆角矩形 SDF 梯度偏移采样点，
 * 是真实改变采样位置，而非叠加颜色。
 *
 * ## 坐标契约（依据 Miuix 0.9.4 字节码）
 * `BackdropEffectScopeImpl.update(DrawScope)` 中 `size = DrawScope.size`，
 * 即 **渲染实际像素尺寸**；`downscaleFactor` 只影响内部模糊层分辨率，
 * **不影响 AGSL 坐标系**。因此 shader 内 `xy` 与 `size` 同为像素，
 * 所有 dp 必须在 Kotlin 侧换算为 px。
 */
@Composable
internal fun Modifier.liquidGlassSurface(
    backdrop: LayerBackdrop?,
    shape: RoundedCornerShape,
    containerColor: Color,
    cornerRadiusPx: Float,
    blurRadius: Float = LiquidGlassBlurRadius,
    refractionAmountPx: Float = LiquidGlassRefractionAmountPx,
    refractionHeightRatio: Float = LiquidGlassRefractionHeightRatio,
): Modifier? {
    if (backdrop == null || !isRuntimeShaderSupported()) return null

    val isLightGlass = containerColor.luminance() >= 0.5f
    val tintAlpha = if (isLightGlass) LiquidGlassTintAlphaLight else LiquidGlassTintAlphaDark
    val blendColors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(containerColor.copy(alpha = tintAlpha)),
        ),
        brightness = if (isLightGlass) 0.94f else 0.90f,
        contrast = if (isLightGlass) 1.02f else 1.06f,
        saturation = if (isLightGlass) 1.12f else 1.08f,
    )
    val highlight = Highlight(
        width = LiquidGlassEdgeWidth,
        alpha = if (isLightGlass) LiquidGlassEdgeAlphaLight else LiquidGlassEdgeAlphaDark,
    )

    val enableRefraction = refractionAmountPx > 0f && refractionHeightRatio > 0f

    // 无折射需求时走原路径（与历史行为完全一致）。
    if (!enableRefraction) {
        return this.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = blendColors,
            highlight = highlight,
        )
    }

    // 需要折射：用 drawBackdrop 自行组合 blur -> runtimeShaderEffect。
    return this.drawBackdrop(
        backdrop = backdrop as Backdrop,
        shape = { shape },
        effects = {
            // 必须先执行 blur：downscaleFactor 与 padding 都在 blur() 内写入
            // （BackdropEffectsKt.blur: padding = 13 * (1 << expLo)）。
            blur(blurRadius)

            // ---- 坐标空间换算（依据 Miuix 0.9.4 字节码）----
            // 1. effectScope.size = ContentDrawScope.size = **气泡布局尺寸**（全分辨率 px，不含 padding）
            // 2. padding = **全分辨率 px**（float，13 × downscaleFactor）
            // 3. renderBlurInto 里 recordLayer 尺寸 = size + padding*2
            // 4. shader 的 coord 作用在录制层上，范围 = (size + padding*2) / downscaleFactor
            // 5. 录制层内气泡内容被 translate(+padding) 绘制，故气泡中心与录制层中心重合
            val scale = downscaleFactor.toFloat().coerceAtLeast(1f)
            val pad = padding
            // 录制层尺寸（降采样后）：用于 clamp 与 p 的原点，保留含 padding 的采样能力。
            val layerW = (size.width + pad * 2f) / scale
            val layerH = (size.height + pad * 2f) / scale
            // 气泡自身尺寸（降采样后）：用于 SDF，使 sd=0 落在真实气泡轮廓上。
            val bubbleW = size.width / scale
            val bubbleH = size.height / scale

            runtimeShaderEffect(
                key = LiquidGlassRefractionKey,
                shaderString = LIQUID_GLASS_REFRACTION_AGSL,
                uniformShaderName = "content",
            ) {
                // 录制层尺寸：p 原点基准 + clamp 上界
                setFloatUniform("size", layerW, layerH)
                // 气泡尺寸：SDF 轮廓基准
                setFloatUniform("bubbleSize", bubbleW, bubbleH)
                setFloatUniform("cornerRadius", cornerRadiusPx / scale)
                // 折射带高度改为「气泡宽度的比例」，让宽气泡也有足够厚的折射带。
                // 单位说明：bubbleW 已是降采样单位，与 SDF 的 sd / refractionHeight 同空间，
                // 因此这里不再除以 scale（否则会二次换算，带宽缩到 1/2）。
                setFloatUniform("refractionHeight", bubbleW * refractionHeightRatio)
                setFloatUniform("refractionAmount", refractionAmountPx / scale)
            }
        },
        highlight = { highlight },
    )
}

/** 供调用方判断是否具备 Liquid Glass 能力。 */
@Composable
internal fun isLiquidGlassAvailable(backdrop: LayerBackdrop?): Boolean =
    backdrop != null && isRuntimeShaderSupported()

/** 供 Shape 参数转换使用。 */
internal fun liquidGlassShape(corner: androidx.compose.ui.unit.Dp): Shape = RoundedCornerShape(corner)

// 模糊：足以糊掉壁纸细节，但仍保留大块色彩与明暗变化。
private const val LiquidGlassBlurRadius = 18f
// tint 收得很低，让壁纸颜色主导气泡内观感。
private const val LiquidGlassTintAlphaLight = 0.14f
private const val LiquidGlassTintAlphaDark = 0.20f
// 边缘高光：克制的一条细线，不做发光。
private val LiquidGlassEdgeWidth = 1.dp
private const val LiquidGlassEdgeAlphaLight = 0.55f
private const val LiquidGlassEdgeAlphaDark = 0.42f

/** AGSL 编译缓存键。 */
private const val LiquidGlassRefractionKey = "eta_liquid_glass_refraction_v1"

/**
 * 边缘折射带高度：占气泡宽度的比例。
 *
 * 早先用固定 16 全分辨率 px，在 770+ px 宽的气泡上只占 2%，
 * 实测有效折射带仅 10-12 px，视觉上不可辨。改为比例后带宽随气泡放大。
 * 0.08 ≈ 777px 气泡对应约 62 全分辨率 px（≈21dp@3x）。
 */
private const val LiquidGlassRefractionHeightRatio = 0.08f

/**
 * 边缘折射位移量（px，全分辨率）：圆角处采样点最多外移多少。
 * 32px 在 3x 屏上约 10dp，足以看出背景被弯折，又不至于撕裂。
 */
private const val LiquidGlassRefractionAmountPx = 32f
