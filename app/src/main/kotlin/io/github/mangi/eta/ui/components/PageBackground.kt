package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mangi.eta.ui.app.ChatWallpaperBackdrop
import io.github.mangi.eta.ui.app.LocalWallpaper
import io.github.mangi.eta.ui.app.LocalWallpaperBitmap
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面级背景：壁纸启用时铺「不透明图片 + 遮罩」，否则使用主题底色。
 *
 * 必须作为页面 Scaffold 内容的第一层、且在 Scaffold 容器为透明时使用，
 * 保证壁纸随页面一起切换，绝不残留到其它路由（避免旧页穿透）。
 */
@Composable
fun PageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val wallpaper = LocalWallpaper.current
    val bitmap = LocalWallpaperBitmap.current
    Box(modifier = modifier.fillMaxSize()) {
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
        content()
    }
}
