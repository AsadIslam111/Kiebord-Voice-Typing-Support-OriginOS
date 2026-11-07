package io.kiebord.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Version 1 of the `.bornotheme` format (`schemaVersion = 1`),
 * representing the legacy Borno Keyboard single-object exported theme.
 */
@Serializable
data class BornoExportedTheme(
    val schemaVersion: Int = 1,
    val builder: String? = null,
    val name: String? = null,
    val themeName: String? = null,
    val backgroundType: Int = 4,
    val backgroundPath: String? = null,
    val backgroundColor: String = "#FF121212",
    val backgroundOpacity: Int = 255,
    val gradStart: String? = null,
    val gradEnd: String? = null,
    val gradAngle: Float = 90.0f,
    val gradType: String = "linear",
    val keyColor: String = "#FF1E1E1E",
    val labelKeyColor: String = "#FFFFFFFF",
    val functionalKeyColor: String = "#FF2C2C2C",
    val labelFunctionalKeyColor: String = "#FFFFFFFF",
    val labelSymKeyColor: String? = null,
    val pressedKeyColor: String? = null,
    val selectedKeyColor: String? = null,
    val stripColor: String = "#00000000",
    val stripTextColor: String? = null,
    val stripIcoColor: String? = null,
    val stripIconColor: String? = null,
    val keyStrokeColor: String = "#00000000",
    val keyStrokeSize: Int = 0,
    val keyShadowColor: String = "#00000000",
    val keyShadowSize: Int = 0,
    val keyRadius: Float = 9.0f,
    val keyCornerSize: Float? = null,
    val enterColor: String = "#FF2196F3",
    val enterIconColor: String? = null,
    val enterColorIcon: String? = null,
    val enterRadius: Float? = null,
    val symColor: String? = null,
    val symRadius: Float? = null,
    val spaceColor: String? = null,
    val popupColor: String? = null,
    val popupCorner: Int = 10,
    val rgbEffect: Int = 0,
    val isFlat: Boolean = false,
    val isModern: Boolean = true,
    val backgroundImageBase64: String? = null,
    val flatKeyLabelColor: String? = null,
    val activeOptionHighlightColor: String? = null
)

/**
 * Type alias unifying BornoExportedTheme under the Borno theme versioning hierarchy as Version 1.
 */
