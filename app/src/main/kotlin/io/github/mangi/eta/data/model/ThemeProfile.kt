package io.github.mangi.eta.data.model

import kotlinx.serialization.Serializable

/**
 * 气泡材质类型：决定用户/AI 气泡的渲染方式。
 *
 * - SOLID：纯色气泡（最稳，任何设备可用）
 * - GLASS：轻量玻璃拟态（半透明 + 细描边 + 柔和阴影），无需特殊硬件
 */
@Serializable
enum class BubbleMaterial(val persistedValue: String) {
    SOLID("solid"),
    GLASS("glass");

    companion object {
        fun fromPersistedValue(value: String?): BubbleMaterial =
            entries.firstOrNull { it.persistedValue == value } ?: SOLID
    }
}

/**
 * 单个角色的聊天气泡样式。
 *
 * 用户气泡与 AI 气泡各持有一份独立配置，互不影响。
 */
@Serializable
data class BubbleStyle(
    /** 气泡填充色；null 表示使用主题 surfaceContainerHigh（用户）/ surface（AI）。 */
    val backgroundArgb: Long? = null,
    /** 气泡文字色；null 表示使用主题 onSurface。 */
    val textArgb: Long? = null,
    /** 圆角（dp），0 为直角。 */
    val cornerRadiusDp: Float = 20f,
    /** 水平内边距（dp）。 */
    val paddingHorizontalDp: Float = 16f,
    /** 垂直内边距（dp）。 */
    val paddingVerticalDp: Float = 12f,
    /** 材质类型。 */
    val material: BubbleMaterial = BubbleMaterial.SOLID,
    /** 气泡最大宽度占屏幕宽度的比例（0.5~1.0）。 */
    val maxWidthFraction: Float = 0.82f,
) {
    fun normalized(): BubbleStyle = copy(
        cornerRadiusDp = normalizeBubbleCornerRadius(cornerRadiusDp),
        paddingHorizontalDp = normalizePadding(paddingHorizontalDp),
        paddingVerticalDp = normalizePadding(paddingVerticalDp),
        maxWidthFraction = normalizeMaxWidthFraction(maxWidthFraction),
    )
}

/** 一组气泡样式：用户 + AI 独立。 */
@Serializable
data class BubbleStyleSet(
    val user: BubbleStyle = BubbleStyle(),
    val ai: BubbleStyle = BubbleStyle(cornerRadiusDp = 20f, maxWidthFraction = 0.9f),
) {
    fun normalized(): BubbleStyleSet = copy(user = user.normalized(), ai = ai.normalized())
}

/**
 * 一个完整主题档案：可创建、编辑、切换、持久化。
 *
 * 与业务解耦：档案只描述外观，不含任何会话/模型/工具状态。
 * 官方升级时只需保证默认档案与旧字段兼容，业务代码零改动。
 */
@Serializable
data class ThemeProfile(
    val id: String = DEFAULT_THEME_PROFILE_ID,
    val name: String = "默认主题",
    /** 主题模式（跟随系统/浅色/深色）。 */
    val themeMode: AppearanceThemeMode = AppearanceThemeMode.SYSTEM,
    /** Monet 动态取色。 */
    val monetEnabled: Boolean = false,
    /** 调色板风格。 */
    val paletteStyle: AppearancePaletteStyle = AppearancePaletteStyle.TONAL_SPOT,
    /** 强调色。 */
    val accentColor: AppearanceAccentColor = AppearanceAccentColor.SYSTEM,
    /** 深色模式下使用纯黑背景。 */
    val pureBlackEnabled: Boolean = false,
    /** 全局界面缩放（0.8~1.1）。 */
    val interfaceScale: Float = DEFAULT_INTERFACE_SCALE,
    /** 气泡圆角倍率（作用于气泡圆角的全局系数）。 */
    val bubbleCornerScale: Float = 1f,
    /** 首页空对话问候语；留空使用内置文案。 */
    val homeGreeting: String = "",
    /** 气泡样式（用户/AI 独立）。 */
    val bubbles: BubbleStyleSet = BubbleStyleSet(),
    /** 双方头像设置。 */
    val avatars: AvatarSettings = AvatarSettings(),
    /** 壁纸/背景配置。 */
    val wallpaper: WallpaperSettings = WallpaperSettings(),
) {
    fun normalized(): ThemeProfile = copy(
        themeMode = themeMode,
        interfaceScale = normalizeInterfaceScale(interfaceScale),
        bubbleCornerScale = normalizeBubbleCornerScale(bubbleCornerScale),
        homeGreeting = normalizeHomeGreeting(homeGreeting),
        bubbles = bubbles.normalized(),
        avatars = avatars.normalized(),
        wallpaper = wallpaper.normalized(),
    )
}

const val DEFAULT_THEME_PROFILE_ID = "default"
const val DEFAULT_THEME_PROFILE_NAME = "默认主题"

const val MIN_BUBBLE_CORNER_RADIUS = 0f
const val MAX_BUBBLE_CORNER_RADIUS = 32f

const val MIN_PADDING = 4f
const val MAX_PADDING = 32f

const val MIN_MAX_WIDTH_FRACTION = 0.5f
const val MAX_MAX_WIDTH_FRACTION = 1f

fun normalizeBubbleCornerRadius(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_BUBBLE_CORNER_RADIUS, MAX_BUBBLE_CORNER_RADIUS)
    } else {
        20f
    }

fun normalizePadding(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_PADDING, MAX_PADDING)
    } else {
        16f
    }

fun normalizeMaxWidthFraction(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_MAX_WIDTH_FRACTION, MAX_MAX_WIDTH_FRACTION)
    } else {
        0.82f
    }
