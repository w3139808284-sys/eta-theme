package io.github.mangi.eta.ui.components

/**
 * Liquid Glass 边缘折射 AGSL（Android Graphics Shading Language）。
 *
 * ## 坐标契约（依据 Miuix 0.9.4 字节码确认）
 * - `BackdropEffectScopeImpl.update(DrawScope)` 里 `size = DrawScope.size`，
 *   即 **渲染时的实际像素尺寸**；`downscaleFactor` 只影响内部模糊层分辨率，
 *   **不影响传给 AGSL 的坐标系**。
 * - 因此 `main(float2 xy)` 的 `xy` 与 `uniform float2 size` **同为像素单位**，
 *   原点在玻璃左上角。
 * - 所有 dp 值必须由 Kotlin 侧 `dp.toPx()` 换算后传入，shader 内不再做单位换算。
 *
 * ## 效果说明
 * 只在边缘带（距轮廓 < refractionHeight px）内沿着 **SDF 梯度方向**偏移采样，
 * 内部区域原样返回。这不是叠加颜色，而是实际改变采样位置，因此能看到背景被"弯折"。
 *
 * ## 边缘法线
 * 用圆角矩形 SDF 的解析梯度，而不是 `normalize(p/halfSize)`：
 * 后者在直边中段会给出斜向法线，导致边缘出现斜向拖影。
 *
 * 基于标准圆角矩形 SDF 推导，参考自 PrismalAGSL（MIT）与 kyant backdrop（Apache-2.0）
 * 的公开实现思路，此处为独立重写。
 */
internal const val LIQUID_GLASS_REFRACTION_AGSL = """
uniform shader content;
// size       = 录制层尺寸（= 气泡尺寸 + 2×padding），降采样后。用于 p 的原点与 clamp 上界。
uniform float2 size;
// bubbleSize = 气泡自身尺寸，降采样后。用于 SDF，使 sd=0 落在真实气泡轮廓上。
uniform float2 bubbleSize;
uniform float cornerRadius;
uniform float refractionHeight;
uniform float refractionAmount;

// 圆角矩形 SDF：内部为负，外部为正。
float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - halfSize + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// SDF 解析梯度（指向外侧的单位法线）。
// 直边段返回轴向法线，圆角段返回径向法线，不会出现斜向拖影。
float2 sdRoundRectGrad(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - halfSize + float2(r);
    float2 sg = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    if (q.x > 0.0 && q.y > 0.0) {
        return sg * normalize(q);           // 圆角区：径向
    }
    if (q.x > q.y) {
        return float2(sg.x, 0.0);           // 左右直边：水平
    }
    return float2(0.0, sg.y);               // 上下直边：垂直
}

half4 main(float2 xy) {
    // p 以「录制层中心」为原点。因为气泡内容被 translate(+padding) 居中绘制在录制层内，
    // 且 padding 四周对称，所以气泡中心与录制层中心重合，该原点对两者通用。
    float2 layerHalf = size * 0.5;
    float2 p = xy - layerHalf;

    // SDF 用「气泡半尺寸」，而不是录制层半尺寸：
    // 录制层比气泡外扩 padding，若用 layerHalf 则 sd=0 会落在录制层边界（气泡外），
    // 导致边缘带整体被推出气泡轮廓、并被 setClip 裁掉。
    float2 bubbleHalf = bubbleSize * 0.5;
    float r = min(cornerRadius, min(bubbleHalf.x, bubbleHalf.y));
    float sd = sdRoundRect(p, bubbleHalf, r);

    // 远离边缘：完全原样，保证正文区域不受影响。
    if (-sd >= refractionHeight) {
        return content.eval(xy);
    }

    // 边缘带权重：轮廓处为 1，向内 refractionHeight 处为 0。
    float t = clamp(1.0 - (-sd) / max(refractionHeight, 0.001), 0.0, 1.0);
    // 圆滑过渡（arc-lift），避免线性过渡产生可见的折线。
    float lift = 1.0 - sqrt(max(1.0 - t * t, 0.0));

    float2 n = sdRoundRectGrad(p, bubbleHalf, r);
    // 向外偏移采样：玻璃"吸入"外侧背景，形成透镜边缘。
    float2 samplePos = xy + n * (lift * refractionAmount);

    // 关键：把采样点夹取到合法范围，而不是越界就退回原坐标。
    // 边缘带本就贴着轮廓，向外偏移极易越界；若越界即回退，边缘带会有大量像素
    // 退化成零位移，使折射"看起来没生效"（已用 D2 涂绿测试实测确认）。
    // 夹取能保证位移真实发生，同时不产生黑边。
    samplePos = clamp(samplePos, float2(0.0), size);
    return content.eval(samplePos);
}
"""
