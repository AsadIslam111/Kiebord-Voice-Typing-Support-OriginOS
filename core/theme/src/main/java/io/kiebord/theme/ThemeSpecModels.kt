package io.kiebord.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.ShiftState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gradient type for background and key styling.
 * Supports linear, radial, sweep (angular), and mesh gradients.
 */
@Serializable
enum class GradientType {
    @SerialName("linear")
    LINEAR,
    @SerialName("radial")
    RADIAL,
    @SerialName("sweep")
    SWEEP,
    @SerialName("mesh")
    MESH
}

/**
 * Control point coordinate and color for mesh gradients.
 */
@Serializable
data class MeshPointSpec(
    val x: Float, // Normalized 0.0f..1.0f horizontal coordinate
    val y: Float, // Normalized 0.0f..1.0f vertical coordinate
    val color: String // Hex string e.g. "#FF6200EE" or "#80FFFFFF"
)

/**
 * Specification for gradients: linear, radial, sweep, and mesh.
 */
@Serializable
data class GradientSpec(
    val type: GradientType = GradientType.LINEAR,
    val colors: List<String> = emptyList(),
    val stops: List<Float>? = null,
    val angle: Float = 90f, // Degrees: 0 = L-to-R, 90 = T-to-B, 180 = R-to-L, 270 = B-to-T
    val centerX: Float = 0.5f, // Normalized 0..1 center X for radial/sweep
    val centerY: Float = 0.5f, // Normalized 0..1 center Y for radial/sweep
    val radius: Float = 0.75f, // Normalized radius relative to size
    val meshPoints: List<MeshPointSpec>? = null
) {
    fun toBrush(size: Size): Brush? {
        val parsedColors = colors.map { ThemeColorUtils.parseHexColor(it, Color.Transparent) }
        val effectiveColors = if (parsedColors.isEmpty() && !meshPoints.isNullOrEmpty()) {
            meshPoints.map { ThemeColorUtils.parseHexColor(it.color, Color.Transparent) }
        } else {
            parsedColors
        }
        if (effectiveColors.isEmpty()) return null
        if (effectiveColors.size == 1) {
            return Brush.linearGradient(listOf(effectiveColors.first(), effectiveColors.first()))
        }

        val w = if (size.width > 0f) size.width else 1080f
        val h = if (size.height > 0f) size.height else 600f

        return when (type) {
            GradientType.LINEAR -> {
                val rad = (angle * PI / 180.0).toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val dx = cos(rad) * (w / 2f)
                val dy = sin(rad) * (h / 2f)
                val start = Offset(cx - dx, cy - dy)
                val end = Offset(cx + dx, cy + dy)
                if (stops != null && stops.size == effectiveColors.size) {
                    val colorStops = stops.zip(effectiveColors).map { Pair(it.first, it.second) }.toTypedArray()
                    Brush.linearGradient(*colorStops, start = start, end = end)
                } else {
                    Brush.linearGradient(effectiveColors, start = start, end = end)
                }
            }
            GradientType.RADIAL -> {
                val center = Offset(w * centerX.coerceIn(0f, 1f), h * centerY.coerceIn(0f, 1f))
                val radiusPx = maxOf(w, h) * radius.coerceAtLeast(0.01f)
                if (stops != null && stops.size == effectiveColors.size) {
                    val colorStops = stops.zip(effectiveColors).map { Pair(it.first, it.second) }.toTypedArray()
                    Brush.radialGradient(*colorStops, center = center, radius = radiusPx)
                } else {
                    Brush.radialGradient(effectiveColors, center = center, radius = radiusPx)
                }
            }
            GradientType.SWEEP -> {
                val center = Offset(w * centerX.coerceIn(0f, 1f), h * centerY.coerceIn(0f, 1f))
                if (stops != null && stops.size == effectiveColors.size) {
                    val colorStops = stops.zip(effectiveColors).map { Pair(it.first, it.second) }.toTypedArray()
                    Brush.sweepGradient(*colorStops, center = center)
                } else {
                    Brush.sweepGradient(effectiveColors, center = center)
                }
            }
            GradientType.MESH -> {
                val center = if (!meshPoints.isNullOrEmpty()) {
                    val avgX = meshPoints.map { it.x }.average().toFloat()
                    val avgY = meshPoints.map { it.y }.average().toFloat()
                    Offset(w * avgX, h * avgY)
                } else {
                    Offset(w * centerX, h * centerY)
                }
                val meshColors = if (!meshPoints.isNullOrEmpty()) {
                    meshPoints.map { ThemeColorUtils.parseHexColor(it.color, Color.Transparent) }
                } else {
                    effectiveColors
                }
                Brush.sweepGradient(meshColors, center = center)
            }
        }
    }
}

