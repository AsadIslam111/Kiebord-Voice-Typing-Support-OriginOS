package io.kiebord.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.ShiftState

@Immutable
data class KeyboardColorScheme(
    val containerBackground: Color,
    val keyBackground: Color,
    val keyForeground: Color,
    val keyBorder: Color,
    val keyShadow: Color = Color(0x00000000),
    val actionKeyBackground: Color,
    val actionKeyForeground: Color,
    val shiftActiveColor: Color,
    val accentColor: Color,
    val moreKeysBackground: Color,
    val guidelineColor: Color,
    val suggestionBackground: Color,
    val suggestionForeground: Color,
    val keyPressedBackground: Color = Color.Unspecified,
    val keyPressedForeground: Color = Color.Unspecified,
    val flatKeyForeground: Color = Color.Unspecified,
    val activeOptionHighlightColor: Color = Color.Unspecified,
    val themeSpec: KeyboardThemeSpec? = null
) {
    val effectiveActiveOptionHighlight: Color
        get() = if (activeOptionHighlightColor != Color.Unspecified) activeOptionHighlightColor else accentColor
    fun resolveKeycapVisual(
        rowIndex: Int = 0,
        keyIndex: Int = 0,
        key: KeyDef,
        isPressed: Boolean = false,
        showKeyBorders: Boolean = true,
        showKeyShadows: Boolean = true,
        size: Size = Size.Zero,
        scaleFactor: Float = 1.0f
    ): ResolvedKeyVisual {
        val spec = themeSpec
        if (spec != null) {
            val style = spec.resolveKeycapStyle(rowIndex, keyIndex, key)
            val bgHex = if (isPressed) (style.pressedBackground ?: style.background) else style.background
            val fgHex = if (isPressed) (style.pressedForeground ?: style.foreground) else style.foreground

            val isComma = key.type == KeyType.CHARACTER && (key.label == "," || key.label == "،")
            val isActionOrModifier = key.type == KeyType.SHIFT || key.type == KeyType.DELETE || key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || isComma
            val isPillColoredKey = key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || key.type == KeyType.SPACE

            val hasContainerBackdrop = spec.background.gradient != null || spec.background.image != null
            val flatKeyBg = if (hasContainerBackdrop) Color.Transparent else containerBackground

            val defaultBg = when {
                key.type == KeyType.ACTION -> accentColor
                key.type == KeyType.SYMBOLS -> actionKeyBackground
                key.type == KeyType.SPACE || key.type == KeyType.SWITCH_LANGUAGE -> if (showKeyBorders || key.type == KeyType.SPACE) keyBackground else flatKeyBg
                showKeyBorders -> if (isActionOrModifier) actionKeyBackground else keyBackground
                else -> flatKeyBg
            }

            val baseBgColor = if (showKeyBorders) {
                if (bgHex != null) cachedParseHex(bgHex, defaultBg) else defaultBg
            } else {
                when {
                    key.type == KeyType.ACTION -> if (bgHex != null) cachedParseHex(bgHex, accentColor) else accentColor
                    key.type == KeyType.SPACE -> {
                        val spaceHex = spec.spaceKey?.background ?: style.background
                        if (spaceHex != null) cachedParseHex(spaceHex, keyBackground) else keyBackground
                    }
                    key.type == KeyType.SYMBOLS -> {
                        val symHex = style.background
                        if (symHex != null) cachedParseHex(symHex, actionKeyBackground) else actionKeyBackground
                    }
                    else -> flatKeyBg
                }
            }

            val activeBgColor = if (isPressed) {
                when {
                    style.pressedBackground != null -> cachedParseHex(style.pressedBackground)
                    key.type == KeyType.ACTION -> accentColor.copy(alpha = 0.8f)
                    keyPressedBackground != Color.Unspecified -> keyPressedBackground
                    else -> accentColor.copy(alpha = 0.28f)
                }
            } else {
                baseBgColor
            }

            val fgColor = when {
                key.type == KeyType.ACTION -> actionKeyForeground
                isPressed && keyPressedForeground != Color.Unspecified -> keyPressedForeground
                !showKeyBorders && style.flatForeground != null -> cachedParseHex(style.flatForeground)
                !showKeyBorders && flatKeyForeground != Color.Unspecified -> flatKeyForeground
                !showKeyBorders -> {
                    val baseFg = if (fgHex != null) cachedParseHex(fgHex) else keyForeground
                    ThemeColorUtils.ensureContrast(baseFg, containerBackground)
                }
                fgHex != null -> cachedParseHex(fgHex)
                else -> keyForeground
            }

            val stroke = style.stroke
            val strokeColor = if (showKeyBorders && stroke != null && stroke.widthDp > 0f) {
                val parsed = cachedParseHex(stroke.color, keyBorder)
                parsed.copy(alpha = (parsed.alpha * stroke.alpha).coerceIn(0f, 1f))
            } else {
                Color.Transparent
            }

            val strokeWidth = if (showKeyBorders && stroke != null && stroke.widthDp > 0f) {
                stroke.widthDp.dp
            } else {
                0.dp
            }

            val hasKeyTile = showKeyBorders || isPillColoredKey
            val shadow = style.shadow
            val shadowColor = if (showKeyShadows && hasKeyTile) {
                if (shadow != null && shadow.elevationDp > 0f) {
                    val parsed = cachedParseHex(shadow.color, keyShadow)
                    if (parsed.alpha > 0.8f) parsed.copy(alpha = 0.28f) else parsed
                } else if (keyShadow.alpha > 0.01f) {
                    if (keyShadow.alpha > 0.8f) keyShadow.copy(alpha = 0.28f) else keyShadow
                } else {
                    Color.Transparent
                }
            } else {
                Color.Transparent
            }

            val shadowElevation = if (showKeyShadows && hasKeyTile) {
                if (shadow != null && shadow.elevationDp > 0f) {
                    shadow.elevationDp.dp
                } else if (keyShadow.alpha > 0.01f) {
                    (1.5f * scaleFactor).dp
                } else {
                    0.dp
                }
            } else {
                0.dp
            }

            val isPill = style.isPill ?: (key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || (!showKeyBorders && key.type == KeyType.SPACE))
            val cornerRadius = style.cornerRadiusDp?.dp

            val brush = if (!isPressed && (showKeyBorders || key.type == KeyType.ACTION || key.type == KeyType.SPACE)) {
                style.gradient?.toBrush(size)
            } else {
                null
            }

            return ResolvedKeyVisual(
                backgroundColor = activeBgColor,
                backgroundBrush = brush,
                foregroundColor = fgColor,
                strokeColor = strokeColor,
                strokeWidthDp = strokeWidth,
                shadowColor = shadowColor,
                shadowElevationDp = shadowElevation,
                cornerRadiusDp = cornerRadius,
                isPill = isPill,
                skin = style.skin,
                alpha = style.alpha
            )
        }

        val isCommaKey = key.type == KeyType.CHARACTER && (key.label == "," || key.label == "،")
        val isActionOrModifier = key.type == KeyType.SHIFT || key.type == KeyType.DELETE || key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || isCommaKey
        val isPillColoredKey = key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || key.type == KeyType.SPACE
        val baseBackground = when {
            key.type == KeyType.ACTION -> accentColor
            key.type == KeyType.SYMBOLS -> actionKeyBackground
            key.type == KeyType.SPACE -> keyBackground
            showKeyBorders -> if (isActionOrModifier) actionKeyBackground else keyBackground
            else -> containerBackground
        }

        val activeBackground = if (isPressed) {
            if (key.type == KeyType.ACTION) {
                accentColor.copy(alpha = 0.8f)
            } else if (keyPressedBackground != Color.Unspecified) {
                keyPressedBackground
            } else {
                accentColor.copy(alpha = 0.28f)
            }
        } else {
            baseBackground
        }

        val fg = when {
            key.type == KeyType.ACTION -> actionKeyForeground
            isPressed && keyPressedForeground != Color.Unspecified -> keyPressedForeground
            !showKeyBorders && flatKeyForeground != Color.Unspecified -> flatKeyForeground
            !showKeyBorders -> ThemeColorUtils.ensureContrast(keyForeground, containerBackground)
            else -> keyForeground
        }

        val isPillShape = key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || (!showKeyBorders && key.type == KeyType.SPACE)
        val hasTile = showKeyBorders || isPillColoredKey
        val strokeCol = if (showKeyBorders && keyBorder.alpha > 0.01f) keyBorder else Color.Transparent
        val strokeWidth = if (showKeyBorders && keyBorder.alpha > 0.01f) 0.5.dp else 0.dp
        val shadowCol = if (showKeyShadows && hasTile && keyShadow.alpha > 0.01f) keyShadow else Color.Transparent
        val shadowElevation = if (showKeyShadows && hasTile && keyShadow.alpha > 0.01f) (1.5f * scaleFactor).dp else 0.dp

        return ResolvedKeyVisual(
            backgroundColor = activeBackground,
            backgroundBrush = null,
            foregroundColor = fg,
            strokeColor = strokeCol,
            strokeWidthDp = strokeWidth,
            shadowColor = shadowCol,
            shadowElevationDp = shadowElevation,
            cornerRadiusDp = null,
            isPill = isPillShape,
            skin = null,
            alpha = 1.0f
        )
    }

    fun getCustomIconName(keyType: KeyType, shiftState: ShiftState = ShiftState.UNSHIFTED): String? {
        val icons = themeSpec?.icons ?: return null
        return when (keyType) {
            KeyType.SHIFT -> when (shiftState) {
                ShiftState.UNSHIFTED -> icons.shift
                ShiftState.ONE_SHOT -> icons.shiftOneShot ?: icons.shift
                ShiftState.CAPS_LOCK -> icons.capsLock ?: icons.shift
            }
            KeyType.DELETE -> icons.backspace
            KeyType.ACTION -> icons.action
            KeyType.SWITCH_LANGUAGE -> icons.language
            else -> null
        }
    }

    companion object {
        private val hexColorCache = java.util.concurrent.ConcurrentHashMap<String, Color>(128)

        private fun cachedParseHex(hex: String): Color {
            hexColorCache[hex]?.let { return it }
            val parsed = ThemeColorUtils.parseHexColor(hex)
            hexColorCache[hex] = parsed
            return parsed
        }

        private fun cachedParseHex(hex: String, fallback: Color): Color {
            hexColorCache[hex]?.let { return it }
            val parsed = ThemeColorUtils.parseHexColor(hex, fallback)
            if (parsed != fallback) {
                hexColorCache[hex] = parsed
            }
            return parsed
        }

        val DefaultDark = KeyboardColorScheme(
            containerBackground = Color(0xFF222222),
            keyBackground = Color(0xFF424242),
            keyForeground = Color(0xFFFFFFFF),
            keyBorder = Color(0x00000000),
            keyShadow = Color(0x4D000000),
            actionKeyBackground = Color(0xFF313131),
            actionKeyForeground = Color(0xFFFFFFFF),
            shiftActiveColor = Color(0xFF426DEF),
            accentColor = Color(0xFF426DEF),
            moreKeysBackground = Color(0xFF424242),
            guidelineColor = Color(0xFF426DEF),
            suggestionBackground = Color(0xFF222222),
            suggestionForeground = Color(0xBFFFFFFF),
            keyPressedBackground = Color(0xFF1D1D1D),
            keyPressedForeground = Color(0xFFFFFFFF)
        )

        val AmoledBlack = KeyboardColorScheme(
            containerBackground = Color(0xFF000000),
            keyBackground = Color(0xFF18181B),
            keyForeground = Color(0xFFFFFFFF),
            keyBorder = Color(0xFF222225),
            keyShadow = Color(0x00000000),
            actionKeyBackground = Color(0xFF111113),
            actionKeyForeground = Color(0xFF04294E),
            shiftActiveColor = Color(0xFFA8C7FA),
            accentColor = Color(0xFFA8C7FA),
            moreKeysBackground = Color(0xFF232328),
            guidelineColor = Color(0xFFA8C7FA),
            suggestionBackground = Color(0xFF000000),
            suggestionForeground = Color(0xFFFFFFFF)
        )

        val DefaultLight = KeyboardColorScheme(
            containerBackground = Color(0xFFE9EAEE),
            keyBackground = Color(0xFFFFFFFF),
            keyForeground = Color(0xFF000000),
            keyBorder = Color(0x00000000),
            keyShadow = Color(0x4D000000),
            actionKeyBackground = Color(0xFFDCDFE5),
            actionKeyForeground = Color(0xFFFFFFFF),
            shiftActiveColor = Color(0xFF90CAF9),
            accentColor = Color(0xFF1973EA),
            moreKeysBackground = Color(0xFFFFFFFF),
            guidelineColor = Color(0xFF1973EA),
            suggestionBackground = Color(0xFFE9EAEE),
            suggestionForeground = Color(0xB3000000),
            keyPressedBackground = Color(0xFFC1C8CD),
            keyPressedForeground = Color(0xFF000000)
        )

        val TokyoNight = KeyboardColorScheme(
            containerBackground = Color(0xFF1A1B26),
            keyBackground = Color(0xFF24283B),
            keyForeground = Color(0xFFC0CAF5),
            keyBorder = Color(0xFF292E42),
            keyShadow = Color(0x00000000),
            actionKeyBackground = Color(0xFF1F2335),
            actionKeyForeground = Color(0xFF1A1B26),
            shiftActiveColor = Color(0xFF7AA2F7),
            accentColor = Color(0xFF7AA2F7),
            moreKeysBackground = Color(0xFF2E334B),
            guidelineColor = Color(0xFF7AA2F7),
            suggestionBackground = Color(0xFF1A1B26),
            suggestionForeground = Color(0xFFC0CAF5)
        )

        val Nord = KeyboardColorScheme(
            containerBackground = Color(0xFF2E3440),
            keyBackground = Color(0xFF3B4252),
            keyForeground = Color(0xFFECEFF4),
            keyBorder = Color(0xFF434C5E),
            keyShadow = Color(0x00000000),
            actionKeyBackground = Color(0xFF353B49),
            actionKeyForeground = Color(0xFF2E3440),
            shiftActiveColor = Color(0xFF88C0D0),
            accentColor = Color(0xFF88C0D0),
            moreKeysBackground = Color(0xFF434C5E),
            guidelineColor = Color(0xFF88C0D0),
            suggestionBackground = Color(0xFF2E3440),
            suggestionForeground = Color(0xFFECEFF4)
        )
    }
}

