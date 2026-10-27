package io.kiebord.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class BornoThemeFile(
    val themes: List<BornoThemeGroup> = emptyList()
)

@Serializable
data class BornoThemeGroup(
    val colors: List<BornoColorTheme> = emptyList()
)

@Serializable
data class BornoColorTheme(
    val id: Int? = null,
    val name: String,
    val backgroundColor: String,
    val backgroundPath: String = "",
    val backgroundType: Int = 1,
    val enterColor: String,
    val enterColorIcon: String = "#ffffffff",
    val functionalKeyColor: String,
    val gradEnd: String = "#ffffffff",
    val gradStart: String = "#ffffffff",
    val isFlat: Boolean = false,
    val keyColor: String,
    val keyRadius: Float = 9.0f,
    val keyShadowColor: String = "#00000000",
    val keyShadowSize: Int = 0,
    val keyStrokeColor: String = "#00000000",
    val keyStrokeSize: Int = 0,
    val labelFunctionalKeyColor: String,
    val labelKeyColor: String,
    val popupColor: String = keyColor,
    val popupCorner: Int = 10,
    val pressedKeyColor: String = functionalKeyColor,
    val selectedKeyColor: String = enterColor,
    val spaceColor: String = keyColor,
    val stripColor: String = "#00ffffff",
    val stripIcoColor: String = labelKeyColor,
    val textSize: Int = 0
)

@Serializable
data class CustomThemeDef(
    val id: String,
    val name: String,
    val isDark: Boolean = true,
    val containerBackground: String,
    val keyBackground: String,
    val keyForeground: String,
    val keyBorder: String = "#00000000",
    val keyShadow: String = "#00000000",
    val actionKeyBackground: String,
    val actionKeyForeground: String,
    val shiftActiveColor: String,
    val accentColor: String,
    val moreKeysBackground: String = keyBackground,
    val guidelineColor: String = accentColor,
    val suggestionBackground: String = containerBackground,
    val suggestionForeground: String = keyForeground,
    val keyPressedBackground: String = actionKeyBackground,
    val keyPressedForeground: String = keyForeground
)

object ThemeColorUtils {

    fun parseHexColor(hex: String, default: Color = Color.Unspecified): Color {
        val clean = hex.trim()
        if (clean.isEmpty()) return default
        return try {
            val s = if (clean.startsWith("#")) clean.substring(1) else clean
            val (a, r, g, b) = when (s.length) {
                3 -> {
                    val r = s.substring(0, 1).repeat(2).toInt(16)
                    val g = s.substring(1, 2).repeat(2).toInt(16)
                    val b = s.substring(2, 3).repeat(2).toInt(16)
                    listOf(255, r, g, b)
                }
                4 -> {
                    val a = s.substring(0, 1).repeat(2).toInt(16)
                    val r = s.substring(1, 2).repeat(2).toInt(16)
                    val g = s.substring(2, 3).repeat(2).toInt(16)
                    val b = s.substring(3, 4).repeat(2).toInt(16)
                    listOf(a, r, g, b)
                }
                6 -> {
                    val r = s.substring(0, 2).toInt(16)
                    val g = s.substring(2, 4).toInt(16)
                    val b = s.substring(4, 6).toInt(16)
                    listOf(255, r, g, b)
                }
                8 -> {
                    val a = s.substring(0, 2).toInt(16)
                    val r = s.substring(2, 4).toInt(16)
                    val g = s.substring(4, 6).toInt(16)
                    val b = s.substring(6, 8).toInt(16)
                    listOf(a, r, g, b)
                }
                else -> return default
            }
            Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
        } catch (_: Exception) {
            default
        }
    }

    fun isColorDark(color: Color): Boolean {
        val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
        return luminance < 0.5
    }

    private fun isTransparent(hex: String): Boolean {
        val clean = hex.trim()
        if (clean.isEmpty()) return true
        return try {
            val s = if (clean.startsWith("#")) clean.substring(1) else clean
            if (s.length == 8) {
                val alpha = s.substring(0, 2).toInt(16)
                alpha < 10
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun BornoColorTheme.toCustomThemeDef(customId: String? = null): CustomThemeDef {
        val assignedId = customId ?: if (id != null) "borno_$id" else "borno_${name.lowercase().replace("[^a-z0-9]".toRegex(), "_")}"
        val bgColor = parseHexColor(backgroundColor, Color.Black)
        val dark = isColorDark(bgColor)

        val border = when {
            keyStrokeSize > 0 && keyStrokeColor.isNotBlank() && keyStrokeColor != "#00000000" -> keyStrokeColor
            dark -> "#1FFFFFFF"
            else -> "#00000000"
        }

        val stripBg = if (isTransparent(stripColor)) backgroundColor else stripColor
        val stripFg = if (stripIcoColor.isNotBlank() && !isTransparent(stripIcoColor)) stripIcoColor else labelKeyColor

        return CustomThemeDef(
            id = assignedId,
            name = name.trim(),
            isDark = dark,
            containerBackground = backgroundColor,
            keyBackground = keyColor,
            keyForeground = labelKeyColor,
            keyBorder = border,
            keyShadow = if (keyShadowSize > 0) keyShadowColor else "#00000000",
            actionKeyBackground = functionalKeyColor,
            actionKeyForeground = labelFunctionalKeyColor,
            shiftActiveColor = if (selectedKeyColor.isNotBlank()) selectedKeyColor else enterColor,
            accentColor = enterColor,
            moreKeysBackground = if (popupColor.isNotBlank()) popupColor else keyColor,
            guidelineColor = enterColor,
            suggestionBackground = stripBg,
            suggestionForeground = stripFg,
            keyPressedBackground = if (pressedKeyColor.isNotBlank()) pressedKeyColor else functionalKeyColor,
            keyPressedForeground = labelKeyColor
        )
    }

    fun CustomThemeDef.toKeyboardColorScheme(): KeyboardColorScheme {
        val darkDefault = if (isDark) KeyboardColorScheme.DefaultDark else KeyboardColorScheme.DefaultLight
        return KeyboardColorScheme(
            containerBackground = parseHexColor(containerBackground, darkDefault.containerBackground),
            keyBackground = parseHexColor(keyBackground, darkDefault.keyBackground),
            keyForeground = parseHexColor(keyForeground, darkDefault.keyForeground),
            keyBorder = parseHexColor(keyBorder, darkDefault.keyBorder),
            keyShadow = parseHexColor(keyShadow, darkDefault.keyShadow),
            actionKeyBackground = parseHexColor(actionKeyBackground, darkDefault.actionKeyBackground),
            actionKeyForeground = parseHexColor(actionKeyForeground, darkDefault.actionKeyForeground),
            shiftActiveColor = parseHexColor(shiftActiveColor, darkDefault.shiftActiveColor),
            accentColor = parseHexColor(accentColor, darkDefault.accentColor),
            moreKeysBackground = parseHexColor(moreKeysBackground, darkDefault.moreKeysBackground),
            guidelineColor = parseHexColor(guidelineColor, darkDefault.guidelineColor),
            suggestionBackground = parseHexColor(suggestionBackground, darkDefault.suggestionBackground),
            suggestionForeground = parseHexColor(suggestionForeground, darkDefault.suggestionForeground),
            keyPressedBackground = parseHexColor(keyPressedBackground, darkDefault.keyPressedBackground),
            keyPressedForeground = parseHexColor(keyPressedForeground, darkDefault.keyPressedForeground)
        )
    }
}
