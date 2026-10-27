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
}