val KeyboardColorScheme.effectiveToolbarBackground: Color
    get() {
        val hasBackdrop = themeSpec?.background?.gradient != null || themeSpec?.background?.image != null
        if (hasBackdrop) {
            val sugSpec = themeSpec?.suggestionBackground
            val isExplicitlySolid = sugSpec?.solidColor != null &&
                !ThemeColorUtils.isTransparent(sugSpec.solidColor) &&
                sugSpec.solidColor != themeSpec.background.solidColor
            if (isExplicitlySolid && suggestionBackground != Color.Unspecified && suggestionBackground.alpha > 0.05f) {
                return suggestionBackground
            }
            return Color.Transparent
        }
        return if (suggestionBackground != Color.Unspecified && suggestionBackground.alpha > 0.05f) {
            suggestionBackground
        } else {
            containerBackground
        }
    }

val KeyboardColorScheme.effectiveToolbarForeground: Color
    get() {
        val hasBackdrop = themeSpec?.background?.gradient != null || themeSpec?.background?.image != null
        val fgCandidate = if (suggestionForeground != Color.Unspecified && suggestionForeground.alpha > 0.05f) {
            suggestionForeground
        } else {
            keyForeground
        }
        if (hasBackdrop) {
            return fgCandidate
        }
        val targetBg = effectiveToolbarBackground
        return if (targetBg.alpha > 0.05f) {
            ThemeColorUtils.ensureContrast(fgCandidate, targetBg)
        } else {
            fgCandidate
        }
    }

val KeyboardColorScheme.effectiveMoreKeysBackground: Color
    get() {
        val hasImageBackdrop = themeSpec?.background?.image != null
        return if (hasImageBackdrop) {
            val isDarkTheme = themeSpec?.isDark ?: ThemeColorUtils.isColorDark(containerBackground)
            val fallback = if (isDarkTheme) Color(0xFF242428) else Color(0xFFFFFFFF)
            ThemeColorUtils.ensureFullOpacity(moreKeysBackground, fallback)
        } else {
            moreKeysBackground
        }
    }


