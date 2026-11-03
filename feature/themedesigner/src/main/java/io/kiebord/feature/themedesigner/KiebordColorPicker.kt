package io.kiebord.feature.themedesigner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.ThemeColorUtils
import io.kiebord.theme.designer.ThemeDesignerColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure Kotlin RGB <-> HSV conversion utilities (safe for JVM unit tests without Android runtime).
 */
internal object ColorHsvUtils {

    fun colorToHsv(color: Color): FloatArray {
        val r = color.red.coerceIn(0f, 1f)
        val g = color.green.coerceIn(0f, 1f)
        val b = color.blue.coerceIn(0f, 1f)

        val maxVal = max(r, max(g, b))
        val minVal = min(r, min(g, b))
        val delta = maxVal - minVal

        var h = when {
            delta == 0f -> 0f
            maxVal == r -> 60f * (((g - b) / delta) % 6f)
            maxVal == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (h < 0f) h += 360f

        val s = if (maxVal == 0f) 0f else delta / maxVal
        val v = maxVal
        return floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    }

    fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
        val hue = ((h % 360f) + 360f) % 360f
        val sat = s.coerceIn(0f, 1f)
        val value = v.coerceIn(0f, 1f)
        val a = alpha.coerceIn(0f, 1f)

        val c = value * sat
        val x = c * (1f - kotlin.math.abs(((hue / 60f) % 2f) - 1f))
        val m = value - c

        val (r1, g1, b1) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = a
        )
    }
}

private val CuratedColorSwatches = listOf(
    // Dark & Slate Neutrals
    Color(0xFF000000), Color(0xFF121316), Color(0xFF1B1B1F), Color(0xFF212328),
    Color(0xFF2C2C30), Color(0xFF38383F), Color(0xFF424242), Color(0xFF6B7280),
    // Light & Crisp Neutrals
    Color(0xFFFFFFFF), Color(0xFFF4F6F9), Color(0xFFEBEEF2), Color(0xFFDCE0E6),
    Color(0xFFE3E2E6), Color(0xFF9EABC0), Color(0xFF1C1C1E), Color(0xFF04294E),
    // Vibrant Accents & Borno Classics
    Color(0xFF4999FD), Color(0xFF2563EB), Color(0xFF0284C7), Color(0xFF06B6D4),
    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E),
    Color(0xFFEF4444), Color(0xFFFF5722), Color(0xFFF59E0B), Color(0xFF10B981),
    Color(0xFF14B8A6), Color(0xFFA8C7FA), Color(0xFF38BDF8), Color(0xFFFDFE03)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun KiebordColorPickerSheet(
    title: String,
    initialColor: Color,
    allowAlpha: Boolean = true,
    colors: ThemeDesignerColors,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialHsv = remember(initialColor) { ColorHsvUtils.colorToHsv(initialColor) }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember(initialColor) { mutableFloatStateOf(initialColor.alpha.coerceIn(0f, 1f)) }

    val currentColor = remember(hue, saturation, value, alpha) {
        ColorHsvUtils.hsvToColor(hue, saturation, value, if (allowAlpha) alpha else 1f)
    }

    var hexText by remember(currentColor) {
        mutableStateOf(ThemeColorUtils.colorToHex(currentColor))
    }

    fun applyPickedColor(picked: Color) {
        val hsv = ColorHsvUtils.colorToHsv(picked)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        alpha = if (allowAlpha) picked.alpha else 1f
        hexText = ThemeColorUtils.colorToHex(picked)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithFadingEdge(rememberScrollState(), topEdgeHeight = 16.dp, bottomEdgeHeight = 20.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current vs New comparison row + Hex input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(initialColor)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Old", fontSize = 11.sp, color = colors.textSecondary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                            .border(1.5.dp, colors.accent, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("New", fontSize = 11.sp, color = colors.accent, fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val cleaned = input.trim()
                        if (cleaned.startsWith("#") && (cleaned.length == 7 || cleaned.length == 9 || cleaned.length == 4)) {
                            val parsed = ThemeColorUtils.parseHexColor(cleaned, Color.Unspecified)
                            if (parsed != Color.Unspecified) {
                                val hsv = ColorHsvUtils.colorToHsv(parsed)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                if (allowAlpha && cleaned.length == 9) {
                                    alpha = parsed.alpha
                                }
                            }
                        }
                    },
                    label = { Text("HEX", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.inputBorder,
                        focusedContainerColor = colors.inputBackground,
                        unfocusedContainerColor = colors.inputBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hue Slider with Rainbow Track
            Text(
                text = "Hue (${hue.roundToInt()}°)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            )
                        )
                )
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.textPrimary,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            // Saturation Slider
            Text(
                text = "Saturation (${(saturation * 100).roundToInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
            Slider(
                value = saturation,
                onValueChange = { saturation = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent
                )
            )

            // Brightness / Value Slider
            Text(
                text = "Brightness (${(value * 100).roundToInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent
                )
            )

            if (allowAlpha) {
                Text(
                    text = "Opacity (${(alpha * 100).roundToInt()}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quick Swatches",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CuratedColorSwatches.forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .clickable { applyPickedColor(swatch) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two stacked filled buttons matching Kiebord's bottom sheet convention
            Button(
                onClick = {
                    onColorSelected(currentColor)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Apply Color",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color(0xFF0F141C) else Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colors.isDark) Color(0xFF2C2C32) else Color(0xFFE0E2EC),
                    contentColor = colors.textPrimary
                )
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

