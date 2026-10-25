package io.kiebord.app.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import io.kiebord.core.theme.R
import kotlinx.coroutines.delay
import java.text.BreakIterator

/**
 * Animated morphing greeting text inspired by Borno Keyboard's GreetingAnimator.
 * Grapheme clusters of the previous word cascade upward and fade out left-to-right,
 * while characters of the incoming word rise from below and fade in with staggered spring easing.
 */
@Composable
fun MorphGreetingText(
    words: List<String> = listOf(
        "Free",
        "Open Source",
        "Multilingual",
        "Privacy Focused"
    ),
    textColor: Color = if (isSystemInDarkTheme()) Color.White else Color.Black,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var nextIndex by remember { mutableIntStateOf((currentIndex + 1) % words.size) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200L) // Hold word
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
            )
            currentIndex = nextIndex
            nextIndex = (currentIndex + 1) % words.size
            progress.snapTo(0f)
        }
    }

    val density = LocalDensity.current
    val textSizePx = with(density) { 38.sp.toPx() }
    val moveDistancePx = with(density) { 26.dp.toPx() }

    val exitEasing = remember { CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f) }
    val enterEasing = remember { CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f) }

    val context = LocalContext.current
    val typeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.keyboard_font) ?: android.graphics.Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    val paint = remember(typeface) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
            this.typeface = typeface
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val p = progress.value
        val fromWord = words[currentIndex]
        val toWord = words[nextIndex]

        val rawClustersFrom = splitIntoGraphemes(fromWord)
        val rawClustersTo = splitIntoGraphemes(toWord)

        paint.textSize = textSizePx
        val maxWordWidth = words.maxOfOrNull { paint.measureText(it) } ?: 0f
        val effectiveTextSizePx = if (w > 0f && maxWordWidth > w * 0.92f) {
            textSizePx * (w * 0.92f / maxWordWidth)
        } else {
            textSizePx
        }
        paint.textSize = effectiveTextSizePx
        paint.color = textColor.toArgb()

        val fontMetrics = paint.fontMetrics
        val cy = h / 2f
        val baseline = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f

        val maxStagger = 0.32f

        drawContext.canvas.nativeCanvas.apply {
            if (p == 0f) {
                // Static draw of current word
                val totalWidth = paint.measureText(fromWord)
                val startX = (w - totalWidth) / 2f
                paint.alpha = 255
                drawText(fromWord, startX, baseline, paint)
            } else {
                // 1. Exiting characters: cascade upward and fade out
                val totalFrom = rawClustersFrom.size
                val wholeWidthFrom = paint.measureText(fromWord)
                val startXFrom = (w - wholeWidthFrom) / 2f
                var charXFrom = startXFrom

                for (i in rawClustersFrom.indices) {
                    val cluster = rawClustersFrom[i]
                    val clusterWidth = paint.measureText(cluster)
                    val delay = if (totalFrom > 1) (i.toFloat() / (totalFrom - 1)) * maxStagger else 0f
                    val localP = ((p - delay) / (1f - maxStagger)).coerceIn(0f, 1f)
                    val easedP = exitEasing.transform(localP)
                    val alphaP = (1f - (localP / 0.65f)).coerceIn(0f, 1f)
                    val alpha = (alphaP * 255).toInt()

                    if (alpha > 0) {
                        paint.alpha = alpha
                        val dy = -moveDistancePx * easedP
                        drawText(cluster, charXFrom, baseline + dy, paint)
                    }
                    charXFrom += clusterWidth
                }

                // 2. Entering characters: cascade upward from below and fade in
                val totalTo = rawClustersTo.size
                val wholeWidthTo = paint.measureText(toWord)
                val startXTo = (w - wholeWidthTo) / 2f
                var charXTo = startXTo

                val enterGlobalP = ((p - 0.12f) / 0.88f).coerceIn(0f, 1f)

                for (i in rawClustersTo.indices) {
                    val cluster = rawClustersTo[i]
                    val clusterWidth = paint.measureText(cluster)
                    val delay = if (totalTo > 1) (i.toFloat() / (totalTo - 1)) * maxStagger else 0f
                    val localP = ((enterGlobalP - delay) / (1f - maxStagger)).coerceIn(0f, 1f)
                    val easedP = enterEasing.transform(localP)
                    val alphaP = (localP / 0.55f).coerceIn(0f, 1f)
                    val alpha = (alphaP * 255).toInt()

                    if (alpha > 0) {
                        paint.alpha = alpha
                        val dy = moveDistancePx * (1f - easedP)
                        drawText(cluster, charXTo, baseline + dy, paint)
                    }
                    charXTo += clusterWidth
                }
            }
        }
    }
}

private fun splitIntoGraphemes(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val clusters = mutableListOf<String>()
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    var start = it.first()
    var end = it.next()
    while (end != BreakIterator.DONE) {
        clusters.add(text.substring(start, end))
        start = end
        end = it.next()
    }
    return clusters
}
