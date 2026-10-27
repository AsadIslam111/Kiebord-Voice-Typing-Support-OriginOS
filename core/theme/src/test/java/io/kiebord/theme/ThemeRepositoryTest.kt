package io.kiebord.theme

import androidx.compose.ui.graphics.Color
import io.kiebord.theme.ThemeColorUtils.isColorDark
import io.kiebord.theme.ThemeColorUtils.parseHexColor
import io.kiebord.theme.ThemeColorUtils.toCustomThemeDef
import io.kiebord.theme.ThemeColorUtils.toKeyboardColorScheme
import io.kiebord.theme.ThemeColorUtils.toKeyboardThemeSpec
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testParseHexColorStandardFormats() {
        val red6 = parseHexColor("#FF0000")
        assertEquals(1.0f, red6.red, 0.01f)
        assertEquals(0.0f, red6.green, 0.01f)
        assertEquals(0.0f, red6.blue, 0.01f)
        assertEquals(1.0f, red6.alpha, 0.01f)

        val blue8 = parseHexColor("#800000FF")
        assertEquals(0.0f, blue8.red, 0.01f)
        assertEquals(0.0f, blue8.green, 0.01f)
        assertEquals(1.0f, blue8.blue, 0.01f)
        assertEquals(0.5f, blue8.alpha, 0.02f)

        val white3 = parseHexColor("#FFF")
        assertEquals(1.0f, white3.red, 0.01f)
        assertEquals(1.0f, white3.green, 0.01f)
        assertEquals(1.0f, white3.blue, 0.01f)

        val semiWhite = parseHexColor("#8FFF")
        assertEquals(1.0f, semiWhite.red, 0.01f)
        assertEquals(0.53f, semiWhite.alpha, 0.05f)

        val fallback = parseHexColor("invalid", Color.Magenta)
        assertEquals(Color.Magenta, fallback)
    }

    @Test
    fun testIsColorDark() {
        assertTrue(isColorDark(Color(0xFF000000)))
        assertTrue(isColorDark(Color(0xFF1B1B1F)))
        assertTrue(isColorDark(Color(0xFF101028)))
        assertFalse(isColorDark(Color(0xFFFFFFFF)))
        assertFalse(isColorDark(Color(0xFFF1F3F4)))
        assertFalse(isColorDark(Color(0xFFD2E0C3)))
    }

    @Test
    fun testBornoColorThemeConversion() {
        val bornoItem = BornoColorTheme(
            id = 1011,
            name = "AMOLED Purity",
            backgroundColor = "#000000",
            keyColor = "#282828",
            labelKeyColor = "#FFFFFF",
            functionalKeyColor = "#181818",
            labelFunctionalKeyColor = "#EEEEEE",
            enterColor = "#FDFE03",
            enterColorIcon = "#000000",
            selectedKeyColor = "#404040",
            keyStrokeColor = "#505050",
            keyStrokeSize = 1
        )

        val customDef = bornoItem.toCustomThemeDef()
        assertEquals("borno_1011", customDef.id)
        assertEquals("AMOLED Purity", customDef.name)
        assertTrue(customDef.isDark)
        assertEquals("#505050", customDef.keyBorder)
        assertEquals("#FDFE03", customDef.accentColor)

        val scheme = customDef.toKeyboardColorScheme()
        assertEquals(0.0f, scheme.containerBackground.red, 0.01f)
        assertEquals(0.0f, scheme.containerBackground.green, 0.01f)
        assertEquals(0.0f, scheme.containerBackground.blue, 0.01f)
    }

    @Test
    fun testParseBornoBundleJson() {
        val sampleBundle = """
            {
              "themes": [
                {
                  "colors": [
                    {
                      "id": 1001,
                      "name": "Material Light",
                      "backgroundColor": "#ffebeff2",
                      "enterColor": "#ff67c3b8",
                      "functionalKeyColor": "#ffdfe1e3",
                      "keyColor": "#ffffffff",
                      "labelFunctionalKeyColor": "#B3000000",
                      "labelKeyColor": "#ff000000"
                    },
                    {
                      "id": 1012,
                      "name": "Cosmic Nebula",
                      "backgroundColor": "#101028",
                      "enterColor": "#800080",
                      "functionalKeyColor": "#202040",
                      "keyColor": "#202040",
                      "labelFunctionalKeyColor": "#E0B0FF",
                      "labelKeyColor": "#E0B0FF"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<BornoThemeFile>(sampleBundle)
        assertEquals(1, parsed.themes.size)
        assertEquals(2, parsed.themes[0].colors.size)

        val first = parsed.themes[0].colors[0]
        assertEquals(1001, first.id)
        assertEquals("Material Light", first.name)

        val second = parsed.themes[0].colors[1]
        assertEquals(1012, second.id)
        assertEquals("Cosmic Nebula", second.name)
    }

    @Test
    fun testParseSingleBornoThemeJson() {
        val singleJson = """
            {
              "id": 1015,
              "name": "Forest Whisper",
              "backgroundColor": "#384231",
              "enterColor": "#87743E",
              "functionalKeyColor": "#6E8458",
              "keyColor": "#546E45",
              "labelFunctionalKeyColor": "#D7DBB9",
              "labelKeyColor": "#D7DBB9"
            }
        """.trimIndent()

        val item = json.decodeFromString<BornoColorTheme>(singleJson)
        assertEquals("Forest Whisper", item.name)
        val def = item.toCustomThemeDef()
        assertEquals("borno_1015", def.id)
        val scheme = def.toKeyboardColorScheme()
        assertNotNull(scheme)
    }

    @Test
    fun testRichThemeSpecSerialization() {
        val themeJson = """
            {
              "id": "neon_cyberpunk",
              "name": "Neon Cyberpunk",
              "author": "Kiebord Team",
              "version": 1,
              "isDark": true,
              "background": {
                "gradient": {
                  "type": "linear",
                  "colors": ["#0F0C29", "#302B63", "#24243E"],
                  "angle": 45.0
                }
              },
              "characterKey": {
                "background": "#801A1B2F",
                "foreground": "#E0E6ED",
                "stroke": {
                  "color": "#40FFFFFF",
                  "widthDp": 1.0,
                  "alpha": 0.5
                },
                "shadow": {
                  "color": "#80000000",
                  "elevationDp": 2.0
                },
                "cornerRadiusDp": 10.0
              },
              "functionalKey": {
                "background": "#80121324",
                "foreground": "#A8C7FA"
              },
              "actionKey": {
                "background": "#FF007F",
                "foreground": "#FFFFFF",
                "isPill": true
              },
              "accentColor": "#FF007F",
              "icons": {
                "shift": "ic_custom_shift",
                "backspace": "ic_custom_delete",
                "action": "ic_custom_return"
              },
              "perKeyOverrides": {
                "3_4": {
                  "background": "#FF007F",
                  "foreground": "#FFFFFF"
                },
                "SPACE": {
                  "cornerRadiusDp": 16.0
                }
              }
            }
        """.trimIndent()

        val spec = json.decodeFromString<KeyboardThemeSpec>(themeJson)
        assertEquals("neon_cyberpunk", spec.id)
        assertEquals("Neon Cyberpunk", spec.name)
        assertTrue(spec.isDark)
        assertNotNull(spec.background.gradient)
        assertEquals(GradientType.LINEAR, spec.background.gradient?.type)
        assertEquals(3, spec.background.gradient?.colors?.size)
        assertEquals(45f, spec.background.gradient?.angle)

        assertEquals("#801A1B2F", spec.characterKey.background)
        assertEquals(1.0f, spec.characterKey.stroke?.widthDp)
        assertEquals(10f, spec.characterKey.cornerRadiusDp)

        assertEquals("ic_custom_shift", spec.icons.shift)
        assertEquals("ic_custom_delete", spec.icons.backspace)

        assertEquals(2, spec.perKeyOverrides.size)
        assertNotNull(spec.perKeyOverrides["3_4"])
        assertEquals("#FF007F", spec.perKeyOverrides["3_4"]?.background)

        val scheme = spec.toKeyboardColorScheme()
        assertNotNull(scheme.themeSpec)
        assertEquals(spec, scheme.themeSpec)
    }

    @Test
    fun testGradientBrushGeneration() {
        val linear = GradientSpec(
            type = GradientType.LINEAR,
            colors = listOf("#FF0000", "#00FF00"),
            angle = 90f
        )
        val linearBrush = linear.toBrush(androidx.compose.ui.geometry.Size(1000f, 600f))
        assertNotNull(linearBrush)

        val radial = GradientSpec(
            type = GradientType.RADIAL,
            colors = listOf("#FFFFFF", "#000000"),
            centerX = 0.5f,
            centerY = 0.5f,
            radius = 0.8f
        )
        val radialBrush = radial.toBrush(androidx.compose.ui.geometry.Size(1000f, 600f))
        assertNotNull(radialBrush)

        val sweep = GradientSpec(
            type = GradientType.SWEEP,
            colors = listOf("#FF0000", "#00FF00", "#0000FF", "#FF0000")
        )
        val sweepBrush = sweep.toBrush(androidx.compose.ui.geometry.Size(1000f, 600f))
        assertNotNull(sweepBrush)

        val mesh = GradientSpec(
            type = GradientType.MESH,
            meshPoints = listOf(
                MeshPointSpec(0.0f, 0.0f, "#FF0055"),
                MeshPointSpec(1.0f, 0.0f, "#00FFCC"),
                MeshPointSpec(0.0f, 1.0f, "#7700FF"),
                MeshPointSpec(1.0f, 1.0f, "#FFAA00")
            )
        )
        val meshBrush = mesh.toBrush(androidx.compose.ui.geometry.Size(1000f, 600f))
        assertNotNull(meshBrush)
    }

    @Test
    fun testPerKeyResolutionPrecedence() {
        val spec = KeyboardThemeSpec(
            id = "test_spec",
            name = "Test",
            background = ThemeBackgroundSpec(solidColor = "#000000"),
            characterKey = KeycapStyleSpec(background = "#111111", foreground = "#FFFFFF"),
            functionalKey = KeycapStyleSpec(background = "#222222", foreground = "#FFFFFF"),
            actionKey = KeycapStyleSpec(background = "#333333", foreground = "#FFFFFF"),
            perKeyOverrides = mapOf(
                "SPACE" to KeycapStyleSpec(background = "#444444"),
                "3_4" to KeycapStyleSpec(background = "#555555"),
                "," to KeycapStyleSpec(background = "#666666")
            )
        )

        val keySpaceAt34 = io.kiebord.common.model.KeyDef(label = "Space", type = io.kiebord.common.model.KeyType.SPACE)
        val resolved34 = spec.resolveKeycapStyle(rowIndex = 3, keyIndex = 4, key = keySpaceAt34)
        assertEquals("#555555", resolved34.background)

        val resolvedOtherSpace = spec.resolveKeycapStyle(rowIndex = 2, keyIndex = 1, key = keySpaceAt34)
        assertEquals("#444444", resolvedOtherSpace.background)

        val keyComma = io.kiebord.common.model.KeyDef(label = ",", type = io.kiebord.common.model.KeyType.CHARACTER)
        val resolvedComma = spec.resolveKeycapStyle(rowIndex = 2, keyIndex = 0, key = keyComma)
        assertEquals("#666666", resolvedComma.background)

        val keyA = io.kiebord.common.model.KeyDef(label = "a", type = io.kiebord.common.model.KeyType.CHARACTER)
        val resolvedA = spec.resolveKeycapStyle(rowIndex = 1, keyIndex = 0, key = keyA)
        assertEquals("#111111", resolvedA.background)
    }

    @Test
    fun testImageBackgroundSpec() {
        val imgJson = """
            {
              "imagePath": "themes/images/aurora.jpg",
              "scaleType": "crop",
              "alpha": 0.85,
              "dimOverlayColor": "#66000000",
              "blurRadiusDp": 12.0
            }
        """.trimIndent()

        val imgSpec = json.decodeFromString<ImageBackgroundSpec>(imgJson)
        assertEquals("themes/images/aurora.jpg", imgSpec.imagePath)
        assertEquals(ImageScaleType.CROP, imgSpec.scaleType)
        assertEquals(0.85f, imgSpec.alpha, 0.01f)
        assertEquals("#66000000", imgSpec.dimOverlayColor)
        assertEquals(12.0f, imgSpec.blurRadiusDp, 0.01f)
    }

    @Test
    fun testEnhancedBornoThemesBundle() {
        val file = java.io.File("src/main/assets/themes/borno_themes.json")
        if (file.exists()) {
            val content = file.readText(Charsets.UTF_8)
            val bundle = json.decodeFromString<BornoThemeFile>(content)
            val themes = bundle.themes.flatMap { it.colors }

            assertEquals(54, themes.size)

            for (theme in themes) {
                assertTrue("Theme ${theme.id} name is empty", theme.name.isNotBlank())
                assertEquals("Theme ${theme.name} must have 8.0 keyRadius", 8.0f, theme.keyRadius, 0.01f)
                assertTrue("Theme ${theme.name} must have keyStrokeSize >= 1", theme.keyStrokeSize >= 1)
                assertTrue("Theme ${theme.name} must have keyShadowSize >= 1", theme.keyShadowSize >= 1)

                val spec = theme.toKeyboardThemeSpec()
                assertNotNull(spec)
                assertEquals(theme.name, spec.name)
                assertEquals(null, spec.characterKey.cornerRadiusDp)
            }

            val cosmic = themes.first { it.id == 1012 }
            assertEquals("Cosmic Nebula", cosmic.name)
            assertEquals(2, cosmic.backgroundType)
            val cosmicSpec = cosmic.toKeyboardThemeSpec()
            assertEquals(GradientType.LINEAR, cosmicSpec.background.gradient?.type)
            assertEquals(2, cosmicSpec.background.gradient?.colors?.size)
        }
    }
}

