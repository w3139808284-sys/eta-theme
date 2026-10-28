package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.mangi.eta.ui.app.ChatWallpaperBackdrop
import io.github.mangi.eta.ui.app.LocalWallpaper
import io.github.mangi.eta.ui.app.LocalWallpaperBitmap
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 供 Liquid Glass 气泡采样的**背景层** backdrop。
 *
 * 关键区别：这里只捕获「壁纸 / 主题底色」这一层，**不包含消息与气泡自身**，
 * 因此气泡用 textureBlur 采样时得到的是它背后真实的背景像素，
 * 不会出现自采样导致的模糊套模糊。
 */
internal val LocalBackgroundBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 页面级背景：壁纸启用时铺「不透明图片 + 遮罩」，否则使用主题底色。
 *
 * 必须作为页面 Scaffold 内容的第一层、且在 Scaffold 容器为透明时使用，
 * 保证壁纸随页面一起切换，绝不残留到其它路由（避免旧页穿透）。
 *
 * 同时在此建立 [LocalBackgroundBackdrop]：背景层被记录为可采样图层，
 * 供上层 Liquid Glass 气泡读取真实背景像素。
 */
@Composable
fun PageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val wallpaper = LocalWallpaper.current
    val bitmap = LocalWallpaperBitmap.current
    val canSample = isRuntimeShaderSupported()
    val fallbackColor = MiuixTheme.colorScheme.background
    // 背景层自身的绘制内容被记录，供气泡采样；与消息区 backdrop 完全分离。
    val backgroundBackdrop = rememberLayerBackdrop {
        drawRect(fallbackColor)
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (canSample) Modifier.layerBackdrop(backgroundBackdrop) else Modifier),
        ) {
            if (wallpaper != null && bitmap != null) {
                ChatWallpaperBackdrop(
                    wallpaper = wallpaper,
                    bitmap = bitmap,
                    surfaceColor = MiuixTheme.colorScheme.surface,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.background),
                )
            }
        }
        CompositionLocalProvider(
            LocalBackgroundBackdrop provides backgroundBackdrop.takeIf { canSample },
        ) {
            content()
        }
    }
}
