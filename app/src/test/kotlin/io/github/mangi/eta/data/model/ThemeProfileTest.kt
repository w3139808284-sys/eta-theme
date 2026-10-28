package io.github.mangi.eta.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeProfileTest {
    @Test
    fun defaultsAreStable() {
        val profile = ThemeProfile()

        assertEquals(DEFAULT_THEME_PROFILE_ID, profile.id)
        assertEquals(AppearanceThemeMode.SYSTEM, profile.themeMode)
        assertFalse(profile.monetEnabled)
        assertEquals(1f, profile.interfaceScale)
        assertEquals(1f, profile.bubbleCornerScale)
        assertEquals("", profile.homeGreeting)
        assertFalse(profile.wallpaper.enabled)
        assertEquals(BubbleMaterial.SOLID, profile.bubbles.user.material)
        assertEquals(BubbleMaterial.SOLID, profile.bubbles.ai.material)
    }

    @Test
    fun normalizedClampsOutOfRangeValues() {
        val profile = ThemeProfile(
            interfaceScale = 5f,
            bubbleCornerScale = 9f,
            homeGreeting = "x".repeat(200),
            bubbles = BubbleStyleSet(
                user = BubbleStyle(cornerRadiusDp = 999f, paddingHorizontalDp = -5f),
                ai = BubbleStyle(maxWidthFraction = 3f),
            ),
            wallpaper = WallpaperSettings(dim = 9f, blurRadius = -1f),
        ).normalized()

        assertEquals(MAX_INTERFACE_SCALE, profile.interfaceScale)
        assertEquals(MAX_BUBBLE_CORNER_SCALE, profile.bubbleCornerScale)
        assertEquals(MAX_HOME_GREETING_LENGTH, profile.homeGreeting.length)
        assertEquals(MAX_BUBBLE_CORNER_RADIUS, profile.bubbles.user.cornerRadiusDp)
        assertEquals(MIN_PADDING, profile.bubbles.user.paddingHorizontalDp)
        assertEquals(MAX_MAX_WIDTH_FRACTION, profile.bubbles.ai.maxWidthFraction)
        assertEquals(MAX_WALLPAPER_DIM, profile.wallpaper.dim)
        assertEquals(MIN_WALLPAPER_BLUR, profile.wallpaper.blurRadius)
    }

    @Test
    fun profileIdIsStableAcrossNormalization() {
        val profile = ThemeProfile(id = "custom", name = "我的主题").normalized()

        assertEquals("custom", profile.id)
        assertEquals("我的主题", profile.name)
    }

    @Test
    fun bubbleMaterialParsesPersistedValue() {
        assertEquals(BubbleMaterial.SOLID, BubbleMaterial.fromPersistedValue("solid"))
        assertEquals(BubbleMaterial.GLASS, BubbleMaterial.fromPersistedValue("glass"))
        assertEquals(BubbleMaterial.SOLID, BubbleMaterial.fromPersistedValue("unknown"))
        assertEquals(BubbleMaterial.SOLID, BubbleMaterial.fromPersistedValue(null))
    }

    @Test
    fun bubbleCornerAndPaddingHelpers() {
        assertEquals(0f, normalizeBubbleCornerRadius(-3f))
        assertEquals(32f, normalizeBubbleCornerRadius(99f))
        assertEquals(20f, normalizeBubbleCornerRadius(Float.NaN))
        assertEquals(4f, normalizePadding(0f))
        assertEquals(32f, normalizePadding(999f))
        assertEquals(0.5f, normalizeMaxWidthFraction(0f))
        assertEquals(1f, normalizeMaxWidthFraction(2f))
    }

    @Test
    fun distinctProfilesAreIndependent() {
        val a = ThemeProfile(id = "a", name = "A")
        val b = ThemeProfile(id = "b", name = "B")

        assertNotEquals(a.id, b.id)
        assertEquals(a.normalized().id, "a")
    }

    @Test
    fun liquidGlassMaterialParsesPersistedValue() {
        assertEquals(BubbleMaterial.LIQUID_GLASS, BubbleMaterial.fromPersistedValue("liquid_glass"))
        assertEquals("liquid_glass", BubbleMaterial.LIQUID_GLASS.persistedValue)
    }

    @Test
    fun unknownPersistedMaterialStillFallsBackToSolid() {
        // 旧版本读到未来新增的未知材质也不能崩，回落 SOLID。
        assertEquals(BubbleMaterial.SOLID, BubbleMaterial.fromPersistedValue("holographic"))
        assertEquals(BubbleMaterial.SOLID, BubbleMaterial.fromPersistedValue(null))
    }

    @Test
    fun existingMaterialsKeepTheirPersistedValues() {
        // 历史行为不能变。
        assertEquals("solid", BubbleMaterial.SOLID.persistedValue)
        assertEquals("glass", BubbleMaterial.GLASS.persistedValue)
    }

    /**
     * 回归防护：气泡设置面板按 ordinal 取材质标签数组。
     * 若枚举条目数与 UI 标签数不一致就会 ArrayIndexOutOfBounds。
     * 这里锁定「枚举顺序稳定」这个契约，UI 侧标签数量需与之同步。
     */
    @Test
    fun materialOrdinalsAreStableAndContiguous() {
        assertEquals(0, BubbleMaterial.SOLID.ordinal)
        assertEquals(1, BubbleMaterial.GLASS.ordinal)
        assertEquals(2, BubbleMaterial.LIQUID_GLASS.ordinal)
        assertEquals(3, BubbleMaterial.entries.size)
    }
}
