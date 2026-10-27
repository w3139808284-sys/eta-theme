package io.github.mangi.eta.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WallpaperSettingsTest {
    @Test
    fun defaultsKeepWallpaperOff() {
        val settings = WallpaperSettings()

        assertFalse(settings.enabled)
        assertEquals(DEFAULT_WALLPAPER_DIM, settings.dim)
        assertEquals(DEFAULT_WALLPAPER_BLUR, settings.blurRadius)
        assertEquals(WallpaperScaleMode.CROP, settings.scaleMode)
    }

    @Test
    fun dimAndBlurAreClampedAndNonFiniteFallsBack() {
        assertEquals(MIN_WALLPAPER_DIM, normalizeWallpaperDim(-1f))
        assertEquals(MAX_WALLPAPER_DIM, normalizeWallpaperDim(4f))
        assertEquals(DEFAULT_WALLPAPER_DIM, normalizeWallpaperDim(Float.NaN))

        assertEquals(MIN_WALLPAPER_BLUR, normalizeWallpaperBlur(-8f))
        assertEquals(MAX_WALLPAPER_BLUR, normalizeWallpaperBlur(1_000f))
        assertEquals(DEFAULT_WALLPAPER_BLUR, normalizeWallpaperBlur(Float.POSITIVE_INFINITY))
    }

    @Test
    fun scaleModeFallsBackForUnknownPersistedValue() {
        assertEquals(WallpaperScaleMode.CROP, WallpaperScaleMode.fromPersistedValue(null))
        assertEquals(WallpaperScaleMode.CROP, WallpaperScaleMode.fromPersistedValue("stretch"))
        assertEquals(WallpaperScaleMode.FIT, WallpaperScaleMode.fromPersistedValue("fit"))
    }

    @Test
    fun normalizedAppliesClamping() {
        val normalized = WallpaperSettings(dim = 9f, blurRadius = -3f).normalized()

        assertEquals(MAX_WALLPAPER_DIM, normalized.dim)
        assertEquals(MIN_WALLPAPER_BLUR, normalized.blurRadius)
    }

    @Test
    fun bubbleCornerScaleIsClampedAndNonFiniteFallsBack() {
        assertEquals(MIN_BUBBLE_CORNER_SCALE, normalizeBubbleCornerScale(-2f))
        assertEquals(MAX_BUBBLE_CORNER_SCALE, normalizeBubbleCornerScale(9f))
        assertEquals(DEFAULT_BUBBLE_CORNER_SCALE, normalizeBubbleCornerScale(Float.NaN))
        assertEquals(DEFAULT_BUBBLE_CORNER_SCALE, AppearanceSettings().bubbleCornerScale)
    }

    @Test
    fun homeGreetingIsTrimmedAndTruncated() {
        assertEquals("", normalizeHomeGreeting("   "))
        assertEquals("hello world", normalizeHomeGreeting("  hello\nworld  "))
        assertEquals(
            MAX_HOME_GREETING_LENGTH,
            normalizeHomeGreeting("x".repeat(MAX_HOME_GREETING_LENGTH + 20)).length,
        )
        assertEquals("", AppearanceSettings().homeGreeting)
    }

    @Test
    fun appearanceNormalizationCarriesWallpaper() {
        val settings = AppearanceSettings(
            bubbleCornerScale = 12f,
            wallpaper = WallpaperSettings(dim = 4f, blurRadius = -2f),
        ).normalized()

        assertEquals(MAX_BUBBLE_CORNER_SCALE, settings.bubbleCornerScale)
        assertEquals(MAX_WALLPAPER_DIM, settings.wallpaper.dim)
        assertEquals(MIN_WALLPAPER_BLUR, settings.wallpaper.blurRadius)
    }
}
