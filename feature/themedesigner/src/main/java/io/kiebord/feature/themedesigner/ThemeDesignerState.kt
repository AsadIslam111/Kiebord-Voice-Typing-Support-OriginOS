package io.kiebord.feature.themedesigner

import androidx.compose.ui.graphics.Color
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.theme.GradientSpec
import io.kiebord.theme.GradientType
import io.kiebord.theme.ImageBackgroundSpec
import io.kiebord.theme.ImageScaleType
import io.kiebord.theme.KeyboardThemeSpec
import io.kiebord.theme.KeycapStyleSpec
import io.kiebord.theme.ShadowSpec
import io.kiebord.theme.StrokeSpec
import io.kiebord.theme.ThemeBackgroundSpec
import io.kiebord.theme.ThemeColorUtils
import kotlin.math.roundToInt

enum class DesignerBackgroundType { SOLID, GRADIENT, IMAGE }

enum class DesignerGradientType { LINEAR, RADIAL }

/**
 * Per-key color override for an individual key (identified by lowercase label e.g. `"q"`, `","`
 * or `KeyType.name` e.g. `"SHIFT"`, `"DELETE"`, `"SYMBOLS"`, `"ACTION"`, `"SPACE"`, `"SWITCH_LANGUAGE"`).
 */
data class PerKeyColorStyle(
    val background: Color? = null,
    val foreground: Color? = null,
    val pressedBackground: Color? = null
) {
    val isEmpty: Boolean
        get() = background == null && foreground == null && pressedBackground == null
}

/**
 * Immutable, UI-agnostic state of the Theme Designer.
 *
 * Every field maps 1:1 to a `.bornotheme` v2 ([KeyboardThemeSpec]) property, so the live preview
 * and the saved file are always produced by the same [toSpec] function.
 */
