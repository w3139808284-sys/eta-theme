package io.github.mangi.eta.data.model

import kotlinx.serialization.Serializable

const val MIN_WALLPAPER_DIM = 0f
const val MAX_WALLPAPER_DIM = 0.85f
const val DEFAULT_WALLPAPER_DIM = 0.35f

const val MIN_WALLPAPER_BLUR = 0f
const val MAX_WALLPAPER_BLUR = 40f
const val DEFAULT_WALLPAPER_BLUR = 0f

/**
 * 壁纸与背景个性化设置。
 *
 * 图片本体保存在 App 私有目录（见 WallpaperStore），这里只持久化显示参数，
 * 因此 DataStore 体积恒定，也不会把用户相册图片写进备份 JSON。
 */
@Serializable
data class WallpaperSettings(
    val enabled: Boolean = false,
    val dim: Float = DEFAULT_WALLPAPER_DIM,
    val blurRadius: Float = DEFAULT_WALLPAPER_BLUR,
    val scaleMode: WallpaperScaleMode = WallpaperScaleMode.CROP,
) {
    fun normalized(): WallpaperSettings = copy(
        dim = normalizeWallpaperDim(dim),
        blurRadius = normalizeWallpaperBlur(blurRadius),
    )
}

@Serializable
enum class WallpaperScaleMode(val persistedValue: String) {
    CROP("crop"),
    FIT("fit");

    companion object {
        fun fromPersistedValue(value: String?): WallpaperScaleMode =
            entries.firstOrNull { it.persistedValue == value } ?: CROP
    }
}

fun normalizeWallpaperDim(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_WALLPAPER_DIM, MAX_WALLPAPER_DIM)
    } else {
        DEFAULT_WALLPAPER_DIM
    }

fun normalizeWallpaperBlur(value: Float): Float =
    if (value.isFinite()) {
        value.coerceIn(MIN_WALLPAPER_BLUR, MAX_WALLPAPER_BLUR)
    } else {
        DEFAULT_WALLPAPER_BLUR
    }
