package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.model.BubbleMaterial
import io.github.mangi.eta.data.model.BubbleStyle
import io.github.mangi.eta.data.model.BubbleStyleSet
import io.github.mangi.eta.data.model.ThemeProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeProfileCodecTest {
    @Test
    fun roundTripPreservesProfiles() {
        val profiles = listOf(
            ThemeProfile(),
            ThemeProfile(
                id = "custom",
                name = "深色玻璃",
                monetEnabled = true,
                bubbles = BubbleStyleSet(
                    user = BubbleStyle(material = BubbleMaterial.GLASS, cornerRadiusDp = 24f),
                    ai = BubbleStyle(material = BubbleMaterial.GLASS, cornerRadiusDp = 16f),
                ),
            ),
        )

        val encoded = ThemeProfileCodec.encode(profiles)
        val decoded = ThemeProfileCodec.decode(encoded)

        assertNotNull(decoded)
        assertEquals(2, decoded!!.size)
        assertEquals("custom", decoded[1].id)
        assertEquals(BubbleMaterial.GLASS, decoded[1].bubbles.user.material)
        assertEquals(24f, decoded[1].bubbles.user.cornerRadiusDp)
    }

    @Test
    fun malformedInputReturnsNull() {
        assertNull(ThemeProfileCodec.decode("not json at all"))
        assertNull(ThemeProfileCodec.decode("""{"version":"one","profiles":"wrong-type"}"""))
        assertNull(ThemeProfileCodec.decode(""))
    }

    @Test
    fun emptyProfilesDecodeToEmptyList() {
        val decoded = ThemeProfileCodec.decode("""{"version":1,"profiles":[]}""")
        assertNotNull(decoded)
        assertEquals(0, decoded!!.size)
    }

    @Test
    fun defaultExportFileNameIsStable() {
        assertEquals("eta-themes.json", ThemeProfileCodec.defaultExportFileName())
    }

    /**
     * 旧版档案 JSON 不含 blurEnabled / topBarBlurStyle，
     * 解码必须成功并回落到历史默认值（true / GAUSSIAN）。
     */
    @Test
    fun legacyJsonWithoutNewFieldsDecodesWithDefaults() {
        // 枚举按 kotlinx.serialization 默认使用枚举名（DARK），非 persistedValue。
        val legacy = """
            {"version":1,"profiles":[{"id":"legacy","name":"旧主题","themeMode":"DARK"}]}
        """.trimIndent()

        val decoded = ThemeProfileCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals(1, decoded!!.size)
        assertEquals("legacy", decoded[0].id)
        assertEquals(true, decoded[0].blurEnabled)
        assertEquals(
            io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle.GAUSSIAN,
            decoded[0].topBarBlurStyle,
        )
    }

    /** 新字段必须能完整往返，不丢配置。 */
    @Test
    fun newBlurFieldsRoundTrip() {
        val profile = ThemeProfile(
            id = "glass",
            name = "玻璃",
            blurEnabled = false,
            topBarBlurStyle =
                io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle.PROGRESSIVE,
        )

        val decoded = ThemeProfileCodec.decode(ThemeProfileCodec.encode(listOf(profile)))

        assertNotNull(decoded)
        assertEquals(false, decoded!![0].blurEnabled)
        assertEquals(
            io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle.PROGRESSIVE,
            decoded[0].topBarBlurStyle,
        )
    }

    /** AI 气泡样式字段必须能往返，且默认值对齐历史渲染。 */
    @Test
    fun aiBubbleStyleRoundTripAndDefaults() {
        val defaults = ThemeProfile().bubbles.ai
        assertEquals(14f, defaults.cornerRadiusDp)
        assertEquals(14f, defaults.paddingHorizontalDp)
        assertEquals(12f, defaults.paddingVerticalDp)

        val profile = ThemeProfile(
            id = "ai-styled",
            bubbles = BubbleStyleSet(
                ai = BubbleStyle(
                    backgroundArgb = 0xFF112233.toLong(),
                    cornerRadiusDp = 28f,
                    paddingHorizontalDp = 20f,
                    paddingVerticalDp = 18f,
                    material = BubbleMaterial.GLASS,
                ),
            ),
        )

        val decoded = ThemeProfileCodec.decode(ThemeProfileCodec.encode(listOf(profile)))

        assertNotNull(decoded)
        val ai = decoded!![0].bubbles.ai
        assertEquals(0xFF112233.toLong(), ai.backgroundArgb)
        assertEquals(28f, ai.cornerRadiusDp)
        assertEquals(20f, ai.paddingHorizontalDp)
        assertEquals(18f, ai.paddingVerticalDp)
        assertEquals(BubbleMaterial.GLASS, ai.material)
    }
}
