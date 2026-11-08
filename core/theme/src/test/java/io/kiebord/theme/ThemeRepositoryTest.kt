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
import org.junit.Assert.assertNull
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

            assertEquals(42, themes.size)
            assertEquals((1..42).toList(), themes.map { it.id })

            var zeroStrokeCount = 0
            var withStrokeCount = 0
            var gradientCount = 0

            for (theme in themes) {
                assertTrue("Theme ${theme.id} name is empty", theme.name.isNotBlank())
                assertEquals("Theme ${theme.name} must have 8.0 keyRadius", 8.0f, theme.keyRadius, 0.01f)

                val spec = theme.toKeyboardThemeSpec()
                assertNotNull(spec)
                assertEquals(theme.name, spec.name)
                assertEquals(null, spec.characterKey.cornerRadiusDp)

                if (theme.backgroundType == 2) {
                    gradientCount++
                    assertNotNull("Gradient theme ${theme.name} must have gradient spec", spec.background.gradient)
                }

                if (theme.keyStrokeSize == 0) {
                    zeroStrokeCount++
                    assertEquals("Zero-stroke theme ${theme.name} must have null stroke spec", null, spec.characterKey.stroke)
                } else {
                    withStrokeCount++
                    assertNotNull("Stroke theme ${theme.name} must have stroke spec", spec.characterKey.stroke)
                    assertEquals(theme.keyStrokeSize.toFloat(), spec.characterKey.stroke?.widthDp ?: 0f, 0.01f)
                }

                assertTrue("Theme ${theme.name} must have valid backgroundColor", theme.backgroundColor.startsWith("#"))
                assertTrue("Theme ${theme.name} must have valid enterColor", theme.enterColor.startsWith("#"))
                assertTrue("Theme ${theme.name} must have valid gradStart", theme.gradStart.startsWith("#") && theme.gradStart != "#ffffffff")
                assertTrue("Theme ${theme.name} must have valid pressedKeyColor", theme.pressedKeyColor.startsWith("#"))
                assertTrue("Theme ${theme.name} must have valid stripColor", theme.stripColor.startsWith("#"))
                assertEquals(8, theme.popupCorner)

                val scheme = spec.toKeyboardColorScheme()
                val toolbarBg = if (scheme.effectiveToolbarBackground.alpha < 0.05f) scheme.containerBackground else scheme.effectiveToolbarBackground
                val toolbarContrast = ThemeColorUtils.contrastRatio(scheme.effectiveToolbarForeground, toolbarBg)
                assertTrue("Theme ${theme.name} toolbar contrast ($toolbarContrast) must be >= 2.8", toolbarContrast >= 2.8f)

                val hoverLabelColor = ThemeColorUtils.ensureContrast(scheme.actionKeyForeground, scheme.accentColor, minContrast = 3.0f)
                val hoverContrast = ThemeColorUtils.contrastRatio(hoverLabelColor, scheme.accentColor)
                assertTrue("Theme ${theme.name} morekeys hover label contrast ($hoverContrast) must be >= 3.0", hoverContrast >= 3.0f)

                val popupLabelColor = ThemeColorUtils.ensureContrast(scheme.keyForeground, scheme.moreKeysBackground, minContrast = 3.0f)
                val popupContrast = ThemeColorUtils.contrastRatio(popupLabelColor, scheme.moreKeysBackground)
                assertTrue("Theme ${theme.name} morekeys popup label contrast ($popupContrast) must be >= 3.0", popupContrast >= 3.0f)
            }

            assertEquals("Exactly 41 themes must have 0 stroke", 41, zeroStrokeCount)
            assertEquals("Exactly 1 theme must have stroke > 0", 1, withStrokeCount)
            assertEquals("Exactly 12 gradient themes must be present", 12, gradientCount)

            val aurora = themes.first { it.id == 5 }
            assertEquals("Aurora Glass", aurora.name)
            assertEquals(2, aurora.backgroundType)
            assertEquals(0, aurora.keyStrokeSize)
            val auroraSpec = aurora.toKeyboardThemeSpec()
            assertNotNull(auroraSpec.background.gradient)
            assertEquals(GradientType.LINEAR, auroraSpec.background.gradient?.type)
            assertEquals(null, auroraSpec.characterKey.stroke)
            val auroraKeyColor = ThemeColorUtils.parseHexColor(aurora.keyColor)
            assertTrue("Aurora Glass keyColor must be translucent", auroraKeyColor.alpha < 0.5f)
            val auroraScheme = auroraSpec.toKeyboardColorScheme()
            assertEquals(0f, auroraScheme.effectiveToolbarBackground.alpha, 0.01f)
            val flatCharVisual = auroraScheme.resolveKeycapVisual(
                key = io.kiebord.common.model.KeyDef(label = "a", type = io.kiebord.common.model.KeyType.CHARACTER),
                showKeyBorders = false
            )
            assertEquals(0f, flatCharVisual.backgroundColor.alpha, 0.01f)
            assertEquals(0f, flatCharVisual.strokeColor.alpha, 0.01f)

            val nebula = themes.first { it.id == 11 }
            assertEquals("Nebula Core", nebula.name)
            assertEquals("radial", nebula.gradType)
            assertEquals(GradientType.RADIAL, nebula.toKeyboardThemeSpec().background.gradient?.type)

            val matLight = themes.first { it.id == 13 }
            assertEquals("Pure Chalk", matLight.name)
            assertEquals(0, matLight.keyStrokeSize)
            assertEquals(null, matLight.toKeyboardThemeSpec().characterKey.stroke)

            val amoled = themes.first { it.id == 42 }
            assertEquals("Cyber AMOLED", amoled.name)
            assertEquals(1, amoled.keyStrokeSize)
            assertEquals("#ff505050", amoled.keyStrokeColor)
            assertEquals(1.0f, amoled.toKeyboardThemeSpec().characterKey.stroke?.widthDp ?: 0f, 0.01f)

            val globeKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.SWITCH_LANGUAGE, icon = "globe")
            val lightGlobeFlat = KeyboardColorScheme.DefaultLight.resolveKeycapVisual(key = globeKey, showKeyBorders = false)
            assertEquals(KeyboardColorScheme.DefaultLight.containerBackground, lightGlobeFlat.backgroundColor)
            val darkGlobeFlat = KeyboardColorScheme.DefaultDark.resolveKeycapVisual(key = globeKey, showKeyBorders = false)
            assertEquals(KeyboardColorScheme.DefaultDark.containerBackground, darkGlobeFlat.backgroundColor)
        }
    }

    @Test
    fun testBornoExportedThemeDirectJson() {
        val bornothemeJson = """
            {
              "schemaVersion": 1,
              "builder": "100",
              "name": "Borno Sunset Orange",
              "backgroundType": 4,
              "backgroundPath": "",
              "backgroundColor": "#1A1A1A",
              "backgroundOpacity": 255,
              "keyColor": "#2D2D2D",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#222222",
              "labelFunctionalKeyColor": "#E0E0E0",
              "enterColor": "#FF5722",
              "enterIconColor": "#FFFFFF",
              "selectedKeyColor": "#FF5722",
              "spaceColor": "#2D2D2D",
              "keyStrokeColor": "#3A3A3A",
              "keyStrokeSize": 1,
              "keyRadius": 12.0,
              "isFlat": false,
              "isModern": true
            }
        """.trimIndent()

        val theme = json.decodeFromString<BornoExportedTheme>(bornothemeJson)
        assertEquals("Borno Sunset Orange", theme.name)
        assertEquals(4, theme.backgroundType)
        assertEquals("#FF5722", theme.enterColor)
        assertEquals("#FFFFFF", theme.enterIconColor)

        val spec = with(ThemeColorUtils) { theme.toKeyboardThemeSpec() }
        assertEquals("Borno Sunset Orange", spec.name)
        assertTrue(spec.isDark)
        assertEquals("#2D2D2D", spec.characterKey.background)
        assertEquals("#FFFFFF", spec.characterKey.foreground)
        assertEquals(12.0f, spec.characterKey.cornerRadiusDp)
        assertEquals("#FF5722", spec.actionKey.background)
        assertEquals("#FFFFFF", spec.actionKey.foreground)
        assertEquals("#3A3A3A", spec.characterKey.stroke?.color)
        assertEquals(1.0f, spec.characterKey.stroke?.widthDp ?: 0f, 0.01f)
    }

    @Test
    fun testBornoExportedThemeBase64String() {
        val rawJson = """
            {
              "schemaVersion": 1,
              "name": "Base64 Borno Theme",
              "backgroundType": 1,
              "backgroundColor": "#000000",
              "backgroundOpacity": 255,
              "keyColor": "#1C1C1E",
              "labelKeyColor": "#F2F2F7",
              "functionalKeyColor": "#2C2C2E",
              "labelFunctionalKeyColor": "#AEAEB2",
              "enterColor": "#0A84FF"
            }
        """.trimIndent()

        val base64 = java.util.Base64.getEncoder().encodeToString(rawJson.toByteArray(Charsets.UTF_8))
        val decoded = ThemeColorUtils.decodeBase64ToStringSafe(base64)
        assertNotNull(decoded)
        assertTrue(decoded!!.contains("Base64 Borno Theme"))

        val theme = json.decodeFromString<BornoExportedTheme>(decoded)
        assertEquals("Base64 Borno Theme", theme.name)
        assertEquals("#0A84FF", theme.enterColor)
    }

    @Test
    fun testBornoExportedThemeWithImageBackground() {
        val imageThemeJson = """
            {
              "schemaVersion": 1,
              "name": "Scenic Mountain",
              "backgroundType": 3,
              "backgroundPath": "/data/user/0/io.kiebord/files/custom_themes/mountain.jpg",
              "backgroundOpacity": 204,
              "backgroundColor": "#121212",
              "keyColor": "#80202020",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#80151515",
              "labelFunctionalKeyColor": "#E0E0E0",
              "enterColor": "#4CAF50"
            }
        """.trimIndent()

        val theme = json.decodeFromString<BornoExportedTheme>(imageThemeJson)
        val spec = with(ThemeColorUtils) { theme.toKeyboardThemeSpec() }
        assertNotNull(spec.background.image)
        assertEquals("/data/user/0/io.kiebord/files/custom_themes/mountain.jpg", spec.background.image?.imagePath)
        assertEquals(204 / 255f, spec.background.image?.alpha ?: 0f, 0.01f)
        assertNotNull(spec.background.image?.dimOverlayColor)
    }

    @Test
    fun testBornoExportedThemeWithGradientBackground() {
        val gradientThemeJson = """
            {
              "schemaVersion": 1,
              "name": "Sunset Gradient",
              "backgroundType": 2,
              "gradStart": "#FF8A00",
              "gradEnd": "#E52E71",
              "gradAngle": 45.0,
              "gradType": "linear",
              "backgroundColor": "#000000",
              "keyColor": "#30FFFFFF",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#20FFFFFF",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#E52E71"
            }
        """.trimIndent()

        val theme = json.decodeFromString<BornoExportedTheme>(gradientThemeJson)
        val spec = with(ThemeColorUtils) { theme.toKeyboardThemeSpec() }
        assertNotNull(spec.background.gradient)
        assertEquals(GradientType.LINEAR, spec.background.gradient?.type)
        assertEquals(listOf("#FF8A00", "#E52E71"), spec.background.gradient?.colors)
        assertEquals(45.0f, spec.background.gradient?.angle ?: 0f, 0.01f)
    }

    @Test
    fun testBornoExportedThemeMissingHoverStateFixed() {
        // Theme with missing pressedKeyColor (null)
        val missingHoverJson = """
            {
              "schemaVersion": 1,
              "name": "No Hover State Theme",
              "backgroundType": 1,
              "backgroundColor": "#000000",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF"
            }
        """.trimIndent()

        val theme = json.decodeFromString<BornoExportedTheme>(missingHoverJson)
        val spec = with(ThemeColorUtils) { theme.toKeyboardThemeSpec() }

        // Character key must have a distinct hover/pressed state generated
        assertNotNull(spec.characterKey.pressedBackground)
        val pressedColor = ThemeColorUtils.parseHexColor(spec.characterKey.pressedBackground!!)
        val baseColor = ThemeColorUtils.parseHexColor(spec.characterKey.background!!)
        assertTrue("Pressed color must be distinct from base key color", pressedColor != baseColor)
        // Since key is dark, hover state should be lighter
        assertTrue("Dark key hover state should be lightened", pressedColor.red > baseColor.red)

        // Action key (Enter) should also have a distinct hover/pressed state
        assertNotNull(spec.actionKey.pressedBackground)
        val enterPressed = ThemeColorUtils.parseHexColor(spec.actionKey.pressedBackground!!)
        val enterBase = ThemeColorUtils.parseHexColor(spec.actionKey.background!!)
        assertTrue("Enter pressed color must be distinct from enter background", enterPressed != enterBase)
    }

    @Test
    fun testBornoExportedThemePreservesAuthoredLabelColors() {
        // Theme with authored white labels on dark keys: must preserve white labels without turning them black
        val themeJson = """
            {
              "schemaVersion": 1,
              "name": "Faithful Contrast Theme",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#151515",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#202020",
              "labelFunctionalKeyColor": "#E0E0E0",
              "enterColor": "#242424",
              "enterIconColor": "#FFFFFF"
            }
        """.trimIndent()

        val theme = json.decodeFromString<BornoExportedTheme>(themeJson)
        val spec = with(ThemeColorUtils) { theme.toKeyboardThemeSpec() }

        assertEquals("#FFFFFF", spec.characterKey.foreground)
        assertEquals("#E0E0E0", spec.functionalKey.foreground)
        assertEquals("#FFFFFF", spec.actionKey.foreground)
    }

    @Test
    fun testBornoExportedThemeStrokeOpacityFixed() {
        // Theme with keyStrokeSize > 0 but stroke color opacity was 0 (#00000000)
        val zeroOpacityStrokeJson = """
            {
              "schemaVersion": 1,
              "name": "Zero Opacity Stroke",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#222222",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#333333",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF",
              "keyStrokeSize": 1,
              "keyStrokeColor": "#00000000"
            }
        """.trimIndent()

        val theme1 = json.decodeFromString<BornoExportedTheme>(zeroOpacityStrokeJson)
        val spec1 = with(ThemeColorUtils) { theme1.toKeyboardThemeSpec() }
        assertNotNull(spec1.characterKey.stroke)
        assertTrue("Stroke alpha must be restored to visible level", spec1.characterKey.stroke!!.alpha >= 0.15f)

        // Theme with 100% opaque stroke on dark keys (#FFFFFFFF)
        val harshOpaqueStrokeJson = """
            {
              "schemaVersion": 1,
              "name": "Harsh Opaque Stroke",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#222222",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#333333",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF",
              "keyStrokeSize": 1,
              "keyStrokeColor": "#FFFFFFFF"
            }
        """.trimIndent()

        val theme2 = json.decodeFromString<BornoExportedTheme>(harshOpaqueStrokeJson)
        val spec2 = with(ThemeColorUtils) { theme2.toKeyboardThemeSpec() }
        assertNotNull(spec2.characterKey.stroke)
        assertTrue("Opaque stroke alpha must be softened to modern translucent keycap border", spec2.characterKey.stroke!!.alpha <= 0.35f)
    }

    @Test
    fun testBornoExportedThemeRetainsKeyShadow() {
        // 1. Theme with default keyShadowColor (#00000000) and keyShadowSize (0) but isFlat = false
        val defaultShadowJson = """
            {
              "schemaVersion": 1,
              "name": "Default Shadow Borno Theme",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF",
              "keyShadowColor": "#00000000",
              "keyShadowSize": 0,
              "isFlat": false
            }
        """.trimIndent()

        val theme1 = json.decodeFromString<BornoExportedTheme>(defaultShadowJson)
        val spec1 = with(ThemeColorUtils) { theme1.toKeyboardThemeSpec() }
        assertNotNull("Character key must retain shadow for non-flat custom theme", spec1.characterKey.shadow)
        assertNotNull("Action key must retain shadow for non-flat custom theme", spec1.actionKey.shadow)
        assertEquals("#4D000000", spec1.characterKey.shadow?.color)

        val def1 = with(ThemeColorUtils) { theme1.toCustomThemeDef() }
        assertEquals("#4D000000", def1.keyShadow)
        val scheme1 = with(ThemeColorUtils) { def1.toKeyboardColorScheme() }
        assertTrue("Scheme keyShadow must have visible alpha", scheme1.keyShadow.alpha > 0.1f)

        // 2. Theme with explicit custom shadow
        val explicitShadowJson = """
            {
              "schemaVersion": 1,
              "name": "Explicit Shadow Theme",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF",
              "keyShadowColor": "#33000000",
              "keyShadowSize": 2,
              "isFlat": false
            }
        """.trimIndent()

        val theme2 = json.decodeFromString<BornoExportedTheme>(explicitShadowJson)
        val spec2 = with(ThemeColorUtils) { theme2.toKeyboardThemeSpec() }
        assertNotNull(spec2.characterKey.shadow)
        assertEquals("#33000000", spec2.characterKey.shadow?.color)
        assertEquals(2.0f, spec2.characterKey.shadow?.elevationDp ?: 0f, 0.01f)

        // 3. Explicitly flat theme must have no shadow
        val flatJson = """
            {
              "schemaVersion": 1,
              "name": "Flat Theme",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF",
              "isFlat": true
            }
        """.trimIndent()

        val theme3 = json.decodeFromString<BornoExportedTheme>(flatJson)
        val spec3 = with(ThemeColorUtils) { theme3.toKeyboardThemeSpec() }
        assertNull("Flat theme must have null shadow", spec3.characterKey.shadow)
    }

    @Test
    fun testCustomThemeDefRetainsKeyShadow() {
        val customDef = CustomThemeDef(
            id = "custom_test",
            name = "Custom Def Test",
            isDark = true,
            containerBackground = "#121212",
            keyBackground = "#1E1E1E",
            keyForeground = "#FFFFFF",
            keyShadow = "#00000000",
            actionKeyBackground = "#2C2C2C",
            actionKeyForeground = "#FFFFFF",
            shiftActiveColor = "#007AFF",
            accentColor = "#007AFF"
        )

        val spec = with(ThemeColorUtils) { customDef.toKeyboardThemeSpec() }
        assertNotNull("Spec character key must retain shadow for tiled keys", spec.characterKey.shadow)
        assertNotNull("Spec action key must retain shadow for tiled keys", spec.actionKey.shadow)

        val scheme = with(ThemeColorUtils) { customDef.toKeyboardColorScheme() }
        assertTrue("Scheme keyShadow must have visible alpha", scheme.keyShadow.alpha > 0.1f)
    }

    @Test
    fun testBornoExportedThemeToolbarTransparentOnImageAndGradientBackdrop() {
        val imageThemeJson = """
            {
              "schemaVersion": 1,
              "name": "Image Toolbar Test",
              "backgroundType": 3,
              "backgroundPath": "/test/path/img.jpg",
              "backgroundColor": "#121212",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF",
              "stripColor": "#00000000",
              "stripIcoColor": "#E0E0E0"
            }
        """.trimIndent()

        val imageTheme = json.decodeFromString<BornoExportedTheme>(imageThemeJson)
        val spec = with(ThemeColorUtils) { imageTheme.toKeyboardThemeSpec() }
        val scheme = spec.toKeyboardColorScheme()
        assertEquals(0f, scheme.effectiveToolbarBackground.alpha, 0.01f)
        assertEquals(ThemeColorUtils.parseHexColor("#E0E0E0"), scheme.effectiveToolbarForeground)

        val gradientThemeJson = """
            {
              "schemaVersion": 1,
              "name": "Gradient Toolbar Test",
              "backgroundType": 2,
              "gradStart": "#101028",
              "gradEnd": "#202050",
              "backgroundColor": "#101028",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF"
            }
        """.trimIndent()

        val gradTheme = json.decodeFromString<BornoExportedTheme>(gradientThemeJson)
        val gradSpec = with(ThemeColorUtils) { gradTheme.toKeyboardThemeSpec() }
        val gradScheme = gradSpec.toKeyboardColorScheme()
        assertEquals(0f, gradScheme.effectiveToolbarBackground.alpha, 0.01f)
    }

    @Test
    fun testBornoExportedThemeMoreKeysFullOpacityOnImageBackdrop() {
        val semiTransparentPopupThemeJson = """
            {
              "schemaVersion": 1,
              "name": "Image MoreKeys Opacity Test",
              "backgroundType": 3,
              "backgroundPath": "/storage/emulated/0/Download/custom_bg.png",
              "backgroundColor": "#121212",
              "keyColor": "#801E1E1E",
              "popupColor": "#80000000",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF"
            }
        """.trimIndent()

        val theme = json.decodeFromString<BornoExportedTheme>(semiTransparentPopupThemeJson)
        val spec = with(ThemeColorUtils) { theme.toKeyboardThemeSpec() }
        val scheme = spec.toKeyboardColorScheme()

        // Opacity should be 1.0f (full opacity) on image theme to prevent overlap with background
        assertEquals(1.0f, scheme.moreKeysBackground.alpha, 0.001f)
        assertEquals(1.0f, scheme.effectiveMoreKeysBackground.alpha, 0.001f)
        assertEquals("#FF000000", spec.moreKeysBackground)

        val customDef = with(ThemeColorUtils) { theme.toCustomThemeDef() }
        assertEquals("#FF000000", customDef.moreKeysBackground)

        // When popupColor is fully transparent (e.g. #00000000), it should fallback to full opacity
        val transparentPopupThemeJson = """
            {
              "schemaVersion": 1,
              "name": "Image Transparent MoreKeys Test",
              "backgroundType": 3,
              "backgroundPath": "/storage/emulated/0/Download/custom_bg.png",
              "backgroundColor": "#121212",
              "keyColor": "#00000000",
              "popupColor": "#00000000",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#007AFF"
            }
        """.trimIndent()

        val transTheme = json.decodeFromString<BornoExportedTheme>(transparentPopupThemeJson)
        val transSpec = with(ThemeColorUtils) { transTheme.toKeyboardThemeSpec() }
        val transScheme = transSpec.toKeyboardColorScheme()
        assertEquals(1.0f, transScheme.moreKeysBackground.alpha, 0.001f)
        assertEquals(1.0f, transScheme.effectiveMoreKeysBackground.alpha, 0.001f)
    }

    @Test
    fun testBornoThemeV1VsV2Identification() {
        // V1 Format (.bornotheme schemaVersion = 1)
        val v1Json = """
            {
              "schemaVersion": 1,
              "name": "Legacy Borno Theme",
              "backgroundType": 1,
              "backgroundColor": "#121212",
              "keyColor": "#1E1E1E",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#2C2C2C",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#FF5722"
            }
        """.trimIndent()

        val v1Theme: BornoThemeV1 = json.decodeFromString(v1Json)
        assertEquals(1, v1Theme.schemaVersion)
        assertEquals("Legacy Borno Theme", v1Theme.name)
        val v1Spec = with(ThemeColorUtils) { v1Theme.toKeyboardThemeSpec() }
        assertEquals("#FF5722", v1Spec.accentColor)

        // V2 Format (.bornotheme schemaVersion = 2 retaining native features)
        val v2Json = """
            {
              "schemaVersion": 2,
              "id": "custom_cyber_v2",
              "name": "Cyberpunk 2077 v2",
              "isDark": true,
              "background": {
                "gradient": {
                  "type": "mesh",
                  "meshPoints": [
                    { "x": 0.0, "y": 0.0, "color": "#FF0F0C29" },
                    { "x": 1.0, "y": 1.0, "color": "#FF24243E" }
                  ]
                }
              },
              "characterKey": {
                "background": "#1A1B2F",
                "foreground": "#E0E7FF",
                "stroke": {
                  "color": "#00FFF5",
                  "widthDp": 0.75,
                  "alpha": 0.5
                },
                "shadow": {
                  "color": "#4D000000",
                  "elevationDp": 2.0
                },
                "cornerRadiusDp": 10.0
              },
              "functionalKey": {
                "background": "#141527",
                "foreground": "#00FFF5"
              },
              "actionKey": {
                "background": "#FF007F",
                "foreground": "#FFFFFF",
                "isPill": true
              },
              "accentColor": "#FF007F",
              "perKeyOverrides": {
                "space": {
                  "background": "#202040",
                  "cornerRadiusDp": 20.0
                }
              }
            }
        """.trimIndent()

        val v2Theme: BornoThemeV2 = json.decodeFromString(v2Json)
        assertEquals(2, v2Theme.schemaVersion)
        assertEquals("Cyberpunk 2077 v2", v2Theme.name)
        assertEquals(GradientType.MESH, v2Theme.background.gradient?.type)
        assertEquals(2, v2Theme.background.gradient?.meshPoints?.size)
        assertEquals(0.75f, v2Theme.characterKey.stroke?.widthDp ?: 0f, 0.01f)
        assertEquals(2.0f, v2Theme.characterKey.shadow?.elevationDp ?: 0f, 0.01f)
        assertTrue(v2Theme.perKeyOverrides.containsKey("space"))
        assertEquals(20.0f, v2Theme.perKeyOverrides["space"]?.cornerRadiusDp ?: 0f, 0.01f)

        // Verify conversion to CustomThemeDef retains fallback representation
        val v2Def = v2Theme.toCustomThemeDef()
        assertEquals("Cyberpunk 2077 v2", v2Def.name)
        assertEquals("#FF007F", v2Def.accentColor)
    }

    @Test
    fun testBornoThemeV2ExportAndEncoding() {
        val spec = KeyboardThemeSpec(
            schemaVersion = 2,
            id = "custom_test_v2",
            name = "Test Export Theme",
            isDark = true,
            background = ThemeBackgroundSpec(solidColor = "#1B1B1F"),
            characterKey = KeycapStyleSpec(
                background = "#2C2C30",
                foreground = "#FFFFFF"
            ),
            accentColor = "#2196F3"
        )

        val exportedJson = json.encodeToString(KeyboardThemeSpec.serializer(), spec)
        assertTrue(exportedJson.contains("\"schemaVersion\": 2") || exportedJson.contains("\"schemaVersion\":2"))
        assertTrue(exportedJson.contains("Test Export Theme"))

        // Test base64 encoding & decoding roundtrip
        val encodedB64 = ThemeColorUtils.encodeBase64Safe(exportedJson)
        val decodedJson = ThemeColorUtils.decodeBase64ToStringSafe(encodedB64)
        assertNotNull(decodedJson)
        val parsedBack = json.decodeFromString<KeyboardThemeSpec>(decodedJson!!)
        assertEquals(2, parsedBack.schemaVersion)
        assertEquals("Test Export Theme", parsedBack.name)
        assertEquals("#2196F3", parsedBack.accentColor)
    }

    @Test
    fun testFlatKeyLabelColorAndActiveOptionHighlightInV2() {
        val themeJson = """
            {
              "schemaVersion": 2,
              "id": "flat_test_v2",
              "name": "Flat Test Theme",
              "background": {
                "solidColor": "#FFFFFF"
              },
              "characterKey": {
                "background": "#000000",
                "foreground": "#FFFFFF"
              },
              "accentColor": "#FF5722",
              "flatKeyLabelColor": "#121212",
              "activeOptionHighlightColor": "#00E5FF"
            }
        """.trimIndent()

        val spec = json.decodeFromString<KeyboardThemeSpec>(themeJson)
        assertEquals("#121212", spec.flatKeyLabelColor)
        assertEquals("#00E5FF", spec.activeOptionHighlightColor)

        val scheme = spec.toKeyboardColorScheme()
        assertEquals(0x12 / 255f, scheme.flatKeyForeground.red, 0.02f)
        assertEquals(0x00 / 255f, scheme.effectiveActiveOptionHighlight.red, 0.02f)
        assertEquals(0xE5 / 255f, scheme.effectiveActiveOptionHighlight.green, 0.02f)
        assertEquals(0xFF / 255f, scheme.effectiveActiveOptionHighlight.blue, 0.02f)

        // When showKeyBorders = true -> foreground is standard characterKey.foreground (#FFFFFF)
        val charKeyDef = io.kiebord.common.model.KeyDef(label = "a")
        val borderedVisual = scheme.resolveKeycapVisual(key = charKeyDef, showKeyBorders = true)
        assertEquals(1.0f, borderedVisual.foregroundColor.red, 0.01f)

        // When showKeyBorders = false -> foreground is flatKeyForeground (#121212)
        val flatVisual = scheme.resolveKeycapVisual(key = charKeyDef, showKeyBorders = false)
        assertEquals(0x12 / 255f, flatVisual.foregroundColor.red, 0.02f)
    }

    @Test
    fun testFlatKeyLabelColorAndActiveOptionHighlightInV1() {
        val v1Json = """
            {
              "schemaVersion": 1,
              "name": "V1 Flat Test",
              "backgroundColor": "#FFFFFF",
              "keyColor": "#000000",
              "labelKeyColor": "#FFFFFF",
              "functionalKeyColor": "#222222",
              "labelFunctionalKeyColor": "#FFFFFF",
              "enterColor": "#FF5722",
              "flatKeyLabelColor": "#333333",
              "activeOptionHighlightColor": "#4CAF50"
            }
        """.trimIndent()

        val v1Theme = json.decodeFromString<BornoExportedTheme>(v1Json)
        assertEquals("#333333", v1Theme.flatKeyLabelColor)
        assertEquals("#4CAF50", v1Theme.activeOptionHighlightColor)

        val customDef = with(ThemeColorUtils) { v1Theme.toCustomThemeDef() }
        assertEquals("#333333", customDef.flatKeyForeground)
        assertEquals("#4CAF50", customDef.activeOptionHighlightColor)

        val scheme = with(ThemeColorUtils) { customDef.toKeyboardColorScheme() }
        assertEquals(0x33 / 255f, scheme.flatKeyForeground.red, 0.02f)
        assertEquals(0x4C / 255f, scheme.effectiveActiveOptionHighlight.red, 0.02f)
    }

    @Test
    fun testAssetsSrcBornoThemeFilesParsing() {
        val lightFile = java.io.File("../../assets-src/light.bornotheme").takeIf { it.exists() }
            ?: java.io.File("../../assets-src/themes/light.bornotheme")
        val darkFile = java.io.File("../../assets-src/dark.bornotheme").takeIf { it.exists() }
            ?: java.io.File("../../assets-src/themes/dark.bornotheme")
        val gradientBlurFile = java.io.File("../../assets-src/gradient_blur.bornotheme").takeIf { it.exists() }
            ?: java.io.File("../../assets-src/themes/gradient_blur.bornotheme")

        if (lightFile.exists()) {
            val lightSpec = json.decodeFromString<KeyboardThemeSpec>(lightFile.readText())
            assertEquals(2, lightSpec.schemaVersion)
            assertEquals("Polar Light", lightSpec.name)
            assertFalse(lightSpec.isDark)
            assertNotNull(lightSpec.flatKeyLabelColor)
            assertNotNull(lightSpec.activeOptionHighlightColor)
            val lightScheme = lightSpec.toKeyboardColorScheme()
            assertNotNull(lightScheme)
        }

        if (darkFile.exists()) {
            val darkSpec = json.decodeFromString<KeyboardThemeSpec>(darkFile.readText())
            assertEquals(2, darkSpec.schemaVersion)
            assertEquals("Obsidian Dark", darkSpec.name)
            assertTrue(darkSpec.isDark)
            assertNotNull(darkSpec.flatKeyLabelColor)
            assertNotNull(darkSpec.activeOptionHighlightColor)
            val darkScheme = darkSpec.toKeyboardColorScheme()
            assertNotNull(darkScheme)
        }

        if (gradientBlurFile.exists()) {
            val gradSpec = json.decodeFromString<KeyboardThemeSpec>(gradientBlurFile.readText())
            assertEquals(2, gradSpec.schemaVersion)
            assertEquals("Aurora Blur", gradSpec.name)
            assertTrue(gradSpec.isDark)
            assertNotNull(gradSpec.background.gradient)
            assertNotNull(gradSpec.flatKeyLabelColor)
            assertNotNull(gradSpec.activeOptionHighlightColor)
            val gradScheme = gradSpec.toKeyboardColorScheme()
            assertNotNull(gradScheme)
        }
    }

    @Test
    fun testAdjustContrast() {
        val darkBg = Color(0xFF121212)
        val highContrastColor = Color(0xFF6366F1)
        val result1 = ThemeColorUtils.adjustContrast(highContrastColor, darkBg, minContrast = 2.8f)
        assertEquals(highContrastColor, result1)

        val lowContrastDarkColor = Color(0xFF1E1E24)
        val result2 = ThemeColorUtils.adjustContrast(lowContrastDarkColor, darkBg, minContrast = 2.8f)
        assertTrue(ThemeColorUtils.contrastRatio(result2, darkBg) >= 2.8f)

        val lightBg = Color(0xFFFFFFFF)
        val lowContrastLightColor = Color(0xFFFFF9C4)
        val result3 = ThemeColorUtils.adjustContrast(lowContrastLightColor, lightBg, minContrast = 2.8f)
        assertTrue(ThemeColorUtils.contrastRatio(result3, lightBg) >= 2.8f)

        val unspecified = Color.Unspecified
        val result4 = ThemeColorUtils.adjustContrast(unspecified, darkBg, minContrast = 2.8f)
        assertEquals(Color.White, result4)
    }

    @Test
    fun testAdaptiveThemeCategoryAndConstants() {
        assertEquals("Adaptive", ThemeCategory.ADAPTIVE.title)
        assertEquals("dynamic_adaptive", DynamicKeyboardTheme.THEME_ADAPTIVE_ID)
        assertEquals("dynamic_adaptive_stroked", DynamicKeyboardTheme.THEME_ADAPTIVE_STROKED_ID)
        assertTrue(DynamicKeyboardTheme.isAdaptiveTheme("dynamic_adaptive"))
        assertTrue(DynamicKeyboardTheme.isAdaptiveTheme("dynamic_adaptive_light_keys"))
        assertTrue(DynamicKeyboardTheme.isAdaptiveTheme("dynamic_adaptive_stroked"))
        assertFalse(DynamicKeyboardTheme.isAdaptiveTheme("default_dark"))
    }

    @Test
    fun testModifierKeyForegroundResolution() {
        val scheme = KeyboardColorScheme.DefaultDark.copy(
            modifierKeyForeground = Color(0xFF123456)
        )
        val shiftKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.SHIFT)
        val shiftVisual = scheme.resolveKeycapVisual(key = shiftKey, showKeyBorders = true)
        assertEquals(Color(0xFF123456), shiftVisual.foregroundColor)

        val commaKey = io.kiebord.common.model.KeyDef(label = ",", type = io.kiebord.common.model.KeyType.CHARACTER)
        val commaVisual = scheme.resolveKeycapVisual(key = commaKey, showKeyBorders = true)
        assertEquals(Color(0xFF123456), commaVisual.foregroundColor)

        val periodKey = io.kiebord.common.model.KeyDef(label = ".", type = io.kiebord.common.model.KeyType.CHARACTER)
        val periodVisual = scheme.resolveKeycapVisual(key = periodKey, showKeyBorders = true)
        assertEquals(Color(0xFF123456), periodVisual.foregroundColor)

        val letterKey = io.kiebord.common.model.KeyDef(label = "a", type = io.kiebord.common.model.KeyType.CHARACTER)
        val letterVisual = scheme.resolveKeycapVisual(key = letterKey, showKeyBorders = true)
        assertEquals(scheme.keyForeground, letterVisual.foregroundColor)
    }

    @Test
    fun testAdaptiveActionKeyResolution() {
        val adaptiveScheme = KeyboardColorScheme.DefaultDark.copy(
            actionKeyBackground = Color(0xFF333333),
            accentColor = Color(0xFF0000FF),
            isAdaptive = true
        )
        val actionKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.ACTION)
        val actionVisual = adaptiveScheme.resolveKeycapVisual(key = actionKey, showKeyBorders = true)
        assertEquals(Color(0xFF333333), actionVisual.backgroundColor)

        val nonAdaptiveScheme = KeyboardColorScheme.DefaultDark.copy(
            actionKeyBackground = Color(0xFF333333),
            accentColor = Color(0xFF0000FF),
            isAdaptive = false
        )
        val nonAdaptiveVisual = nonAdaptiveScheme.resolveKeycapVisual(key = actionKey, showKeyBorders = true)
        assertEquals(Color(0xFF0000FF), nonAdaptiveVisual.backgroundColor)
    }

    @Test
    fun testAdaptiveModifierAndKeySeparation() {
        val adaptiveScheme = KeyboardColorScheme.DefaultLight.copy(
            keyBackground = Color(0xFFFFFFFF),
            keyForeground = Color(0xFF272729),
            modifierKeyBackground = Color(0xFFE2E1E6),
            modifierKeyForeground = Color(0xFF444348),
            actionKeyBackground = Color(0xFFD0E4FF),
            actionKeyForeground = Color(0xFF001D36),
            isAdaptive = true
        )

        val letterKey = io.kiebord.common.model.KeyDef(label = "q", type = io.kiebord.common.model.KeyType.CHARACTER)
        val letterVisual = adaptiveScheme.resolveKeycapVisual(key = letterKey, showKeyBorders = true)
        assertEquals(Color(0xFFFFFFFF), letterVisual.backgroundColor)
        assertEquals(Color(0xFF272729), letterVisual.foregroundColor)

        val spaceKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.SPACE)
        val spaceVisual = adaptiveScheme.resolveKeycapVisual(key = spaceKey, showKeyBorders = true)
        assertEquals(Color(0xFFFFFFFF), spaceVisual.backgroundColor)
        assertEquals(Color(0xFF272729), spaceVisual.foregroundColor)

        val shiftKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.SHIFT)
        val shiftVisual = adaptiveScheme.resolveKeycapVisual(key = shiftKey, showKeyBorders = true)
        assertEquals(Color(0xFFE2E1E6), shiftVisual.backgroundColor)
        assertEquals(Color(0xFF444348), shiftVisual.foregroundColor)

        val deleteKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.DELETE)
        val deleteVisual = adaptiveScheme.resolveKeycapVisual(key = deleteKey, showKeyBorders = true)
        assertEquals(Color(0xFFE2E1E6), deleteVisual.backgroundColor)
        assertEquals(Color(0xFF444348), deleteVisual.foregroundColor)

        val commaKey = io.kiebord.common.model.KeyDef(label = ",", type = io.kiebord.common.model.KeyType.CHARACTER)
        val commaVisual = adaptiveScheme.resolveKeycapVisual(key = commaKey, showKeyBorders = true)
        assertEquals(Color(0xFFE2E1E6), commaVisual.backgroundColor)
        assertEquals(Color(0xFF444348), commaVisual.foregroundColor)

        val periodKey = io.kiebord.common.model.KeyDef(label = ".", type = io.kiebord.common.model.KeyType.CHARACTER)
        val periodVisual = adaptiveScheme.resolveKeycapVisual(key = periodKey, showKeyBorders = true)
        assertEquals(Color(0xFFE2E1E6), periodVisual.backgroundColor)
        assertEquals(Color(0xFF444348), periodVisual.foregroundColor)

        val symbolsKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.SYMBOLS)
        val symbolsVisual = adaptiveScheme.resolveKeycapVisual(key = symbolsKey, showKeyBorders = true)
        assertEquals(Color(0xFFE2E1E6), symbolsVisual.backgroundColor)
        assertEquals(Color(0xFF444348), symbolsVisual.foregroundColor)

        val actionKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.ACTION)
        val actionVisual = adaptiveScheme.resolveKeycapVisual(key = actionKey, showKeyBorders = true)
        assertEquals(Color(0xFFD0E4FF), actionVisual.backgroundColor)
        assertEquals(Color(0xFF001D36), actionVisual.foregroundColor)

    }
}

