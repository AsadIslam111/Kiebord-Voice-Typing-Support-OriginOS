package io.kiebord.theme

import androidx.compose.ui.graphics.Color
import io.kiebord.theme.ThemeColorUtils.isColorDark
import io.kiebord.theme.ThemeColorUtils.parseHexColor
import io.kiebord.theme.ThemeColorUtils.toCustomThemeDef
import io.kiebord.theme.ThemeColorUtils.toKeyboardColorScheme
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
}
