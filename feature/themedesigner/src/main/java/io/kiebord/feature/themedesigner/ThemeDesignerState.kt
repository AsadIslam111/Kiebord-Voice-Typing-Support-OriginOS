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

/**
 * Curated color & gradient palette preset with 3 preview swatch circles and full key styling.
 */
data class ThemeStudioPreset(
    val id: String,
    val name: String,
    val isGradient: Boolean,
    val circle1: Color,
    val circle2: Color,
    val circle3: Color,
    val applyTo: (ThemeDesignerState) -> ThemeDesignerState
)

object ThemeStudioPresets {

    val SolidPresets: List<ThemeStudioPreset> = listOf(
        ThemeStudioPreset(
            id = "solid_default_dark",
            name = "Default Dark",
            isGradient = false,
            circle1 = Color(0xFF1B1B1F),
            circle2 = Color(0xFF2C2C30),
            circle3 = Color(0xFF4999FD),
            applyTo = { s ->
                ThemeDesignerState.defaultDark(s.id).copy(
                    name = s.name,
                    perKeyOverrides = s.perKeyOverrides,
                    fontPath = s.fontPath,
                    icons = s.icons
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_default_light",
            name = "Default Light",
            isGradient = false,
            circle1 = Color(0xFFF4F6F9),
            circle2 = Color(0xFFFFFFFF),
            circle3 = Color(0xFF2563EB),
            applyTo = { s ->
                ThemeDesignerState.defaultLight(s.id).copy(
                    name = s.name,
                    perKeyOverrides = s.perKeyOverrides,
                    fontPath = s.fontPath,
                    icons = s.icons
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_amoled",
            name = "AMOLED Black",
            isGradient = false,
            circle1 = Color(0xFF000000),
            circle2 = Color(0xFF18181B),
            circle3 = Color(0xFFA8C7FA),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFF000000),
                    keyBackground = Color(0xFF18181B),
                    keyForeground = Color(0xFFFFFFFF),
                    flatKeyForeground = Color(0xFFFFFFFF),
                    keyPressedBackground = Color(0xFF27272A),
                    functionalBackground = Color(0xFF111113),
                    functionalForeground = Color(0xFFD4D4D8),
                    actionBackground = Color(0xFFA8C7FA),
                    actionForeground = Color(0xFF04294E),
                    accentColor = Color(0xFFA8C7FA),
                    shiftActiveColor = Color(0xFFA8C7FA),
                    activeOptionHighlightColor = Color(0xFF38BDF8),
                    suggestionBackground = Color(0xFF000000),
                    suggestionForeground = Color(0xFFFFFFFF),
                    moreKeysBackground = Color(0xFF232328),
                    strokeEnabled = true,
                    strokeColor = Color(0xFF222225),
                    strokeWidthDp = 0.75f,
                    shadowEnabled = false
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_tokyo_night",
            name = "Tokyo Night",
            isGradient = false,
            circle1 = Color(0xFF1A1B26),
            circle2 = Color(0xFF24283B),
            circle3 = Color(0xFF7AA2F7),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFF1A1B26),
                    keyBackground = Color(0xFF24283B),
                    keyForeground = Color(0xFFC0CAF5),
                    flatKeyForeground = Color(0xFFC0CAF5),
                    keyPressedBackground = Color(0xFF333954),
                    functionalBackground = Color(0xFF1F2335),
                    functionalForeground = Color(0xFFA9B1D6),
                    actionBackground = Color(0xFF7AA2F7),
                    actionForeground = Color(0xFF1A1B26),
                    accentColor = Color(0xFF7AA2F7),
                    shiftActiveColor = Color(0xFF7AA2F7),
                    activeOptionHighlightColor = Color(0xFF7DCFFF),
                    suggestionBackground = Color(0xFF1A1B26),
                    suggestionForeground = Color(0xFFC0CAF5),
                    moreKeysBackground = Color(0xFF2E334B),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_nord",
            name = "Nord Frost",
            isGradient = false,
            circle1 = Color(0xFF2E3440),
            circle2 = Color(0xFF3B4252),
            circle3 = Color(0xFF88C0D0),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFF2E3440),
                    keyBackground = Color(0xFF3B4252),
                    keyForeground = Color(0xFFECEFF4),
                    flatKeyForeground = Color(0xFFECEFF4),
                    keyPressedBackground = Color(0xFF4C566A),
                    functionalBackground = Color(0xFF353B49),
                    functionalForeground = Color(0xFFD8DEE9),
                    actionBackground = Color(0xFF88C0D0),
                    actionForeground = Color(0xFF2E3440),
                    accentColor = Color(0xFF88C0D0),
                    shiftActiveColor = Color(0xFF88C0D0),
                    activeOptionHighlightColor = Color(0xFF81A1C1),
                    suggestionBackground = Color(0xFF2E3440),
                    suggestionForeground = Color(0xFFECEFF4),
                    moreKeysBackground = Color(0xFF434C5E),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x40000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_catppuccin",
            name = "Catppuccin",
            isGradient = false,
            circle1 = Color(0xFF1E1E2E),
            circle2 = Color(0xFF313244),
            circle3 = Color(0xFFCBA6F7),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFF1E1E2E),
                    keyBackground = Color(0xFF313244),
                    keyForeground = Color(0xFFCDD6F4),
                    flatKeyForeground = Color(0xFFCDD6F4),
                    keyPressedBackground = Color(0xFF45475A),
                    functionalBackground = Color(0xFF181825),
                    functionalForeground = Color(0xFFBAC2DE),
                    actionBackground = Color(0xFFCBA6F7),
                    actionForeground = Color(0xFF11111B),
                    accentColor = Color(0xFFCBA6F7),
                    shiftActiveColor = Color(0xFFCBA6F7),
                    activeOptionHighlightColor = Color(0xFFF5C2E7),
                    suggestionBackground = Color(0xFF1E1E2E),
                    suggestionForeground = Color(0xFFCDD6F4),
                    moreKeysBackground = Color(0xFF45475A),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_emerald",
            name = "Emerald Pine",
            isGradient = false,
            circle1 = Color(0xFF0F1F1C),
            circle2 = Color(0xFF1B332E),
            circle3 = Color(0xFF10B981),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFF0F1F1C),
                    keyBackground = Color(0xFF1B332E),
                    keyForeground = Color(0xFFECFDF5),
                    flatKeyForeground = Color(0xFFECFDF5),
                    keyPressedBackground = Color(0xFF264740),
                    functionalBackground = Color(0xFF142824),
                    functionalForeground = Color(0xFFA7F3D0),
                    actionBackground = Color(0xFF10B981),
                    actionForeground = Color(0xFF062E22),
                    accentColor = Color(0xFF10B981),
                    shiftActiveColor = Color(0xFF10B981),
                    activeOptionHighlightColor = Color(0xFF34D399),
                    suggestionBackground = Color(0xFF0F1F1C),
                    suggestionForeground = Color(0xFFECFDF5),
                    moreKeysBackground = Color(0xFF264740),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_rose_pine",
            name = "Rose Pine",
            isGradient = false,
            circle1 = Color(0xFF191724),
            circle2 = Color(0xFF26233A),
            circle3 = Color(0xFFEBBCBA),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFF191724),
                    keyBackground = Color(0xFF26233A),
                    keyForeground = Color(0xFFE0DEF4),
                    flatKeyForeground = Color(0xFFE0DEF4),
                    keyPressedBackground = Color(0xFF403D52),
                    functionalBackground = Color(0xFF1F1D2E),
                    functionalForeground = Color(0xFF908CAA),
                    actionBackground = Color(0xFFEBBCBA),
                    actionForeground = Color(0xFF191724),
                    accentColor = Color(0xFFEBBCBA),
                    shiftActiveColor = Color(0xFFEBBCBA),
                    activeOptionHighlightColor = Color(0xFFC4A7E7),
                    suggestionBackground = Color(0xFF191724),
                    suggestionForeground = Color(0xFFE0DEF4),
                    moreKeysBackground = Color(0xFF26233A),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_warm_latte",
            name = "Warm Latte",
            isGradient = false,
            circle1 = Color(0xFFF5F2EB),
            circle2 = Color(0xFFFFFFFF),
            circle3 = Color(0xFFEA580C),
            applyTo = { s ->
                s.copy(
                    isDark = false,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFFF5F2EB),
                    keyBackground = Color(0xFFFFFFFF),
                    keyForeground = Color(0xFF292524),
                    flatKeyForeground = Color(0xFF292524),
                    keyPressedBackground = Color(0xFFE7E5E4),
                    functionalBackground = Color(0xFFE7E2D8),
                    functionalForeground = Color(0xFF44403C),
                    actionBackground = Color(0xFFEA580C),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFFEA580C),
                    shiftActiveColor = Color(0xFFEA580C),
                    activeOptionHighlightColor = Color(0xFFD97706),
                    suggestionBackground = Color(0xFFF5F2EB),
                    suggestionForeground = Color(0xFF292524),
                    moreKeysBackground = Color(0xFFFFFFFF),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x1F000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "solid_mint_breeze",
            name = "Mint Breeze",
            isGradient = false,
            circle1 = Color(0xFFECFDF5),
            circle2 = Color(0xFFFFFFFF),
            circle3 = Color(0xFF059669),
            applyTo = { s ->
                s.copy(
                    isDark = false,
                    backgroundType = DesignerBackgroundType.SOLID,
                    backgroundColor = Color(0xFFECFDF5),
                    keyBackground = Color(0xFFFFFFFF),
                    keyForeground = Color(0xFF064E3B),
                    flatKeyForeground = Color(0xFF064E3B),
                    keyPressedBackground = Color(0xFFD1FAE5),
                    functionalBackground = Color(0xFFD1FAE5),
                    functionalForeground = Color(0xFF065F46),
                    actionBackground = Color(0xFF059669),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFF059669),
                    shiftActiveColor = Color(0xFF059669),
                    activeOptionHighlightColor = Color(0xFF10B981),
                    suggestionBackground = Color(0xFFECFDF5),
                    suggestionForeground = Color(0xFF064E3B),
                    moreKeysBackground = Color(0xFFFFFFFF),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x1A000000)
                )
            }
        )
    )

    val GradientPresets: List<ThemeStudioPreset> = listOf(
        ThemeStudioPreset(
            id = "grad_midnight_aurora",
            name = "Midnight Aurora",
            isGradient = true,
            circle1 = Color(0xFF0F0C29),
            circle2 = Color(0xFF302B63),
            circle3 = Color(0xFF00F5D4),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFF0F0C29),
                    gradientEnd = Color(0xFF302B63),
                    gradientAngle = 135f,
                    keyBackground = Color(0x29FFFFFF),
                    keyForeground = Color(0xFFFFFFFF),
                    flatKeyForeground = Color(0xFFFFFFFF),
                    keyPressedBackground = Color(0x47FFFFFF),
                    functionalBackground = Color(0x1FFFFFFF),
                    functionalForeground = Color(0xFFE2E8F0),
                    actionBackground = Color(0xFF00F5D4),
                    actionForeground = Color(0xFF0B132B),
                    accentColor = Color(0xFF00F5D4),
                    shiftActiveColor = Color(0xFF00F5D4),
                    activeOptionHighlightColor = Color(0xFF38BDF8),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFFFFFFFF),
                    moreKeysBackground = Color(0xFF24204A),
                    strokeEnabled = true,
                    strokeColor = Color(0x38FFFFFF),
                    strokeWidthDp = 0.75f,
                    strokeAlpha = 0.7f,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_oceanic_cyber",
            name = "Oceanic Cyber",
            isGradient = true,
            circle1 = Color(0xFF0F2027),
            circle2 = Color(0xFF2C5364),
            circle3 = Color(0xFF26D0CE),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFF0F2027),
                    gradientEnd = Color(0xFF2C5364),
                    gradientAngle = 90f,
                    keyBackground = Color(0x2BFFFFFF),
                    keyForeground = Color(0xFFF1F5F9),
                    flatKeyForeground = Color(0xFFF1F5F9),
                    keyPressedBackground = Color(0x47FFFFFF),
                    functionalBackground = Color(0x1FFFFFFF),
                    functionalForeground = Color(0xFFCBD5E1),
                    actionBackground = Color(0xFF26D0CE),
                    actionForeground = Color(0xFF082F49),
                    accentColor = Color(0xFF26D0CE),
                    shiftActiveColor = Color(0xFF26D0CE),
                    activeOptionHighlightColor = Color(0xFF38BDF8),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFFF1F5F9),
                    moreKeysBackground = Color(0xFF1A3644),
                    strokeEnabled = true,
                    strokeColor = Color(0x33FFFFFF),
                    strokeWidthDp = 0.75f,
                    strokeAlpha = 0.65f,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_sunset_synth",
            name = "Sunset Synth",
            isGradient = true,
            circle1 = Color(0xFF2B1055),
            circle2 = Color(0xFF7597DE),
            circle3 = Color(0xFFFF6B6B),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFF2B1055),
                    gradientEnd = Color(0xFF7597DE),
                    gradientAngle = 120f,
                    keyBackground = Color(0x2EFFFFFF),
                    keyForeground = Color(0xFFFFFFFF),
                    flatKeyForeground = Color(0xFFFFFFFF),
                    keyPressedBackground = Color(0x4DFFFFFF),
                    functionalBackground = Color(0x21FFFFFF),
                    functionalForeground = Color(0xFFF1F5F9),
                    actionBackground = Color(0xFFFF6B6B),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFFFF6B6B),
                    shiftActiveColor = Color(0xFFFF6B6B),
                    activeOptionHighlightColor = Color(0xFFF472B6),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFFFFFFFF),
                    moreKeysBackground = Color(0xFF361D6E),
                    strokeEnabled = true,
                    strokeColor = Color(0x38FFFFFF),
                    strokeWidthDp = 0.75f,
                    strokeAlpha = 0.7f,
                    shadowEnabled = true,
                    shadowColor = Color(0x40000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_crimson_ember",
            name = "Crimson Ember",
            isGradient = true,
            circle1 = Color(0xFF1F0808),
            circle2 = Color(0xFF5C162E),
            circle3 = Color(0xFFF43F5E),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFF1F0808),
                    gradientEnd = Color(0xFF5C162E),
                    gradientAngle = 110f,
                    keyBackground = Color(0x29FFFFFF),
                    keyForeground = Color(0xFFFFF1F2),
                    flatKeyForeground = Color(0xFFFFF1F2),
                    keyPressedBackground = Color(0x45FFFFFF),
                    functionalBackground = Color(0x1FFFFFFF),
                    functionalForeground = Color(0xFFFECDD3),
                    actionBackground = Color(0xFFF43F5E),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFFF43F5E),
                    shiftActiveColor = Color(0xFFF43F5E),
                    activeOptionHighlightColor = Color(0xFFFB7185),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFFFFF1F2),
                    moreKeysBackground = Color(0xFF3F1020),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_emerald_nebula",
            name = "Emerald Nebula",
            isGradient = true,
            circle1 = Color(0xFF051923),
            circle2 = Color(0xFF006466),
            circle3 = Color(0xFF34D399),
            applyTo = { s ->
                s.copy(
                    isDark = true,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFF051923),
                    gradientEnd = Color(0xFF006466),
                    gradientAngle = 135f,
                    keyBackground = Color(0x29FFFFFF),
                    keyForeground = Color(0xFFECFDF5),
                    flatKeyForeground = Color(0xFFECFDF5),
                    keyPressedBackground = Color(0x45FFFFFF),
                    functionalBackground = Color(0x1CFFFFFF),
                    functionalForeground = Color(0xFFA7F3D0),
                    actionBackground = Color(0xFF34D399),
                    actionForeground = Color(0xFF042F2E),
                    accentColor = Color(0xFF34D399),
                    shiftActiveColor = Color(0xFF34D399),
                    activeOptionHighlightColor = Color(0xFF2DD4BF),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFFECFDF5),
                    moreKeysBackground = Color(0xFF06373A),
                    strokeEnabled = true,
                    strokeColor = Color(0x33FFFFFF),
                    strokeWidthDp = 0.75f,
                    strokeAlpha = 0.6f,
                    shadowEnabled = true,
                    shadowColor = Color(0x4D000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_sky_blossom",
            name = "Sky Blossom",
            isGradient = true,
            circle1 = Color(0xFFE0EAFC),
            circle2 = Color(0xFFFCE7F3),
            circle3 = Color(0xFF6366F1),
            applyTo = { s ->
                s.copy(
                    isDark = false,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFFE0EAFC),
                    gradientEnd = Color(0xFFFCE7F3),
                    gradientAngle = 120f,
                    keyBackground = Color(0xD9FFFFFF),
                    keyForeground = Color(0xFF1E1B4B),
                    flatKeyForeground = Color(0xFF1E1B4B),
                    keyPressedBackground = Color(0xFFE0E7FF),
                    functionalBackground = Color(0xB3FFFFFF),
                    functionalForeground = Color(0xFF312E81),
                    actionBackground = Color(0xFF6366F1),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFF6366F1),
                    shiftActiveColor = Color(0xFF6366F1),
                    activeOptionHighlightColor = Color(0xFF8B5CF6),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFF1E1B4B),
                    moreKeysBackground = Color(0xFFFFFFFF),
                    strokeEnabled = true,
                    strokeColor = Color(0x66FFFFFF),
                    strokeWidthDp = 0.75f,
                    strokeAlpha = 0.8f,
                    shadowEnabled = true,
                    shadowColor = Color(0x1F000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_peach_sorbet",
            name = "Peach Sorbet",
            isGradient = true,
            circle1 = Color(0xFFFFEDD5),
            circle2 = Color(0xFFFECDD3),
            circle3 = Color(0xFFF43F5E),
            applyTo = { s ->
                s.copy(
                    isDark = false,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFFFFEDD5),
                    gradientEnd = Color(0xFFFECDD3),
                    gradientAngle = 115f,
                    keyBackground = Color(0xD9FFFFFF),
                    keyForeground = Color(0xFF4C0519),
                    flatKeyForeground = Color(0xFF4C0519),
                    keyPressedBackground = Color(0xFFFFE4E6),
                    functionalBackground = Color(0xB3FFFFFF),
                    functionalForeground = Color(0xFF881337),
                    actionBackground = Color(0xFFF43F5E),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFFF43F5E),
                    shiftActiveColor = Color(0xFFF43F5E),
                    activeOptionHighlightColor = Color(0xFFEA580C),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFF4C0519),
                    moreKeysBackground = Color(0xFFFFFFFF),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x1F000000)
                )
            }
        ),
        ThemeStudioPreset(
            id = "grad_glacier_mist",
            name = "Glacier Mist",
            isGradient = true,
            circle1 = Color(0xFFE0F2FE),
            circle2 = Color(0xFFCCFBF1),
            circle3 = Color(0xFF0284C7),
            applyTo = { s ->
                s.copy(
                    isDark = false,
                    backgroundType = DesignerBackgroundType.GRADIENT,
                    gradientType = DesignerGradientType.LINEAR,
                    gradientStart = Color(0xFFE0F2FE),
                    gradientEnd = Color(0xFFCCFBF1),
                    gradientAngle = 100f,
                    keyBackground = Color(0xD9FFFFFF),
                    keyForeground = Color(0xFF0C4A6E),
                    flatKeyForeground = Color(0xFF0C4A6E),
                    keyPressedBackground = Color(0xFFBAE6FD),
                    functionalBackground = Color(0xB3FFFFFF),
                    functionalForeground = Color(0xFF075985),
                    actionBackground = Color(0xFF0284C7),
                    actionForeground = Color(0xFFFFFFFF),
                    accentColor = Color(0xFF0284C7),
                    shiftActiveColor = Color(0xFF0284C7),
                    activeOptionHighlightColor = Color(0xFF0D9488),
                    suggestionBackground = Color(0x00000000),
                    suggestionForeground = Color(0xFF0C4A6E),
                    moreKeysBackground = Color(0xFFFFFFFF),
                    strokeEnabled = false,
                    shadowEnabled = true,
                    shadowColor = Color(0x1A000000)
                )
            }
        )
    )
}
