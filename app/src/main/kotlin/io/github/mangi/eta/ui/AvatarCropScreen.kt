package io.github.mangi.eta.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 头像裁剪页：拖动 + 双指缩放 + 居中方形取景框，确认后输出正方形位图。
 *
 * 纯 Compose 手势实现，不引入第三方裁剪库。取景框固定为正方形，
 * 保证圆形/圆角方形头像裁剪后都不变形。
 *
 * 变换模型：底图先按 cover 铺满取景框，再叠加用户缩放 [scale] 与平移 [offset]（像素）。
 */
@Composable
internal fun AvatarCropScreen(
    source: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val imageBitmap = remember(source) { source.asImageBitmap() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // 布局期确定的取景框边长与左下角，供手势与裁剪共用。
    var viewportSize by remember { mutableStateOf(0f) }
    var viewportOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset += pan
                    }
                },
        ) {
            val viewport = min(size.width, size.height) * 0.78f
            val left = (size.width - viewport) / 2f
            val top = (size.height - viewport) / 2f
            if (viewportSize != viewport || viewportOrigin != Offset(left, top)) {
                viewportSize = viewport
                viewportOrigin = Offset(left, top)
            }

            // cover 基准缩放：保证取景框内始终被图片填满。
            val baseScale = max(viewport / imageBitmap.width, viewport / imageBitmap.height)
            val drawW = imageBitmap.width * baseScale * scale
            val drawH = imageBitmap.height * baseScale * scale
            val dx = left + (viewport - drawW) / 2f + offset.x
            val dy = top + (viewport - drawH) / 2f + offset.y

            drawImage(
                image = imageBitmap,
                srcSize = IntSize(imageBitmap.width, imageBitmap.height),
                dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                filterQuality = FilterQuality.High,
            )

            // 取景框外压暗（EvenOdd 挖洞）。
            val maskApi = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addRect(Rect(left, top, left + viewport, top + viewport))
                fillType = PathFillType.EvenOdd
            }
            drawPath(maskApi, Color.Black.copy(alpha = 0.62f))

            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(viewport, viewport),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "裁剪头像",
                color = Color.White,
                style = MiuixTheme.textStyles.title3,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                CropActionButton(
                    text = "取消",
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onCancel,
                )
                Spacer(Modifier.width(12.dp))
                CropActionButton(
                    text = "确定",
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val viewport = viewportSize
                        if (viewport <= 0f) return@CropActionButton
                        cropToSquare(
                            source = source,
                            viewport = viewport,
                            scale = scale,
                            offset = offset,
                        )?.let(onConfirm)
                    },
                )
            }
        }
    }
}

@Composable
private fun CropActionButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = if (primary) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                },
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSurface
            },
            style = MiuixTheme.textStyles.body1,
        )
    }
}

/**
 * 按当前视图变换把取景框区域裁成正方形位图。
 *
 * 与 Canvas 中的绘制保持同一套换算：cover 基准 × 用户 scale，再叠加 offset 平移，
 * 反解出取景框中心在源图中的位置后取正方形区域。
 */
private fun cropToSquare(
    source: Bitmap,
    viewport: Float,
    scale: Float,
    offset: Offset,
): Bitmap? = runCatching {
    val baseScale = max(viewport / source.width, viewport / source.height)
    val effectiveScale = baseScale * scale
    if (effectiveScale <= 0f) return@runCatching null

    // 取景框在源图上的等效边长。
    val cropSide = (viewport / effectiveScale).roundToInt()
        .coerceIn(1, min(source.width, source.height))

    // 底图绘制原点在取景框坐标系中的偏移为 (viewport-drawW)/2 + offset，
    // 反解取景框中心映射到源图的坐标。
    val drawW = source.width * effectiveScale
    val drawH = source.height * effectiveScale
    val imageLeftInViewport = (viewport - drawW) / 2f + offset.x
    val imageTopInViewport = (viewport - drawH) / 2f + offset.y

    val centerXInSource = (viewport / 2f - imageLeftInViewport) / effectiveScale
    val centerYInSource = (viewport / 2f - imageTopInViewport) / effectiveScale

    val x = (centerXInSource - cropSide / 2f).roundToInt().coerceAtLeast(0)
    val y = (centerYInSource - cropSide / 2f).roundToInt().coerceAtLeast(0)
    val w = cropSide.coerceAtMost(source.width - x).coerceAtLeast(1)
    val h = cropSide.coerceAtMost(source.height - y).coerceAtLeast(1)
    val side = min(w, h).coerceAtLeast(1)

    Bitmap.createBitmap(source, x, y, side, side)
}.getOrNull()