typealias BornoThemeV1 = BornoExportedTheme

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
    val textSize: Int = 0,
    val activeOptionHighlightColor: String? = null
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
    val keyPressedForeground: String = keyForeground,
    val flatKeyForeground: String? = null,
    val activeOptionHighlightColor: String? = null
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

    fun adjustContrast(
        color: Color,
        background: Color,
        minContrast: Float = 2.8f,
        fallbackLight: Color = Color.White,
        fallbackDark: Color = Color(0xFF1B1B1F)
    ): Color {
        val darkBg = isColorDark(background)
        val targetFallback = if (darkBg) fallbackLight else fallbackDark
        if (color == Color.Unspecified || color.alpha < 0.25f) {
            return targetFallback
        }
        val currentRatio = contrastRatio(color, background)
        if (currentRatio >= minContrast) {
            return color
        }
        val target = targetFallback
        for (step in 1..10) {
            val t = step / 10f
            val candidate = Color(
                red = color.red + (target.red - color.red) * t,
                green = color.green + (target.green - color.green) * t,
                blue = color.blue + (target.blue - color.blue) * t,
                alpha = 1f
            )
            if (contrastRatio(candidate, background) >= minContrast) {
                return candidate
            }
        }
        return target
    }

    fun isTransparent(hex: String): Boolean {
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
            isTransparent(stripColor) && (backgroundType == 2 || backgroundType == 3) -> "#00000000"
            isTransparent(stripColor) -> backgroundColor
            else -> stripColor
        }
        val stripFg = if (stripIcoColor.isNotBlank() && !isTransparent(stripIcoColor)) stripIcoColor else labelKeyColor

        val shadow = resolveKeyShadow(
            rawShadowColor = keyShadowColor,
            shadowSize = keyShadowSize,
            isFlat = isFlat,
            isDark = dark
        )
        val resolvedKeyShadow = shadow?.color ?: "#00000000"

        return CustomThemeDef(
            id = assignedId,
            name = name.trim(),
            isDark = dark,
            containerBackground = backgroundColor,
            keyBackground = keyColor,
            keyForeground = labelKeyColor,
            keyBorder = border,
            keyShadow = resolvedKeyShadow,
            actionKeyBackground = functionalKeyColor,
            actionKeyForeground = labelFunctionalKeyColor,
            shiftActiveColor = if (selectedKeyColor.isNotBlank()) selectedKeyColor else enterColor,
            accentColor = enterColor,
            moreKeysBackground = if (backgroundType == 3) {
                ensureFullOpacityHex(if (popupColor.isNotBlank()) popupColor else keyColor, if (dark) "#FF242428" else "#FFFFFFFF")
            } else {
                if (popupColor.isNotBlank()) popupColor else keyColor
            },
            guidelineColor = if (guidelineColor.isNotBlank()) guidelineColor else enterColor,
            suggestionBackground = stripBg,
            suggestionForeground = stripFg,
            keyPressedBackground = if (pressedKeyColor.isNotBlank()) pressedKeyColor else functionalKeyColor,
            keyPressedForeground = labelKeyColor,
            activeOptionHighlightColor = activeOptionHighlightColor
        )
    }

    fun CustomThemeDef.toKeyboardThemeSpec(): KeyboardThemeSpec {
        val isFlatBorder = keyBorder.isBlank() || keyBorder == "#00000000"
        val hasExplicitShadow = keyShadow.isNotBlank() && keyShadow != "#00000000" && !isTransparent(keyShadow)
        val hasKeyTile = !isTransparent(keyBackground)
        val shadowSpec = if (hasExplicitShadow) {
            ShadowSpec(color = keyShadow, elevationDp = 1.5f)
        } else if (hasKeyTile) {
            ShadowSpec(color = if (isDark) "#4D000000" else "#33000000", elevationDp = 1.5f)
        } else {
            null
        }
        return KeyboardThemeSpec(
            id = id,
            name = name,
            isDark = isDark,
            background = ThemeBackgroundSpec(solidColor = containerBackground),
            characterKey = KeycapStyleSpec(
                background = keyBackground,
                foreground = keyForeground,
                stroke = if (!isFlatBorder) StrokeSpec(color = keyBorder, widthDp = 0.5f) else null,
                shadow = shadowSpec,
                pressedBackground = if (keyPressedBackground.isNotBlank()) keyPressedBackground else null,
                pressedForeground = if (keyPressedForeground.isNotBlank()) keyPressedForeground else null
            ),
            functionalKey = KeycapStyleSpec(
                background = actionKeyBackground,
                foreground = actionKeyForeground,
                stroke = if (!isFlatBorder) StrokeSpec(color = keyBorder, widthDp = 0.5f) else null,
                shadow = shadowSpec
            ),
            actionKey = KeycapStyleSpec(
                background = accentColor,
                foreground = actionKeyForeground,
                shadow = shadowSpec,
                isPill = true
            ),
            accentColor = accentColor,
            shiftActiveColor = shiftActiveColor,
            suggestionBackground = ThemeBackgroundSpec(solidColor = suggestionBackground),
            suggestionForeground = suggestionForeground,
            moreKeysBackground = moreKeysBackground,
            guidelineColor = guidelineColor,
            flatKeyLabelColor = flatKeyForeground,
            activeOptionHighlightColor = activeOptionHighlightColor
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

        val shadow = resolveKeyShadow(
            rawShadowColor = keyShadowColor,
            shadowSize = keyShadowSize,
            isFlat = isFlat,
            isDark = dark
        )

        val cornerRadius = if (keyRadius > 0f && keyRadius != 8f && keyRadius != 9f) keyRadius else null

        val stripBg = when {
            isTransparent(stripColor) && (backgroundType == 2 || backgroundType == 3) -> "#00000000"
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
                shadow = shadow,
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
            moreKeysBackground = if (backgroundType == 3) {
                ensureFullOpacityHex(if (popupColor.isNotBlank()) popupColor else keyColor, if (dark) "#FF242428" else "#FFFFFFFF")
            } else {
                if (popupColor.isNotBlank()) popupColor else keyColor
            },
            guidelineColor = if (guidelineColor.isNotBlank()) guidelineColor else enterColor,
            activeOptionHighlightColor = activeOptionHighlightColor
        )
    }

    fun colorToHex(color: Color): String {
        val a = (color.alpha * 255f).toInt().coerceIn(0, 255)
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X%02X", a, r, g, b)
    }

    fun calculateHoverState(baseColor: Color): Color {
        return if (isColorDark(baseColor)) {
            Color(
                red = (baseColor.red * 0.78f + 0.22f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.78f + 0.22f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.78f + 0.22f).coerceIn(0f, 1f),
                alpha = if (baseColor.alpha < 0.2f) 0.35f else baseColor.alpha
            )
        } else {
            Color(
                red = (baseColor.red * 0.82f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.82f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.82f).coerceIn(0f, 1f),
                alpha = if (baseColor.alpha < 0.2f) 0.35f else baseColor.alpha
            )
        }
    }

    fun ensureFullOpacity(color: Color, fallbackIfTransparent: Color): Color {
        return if (color == Color.Unspecified || color.alpha < 0.05f) {
            fallbackIfTransparent
        } else {
            color.copy(alpha = 1.0f)
        }
    }

    fun ensureFullOpacityHex(hex: String?, fallbackIfTransparent: String): String {
        if (hex.isNullOrBlank() || isTransparent(hex)) {
            return fallbackIfTransparent
        }
        val clean = hex.trim().removePrefix("#")
        return when (clean.length) {
            8 -> "#FF" + clean.substring(2)
            6 -> "#$clean"
            else -> {
                val parsed = parseHexColor(hex, Color.Unspecified)
                if (parsed == Color.Unspecified || parsed.alpha < 0.05f) {
                    fallbackIfTransparent
                } else {
                    colorToHex(parsed.copy(alpha = 1.0f))
                }
            }
        }
    }

    fun resolveHoverColor(
        candidateHex: String?,
        baseColor: Color
    ): String {
        if (candidateHex.isNullOrBlank() || isTransparent(candidateHex)) {
            return colorToHex(calculateHoverState(baseColor))
        }
        val candidateColor = parseHexColor(candidateHex, Color.Unspecified)
        if (candidateColor == Color.Unspecified || candidateColor.alpha < 0.05f) {
            return colorToHex(calculateHoverState(baseColor))
        }
        return candidateHex
    }

    fun fixKeyStrokeColorAndOpacity(
        rawStrokeColor: String?,
        keyStrokeSize: Int,
        keyBackground: Color
    ): Pair<String, Float>? {
        if (keyStrokeSize <= 0) return null
        val parsedStroke = if (!rawStrokeColor.isNullOrBlank()) parseHexColor(rawStrokeColor, Color.Unspecified) else Color.Unspecified

        if (parsedStroke == Color.Unspecified || parsedStroke.alpha < 0.05f || rawStrokeColor == "#00000000") {
            return if (isColorDark(keyBackground)) {
                Pair("#FFFFFF", 0.22f)
            } else {
                Pair("#000000", 0.14f)
            }
        }

        if (parsedStroke.alpha > 0.9f) {
            val isStrokeWhiteOrLight = !isColorDark(parsedStroke)
            val isBgDark = isColorDark(keyBackground)
            val softenedAlpha = when {
                isBgDark && isStrokeWhiteOrLight -> 0.28f
                !isBgDark && !isStrokeWhiteOrLight -> 0.16f
                else -> 0.40f
            }
            return Pair(rawStrokeColor ?: colorToHex(parsedStroke.copy(alpha = 1.0f)), softenedAlpha)
        }

        return Pair(rawStrokeColor ?: colorToHex(parsedStroke.copy(alpha = 1.0f)), parsedStroke.alpha)
    }

    fun resolveKeyShadow(
        rawShadowColor: String?,
        shadowSize: Int,
        isFlat: Boolean,
        isDark: Boolean
    ): ShadowSpec? {
        if (isFlat) return null

        val hasValidColor = !rawShadowColor.isNullOrBlank() &&
            rawShadowColor != "#00000000" &&
            !isTransparent(rawShadowColor)

        val elevation = if (shadowSize > 0) shadowSize.toFloat().coerceAtLeast(1.0f) else 1.5f

        val finalColor = when {
            hasValidColor -> {
                val parsed = parseHexColor(rawShadowColor!!, Color.Unspecified)
                if (parsed != Color.Unspecified && parsed.alpha < 0.05f) {
                    if (isDark) "#4D000000" else "#33000000"
                } else if (parsed != Color.Unspecified && parsed.alpha > 0.8f) {
                    colorToHex(parsed.copy(alpha = 0.28f))
                } else {
                    rawShadowColor
                }
            }
            else -> {
                if (isDark) "#4D000000" else "#33000000"
            }
        }

        return ShadowSpec(color = finalColor, elevationDp = elevation)
    }

    fun BornoExportedTheme.toKeyboardThemeSpec(customId: String? = null, resolvedImagePath: String? = null): KeyboardThemeSpec {
        val themeTitle = (name ?: themeName ?: "Imported Theme").trim().ifEmpty { "Imported Theme" }
        val assignedId = customId ?: "custom_${System.currentTimeMillis()}_${themeTitle.lowercase().replace("[^a-z0-9]".toRegex(), "_")}"
        val bgColor = parseHexColor(backgroundColor, Color.Black)
        val dark = isColorDark(bgColor)

        val imagePathFinal = resolvedImagePath ?: backgroundPath?.takeIf { it.isNotBlank() }
        val effectiveBgType = when {
            !imagePathFinal.isNullOrBlank() -> 3
            backgroundType == 2 || (!gradStart.isNullOrBlank() && !gradEnd.isNullOrBlank() && gradStart != gradEnd && !isTransparent(gradEnd)) -> 2
            else -> backgroundType
        }

        val bgSpec = when {
            effectiveBgType == 3 && !imagePathFinal.isNullOrBlank() -> {
                val alphaFloat = (backgroundOpacity.coerceIn(0, 255) / 255f)
                val dimOverlay = if (backgroundOpacity in 1..254) {
                    val darkAlpha = (255 - backgroundOpacity).coerceIn(0, 255)
                    String.format("#%02X000000", darkAlpha)
                } else null
                ThemeBackgroundSpec(
                    solidColor = backgroundColor,
                    image = ImageBackgroundSpec(
                        imagePath = imagePathFinal,
                        alpha = alphaFloat,
                        dimOverlayColor = dimOverlay
                    )
                )
            }
            effectiveBgType == 2 && !gradStart.isNullOrBlank() && !gradEnd.isNullOrBlank() -> {
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

        val parsedKeyBg = parseHexColor(keyColor, Color(0xFF1E1E1E))
        val parsedFuncBg = parseHexColor(functionalKeyColor, Color(0xFF2C2C2C))
        val parsedEnterBg = parseHexColor(enterColor, Color(0xFF2196F3))

        val resolvedEnterIcon = enterIconColor ?: enterColorIcon ?: "#FFFFFFFF"

        // Missing hover / pressed state check & fix
        val resolvedCharPressedBg = resolveHoverColor(pressedKeyColor, parsedKeyBg)
        val resolvedFuncPressedBg = resolveHoverColor(pressedKeyColor, parsedFuncBg)
        val resolvedEnterPressedBg = resolveHoverColor(null, parsedEnterBg)

        // Key stroke opacity check & fix
        val strokeInfo = fixKeyStrokeColorAndOpacity(keyStrokeColor, keyStrokeSize, parsedKeyBg)
        val stroke = if (strokeInfo != null) {
            StrokeSpec(
                color = strokeInfo.first,
                widthDp = keyStrokeSize.toFloat().coerceAtLeast(0.5f),
                alpha = strokeInfo.second
            )
        } else null

        val shadow = resolveKeyShadow(
            rawShadowColor = keyShadowColor,
            shadowSize = keyShadowSize,
            isFlat = isFlat,
            isDark = dark
        )

        val resolvedRadius = keyCornerSize ?: keyRadius
        val cornerRadius = if (resolvedRadius > 0f && resolvedRadius != 8f && resolvedRadius != 9f) resolvedRadius else null
        val enterCornerRadius = enterRadius?.takeIf { it > 0f && it != 8f && it != 9f } ?: cornerRadius

        val stripBg = when {
            isTransparent(stripColor) && (effectiveBgType == 2 || effectiveBgType == 3) -> "#00000000"
            isTransparent(stripColor) -> backgroundColor
            else -> stripColor
        }
        val effStripIco = stripIconColor ?: stripIcoColor
        val stripFg = if (!effStripIco.isNullOrBlank() && !isTransparent(effStripIco)) effStripIco else (stripTextColor ?: labelKeyColor)

        return KeyboardThemeSpec(
            id = assignedId,
            name = themeTitle,
            isDark = dark,
            background = bgSpec,
            characterKey = KeycapStyleSpec(
                background = keyColor,
                foreground = labelKeyColor,
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = cornerRadius,
                pressedBackground = resolvedCharPressedBg,
                pressedForeground = labelKeyColor.ifBlank { null }
            ),
            functionalKey = KeycapStyleSpec(
                background = functionalKeyColor,
                foreground = labelFunctionalKeyColor,
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = cornerRadius,
                pressedBackground = resolvedFuncPressedBg,
                pressedForeground = labelFunctionalKeyColor.ifBlank { null }
            ),
            actionKey = KeycapStyleSpec(
                background = enterColor,
                foreground = resolvedEnterIcon,
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = enterCornerRadius,
                pressedBackground = resolvedEnterPressedBg,
                pressedForeground = resolvedEnterIcon,
                isPill = true
            ),
            spaceKey = if (!spaceColor.isNullOrBlank() && spaceColor != keyColor) {
                val parsedSpaceBg = parseHexColor(spaceColor, parsedKeyBg)
                KeycapStyleSpec(
                    background = spaceColor,
                    foreground = labelKeyColor,
                    stroke = stroke,
                    shadow = shadow,
                    cornerRadiusDp = cornerRadius,
                    pressedBackground = resolveHoverColor(null, parsedSpaceBg),
                    pressedForeground = labelKeyColor.ifBlank { null }
                )
            } else null,
            accentColor = enterColor,
            shiftActiveColor = if (!selectedKeyColor.isNullOrBlank()) selectedKeyColor else enterColor,
            suggestionBackground = ThemeBackgroundSpec(solidColor = stripBg),
            suggestionForeground = stripFg,
            moreKeysBackground = if (effectiveBgType == 3) {
                ensureFullOpacityHex(if (!popupColor.isNullOrBlank()) popupColor else keyColor, if (dark) "#FF242428" else "#FFFFFFFF")
            } else {
                if (!popupColor.isNullOrBlank()) popupColor else keyColor
            },
            guidelineColor = enterColor,
            flatKeyLabelColor = flatKeyLabelColor,
            activeOptionHighlightColor = activeOptionHighlightColor
        )
    }

    fun BornoExportedTheme.toCustomThemeDef(customId: String? = null, resolvedImagePath: String? = null): CustomThemeDef {
        val themeTitle = (name ?: themeName ?: "Imported Theme").trim().ifEmpty { "Imported Theme" }
        val assignedId = customId ?: "custom_${System.currentTimeMillis()}_${themeTitle.lowercase().replace("[^a-z0-9]".toRegex(), "_")}"
        val bgColor = parseHexColor(backgroundColor, Color.Black)
        val dark = isColorDark(bgColor)

        val imagePathFinal = resolvedImagePath ?: backgroundPath?.takeIf { it.isNotBlank() }
        val effectiveBgType = when {
            !imagePathFinal.isNullOrBlank() -> 3
            backgroundType == 2 || (!gradStart.isNullOrBlank() && !gradEnd.isNullOrBlank() && gradStart != gradEnd && !isTransparent(gradEnd)) -> 2
            else -> backgroundType
        }

        val parsedKeyBg = parseHexColor(keyColor, Color(0xFF1E1E1E))
        val resolvedEnterIcon = enterIconColor ?: enterColorIcon ?: "#FFFFFFFF"

        val effStripBg = when {
            isTransparent(stripColor) && (effectiveBgType == 2 || effectiveBgType == 3) -> "#00000000"
            isTransparent(stripColor) -> backgroundColor
            else -> stripColor
        }
        val effStripIco = stripIconColor ?: stripIcoColor
        val stripFg = if (!effStripIco.isNullOrBlank() && !isTransparent(effStripIco)) effStripIco else (stripTextColor ?: labelKeyColor)

        val resolvedPressedBg = resolveHoverColor(pressedKeyColor, parsedKeyBg)
        val strokeInfo = fixKeyStrokeColorAndOpacity(keyStrokeColor, keyStrokeSize, parsedKeyBg)
        val resolvedBorder = if (strokeInfo != null) strokeInfo.first else "#00000000"

        val resolvedShadow = resolveKeyShadow(
            rawShadowColor = keyShadowColor,
            shadowSize = keyShadowSize,
            isFlat = isFlat,
            isDark = dark
        )
        val resolvedKeyShadowColor = resolvedShadow?.color ?: if (!isFlat) (if (dark) "#4D000000" else "#33000000") else "#00000000"

        return CustomThemeDef(
            id = assignedId,
            name = themeTitle,
            isDark = dark,
            containerBackground = if (backgroundType == 2 && !gradStart.isNullOrBlank()) gradStart else backgroundColor,
            keyBackground = keyColor,
            keyForeground = labelKeyColor,
            keyBorder = resolvedBorder,
            keyShadow = resolvedKeyShadowColor,
            actionKeyBackground = enterColor,
            actionKeyForeground = resolvedEnterIcon,
            shiftActiveColor = if (!selectedKeyColor.isNullOrBlank()) selectedKeyColor else enterColor,
            accentColor = enterColor,
            moreKeysBackground = if (effectiveBgType == 3) {
                ensureFullOpacityHex(if (!popupColor.isNullOrBlank()) popupColor else keyColor, if (dark) "#FF242428" else "#FFFFFFFF")
            } else {
                if (!popupColor.isNullOrBlank()) popupColor else keyColor
            },
            guidelineColor = enterColor,
            suggestionBackground = effStripBg,
            suggestionForeground = stripFg,
            keyPressedBackground = resolvedPressedBg,
            keyPressedForeground = labelKeyColor,
            flatKeyForeground = flatKeyLabelColor,
            activeOptionHighlightColor = activeOptionHighlightColor
        )
    }

    fun decodeBase64Safe(input: String): ByteArray {
        val raw = input.trim().removePrefix("\uFEFF").replace("\n", "").replace("\r", "").replace("\"", "").trim()
        val clean = when (raw.length % 4) {
            2 -> "$raw=="
            3 -> "$raw="
            else -> raw
        }
        return try {
            java.util.Base64.getDecoder().decode(clean)
        } catch (_: Throwable) {
            try {
                java.util.Base64.getUrlDecoder().decode(clean)
            } catch (_: Throwable) {
                try {
                    android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                } catch (_: Throwable) {
                    try {
                        android.util.Base64.decode(clean, android.util.Base64.URL_SAFE)
                    } catch (_: Throwable) {
                        try {
                            android.util.Base64.decode(raw, android.util.Base64.NO_PADDING)
                        } catch (_: Throwable) {
                            ByteArray(0)
                        }
                    }
                }
            }
        }
    }

    fun decodeBase64ToStringSafe(input: String): String? {
        val bytes = decodeBase64Safe(input)
        if (bytes.isEmpty()) return null
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    fun encodeBase64Safe(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            try {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (_: Throwable) {
                ""
            }
        }
    }

    fun encodeBase64Safe(text: String): String {
        return encodeBase64Safe(text.toByteArray(Charsets.UTF_8))
    }

    fun CustomThemeDef.toKeyboardColorScheme(): KeyboardColorScheme {
        val darkDefault = if (isDark) KeyboardColorScheme.DefaultDark else KeyboardColorScheme.DefaultLight
        val defaultShadow = if (isDark) darkDefault.keyShadow else Color(0x33000000)
        val hasExplicitShadow = keyShadow.isNotBlank() && keyShadow != "#00000000" && !isTransparent(keyShadow)
        val hasKeyTile = !isTransparent(keyBackground)
        val resolvedKeyShadow = if (hasExplicitShadow) {
            parseHexColor(keyShadow, defaultShadow)
        } else if (hasKeyTile) {
            defaultShadow
        } else {
            Color(0x00000000)
        }

        val spec = toKeyboardThemeSpec()
        val hasImageBackdrop = spec.background.image != null
        val rawMoreKeys = parseHexColor(moreKeysBackground, darkDefault.moreKeysBackground)
        val resolvedMoreKeys = if (hasImageBackdrop) {
            val fallback = if (isDark) Color(0xFF242428) else Color(0xFFFFFFFF)
            ensureFullOpacity(rawMoreKeys, fallback)
        } else {
            rawMoreKeys
        }

        return KeyboardColorScheme(
            containerBackground = parseHexColor(containerBackground, darkDefault.containerBackground),
            keyBackground = parseHexColor(keyBackground, darkDefault.keyBackground),
            keyForeground = parseHexColor(keyForeground, darkDefault.keyForeground),
            keyBorder = parseHexColor(keyBorder, darkDefault.keyBorder),
            keyShadow = resolvedKeyShadow,
            actionKeyBackground = parseHexColor(actionKeyBackground, darkDefault.actionKeyBackground),
            actionKeyForeground = parseHexColor(actionKeyForeground, darkDefault.actionKeyForeground),
            shiftActiveColor = parseHexColor(shiftActiveColor, darkDefault.shiftActiveColor),
            accentColor = parseHexColor(accentColor, darkDefault.accentColor),
            moreKeysBackground = resolvedMoreKeys,
            guidelineColor = parseHexColor(guidelineColor, darkDefault.guidelineColor),
            suggestionBackground = parseHexColor(suggestionBackground, darkDefault.suggestionBackground),
            suggestionForeground = parseHexColor(suggestionForeground, darkDefault.suggestionForeground),
            keyPressedBackground = parseHexColor(keyPressedBackground, darkDefault.keyPressedBackground),
            keyPressedForeground = parseHexColor(keyPressedForeground, darkDefault.keyPressedForeground),
            flatKeyForeground = flatKeyForeground?.let { parseHexColor(it) } ?: Color.Unspecified,
            activeOptionHighlightColor = activeOptionHighlightColor?.let { parseHexColor(it) } ?: Color.Unspecified,
            themeSpec = spec
        )
    }
}
