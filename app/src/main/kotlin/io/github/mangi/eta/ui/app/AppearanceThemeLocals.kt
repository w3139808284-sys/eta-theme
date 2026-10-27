package io.github.mangi.eta.ui.app

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle
import io.github.mangi.eta.data.model.BubbleStyleSet
import io.github.mangi.eta.data.model.WallpaperSettings

internal val LocalAppearanceSettings = staticCompositionLocalOf { AppearanceSettings() }
internal val LocalBlurEnabled = staticCompositionLocalOf { true }
internal val LocalTopBarBlurStyle = staticCompositionLocalOf { AppearanceTopBarBlurStyle.GAUSSIAN }
internal val LocalPlatformDensity = staticCompositionLocalOf<Density?> { null }

/** 用户/AI 气泡独立样式（来自激活主题档案）。 */
internal val LocalBubbleStyles = staticCompositionLocalOf { BubbleStyleSet() }

/** 当前生效的壁纸设置；null 表示该页面不显示壁纸。 */
internal val LocalWallpaper = staticCompositionLocalOf<WallpaperSettings?> { null }

/** 壁纸是否已开启并已成功解码；页面容器据此改用透明底。 */
internal val LocalWallpaperActive = staticCompositionLocalOf { false }

/** 已解码的壁纸位图；null 表示壁纸未开启或尚未解码完成。 */
internal val LocalWallpaperBitmap = staticCompositionLocalOf<ImageBitmap?> { null }