/**
 * Image scale type for background images.
 */
@Serializable
enum class ImageScaleType {
    @SerialName("crop")
    CROP,
    @SerialName("fit")
    FIT,
    @SerialName("fill")
    FILL,
    @SerialName("tile")
    TILE
}

/**
 * Image background configuration: image path/URI, scaling, opacity, dimming, and blur.
 */
@Serializable
data class ImageBackgroundSpec(
    val imagePath: String, // URI string, file path, asset path, or drawable name
    val scaleType: ImageScaleType = ImageScaleType.CROP,
    val alpha: Float = 1.0f,
    val dimOverlayColor: String? = null, // e.g. "#80000000" for 50% dark overlay
    val blurRadiusDp: Float = 0f
)

/**
 * Background specification supporting solid color, gradient, or image background.
 */
@Serializable
data class ThemeBackgroundSpec(
    val solidColor: String? = null,
    val gradient: GradientSpec? = null,
    val image: ImageBackgroundSpec? = null
) {
    fun toBrush(size: Size): Brush? {
        gradient?.toBrush(size)?.let { return it }
        solidColor?.let {
            val color = ThemeColorUtils.parseHexColor(it, Color.Unspecified)
            if (color != Color.Unspecified) {
                return Brush.linearGradient(listOf(color, color))
            }
        }
        return null
    }

    fun toSolidColor(default: Color = Color.Unspecified): Color {
        solidColor?.let { return ThemeColorUtils.parseHexColor(it, default) }
        gradient?.colors?.firstOrNull()?.let { return ThemeColorUtils.parseHexColor(it, default) }
        return default
    }
}

/**
 * Keycap border/stroke specification.
 */
@Serializable
data class StrokeSpec(
    val color: String = "#00000000",
    val widthDp: Float = 1.0f,
    val alpha: Float = 1.0f
)

/**
 * Keycap shadow/elevation specification.
 */
@Serializable
data class ShadowSpec(
    val color: String = "#00000000",
    val elevationDp: Float = 0.0f,
    val radiusDp: Float = 0.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f
)

/**
 * Key skin specification for 9-patch or PNG drawable skins.
 */
@Serializable
data class KeySkinSpec(
    val normalSkin: String, // Resource name or asset path
    val pressedSkin: String? = null,
    val isNinePatch: Boolean = false
)

/**
 * Full keycap style specification supporting colors, gradients, stroke, shadow,
 * corner radius, semi-transparent alpha, 9-patch skin, and pressed states.
 */
@Serializable
data class KeycapStyleSpec(
    val background: String? = null,
    val gradient: GradientSpec? = null,
    val foreground: String? = null,
    val stroke: StrokeSpec? = null,
    val shadow: ShadowSpec? = null,
    val cornerRadiusDp: Float? = null,
    val isPill: Boolean? = null,
    val alpha: Float = 1.0f,
    val skin: KeySkinSpec? = null,
    val pressedBackground: String? = null,
    val pressedForeground: String? = null,
    val flatForeground: String? = null
) {
    fun merge(override: KeycapStyleSpec?): KeycapStyleSpec {
        if (override == null) return this
        return KeycapStyleSpec(
            background = override.background ?: this.background,
            gradient = override.gradient ?: this.gradient,
            foreground = override.foreground ?: this.foreground,
            stroke = override.stroke ?: this.stroke,
            shadow = override.shadow ?: this.shadow,
            cornerRadiusDp = override.cornerRadiusDp ?: this.cornerRadiusDp,
            isPill = override.isPill ?: this.isPill,
            alpha = if (override.alpha != 1.0f) override.alpha else this.alpha,
            skin = override.skin ?: this.skin,
            pressedBackground = override.pressedBackground ?: this.pressedBackground,
            pressedForeground = override.pressedForeground ?: this.pressedForeground,
            flatForeground = override.flatForeground ?: this.flatForeground
        )
    }
}

/**
 * Custom icon overrides for keyboard action and functional keys.
 */
