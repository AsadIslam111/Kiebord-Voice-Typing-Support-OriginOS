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
    val gradAngle: Float = 90.0f,
    val gradEnd: String = "#ffffffff",
    val gradStart: String = "#ffffffff",
    val gradType: String = "linear",
    val guidelineColor: String = enterColor,
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

    fun calculateLuminance(color: Color): Float {
        fun channel(c: Float): Float {
            return if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055) / 1.055), 2.4).toFloat()
        }
        return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
    }

    fun contrastRatio(color1: Color, color2: Color): Float {
        val l1 = calculateLuminance(color1)
        val l2 = calculateLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun ensureContrast(
        foreground: Color,
        background: Color,
        minContrast: Float = 2.8f,
        fallbackLight: Color = Color.White,
        fallbackDark: Color = Color(0xFF1B1B1F)
    ): Color {
        if (foreground == Color.Unspecified || foreground.alpha < 0.25f) {
            val bgLum = calculateLuminance(background)
            return if (bgLum < 0.5f) fallbackLight else fallbackDark
        }
        val ratio = contrastRatio(foreground, background)
        if (ratio >= minContrast) {
            return foreground
        }
        val contrastWithLight = contrastRatio(fallbackLight, background)
        val contrastWithDark = contrastRatio(fallbackDark, background)
        return if (contrastWithLight >= contrastWithDark) fallbackLight else fallbackDark
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

        val border = if (keyStrokeSize > 0 && keyStrokeColor.isNotBlank() && keyStrokeColor != "#00000000") {
            keyStrokeColor
        } else {
            "#00000000"
        }

        val stripBg = when {
            isTransparent(stripColor) && backgroundType == 2 -> "#00000000"
            isTransparent(stripColor) -> backgroundColor
            else -> stripColor
        }
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
            guidelineColor = if (guidelineColor.isNotBlank()) guidelineColor else enterColor,
            suggestionBackground = stripBg,
            suggestionForeground = stripFg,
            keyPressedBackground = if (pressedKeyColor.isNotBlank()) pressedKeyColor else functionalKeyColor,
            keyPressedForeground = labelKeyColor
        )
    }

    fun CustomThemeDef.toKeyboardThemeSpec(): KeyboardThemeSpec {
        val isFlatBorder = keyBorder.isBlank() || keyBorder == "#00000000"
        val isFlatShadow = keyShadow.isBlank() || keyShadow == "#00000000"
        return KeyboardThemeSpec(
            id = id,
            name = name,
            isDark = isDark,
            background = ThemeBackgroundSpec(solidColor = containerBackground),
            characterKey = KeycapStyleSpec(
                background = keyBackground,
                foreground = keyForeground,
                stroke = if (!isFlatBorder) StrokeSpec(color = keyBorder, widthDp = 0.5f) else null,
                shadow = if (!isFlatShadow) ShadowSpec(color = keyShadow, elevationDp = 1.5f) else null,
                pressedBackground = if (keyPressedBackground.isNotBlank()) keyPressedBackground else null,
                pressedForeground = if (keyPressedForeground.isNotBlank()) keyPressedForeground else null
            ),
            functionalKey = KeycapStyleSpec(
                background = actionKeyBackground,
                foreground = actionKeyForeground,
                stroke = if (!isFlatBorder) StrokeSpec(color = keyBorder, widthDp = 0.5f) else null,
                shadow = if (!isFlatShadow) ShadowSpec(color = keyShadow, elevationDp = 1.5f) else null
            ),
            actionKey = KeycapStyleSpec(
                background = accentColor,
                foreground = actionKeyForeground,
                isPill = true
            ),
            accentColor = accentColor,
            shiftActiveColor = shiftActiveColor,
            suggestionBackground = ThemeBackgroundSpec(solidColor = suggestionBackground),
            suggestionForeground = suggestionForeground,
            moreKeysBackground = moreKeysBackground,
            guidelineColor = guidelineColor
        )
    }

    fun BornoColorTheme.toKeyboardThemeSpec(customId: String? = null): KeyboardThemeSpec {
        val assignedId = customId ?: if (id != null) "borno_$id" else "borno_${name.lowercase().replace("[^a-z0-9]".toRegex(), "_")}"
        val bgColor = parseHexColor(backgroundColor, Color.Black)
        val dark = isColorDark(bgColor)

        val bgSpec = when {
            backgroundType == 3 && backgroundPath.isNotBlank() -> {
                ThemeBackgroundSpec(
                    solidColor = backgroundColor,
                    image = ImageBackgroundSpec(imagePath = backgroundPath)
                )
            }
            backgroundType == 2 && gradStart.isNotBlank() && gradEnd.isNotBlank() && gradStart != "#ffffffff" -> {
                val isRadial = gradType.equals("radial", ignoreCase = true)
                ThemeBackgroundSpec(
                    gradient = GradientSpec(
                        type = if (isRadial) GradientType.RADIAL else GradientType.LINEAR,
                        colors = listOf(gradStart, gradEnd),
                        angle = gradAngle,
                        centerX = 0.5f,
                        centerY = 0.5f,
                        radius = 0.85f
                    )
                )
            }
            else -> {
                ThemeBackgroundSpec(solidColor = backgroundColor)
            }
        }

        val stroke = if (keyStrokeSize > 0 && keyStrokeColor.isNotBlank() && keyStrokeColor != "#00000000") {
            StrokeSpec(color = keyStrokeColor, widthDp = keyStrokeSize.toFloat().coerceAtLeast(0.5f))
        } else null

        val shadow = if (!isFlat && keyShadowSize > 0 && keyShadowColor.isNotBlank() && keyShadowColor != "#00000000") {
            ShadowSpec(color = keyShadowColor, elevationDp = keyShadowSize.toFloat().coerceAtLeast(1.0f))
        } else null

        val cornerRadius = if (keyRadius > 0f && keyRadius != 8f && keyRadius != 9f) keyRadius else null

        val stripBg = when {
            isTransparent(stripColor) && backgroundType == 2 -> "#00000000"
            isTransparent(stripColor) -> backgroundColor
            else -> stripColor
        }
        val stripFg = if (stripIcoColor.isNotBlank() && !isTransparent(stripIcoColor)) stripIcoColor else labelKeyColor

        val resolvedPressedBg = pressedKeyColor.ifBlank { null }

        return KeyboardThemeSpec(
            id = assignedId,
            name = name.trim(),
            isDark = dark,
            background = bgSpec,
            characterKey = KeycapStyleSpec(
                background = keyColor,
                foreground = labelKeyColor,
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = cornerRadius,
                pressedBackground = resolvedPressedBg,
                pressedForeground = labelKeyColor.ifBlank { null }
            ),
            functionalKey = KeycapStyleSpec(
                background = functionalKeyColor,
                foreground = labelFunctionalKeyColor,
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = cornerRadius,
                pressedBackground = resolvedPressedBg,
                pressedForeground = labelFunctionalKeyColor.ifBlank { null }
            ),
            actionKey = KeycapStyleSpec(
                background = enterColor,
                foreground = enterColorIcon.ifBlank { "#ffffffff" },
                isPill = true
            ),
            spaceKey = if (spaceColor.isNotBlank() && spaceColor != keyColor) {
                KeycapStyleSpec(
                    background = spaceColor,
                    foreground = labelKeyColor,
                    stroke = stroke,
                    shadow = shadow,
                    cornerRadiusDp = cornerRadius,
                    pressedBackground = resolvedPressedBg,
                    pressedForeground = labelKeyColor.ifBlank { null }
                )
            } else null,
            accentColor = enterColor,
            shiftActiveColor = if (selectedKeyColor.isNotBlank()) selectedKeyColor else enterColor,
            suggestionBackground = ThemeBackgroundSpec(solidColor = stripBg),
            suggestionForeground = stripFg,
            moreKeysBackground = if (popupColor.isNotBlank()) popupColor else keyColor,
            guidelineColor = if (guidelineColor.isNotBlank()) guidelineColor else enterColor
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
            keyPressedForeground = parseHexColor(keyPressedForeground, darkDefault.keyPressedForeground),
            themeSpec = toKeyboardThemeSpec()
        )
    }
}