data class ThemeDesignerState(
    val id: String,
    val name: String = "My Theme",
    val isDark: Boolean = true,

    // Surface & background
    val backgroundType: DesignerBackgroundType = DesignerBackgroundType.SOLID,
    val backgroundColor: Color = Color(0xFF1B1B1F),
    val gradientType: DesignerGradientType = DesignerGradientType.LINEAR,
    val gradientStart: Color = Color(0xFF0F0C29),
    val gradientEnd: Color = Color(0xFF302B63),
    val gradientAngle: Float = 90f,
    val imagePath: String? = null,
    val imageBlurDp: Float = 0f,
    val imageDim: Float = 0.25f,
    val imageScaleType: ImageScaleType = ImageScaleType.CROP,

    // Character keys
    val keyBackground: Color = Color(0xFF2C2C30),
    val keyForeground: Color = Color(0xFFE3E2E6),
    val flatKeyForeground: Color = Color(0xFFE3E2E6),
    val keyPressedBackground: Color = Color(0xFF3A3A40),
    val keyCornerRadiusDp: Float = 8f,

    // Spacebar customization
    val spaceCustomEnabled: Boolean = false,
    val spaceBackground: Color = Color(0xFF2C2C30),
    val spaceForeground: Color = Color(0xFFE3E2E6),
    val spacePressedBackground: Color = Color(0xFF3A3A40),
    val spaceIsPill: Boolean = false,
    val spaceCornerRadiusDp: Float = 8f,

    // Modifier & action keys
    val functionalBackground: Color = Color(0xFF242428),
    val functionalForeground: Color = Color(0xFFC7C9D1),
    val actionBackground: Color = Color(0xFF4999FD),
    val actionForeground: Color = Color(0xFFFFFFFF),
    val actionIsPill: Boolean = true,

    // Per-key color overrides
    val perKeyOverrides: Map<String, PerKeyColorStyle> = emptyMap(),

    // Borders & shadows
    val strokeEnabled: Boolean = false,
    val strokeColor: Color = Color(0x33FFFFFF),
    val strokeWidthDp: Float = 0.75f,
    val strokeAlpha: Float = 1f,
    val shadowEnabled: Boolean = true,
    val shadowColor: Color = Color(0x4D000000),
    val shadowElevationDp: Float = 1.5f,

    // Accents & overlays
    val accentColor: Color = Color(0xFF4999FD),
    val shiftActiveColor: Color = Color(0xFF4999FD),
    val activeOptionHighlightColor: Color = Color(0xFF06B6D4),
    val suggestionBackground: Color = Color(0xFF1B1B1F),
    val suggestionForeground: Color = Color(0xFFE3E2E6),
    val moreKeysBackground: Color = Color(0xFF38383F),

    // Underlying preserved assets (supported in .bornotheme packages, not exposed as UI settings)
    val fontPath: String? = null,
    val icons: io.kiebord.theme.ThemeIconsSpec = io.kiebord.theme.ThemeIconsSpec()
) {

    fun baseDefault(): ThemeDesignerState =
        if (isDark) defaultDark(id) else defaultLight(id)

    fun resetSurfaceSection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            backgroundType = def.backgroundType,
            backgroundColor = def.backgroundColor,
            gradientType = def.gradientType,
            gradientStart = def.gradientStart,
            gradientEnd = def.gradientEnd,
            gradientAngle = def.gradientAngle,
            imagePath = null,
            imageBlurDp = def.imageBlurDp,
            imageDim = def.imageDim,
            imageScaleType = def.imageScaleType
        )
    }

    fun resetCharacterKeysSection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            keyBackground = def.keyBackground,
            keyForeground = def.keyForeground,
            flatKeyForeground = def.flatKeyForeground,
            keyPressedBackground = def.keyPressedBackground,
            keyCornerRadiusDp = def.keyCornerRadiusDp
        )
    }

    fun resetSpaceKeySection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            spaceCustomEnabled = false,
            spaceBackground = def.spaceBackground,
            spaceForeground = def.spaceForeground,
            spacePressedBackground = def.spacePressedBackground,
            spaceIsPill = def.spaceIsPill,
            spaceCornerRadiusDp = def.spaceCornerRadiusDp
        )
    }

    fun resetModifiersSection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            functionalBackground = def.functionalBackground,
            functionalForeground = def.functionalForeground,
            shiftActiveColor = def.shiftActiveColor
        )
    }

    fun resetActionKeySection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            actionBackground = def.actionBackground,
            actionForeground = def.actionForeground,
            actionIsPill = def.actionIsPill
        )
    }

    fun resetModifiersActionSection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            functionalBackground = def.functionalBackground,
            functionalForeground = def.functionalForeground,
            actionBackground = def.actionBackground,
            actionForeground = def.actionForeground,
            actionIsPill = def.actionIsPill
        )
    }

    fun resetBordersShadowsSection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            strokeEnabled = def.strokeEnabled,
            strokeColor = def.strokeColor,
            strokeWidthDp = def.strokeWidthDp,
            strokeAlpha = def.strokeAlpha,
            shadowEnabled = def.shadowEnabled,
            shadowColor = def.shadowColor,
            shadowElevationDp = def.shadowElevationDp
        )
    }

    fun resetAccentsOverlaysSection(): ThemeDesignerState {
        val def = baseDefault()
        return copy(
            accentColor = def.accentColor,
            shiftActiveColor = def.shiftActiveColor,
            activeOptionHighlightColor = def.activeOptionHighlightColor,
            suggestionBackground = def.suggestionBackground,
            suggestionForeground = def.suggestionForeground,
            moreKeysBackground = def.moreKeysBackground
        )
    }

    fun resetPerKeySection(): ThemeDesignerState =
        copy(perKeyOverrides = emptyMap())

    fun resetTypographySection(): ThemeDesignerState =
        copy(fontPath = null)

    fun toSpec(): KeyboardThemeSpec {
        val hex = ThemeColorUtils::colorToHex

        val background = when (backgroundType) {
            DesignerBackgroundType.SOLID -> ThemeBackgroundSpec(solidColor = hex(backgroundColor))
            DesignerBackgroundType.GRADIENT -> ThemeBackgroundSpec(
                solidColor = hex(gradientStart),
                gradient = GradientSpec(
                    type = if (gradientType == DesignerGradientType.RADIAL) GradientType.RADIAL else GradientType.LINEAR,
                    colors = listOf(hex(gradientStart), hex(gradientEnd)),
                    angle = gradientAngle
                )
            )
            DesignerBackgroundType.IMAGE -> {
                val path = imagePath
                if (path.isNullOrBlank()) {
                    ThemeBackgroundSpec(solidColor = hex(backgroundColor))
                } else {
                    val dimAlpha = (imageDim.coerceIn(0f, 1f) * 255f).roundToInt()
                    ThemeBackgroundSpec(
                        solidColor = hex(backgroundColor),
                        image = ImageBackgroundSpec(
                            imagePath = path,
                            scaleType = imageScaleType,
                            dimOverlayColor = if (dimAlpha > 0) String.format("#%02X000000", dimAlpha) else null,
                            blurRadiusDp = imageBlurDp
                        )
                    )
                }
            }
        }

        val stroke = if (strokeEnabled) StrokeSpec(hex(strokeColor), strokeWidthDp, strokeAlpha) else null
        val shadow = if (shadowEnabled) ShadowSpec(hex(shadowColor), shadowElevationDp) else null

        val spaceSpec = if (spaceCustomEnabled) {
            KeycapStyleSpec(
                background = hex(spaceBackground),
                foreground = hex(spaceForeground),
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = spaceCornerRadiusDp,
                isPill = spaceIsPill,
                pressedBackground = hex(spacePressedBackground),
                flatForeground = hex(spaceForeground)
            )
        } else null

        val serializedOverrides = perKeyOverrides
            .filterValues { !it.isEmpty }
            .mapValues { (_, style) ->
                KeycapStyleSpec(
                    background = style.background?.let { hex(it) },
                    foreground = style.foreground?.let { hex(it) },
                    pressedBackground = style.pressedBackground?.let { hex(it) },
                    flatForeground = style.foreground?.let { hex(it) }
                )
            }

        return KeyboardThemeSpec(
            schemaVersion = 2,
            id = id,
            name = name.ifBlank { "My Theme" },
            author = "Kiebord Theme Studio",
            isDark = isDark,
            background = background,
            characterKey = KeycapStyleSpec(
                background = hex(keyBackground),
                foreground = hex(keyForeground),
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = keyCornerRadiusDp,
                pressedBackground = hex(keyPressedBackground),
                flatForeground = hex(flatKeyForeground)
            ),
            functionalKey = KeycapStyleSpec(
                background = hex(functionalBackground),
                foreground = hex(functionalForeground),
                stroke = stroke,
                shadow = shadow,
                cornerRadiusDp = keyCornerRadiusDp,
                pressedBackground = hex(keyPressedBackground)
            ),
            actionKey = KeycapStyleSpec(
                background = hex(actionBackground),
                foreground = hex(actionForeground),
                shadow = shadow,
                cornerRadiusDp = keyCornerRadiusDp,
                isPill = actionIsPill
            ),
            spaceKey = spaceSpec,
            accentColor = hex(accentColor),
            shiftActiveColor = hex(shiftActiveColor),
            suggestionBackground = ThemeBackgroundSpec(solidColor = hex(suggestionBackground)),
            suggestionForeground = hex(suggestionForeground),
            moreKeysBackground = hex(moreKeysBackground),
            guidelineColor = hex(accentColor),
            flatKeyLabelColor = hex(flatKeyForeground),
            activeOptionHighlightColor = hex(activeOptionHighlightColor),
            fontPath = fontPath,
            icons = icons,
            perKeyOverrides = serializedOverrides
        )
    }

    companion object {

        fun newId(): String = "custom_studio_${System.currentTimeMillis()}"

        /**
         * Maps a tapped [KeyDef] on the live keyboard preview to its canonical key in [perKeyOverrides].
         */
        fun overrideKeyFor(key: KeyDef): String {
            return when (key.type) {
                KeyType.CHARACTER -> key.label.lowercase().trim().ifEmpty { key.type.name }
                else -> key.type.name
            }
        }

        /**
         * Friendly display label for an override key in the UI.
         */
        fun displayNameForOverrideKey(overrideKey: String): String {
            return when (overrideKey) {
                KeyType.SHIFT.name -> "Shift"
                KeyType.DELETE.name -> "Delete"
                KeyType.SYMBOLS.name -> "?123 Symbols"
                KeyType.SPACE.name -> "Spacebar"
                KeyType.ACTION.name -> "Enter / Action"
                KeyType.SWITCH_LANGUAGE.name -> "Globe"
                KeyType.MORE_KEYS.name -> "More Keys"
                "," -> "Comma"
                "." -> "Period"
                else -> if (overrideKey.length == 1) {
                    "Key \"${overrideKey.uppercase()}\""
                } else {
                    "Key \"$overrideKey\""
                }
            }
        }

        fun defaultDark(id: String = newId()) = ThemeDesignerState(id = id)

        fun defaultLight(id: String = newId()) = ThemeDesignerState(
            id = id,
            isDark = false,
            backgroundColor = Color(0xFFF4F6F9),
            gradientStart = Color(0xFFE0EAFC),
            gradientEnd = Color(0xFFCFDEF3),
            keyBackground = Color(0xFFFFFFFF),
            keyForeground = Color(0xFF1C1C1E),
            flatKeyForeground = Color(0xFF18191B),
            keyPressedBackground = Color(0xFFE5E9F0),
            spaceBackground = Color(0xFFFFFFFF),
            spaceForeground = Color(0xFF1C1C1E),
            spacePressedBackground = Color(0xFFE5E9F0),
            functionalBackground = Color(0xFFEAECEF),
            functionalForeground = Color(0xFF1C1C1E),
            actionBackground = Color(0xFF2563EB),
            actionForeground = Color(0xFFFFFFFF),
            strokeColor = Color(0xFFE2E6EC),
            shadowColor = Color(0x14000000),
            accentColor = Color(0xFF2563EB),
            shiftActiveColor = Color(0xFF2563EB),
            activeOptionHighlightColor = Color(0xFF0284C7),
            suggestionBackground = Color(0xFFF4F6F9),
            suggestionForeground = Color(0xFF1C1C1E),
            moreKeysBackground = Color(0xFFFFFFFF)
        )

        /** Builds an editable state from an existing theme (used by "Edit in Theme Studio"). */
        fun fromSpec(spec: KeyboardThemeSpec): ThemeDesignerState {
            val base = if (spec.isDark) defaultDark(spec.id) else defaultLight(spec.id)
            fun c(hex: String?, fallback: Color): Color =
                if (hex.isNullOrBlank()) fallback else ThemeColorUtils.parseHexColor(hex, fallback)
            fun cOrNull(hex: String?): Color? =
                if (hex.isNullOrBlank()) null else ThemeColorUtils.parseHexColor(hex, Color.Unspecified).takeIf { it != Color.Unspecified }

            val bg = spec.background
            val gradient = bg.gradient
            val image = bg.image
            val bgType = when {
                image != null -> DesignerBackgroundType.IMAGE
                gradient != null && gradient.colors.size >= 2 -> DesignerBackgroundType.GRADIENT
                else -> DesignerBackgroundType.SOLID
            }
            val dim = image?.dimOverlayColor?.let { ThemeColorUtils.parseHexColor(it, Color.Transparent).alpha } ?: 0f
            val ch = spec.characterKey
            val fn = spec.functionalKey
            val act = spec.actionKey
            val sp = spec.spaceKey

            val charBg = c(ch.background, base.keyBackground)
            val charFg = c(ch.foreground, base.keyForeground)
            val charPressedBg = c(ch.pressedBackground, base.keyPressedBackground)
            val charRadius = ch.cornerRadiusDp ?: base.keyCornerRadiusDp

            val parsedOverrides = spec.perKeyOverrides.mapNotNull { (k, v) ->
                val style = PerKeyColorStyle(
                    background = cOrNull(v.background),
                    foreground = cOrNull(v.foreground),
                    pressedBackground = cOrNull(v.pressedBackground)
                )
                if (style.isEmpty) null else k to style
            }.toMap()

            return base.copy(
                name = spec.name,
                isDark = spec.isDark,
                backgroundType = bgType,
                backgroundColor = c(bg.solidColor ?: gradient?.colors?.firstOrNull(), base.backgroundColor),
                gradientType = if (gradient?.type == GradientType.RADIAL) DesignerGradientType.RADIAL else DesignerGradientType.LINEAR,
                gradientStart = c(gradient?.colors?.getOrNull(0), base.gradientStart),
                gradientEnd = c(gradient?.colors?.lastOrNull(), base.gradientEnd),
                gradientAngle = gradient?.angle ?: base.gradientAngle,
                imagePath = image?.imagePath,
                imageBlurDp = image?.blurRadiusDp ?: 0f,
                imageDim = dim,
                imageScaleType = image?.scaleType ?: ImageScaleType.CROP,
                keyBackground = charBg,
                keyForeground = charFg,
                flatKeyForeground = c(spec.flatKeyLabelColor ?: ch.flatForeground ?: ch.foreground, base.flatKeyForeground),
                keyPressedBackground = charPressedBg,
                keyCornerRadiusDp = charRadius,
                spaceCustomEnabled = sp != null,
                spaceBackground = c(sp?.background, charBg),
                spaceForeground = c(sp?.foreground, charFg),
                spacePressedBackground = c(sp?.pressedBackground, charPressedBg),
                spaceIsPill = sp?.isPill ?: false,
                spaceCornerRadiusDp = sp?.cornerRadiusDp ?: charRadius,
                functionalBackground = c(fn.background, base.functionalBackground),
                functionalForeground = c(fn.foreground, base.functionalForeground),
                actionBackground = c(act.background ?: spec.accentColor, base.actionBackground),
                actionForeground = c(act.foreground, base.actionForeground),
                actionIsPill = act.isPill ?: base.actionIsPill,
                perKeyOverrides = parsedOverrides,
                strokeEnabled = ch.stroke != null && ch.stroke!!.widthDp > 0f,
                strokeColor = c(ch.stroke?.color, base.strokeColor),
                strokeWidthDp = ch.stroke?.widthDp ?: base.strokeWidthDp,
                strokeAlpha = ch.stroke?.alpha ?: base.strokeAlpha,
                shadowEnabled = ch.shadow != null && ch.shadow!!.elevationDp > 0f,
                shadowColor = c(ch.shadow?.color, base.shadowColor),
                shadowElevationDp = ch.shadow?.elevationDp ?: base.shadowElevationDp,
                accentColor = c(spec.accentColor, base.accentColor),
                shiftActiveColor = c(spec.shiftActiveColor, base.shiftActiveColor),
                activeOptionHighlightColor = c(spec.activeOptionHighlightColor ?: spec.accentColor, base.activeOptionHighlightColor),
                suggestionBackground = c(spec.suggestionBackground?.solidColor, base.suggestionBackground),
                suggestionForeground = c(spec.suggestionForeground, base.suggestionForeground),
                moreKeysBackground = c(spec.moreKeysBackground, base.moreKeysBackground),
                fontPath = spec.fontPath,
                icons = spec.icons
            )
        }
    }
}