@Serializable
data class ThemeIconsSpec(
    val shift: String? = null,
    val shiftOneShot: String? = null,
    val capsLock: String? = null,
    val backspace: String? = null,
    val action: String? = null,
    val language: String? = null,
    val emoji: String? = null,
    val settings: String? = null,
    val voice: String? = null,
    val search: String? = null,
    val moreKeys: String? = null
)

/**
 * Master keyboard theme definition supporting gradients, image backgrounds,
 * custom strokes, shadows, 9-patch skins, custom icons, and per-key overrides.
 *
 * This represents Version 2 of the unified `.bornotheme` format (`schemaVersion = 2`),
 * retaining all rich native Kiebord styling capabilities.
 */
@Serializable
data class KeyboardThemeSpec(
    val schemaVersion: Int = 2,
    val id: String,
    val name: String,
    val author: String = "Kiebord",
    val version: Int = 1,
    val isDark: Boolean = true,
    val background: ThemeBackgroundSpec,
    val characterKey: KeycapStyleSpec = KeycapStyleSpec(),
    val functionalKey: KeycapStyleSpec = KeycapStyleSpec(),
    val actionKey: KeycapStyleSpec = KeycapStyleSpec(),
    val spaceKey: KeycapStyleSpec? = null,
    val accentColor: String = "#A8C7FA",
    val shiftActiveColor: String = accentColor,
    val suggestionBackground: ThemeBackgroundSpec? = null,
    val suggestionForeground: String = "#E3E2E6",
    val moreKeysBackground: String = "#38383F",
    val guidelineColor: String = accentColor,
    val flatKeyLabelColor: String? = null,
    val activeOptionHighlightColor: String? = null,
    val fontPath: String? = null,
    val icons: ThemeIconsSpec = ThemeIconsSpec(),
    val perKeyOverrides: Map<String, KeycapStyleSpec> = emptyMap(),
    val backgroundImageBase64: String? = null
) {
    /**
     * Resolves the styling for an individual key by checking:
     * 1. Coordinate index override ("rowIndex_keyIndex", e.g. "3_4")
     * 2. Key label override (e.g. "space", "enter", ",")
     * 3. Key type override (e.g. "SPACE", "ACTION", "SHIFT")
     * 4. Base key type style (characterKey, functionalKey, actionKey, spaceKey)
     */
    fun resolveKeycapStyle(
        rowIndex: Int,
        keyIndex: Int,
        key: KeyDef
    ): KeycapStyleSpec {
        val base = when (key.type) {
            KeyType.ACTION -> actionKey
            KeyType.SPACE -> spaceKey ?: characterKey
            KeyType.SHIFT,
            KeyType.DELETE,
            KeyType.SYMBOLS,
            KeyType.SWITCH_LANGUAGE,
            KeyType.MORE_KEYS -> functionalKey
            KeyType.CHARACTER -> {
                val isComma = key.label == "," || key.label == "،"
                if (isComma) functionalKey else characterKey
            }
        }

        val typeOverride = perKeyOverrides[key.type.name]
        val labelKey = key.label.lowercase().trim()
        val labelOverride = if (labelKey.isNotEmpty()) perKeyOverrides[labelKey] else null
        val coordKey = "${rowIndex}_${keyIndex}"
        val coordOverride = perKeyOverrides[coordKey]

        var merged = base
        if (typeOverride != null) merged = merged.merge(typeOverride)
        if (labelOverride != null) merged = merged.merge(labelOverride)
        if (coordOverride != null) merged = merged.merge(coordOverride)

        return merged
    }

    fun resolvePerKeyOverride(
        rowIndex: Int,
        keyIndex: Int,
        key: KeyDef
    ): KeycapStyleSpec? {
        if (perKeyOverrides.isEmpty()) return null
        val typeOverride = perKeyOverrides[key.type.name]
        val labelKey = key.label.lowercase().trim()
        val labelOverride = if (labelKey.isNotEmpty()) perKeyOverrides[labelKey] else null
        val coordKey = "${rowIndex}_${keyIndex}"
        val coordOverride = perKeyOverrides[coordKey]
        if (typeOverride == null && labelOverride == null && coordOverride == null) return null
        var merged = KeycapStyleSpec()
        if (typeOverride != null) merged = merged.merge(typeOverride)
        if (labelOverride != null) merged = merged.merge(labelOverride)
        if (coordOverride != null) merged = merged.merge(coordOverride)
        return merged
    }

    fun toKeyboardColorScheme(): KeyboardColorScheme {
        val charBg = characterKey.background?.let { ThemeColorUtils.parseHexColor(it) }
            ?: if (isDark) Color(0xFF2C2C30) else Color(0xFFFFFFFF)
        val charFg = characterKey.foreground?.let { ThemeColorUtils.parseHexColor(it) }
            ?: if (isDark) Color(0xFFE3E2E6) else Color(0xFF000000)
        val funcBg = functionalKey.background?.let { ThemeColorUtils.parseHexColor(it) }
            ?: if (isDark) Color(0xFF242428) else Color(0xFFDBDFE6)
        val funcFg = functionalKey.foreground?.let { ThemeColorUtils.parseHexColor(it) }
            ?: if (isDark) Color(0xFF04294E) else Color(0xFF000000)
        val actBg = actionKey.background?.let { ThemeColorUtils.parseHexColor(it) }
            ?: ThemeColorUtils.parseHexColor(accentColor)
        val actFg = actionKey.foreground?.let { ThemeColorUtils.parseHexColor(it) }
            ?: if (isDark) Color(0xFF04294E) else Color(0xFFFFFFFF)

        val strokeCol = characterKey.stroke?.color?.let { ThemeColorUtils.parseHexColor(it) }
            ?: Color(0x00000000)
        val shadowCol = characterKey.shadow?.color?.let { ThemeColorUtils.parseHexColor(it) }
            ?: Color(0x00000000)

        val containerBg = background.toSolidColor(if (isDark) Color(0xFF1B1B1F) else Color(0xFFF1F3F4))
        val sugBg = suggestionBackground?.toSolidColor(containerBg) ?: containerBg

        return KeyboardColorScheme(
            containerBackground = containerBg,
            keyBackground = charBg,
            keyForeground = charFg,
            keyBorder = strokeCol,
            keyShadow = shadowCol,
            actionKeyBackground = funcBg,
            actionKeyForeground = actFg,
            shiftActiveColor = ThemeColorUtils.parseHexColor(shiftActiveColor),
            accentColor = ThemeColorUtils.parseHexColor(accentColor),
            moreKeysBackground = ThemeColorUtils.parseHexColor(moreKeysBackground),
            guidelineColor = ThemeColorUtils.parseHexColor(guidelineColor),
            suggestionBackground = sugBg,
            suggestionForeground = ThemeColorUtils.parseHexColor(suggestionForeground),
            keyPressedBackground = characterKey.pressedBackground?.let { ThemeColorUtils.parseHexColor(it) } ?: Color.Unspecified,
            keyPressedForeground = characterKey.pressedForeground?.let { ThemeColorUtils.parseHexColor(it) } ?: Color.Unspecified,
            flatKeyForeground = flatKeyLabelColor?.let { ThemeColorUtils.parseHexColor(it) } ?: Color.Unspecified,
            activeOptionHighlightColor = activeOptionHighlightColor?.let { ThemeColorUtils.parseHexColor(it) } ?: Color.Unspecified,
            fontPath = fontPath,
            themeSpec = this
        )
    }

    /**
     * Produces the canonical `.bornotheme` filename stem (`Theme_name_designer_name`).
     */
    fun fileBaseName(): String {
        val themePart = name
            .trim()
            .replace("[^a-zA-Z0-9_\\- ]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "_")
            .ifEmpty { "My_Theme" }
        val designerPart = author
            .trim()
            .takeIf { it.isNotEmpty() && !it.equals("Kiebord", ignoreCase = true) }
            ?.replace("[^a-zA-Z0-9_\\- ]".toRegex(), "")
            ?.trim()
            ?.replace("\\s+".toRegex(), "_")
            .orEmpty()
        return if (designerPart.isNotEmpty()) "${themePart}_${designerPart}" else themePart
    }
}

/**
 * Holds fully resolved visual properties for rendering a keycap.
 */
data class ResolvedKeyVisual(
    val backgroundColor: Color,
    val backgroundBrush: Brush?,
    val foregroundColor: Color,
    val strokeColor: Color,
    val strokeWidthDp: Dp,
    val shadowColor: Color,
    val shadowElevationDp: Dp,
    val cornerRadiusDp: Dp?,
    val isPill: Boolean,
    val skin: KeySkinSpec?,
    val alpha: Float
)

/**
 * Loads a bitmap from local file path, content URI, or assets.
 */
fun loadThemeBitmap(context: Context, path: String): ImageBitmap? {
    if (path.isBlank()) return null
    return try {
        when {
            path.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
            path.startsWith("assets/") -> {
                val assetName = path.removePrefix("assets/")
                context.assets.open(assetName).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
            path.startsWith("/") || File(path).exists() -> {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            }
            else -> {
                val resId = context.resources.getIdentifier(path, "drawable", context.packageName)
                if (resId != 0) {
                    BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
                } else null
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Composable background renderer supporting images, dimming overlays, and blur.
 */
@Composable
fun ThemeImageBackgroundView(
    spec: ImageBackgroundSpec,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(spec.imagePath) { loadThemeBitmap(context, spec.imagePath) }
    if (bitmap != null) {
        val contentScale = when (spec.scaleType) {
            ImageScaleType.CROP -> ContentScale.Crop
            ImageScaleType.FIT -> ContentScale.Fit
            ImageScaleType.FILL -> ContentScale.FillBounds
            ImageScaleType.TILE -> ContentScale.None
        }
        val blurModifier = if (spec.blurRadiusDp > 0f) Modifier.blur(spec.blurRadiusDp.dp) else Modifier

        Box(modifier = modifier.fillMaxSize()) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(spec.alpha)
                    .then(blurModifier)
            )
            if (!spec.dimOverlayColor.isNullOrBlank()) {
                val dimColor = ThemeColorUtils.parseHexColor(spec.dimOverlayColor, Color.Transparent)
                if (dimColor.alpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(dimColor)
                    )
                }
            }
        }
    }
}

/**
 * Type alias unifying KeyboardThemeSpec under the Borno theme versioning hierarchy as Version 2.
 */
typealias BornoThemeV2 = KeyboardThemeSpec

/**
 * Maps a rich native KeyboardThemeSpec (BornoThemeV2) to a fallback CustomThemeDef.
 */
fun KeyboardThemeSpec.toCustomThemeDef(): CustomThemeDef {
    val charBg = characterKey.background ?: if (isDark) "#2C2C30" else "#FFFFFF"
    val charFg = characterKey.foreground ?: if (isDark) "#E3E2E6" else "#000000"
    val funcBg = functionalKey.background ?: if (isDark) "#242428" else "#DBDFE6"
    val funcFg = functionalKey.foreground ?: if (isDark) "#04294E" else "#000000"
    val actBg = actionKey.background ?: accentColor
    val actFg = actionKey.foreground ?: if (isDark) "#04294E" else "#FFFFFF"

    val hasImageBackdrop = background.image != null
    val effectiveMoreKeys = if (hasImageBackdrop) {
        ThemeColorUtils.ensureFullOpacityHex(moreKeysBackground, if (isDark) "#FF242428" else "#FFFFFFFF")
    } else {
        moreKeysBackground
    }

    val defaultShadow = if (!ThemeColorUtils.isTransparent(charBg)) (if (isDark) "#4D000000" else "#33000000") else "#00000000"

    return CustomThemeDef(
        id = id,
        name = name,
        isDark = isDark,
        containerBackground = background.solidColor ?: background.gradient?.colors?.firstOrNull() ?: if (isDark) "#1B1B1F" else "#F1F3F4",
        keyBackground = charBg,
        keyForeground = charFg,
        keyBorder = characterKey.stroke?.color ?: "#00000000",
        keyShadow = characterKey.shadow?.color ?: defaultShadow,
        actionKeyBackground = funcBg,
        actionKeyForeground = actFg,
        shiftActiveColor = shiftActiveColor,
        accentColor = accentColor,
        moreKeysBackground = effectiveMoreKeys,
        guidelineColor = guidelineColor,
        suggestionBackground = suggestionBackground?.solidColor ?: (background.solidColor ?: if (isDark) "#1B1B1F" else "#F1F3F4"),
        suggestionForeground = suggestionForeground,
        keyPressedBackground = characterKey.pressedBackground ?: funcBg,
        keyPressedForeground = characterKey.pressedForeground ?: charFg,
        flatKeyForeground = flatKeyLabelColor ?: characterKey.flatForeground,
        activeOptionHighlightColor = activeOptionHighlightColor
    )
}

