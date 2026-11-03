package io.kiebord.feature.themedesigner

import androidx.compose.ui.graphics.Color
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.theme.ThemeIconsSpec
import io.kiebord.theme.designer.ThemeDesignerRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ThemeDesignerStateTest {

    @Test
    fun `defaultDark round-trips through KeyboardThemeSpec faithfully`() {
        val original = ThemeDesignerState.defaultDark()
            .copy(
                id = "custom_test_dark",
                name = "My Test Dark",
                strokeEnabled = true,
                strokeWidthDp = 1.5f,
                shadowEnabled = true,
                shadowElevationDp = 3.0f,
                fontPath = "fonts/custom_font.ttf",
                icons = ThemeIconsSpec(shift = "icons/shift.png", action = "icons/enter.png")
            )

        val spec = original.toSpec()
        val restored = ThemeDesignerState.fromSpec(spec)

        assertEquals("custom_test_dark", restored.id)
        assertEquals("My Test Dark", restored.name)
        assertEquals(DesignerBackgroundType.SOLID, restored.backgroundType)
        assertEquals(original.backgroundColor, restored.backgroundColor)
        assertEquals(original.keyBackground, restored.keyBackground)
        assertEquals(original.keyPressedBackground, restored.keyPressedBackground)
        assertEquals(original.keyForeground, restored.keyForeground)
        assertEquals(original.actionBackground, restored.actionBackground)
        assertEquals(original.flatKeyForeground, restored.flatKeyForeground)
        assertEquals(original.activeOptionHighlightColor, restored.activeOptionHighlightColor)
        assertEquals(true, restored.strokeEnabled)
        assertEquals(1.5f, restored.strokeWidthDp, 0.01f)
        assertEquals(true, restored.shadowEnabled)
        assertEquals(3.0f, restored.shadowElevationDp, 0.01f)
        assertEquals("fonts/custom_font.ttf", restored.fontPath)
        assertEquals("icons/shift.png", restored.icons.shift)
        assertEquals("icons/enter.png", restored.icons.action)
    }

    @Test
    fun `spaceKey and perKeyOverrides round-trip and reset cleanly`() {
        val original = ThemeDesignerState.defaultDark().copy(
            spaceCustomEnabled = true,
            spaceBackground = Color(0xFF334155),
            spaceForeground = Color(0xFFF8FAFC),
            spacePressedBackground = Color(0xFF475569),
            spaceIsPill = true,
            spaceCornerRadiusDp = 16f,
            perKeyOverrides = mapOf(
                KeyType.SHIFT.name to PerKeyColorStyle(
                    background = Color(0xFF7C3AED),
                    foreground = Color(0xFFFFFFFF)
                ),
                "q" to PerKeyColorStyle(
                    background = Color(0xFF0EA5E9),
                    foreground = Color(0xFF0F172A),
                    pressedBackground = Color(0xFF38BDF8)
                )
            )
        )

        val spec = original.toSpec()
        assertNotNull(spec.spaceKey)
        assertEquals(2, spec.perKeyOverrides.size)

        val restored = ThemeDesignerState.fromSpec(spec)
        assertTrue(restored.spaceCustomEnabled)
        assertEquals(Color(0xFF334155), restored.spaceBackground)
        assertEquals(Color(0xFFF8FAFC), restored.spaceForeground)
        assertEquals(Color(0xFF475569), restored.spacePressedBackground)
        assertTrue(restored.spaceIsPill)

        val shiftOverride = restored.perKeyOverrides[KeyType.SHIFT.name]
        assertNotNull(shiftOverride)
        assertEquals(Color(0xFF7C3AED), shiftOverride?.background)
        assertEquals(Color(0xFFFFFFFF), shiftOverride?.foreground)

        val qOverride = restored.perKeyOverrides["q"]
        assertNotNull(qOverride)
        assertEquals(Color(0xFF0EA5E9), qOverride?.background)
        assertEquals(Color(0xFF0F172A), qOverride?.foreground)
        assertEquals(Color(0xFF38BDF8), qOverride?.pressedBackground)

        // Verify section quick resets
        val afterSpaceReset = restored.resetSpaceKeySection()
        assertFalse(afterSpaceReset.spaceCustomEnabled)

        val afterPerKeyReset = restored.resetPerKeySection()
        assertTrue(afterPerKeyReset.perKeyOverrides.isEmpty())

        // Verify overrideKeyFor mapping
        assertEquals("q", ThemeDesignerState.overrideKeyFor(KeyDef(label = "Q", type = KeyType.CHARACTER)))
        assertEquals(KeyType.SHIFT.name, ThemeDesignerState.overrideKeyFor(KeyDef(label = "Shift", type = KeyType.SHIFT)))
    }

    @Test
    fun `gradient and wallpaper modes round-trip through KeyboardThemeSpec`() {
        val gradientState = ThemeDesignerState.defaultLight().copy(
            backgroundType = DesignerBackgroundType.GRADIENT,
            gradientStart = Color(0xFF1A2980),
            gradientEnd = Color(0xFF26D0CE),
            gradientAngle = 45f
        )
        val gradientSpec = gradientState.toSpec()
        val restoredGradient = ThemeDesignerState.fromSpec(gradientSpec)

        assertEquals(DesignerBackgroundType.GRADIENT, restoredGradient.backgroundType)
        assertEquals(Color(0xFF1A2980), restoredGradient.gradientStart)
        assertEquals(Color(0xFF26D0CE), restoredGradient.gradientEnd)
        assertEquals(45f, restoredGradient.gradientAngle, 0.01f)

        val imageState = ThemeDesignerState.defaultDark().copy(
            backgroundType = DesignerBackgroundType.IMAGE,
            imagePath = "images/bg_wallpaper.jpg",
            imageBlurDp = 14f,
            imageDim = 0.4f
        )
        val imageSpec = imageState.toSpec()
        val restoredImage = ThemeDesignerState.fromSpec(imageSpec)

        assertEquals(DesignerBackgroundType.IMAGE, restoredImage.backgroundType)
        assertEquals("images/bg_wallpaper.jpg", restoredImage.imagePath)
        assertEquals(14f, restoredImage.imageBlurDp, 0.01f)
        assertEquals(0.4f, restoredImage.imageDim, 0.02f)
    }

    @Test
    fun `ColorHsvUtils converts between RGB and HSV accurately`() {
        val testColors = listOf(
            Color(0xFFE53935),
            Color(0xFF26D0CE),
            Color(0xFF1B1C20),
            Color(0x805C6BC0),
            Color.White,
            Color.Black
        )

        for (color in testColors) {
            val (h, s, v) = ColorHsvUtils.colorToHsv(color)
            val reconstructed = ColorHsvUtils.hsvToColor(h, s, v, color.alpha)

            assertTrue("Red mismatch for $color", abs(color.red - reconstructed.red) < 0.02f)
            assertTrue("Green mismatch for $color", abs(color.green - reconstructed.green) < 0.02f)
            assertTrue("Blue mismatch for $color", abs(color.blue - reconstructed.blue) < 0.02f)
            assertTrue("Alpha mismatch for $color", abs(color.alpha - reconstructed.alpha) < 0.01f)
        }
    }

    @Test
    fun `ThemeDesignerRegistry discovers ThemeDesignerFeatureImpl at runtime`() {
        assertTrue(ThemeDesignerRegistry.isAvailable)
        assertNotNull(ThemeDesignerRegistry.feature)
    }
}
