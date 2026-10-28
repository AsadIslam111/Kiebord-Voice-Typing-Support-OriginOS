package io.kiebord.feature.ime.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Wraps a keyboard preview layout (such as [KeyboardRootView]) and scales it proportionally
 * so that all keycaps, row gaps, key horizontal gaps, corner radii, borders, shadows,
 * fonts, and icons scale uniformly according to its container size.
 */
@Composable
fun ScaledKeyboardPreview(
    modifier: Modifier = Modifier,
    baseWidth: Dp = if (LocalConfiguration.current.screenWidthDp > 500) 390.dp else LocalConfiguration.current.screenWidthDp.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val baseWidthPx = baseWidth.roundToPx()
                val childConstraints = Constraints(
                    minWidth = baseWidthPx,
                    maxWidth = baseWidthPx,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity
                )
                val placeable = measurable.measure(childConstraints)
                val scale = if (baseWidthPx > 0) constraints.maxWidth.toFloat() / baseWidthPx else 1f
                val scaledWidth = constraints.maxWidth
                val scaledHeight = (placeable.height * scale).roundToInt()

                layout(scaledWidth, scaledHeight) {
                    placeable.placeWithLayer(0, 0) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            }
    ) {
        content()
    }
}
