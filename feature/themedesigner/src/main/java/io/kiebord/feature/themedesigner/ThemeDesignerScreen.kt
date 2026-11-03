package io.kiebord.feature.themedesigner

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.min
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.feature.ime.ui.KeyboardRootView
import io.kiebord.feature.ime.ui.ScaledKeyboardPreview
import io.kiebord.theme.ImageScaleType
import io.kiebord.theme.KiebordTheme
import io.kiebord.theme.ThemeColorUtils
import io.kiebord.theme.designer.ThemeDesignerColors
import io.kiebord.theme.designer.ThemeDesignerPreviewConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun Modifier.verticalFadingEdge(
    scrollState: ScrollState,
    topEdgeHeight: Dp = 20.dp,
    bottomEdgeHeight: Dp = 28.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val topFadePx = min(topEdgeHeight.toPx(), scrollState.value.toFloat())
        if (topFadePx > 0.5f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, topFadePx),
                blendMode = BlendMode.DstOut
            )
        }
        val maxScroll = scrollState.maxValue
        if (maxScroll in 1 until Int.MAX_VALUE) {
            val remainingPx = (maxScroll - scrollState.value).coerceAtLeast(0).toFloat()
            val bottomFadePx = min(bottomEdgeHeight.toPx(), remainingPx)
            if (bottomFadePx > 0.5f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = size.height - bottomFadePx,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - bottomFadePx),
                    size = Size(size.width, bottomFadePx),
                    blendMode = BlendMode.DstOut
                )
            }
        }
    }

internal fun Modifier.horizontalFadingEdge(
    scrollState: ScrollState,
    edgeWidth: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val maxEdgePx = edgeWidth.toPx()
        val leftFadePx = min(maxEdgePx, scrollState.value.toFloat())
        if (leftFadePx > 0.5f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = leftFadePx
                ),
                topLeft = Offset.Zero,
                size = Size(leftFadePx, size.height),
                blendMode = BlendMode.DstOut
            )
        }
        val maxScroll = scrollState.maxValue
        if (maxScroll in 1 until Int.MAX_VALUE) {
            val remainingPx = (maxScroll - scrollState.value).coerceAtLeast(0).toFloat()
            val rightFadePx = min(maxEdgePx, remainingPx)
            if (rightFadePx > 0.5f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = size.width - rightFadePx,
                        endX = size.width
                    ),
                    topLeft = Offset(size.width - rightFadePx, 0f),
                    size = Size(rightFadePx, size.height),
                    blendMode = BlendMode.DstOut
                )
            }
        }
    }

internal fun Modifier.verticalScrollWithFadingEdge(
    scrollState: ScrollState,
    topEdgeHeight: Dp = 20.dp,
    bottomEdgeHeight: Dp = 28.dp
): Modifier = this
    .verticalFadingEdge(scrollState, topEdgeHeight, bottomEdgeHeight)
    .verticalScroll(scrollState)

internal fun Modifier.horizontalScrollWithFadingEdge(
    scrollState: ScrollState,
    edgeWidth: Dp = 24.dp
): Modifier = this
    .horizontalFadingEdge(scrollState, edgeWidth)
    .horizontalScroll(scrollState)

private enum class ItemPos { TOP, MIDDLE, BOTTOM, ALONE }

private enum class DesignerKeyTab(val title: String) {
    NORMAL("Normal"),
    MODIFIERS("Modifiers"),
    ACTION("Action"),
    SPACE("Spacebar"),
    PER_KEY("Per-Key")
}

private fun itemShape(pos: ItemPos, outer: Dp = 20.dp, inner: Dp = 4.dp): RoundedCornerShape =
    when (pos) {
        ItemPos.TOP -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        ItemPos.MIDDLE -> RoundedCornerShape(inner)
        ItemPos.BOTTOM -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        ItemPos.ALONE -> RoundedCornerShape(outer)
    }

private data class ActiveColorRequest(
    val title: String,
    val initial: Color,
    val allowAlpha: Boolean = true,
    val onPick: (Color) -> Unit
)

private val QuickOverrideTargets = listOf(
    KeyType.SHIFT.name to "Shift",
    KeyType.DELETE.name to "Delete",
    KeyType.SYMBOLS.name to "?123",
    "," to "Comma",
    "." to "Period",
    KeyType.SPACE.name to "Space",
    KeyType.ACTION.name to "Enter",
    KeyType.SWITCH_LANGUAGE.name to "Globe",
    "q" to "Q",
    "w" to "W",
    "e" to "E",
    "r" to "R",
    "t" to "T",
    "y" to "Y",
    "a" to "A",
    "s" to "S",
    "d" to "D",
    "f" to "F"
)

@Composable
internal fun ThemeDesignerScreenContent(
    editingThemeId: String?,
    colors: ThemeDesignerColors,
    previewConfig: ThemeDesignerPreviewConfig,
    onBack: () -> Unit,
    onThemeSaved: (themeId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saver = remember(context) { ThemeDesignerSaver(context) }

    DisposableEffect(Unit) {
        onDispose { saver.clearSession() }
    }

    val initialState = remember(editingThemeId) {
        val loaded = editingThemeId?.let { saver.loadExisting(it) }
        if (loaded != null) {
            ThemeDesignerState.fromSpec(loaded)
        } else if (colors.isDark) {
            ThemeDesignerState.defaultDark()
        } else {
            ThemeDesignerState.defaultLight()
        }
    }

    var state by remember(editingThemeId) { mutableStateOf(initialState) }
    val defState = remember(state.isDark, state.id) { state.baseDefault() }

    var previewBorders by remember { mutableStateOf(previewConfig.showKeyBorders) }
    var previewShadows by remember { mutableStateOf(previewConfig.showKeyShadows) }
    var previewShift by remember { mutableStateOf(ShiftState.UNSHIFTED) }
    var activeKeyTab by remember { mutableStateOf(DesignerKeyTab.NORMAL) }
    var selectedPerKeyTarget by remember { mutableStateOf(KeyType.SHIFT.name) }
    var activeColorRequest by remember { mutableStateOf<ActiveColorRequest?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val workingSpec = remember(state) { state.toSpec() }
    val workingScheme = remember(workingSpec) { workingSpec.toKeyboardColorScheme() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    saver.importAsset(uri, "wallpaper", "jpg")
                }
                if (path != null) {
                    state = state.copy(
                        backgroundType = DesignerBackgroundType.IMAGE,
                        imagePath = path
                    )
                } else {
                    Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Pinned Top Demo Keyboard Preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KiebordTheme(colorScheme = workingScheme) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                            .background(workingScheme.containerBackground)
                    ) {
                        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                        val baseWidth = if (screenWidth > 500.dp) 390.dp else screenWidth
                        val fullRowHeight = if (previewConfig.showNumberRow) 54.dp else 61.dp
                        val targetRowHeight = fullRowHeight * previewConfig.keyboardHeightScale

                        ScaledKeyboardPreview(
                            baseWidth = baseWidth,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KeyboardRootView(
                                layout = PredefinedLayouts.EnglishQwerty,
                                shiftState = previewShift,
                                onKeyTap = { tappedKey ->
                                    val targetKey = ThemeDesignerState.overrideKeyFor(tappedKey)
                                    selectedPerKeyTarget = targetKey
                                    activeKeyTab = DesignerKeyTab.PER_KEY
                                },
                                onKeyLongPress = { tappedKey ->
                                    val targetKey = ThemeDesignerState.overrideKeyFor(tappedKey)
                                    selectedPerKeyTarget = targetKey
                                    activeKeyTab = DesignerKeyTab.PER_KEY
                                },
                                showKeyBorders = previewBorders,
                                showKeyShadows = previewShadows,
                                defaultShowNumberRow = previewConfig.showNumberRow,
                                defaultShowSecondaryKeys = false,
                                defaultShowKeyPopups = false,
                                initialRowHeight = targetRowHeight,
                                heightScalePortrait = previewConfig.keyboardHeightScale,
                                applySystemInsets = false,
                                isInteractive = true,
                                gestureTypingEnabled = false,
                                navBarBottomInset = 0.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Per-Key Tap Hint + Quick Reset Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TouchApp,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Tap any key to style • Selected: ${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Reset All Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.inputBackground)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                state = initialState
                                Toast.makeText(context, "Reset all changes", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Quick Reset All",
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Preview Toggles Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewChip(
                        label = "Borders",
                        active = previewBorders,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { previewBorders = !previewBorders }
                    )
                    PreviewChip(
                        label = "Shadows",
                        active = previewShadows,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { previewShadows = !previewShadows }
                    )
                    PreviewChip(
                        label = when (previewShift) {
                            ShiftState.UNSHIFTED -> "Shift: abc"
                            ShiftState.ONE_SHOT -> "Shift: Abc"
                            ShiftState.CAPS_LOCK -> "Shift: ABC"
                        },
                        active = previewShift != ShiftState.UNSHIFTED,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            previewShift = when (previewShift) {
                                ShiftState.UNSHIFTED -> ShiftState.ONE_SHOT
                                ShiftState.ONE_SHOT -> ShiftState.CAPS_LOCK
                                ShiftState.CAPS_LOCK -> ShiftState.UNSHIFTED
                            }
                        }
                    )
                }
            }
        }

        // 2. Scrollable Customization Sections
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalFadingEdge(scrollState, topEdgeHeight = 20.dp, bottomEdgeHeight = 28.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            // --- SECTION: THEME IDENTITY ---
            SectionHeader(
                title = "Theme Identity",
                colors = colors,
                onReset = {
                    state = if (state.isDark) {
                        ThemeDesignerState.defaultDark(state.id)
                    } else {
                        ThemeDesignerState.defaultLight(state.id)
                    }
                },
                resetLabel = "Reset Preset"
            )
            DesignerGroup {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = itemShape(ItemPos.TOP),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { state = state.copy(name = it) },
                            label = { Text("Theme Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.inputBorder,
                                focusedContainerColor = colors.inputBackground,
                                unfocusedContainerColor = colors.inputBackground,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                DesignerToggleRow(
                    title = "Dark Theme Base",
                    subtitle = "Informs default contrast & system adaptive behavior",
                    checked = state.isDark,
                    pos = ItemPos.BOTTOM,
                    colors = colors,
                    onCheckedChange = { state = state.copy(isDark = it) }
                )
            }

            // --- SECTION: SURFACE & BACKGROUND ---
            SectionHeader(
                title = "Surface & Background",
                colors = colors,
                onReset = { state = state.resetSurfaceSection() }
            )
            DesignerGroup {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = itemShape(ItemPos.TOP),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesignerBackgroundType.entries.forEach { bgType ->
                            SegmentPill(
                                text = when (bgType) {
                                    DesignerBackgroundType.SOLID -> "Solid"
                                    DesignerBackgroundType.GRADIENT -> "Gradient"
                                    DesignerBackgroundType.IMAGE -> "Wallpaper"
                                },
                                selected = state.backgroundType == bgType,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                onClick = { state = state.copy(backgroundType = bgType) }
                            )
                        }
                    }
                }

                when (state.backgroundType) {
                    DesignerBackgroundType.SOLID -> {
                        PresetPaletteStripRow(
                            title = "Color & Key Presets",
                            presets = ThemeStudioPresets.SolidPresets,
                            selectedPresetId = ThemeStudioPresets.SolidPresets.firstOrNull {
                                state.backgroundType == DesignerBackgroundType.SOLID &&
                                    state.backgroundColor == it.circle1 &&
                                    state.keyBackground == it.circle2 &&
                                    state.actionBackground == it.circle3
                            }?.id,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onSelectPreset = { preset ->
                                state = preset.applyTo(state)
                            }
                        )

                        DesignerColorRow(
                            title = "Background Color",
                            subtitle = "Keyboard container surface",
                            color = state.backgroundColor,
                            pos = ItemPos.BOTTOM,
                            colors = colors,
                            onReset = if (state.backgroundColor != defState.backgroundColor) {
                                { state = state.copy(backgroundColor = defState.backgroundColor, suggestionBackground = defState.backgroundColor) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Background Color",
                                    initial = state.backgroundColor,
                                    onPick = {
                                        state = state.copy(
                                            backgroundColor = it,
                                            suggestionBackground = it
                                        )
                                    }
                                )
                            }
                        )
                    }

                    DesignerBackgroundType.GRADIENT -> {
                        PresetPaletteStripRow(
                            title = "Gradient & Key Presets",
                            presets = ThemeStudioPresets.GradientPresets,
                            selectedPresetId = ThemeStudioPresets.GradientPresets.firstOrNull {
                                state.backgroundType == DesignerBackgroundType.GRADIENT &&
                                    state.gradientStart == it.circle1 &&
                                    state.gradientEnd == it.circle2 &&
                                    state.actionBackground == it.circle3
                            }?.id,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onSelectPreset = { preset ->
                                state = preset.applyTo(state)
                            }
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = itemShape(ItemPos.MIDDLE),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gradient Style",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                DesignerGradientType.entries.forEach { gType ->
                                    SegmentPill(
                                        text = if (gType == DesignerGradientType.LINEAR) "Linear" else "Radial",
                                        selected = state.gradientType == gType,
                                        colors = colors,
                                        onClick = { state = state.copy(gradientType = gType) }
                                    )
                                }
                            }
                        }

                        DesignerColorRow(
                            title = "Gradient Start Color",
                            color = state.gradientStart,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.gradientStart != defState.gradientStart) {
                                { state = state.copy(gradientStart = defState.gradientStart) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Gradient Start Color",
                                    initial = state.gradientStart,
                                    onPick = { state = state.copy(gradientStart = it) }
                                )
                            }
                        )

                        DesignerColorRow(
                            title = "Gradient End Color",
                            color = state.gradientEnd,
                            pos = if (state.gradientType == DesignerGradientType.LINEAR) ItemPos.MIDDLE else ItemPos.BOTTOM,
                            colors = colors,
                            onReset = if (state.gradientEnd != defState.gradientEnd) {
                                { state = state.copy(gradientEnd = defState.gradientEnd) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Gradient End Color",
                                    initial = state.gradientEnd,
                                    onPick = { state = state.copy(gradientEnd = it) }
                                )
                            }
                        )

                        if (state.gradientType == DesignerGradientType.LINEAR) {
                            DesignerSliderRow(
                                title = "Gradient Angle",
                                valueLabel = "${state.gradientAngle.roundToInt()}°",
                                value = state.gradientAngle,
                                valueRange = 0f..360f,
                                pos = ItemPos.BOTTOM,
                                colors = colors,
                                onReset = if (abs(state.gradientAngle - defState.gradientAngle) > 0.5f) {
                                    { state = state.copy(gradientAngle = defState.gradientAngle) }
                                } else null,
                                onValueChange = { state = state.copy(gradientAngle = it) }
                            )
                        }
                    }

                    DesignerBackgroundType.IMAGE -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape(ItemPos.MIDDLE))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            shape = itemShape(ItemPos.MIDDLE),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (state.imagePath.isNullOrBlank()) "Choose Wallpaper Image" else "Change Wallpaper Image",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = state.imagePath?.let { File(it).name } ?: "PNG, JPG, or WebP from gallery",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!state.imagePath.isNullOrBlank()) {
                                    IconButton(onClick = { state = state.copy(imagePath = null) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove wallpaper",
                                            tint = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        DesignerSliderRow(
                            title = "Wallpaper Blur",
                            valueLabel = "${state.imageBlurDp.roundToInt()} dp",
                            value = state.imageBlurDp,
                            valueRange = 0f..25f,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.imageBlurDp > 0.1f) {
                                { state = state.copy(imageBlurDp = 0f) }
                            } else null,
                            onValueChange = { state = state.copy(imageBlurDp = it) }
                        )

                        DesignerSliderRow(
                            title = "Dimming Overlay",
                            valueLabel = "${(state.imageDim * 100).roundToInt()}%",
                            value = state.imageDim,
                            valueRange = 0f..0.85f,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (abs(state.imageDim - defState.imageDim) > 0.01f) {
                                { state = state.copy(imageDim = defState.imageDim) }
                            } else null,
                            onValueChange = { state = state.copy(imageDim = it) }
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = itemShape(ItemPos.BOTTOM),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scale",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                listOf(
                                    ImageScaleType.CROP to "Crop",
                                    ImageScaleType.FIT to "Fit",
                                    ImageScaleType.FILL to "Fill"
                                ).forEach { (mode, label) ->
                                    SegmentPill(
                                        text = label,
                                        selected = state.imageScaleType == mode,
                                        colors = colors,
                                        onClick = { state = state.copy(imageScaleType = mode) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION: TABBED KEY PROPERTIES (Normal / Modifiers / Action / Spacebar / Per-Key) ---
            val keyTabResetPair: Pair<String, (() -> Unit)?> = when (activeKeyTab) {
                DesignerKeyTab.NORMAL -> "Reset Normal" to { state = state.resetCharacterKeysSection() }
                DesignerKeyTab.MODIFIERS -> "Reset Modifiers" to { state = state.resetModifiersSection() }
                DesignerKeyTab.ACTION -> "Reset Action" to { state = state.resetActionKeySection() }
                DesignerKeyTab.SPACE -> "Reset Spacebar" to if (state.spaceCustomEnabled) {
                    { state = state.resetSpaceKeySection() }
                } else null
                DesignerKeyTab.PER_KEY -> "Clear All (${state.perKeyOverrides.size})" to if (state.perKeyOverrides.isNotEmpty()) {
                    { state = state.resetPerKeySection() }
                } else null
            }

            SectionHeader(
                title = "Keys & Keycaps",
                colors = colors,
                onReset = keyTabResetPair.second,
                resetLabel = keyTabResetPair.first
            )

            DesignerGroup {
                // Tabbed Navigation Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = itemShape(ItemPos.TOP),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScrollWithFadingEdge(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesignerKeyTab.entries.forEach { tab ->
                            val badgeLabel = when (tab) {
                                DesignerKeyTab.SPACE -> if (state.spaceCustomEnabled) "${tab.title} •" else tab.title
                                DesignerKeyTab.PER_KEY -> if (state.perKeyOverrides.isNotEmpty()) "${tab.title} (${state.perKeyOverrides.size})" else tab.title
                                else -> tab.title
                            }
                            SegmentPill(
                                text = badgeLabel,
                                selected = activeKeyTab == tab,
                                colors = colors,
                                onClick = { activeKeyTab = tab }
                            )
                        }
                    }
                }

                when (activeKeyTab) {
                    DesignerKeyTab.NORMAL -> {
                        DesignerColorRow(
                            title = "Keycap Background",
                            subtitle = "Letters & numbers fill (supports glass opacity)",
                            color = state.keyBackground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.keyBackground != defState.keyBackground) {
                                { state = state.copy(keyBackground = defState.keyBackground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Keycap Background",
                                    initial = state.keyBackground,
                                    onPick = { state = state.copy(keyBackground = it) }
                                )
                            }
                        )
                        DesignerColorRow(
                            title = "Key Label Color",
                            subtitle = "Primary glyph & character text color",
                            color = state.keyForeground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.keyForeground != defState.keyForeground) {
                                { state = state.copy(keyForeground = defState.keyForeground, flatKeyForeground = defState.flatKeyForeground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Key Label Color",
                                    initial = state.keyForeground,
                                    onPick = {
                                        state = state.copy(
                                            keyForeground = it,
                                            flatKeyForeground = it
                                        )
                                    }
                                )
                            }
                        )
                        DesignerColorRow(
                            title = "Borderless Key Label Color",
                            subtitle = "Used when Key Borders toggle is disabled",
                            color = state.flatKeyForeground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.flatKeyForeground != defState.flatKeyForeground) {
                                { state = state.copy(flatKeyForeground = defState.flatKeyForeground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Borderless Key Label Color",
                                    initial = state.flatKeyForeground,
                                    onPick = { state = state.copy(flatKeyForeground = it) }
                                )
                            }
                        )
                        DesignerColorRow(
                            title = "Key Pressed Background",
                            subtitle = "Highlight color while a key is held down",
                            color = state.keyPressedBackground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.keyPressedBackground != defState.keyPressedBackground) {
                                { state = state.copy(keyPressedBackground = defState.keyPressedBackground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Key Pressed Background",
                                    initial = state.keyPressedBackground,
                                    onPick = { state = state.copy(keyPressedBackground = it) }
                                )
                            }
                        )
                        DesignerSliderRow(
                            title = "Key Corner Radius",
                            valueLabel = "${state.keyCornerRadiusDp.roundToInt()} dp",
                            value = state.keyCornerRadiusDp,
                            valueRange = 0f..24f,
                            pos = ItemPos.BOTTOM,
                            colors = colors,
                            onReset = if (abs(state.keyCornerRadiusDp - defState.keyCornerRadiusDp) > 0.2f) {
                                { state = state.copy(keyCornerRadiusDp = defState.keyCornerRadiusDp) }
                            } else null,
                            onValueChange = { state = state.copy(keyCornerRadiusDp = it) }
                        )
                    }

                    DesignerKeyTab.MODIFIERS -> {
                        DesignerColorRow(
                            title = "Modifier Key Background",
                            subtitle = "Shift, Backspace, ?123, Comma",
                            color = state.functionalBackground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.functionalBackground != defState.functionalBackground) {
                                { state = state.copy(functionalBackground = defState.functionalBackground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Modifier Key Background",
                                    initial = state.functionalBackground,
                                    onPick = { state = state.copy(functionalBackground = it) }
                                )
                            }
                        )
                        DesignerColorRow(
                            title = "Modifier Key Icon / Label",
                            subtitle = "Icon and text tint on modifier keys",
                            color = state.functionalForeground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.functionalForeground != defState.functionalForeground) {
                                { state = state.copy(functionalForeground = defState.functionalForeground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Modifier Key Icon / Label",
                                    initial = state.functionalForeground,
                                    onPick = { state = state.copy(functionalForeground = it) }
                                )
                            }
                        )
                        DesignerColorRow(
                            title = "Shift Active Indicator",
                            subtitle = "Highlight color when Shift or Caps Lock is active",
                            color = state.shiftActiveColor,
                            pos = ItemPos.BOTTOM,
                            colors = colors,
                            onReset = if (state.shiftActiveColor != defState.shiftActiveColor) {
                                { state = state.copy(shiftActiveColor = defState.shiftActiveColor) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Shift Active Indicator",
                                    initial = state.shiftActiveColor,
                                    onPick = { state = state.copy(shiftActiveColor = it) }
                                )
                            }
                        )
                    }

                    DesignerKeyTab.ACTION -> {
                        DesignerColorRow(
                            title = "Action Key Background",
                            subtitle = "Enter, Search, Send, Go",
                            color = state.actionBackground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.actionBackground != defState.actionBackground) {
                                { state = state.copy(actionBackground = defState.actionBackground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Action Key Background",
                                    initial = state.actionBackground,
                                    onPick = {
                                        state = state.copy(
                                            actionBackground = it,
                                            accentColor = it
                                        )
                                    }
                                )
                            }
                        )
                        DesignerColorRow(
                            title = "Action Key Icon / Label",
                            subtitle = "Icon and label color inside the Enter key",
                            color = state.actionForeground,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (state.actionForeground != defState.actionForeground) {
                                { state = state.copy(actionForeground = defState.actionForeground) }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "Action Key Icon / Label",
                                    initial = state.actionForeground,
                                    onPick = { state = state.copy(actionForeground = it) }
                                )
                            }
                        )
                        DesignerToggleRow(
                            title = "Pill-Shaped Action Key",
                            subtitle = "Rounded stadium pill silhouette for Enter key",
                            checked = state.actionIsPill,
                            pos = ItemPos.BOTTOM,
                            colors = colors,
                            onCheckedChange = { state = state.copy(actionIsPill = it) }
                        )
                    }

                    DesignerKeyTab.SPACE -> {
                        DesignerToggleRow(
                            title = "Custom Spacebar Style",
                            subtitle = if (state.spaceCustomEnabled) {
                                "Using dedicated spacebar colors & shape"
                            } else {
                                "Inherits Normal Key colors by default"
                            },
                            checked = state.spaceCustomEnabled,
                            pos = if (state.spaceCustomEnabled) ItemPos.MIDDLE else ItemPos.BOTTOM,
                            colors = colors,
                            onCheckedChange = { enabled ->
                                state = if (enabled) {
                                    state.copy(
                                        spaceCustomEnabled = true,
                                        spaceBackground = state.spaceBackground,
                                        spaceForeground = state.spaceForeground,
                                        spacePressedBackground = state.spacePressedBackground,
                                        spaceCornerRadiusDp = state.keyCornerRadiusDp
                                    )
                                } else {
                                    state.copy(spaceCustomEnabled = false)
                                }
                            }
                        )

                        if (state.spaceCustomEnabled) {
                            DesignerColorRow(
                                title = "Spacebar Background",
                                subtitle = "Fill color of the spacebar keycap",
                                color = state.spaceBackground,
                                pos = ItemPos.MIDDLE,
                                colors = colors,
                                onReset = if (state.spaceBackground != state.keyBackground) {
                                    { state = state.copy(spaceBackground = state.keyBackground) }
                                } else null,
                                onClick = {
                                    activeColorRequest = ActiveColorRequest(
                                        title = "Spacebar Background",
                                        initial = state.spaceBackground,
                                        onPick = { state = state.copy(spaceBackground = it) }
                                    )
                                }
                            )
                            DesignerColorRow(
                                title = "Spacebar Label Color",
                                subtitle = "Language name & caption text on spacebar",
                                color = state.spaceForeground,
                                pos = ItemPos.MIDDLE,
                                colors = colors,
                                onReset = if (state.spaceForeground != state.keyForeground) {
                                    { state = state.copy(spaceForeground = state.keyForeground) }
                                } else null,
                                onClick = {
                                    activeColorRequest = ActiveColorRequest(
                                        title = "Spacebar Label Color",
                                        initial = state.spaceForeground,
                                        onPick = { state = state.copy(spaceForeground = it) }
                                    )
                                }
                            )
                            DesignerColorRow(
                                title = "Spacebar Pressed Highlight",
                                color = state.spacePressedBackground,
                                pos = ItemPos.MIDDLE,
                                colors = colors,
                                onReset = if (state.spacePressedBackground != state.keyPressedBackground) {
                                    { state = state.copy(spacePressedBackground = state.keyPressedBackground) }
                                } else null,
                                onClick = {
                                    activeColorRequest = ActiveColorRequest(
                                        title = "Spacebar Pressed Highlight",
                                        initial = state.spacePressedBackground,
                                        onPick = { state = state.copy(spacePressedBackground = it) }
                                    )
                                }
                            )
                            DesignerToggleRow(
                                title = "Pill-Shaped Spacebar",
                                subtitle = "Full stadium capsule shape for the spacebar",
                                checked = state.spaceIsPill,
                                pos = if (state.spaceIsPill) ItemPos.BOTTOM else ItemPos.MIDDLE,
                                colors = colors,
                                onCheckedChange = { state = state.copy(spaceIsPill = it) }
                            )
                            if (!state.spaceIsPill) {
                                DesignerSliderRow(
                                    title = "Spacebar Corner Radius",
                                    valueLabel = "${state.spaceCornerRadiusDp.roundToInt()} dp",
                                    value = state.spaceCornerRadiusDp,
                                    valueRange = 0f..24f,
                                    pos = ItemPos.BOTTOM,
                                    colors = colors,
                                    onReset = if (abs(state.spaceCornerRadiusDp - state.keyCornerRadiusDp) > 0.2f) {
                                        { state = state.copy(spaceCornerRadiusDp = state.keyCornerRadiusDp) }
                                    } else null,
                                    onValueChange = { state = state.copy(spaceCornerRadiusDp = it) }
                                )
                            }
                        }
                    }

                    DesignerKeyTab.PER_KEY -> {
                        val activeOverrideForTarget = state.perKeyOverrides[selectedPerKeyTarget] ?: PerKeyColorStyle()
                        val defaultBgForTarget = when (selectedPerKeyTarget) {
                            KeyType.ACTION.name -> state.actionBackground
                            KeyType.SPACE.name -> if (state.spaceCustomEnabled) state.spaceBackground else state.keyBackground
                            KeyType.SHIFT.name,
                            KeyType.DELETE.name,
                            KeyType.SYMBOLS.name,
                            KeyType.SWITCH_LANGUAGE.name,
                            "," -> state.functionalBackground
                            else -> state.keyBackground
                        }
                        val defaultFgForTarget = when (selectedPerKeyTarget) {
                            KeyType.ACTION.name -> state.actionForeground
                            KeyType.SPACE.name -> if (state.spaceCustomEnabled) state.spaceForeground else state.keyForeground
                            KeyType.SHIFT.name,
                            KeyType.DELETE.name,
                            KeyType.SYMBOLS.name,
                            KeyType.SWITCH_LANGUAGE.name,
                            "," -> state.functionalForeground
                            else -> state.keyForeground
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = itemShape(ItemPos.MIDDLE),
                            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Editing: ${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Tap any key on the top keyboard preview or select below",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                    if (!activeOverrideForTarget.isEmpty) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.inputBackground)
                                                .clickable {
                                                    val updated = state.perKeyOverrides.toMutableMap()
                                                    updated.remove(selectedPerKeyTarget)
                                                    state = state.copy(perKeyOverrides = updated)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = "Reset this key",
                                                tint = colors.accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Reset Key",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.accent
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Key Selector Horizontal Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScrollWithFadingEdge(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickOverrideTargets.forEach { (keyId, label) ->
                                        val hasCustom = state.perKeyOverrides[keyId]?.isEmpty == false
                                        val isSelected = selectedPerKeyTarget == keyId
                                        SegmentPill(
                                            text = if (hasCustom) "$label •" else label,
                                            selected = isSelected,
                                            colors = colors,
                                            onClick = { selectedPerKeyTarget = keyId }
                                        )
                                    }
                                }

                                // Currently customized keys summary pills
                                if (state.perKeyOverrides.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Customized Keys (${state.perKeyOverrides.size}):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScrollWithFadingEdge(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        state.perKeyOverrides.entries.forEach { (k, style) ->
                                            val isCurrent = selectedPerKeyTarget == k
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isCurrent) colors.accent.copy(alpha = 0.2f) else colors.inputBackground)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isCurrent) colors.accent else colors.cardBorder,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedPerKeyTarget = k }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(style.background ?: defaultBgForTarget)
                                                        .border(1.dp, colors.cardBorder, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = ThemeDesignerState.displayNameForOverrideKey(k),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Remove key override",
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            val updated = state.perKeyOverrides.toMutableMap()
                                                            updated.remove(k)
                                                            state = state.copy(perKeyOverrides = updated)
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DesignerColorRow(
                            title = "${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)} Background",
                            subtitle = if (activeOverrideForTarget.background != null) "Custom per-key fill active" else "Using default category fill",
                            color = activeOverrideForTarget.background ?: defaultBgForTarget,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (activeOverrideForTarget.background != null) {
                                {
                                    val updated = state.perKeyOverrides.toMutableMap()
                                    val next = activeOverrideForTarget.copy(background = null)
                                    if (next.isEmpty) updated.remove(selectedPerKeyTarget) else updated[selectedPerKeyTarget] = next
                                    state = state.copy(perKeyOverrides = updated)
                                }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)} Background",
                                    initial = activeOverrideForTarget.background ?: defaultBgForTarget,
                                    onPick = { picked ->
                                        val updated = state.perKeyOverrides.toMutableMap()
                                        updated[selectedPerKeyTarget] = activeOverrideForTarget.copy(background = picked)
                                        state = state.copy(perKeyOverrides = updated)
                                    }
                                )
                            }
                        )

                        DesignerColorRow(
                            title = "${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)} Label / Icon",
                            subtitle = if (activeOverrideForTarget.foreground != null) "Custom per-key text/icon color active" else "Using default category label color",
                            color = activeOverrideForTarget.foreground ?: defaultFgForTarget,
                            pos = ItemPos.MIDDLE,
                            colors = colors,
                            onReset = if (activeOverrideForTarget.foreground != null) {
                                {
                                    val updated = state.perKeyOverrides.toMutableMap()
                                    val next = activeOverrideForTarget.copy(foreground = null)
                                    if (next.isEmpty) updated.remove(selectedPerKeyTarget) else updated[selectedPerKeyTarget] = next
                                    state = state.copy(perKeyOverrides = updated)
                                }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)} Label / Icon",
                                    initial = activeOverrideForTarget.foreground ?: defaultFgForTarget,
                                    onPick = { picked ->
                                        val updated = state.perKeyOverrides.toMutableMap()
                                        updated[selectedPerKeyTarget] = activeOverrideForTarget.copy(foreground = picked)
                                        state = state.copy(perKeyOverrides = updated)
                                    }
                                )
                            }
                        )

                        DesignerColorRow(
                            title = "${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)} Pressed Color",
                            subtitle = if (activeOverrideForTarget.pressedBackground != null) "Custom per-key pressed fill active" else "Using default pressed highlight",
                            color = activeOverrideForTarget.pressedBackground ?: state.keyPressedBackground,
                            pos = ItemPos.BOTTOM,
                            colors = colors,
                            onReset = if (activeOverrideForTarget.pressedBackground != null) {
                                {
                                    val updated = state.perKeyOverrides.toMutableMap()
                                    val next = activeOverrideForTarget.copy(pressedBackground = null)
                                    if (next.isEmpty) updated.remove(selectedPerKeyTarget) else updated[selectedPerKeyTarget] = next
                                    state = state.copy(perKeyOverrides = updated)
                                }
                            } else null,
                            onClick = {
                                activeColorRequest = ActiveColorRequest(
                                    title = "${ThemeDesignerState.displayNameForOverrideKey(selectedPerKeyTarget)} Pressed Color",
                                    initial = activeOverrideForTarget.pressedBackground ?: state.keyPressedBackground,
                                    onPick = { picked ->
                                        val updated = state.perKeyOverrides.toMutableMap()
                                        updated[selectedPerKeyTarget] = activeOverrideForTarget.copy(pressedBackground = picked)
                                        state = state.copy(perKeyOverrides = updated)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // --- SECTION: BORDERS & SHADOWS ---
            SectionHeader(
                title = "Borders & Drop Shadows",
                colors = colors,
                onReset = { state = state.resetBordersShadowsSection() }
            )
            DesignerGroup {
                DesignerToggleRow(
                    title = "Key Border Stroke",
                    subtitle = "Draw a crisp outline around each keycap",
                    checked = state.strokeEnabled,
                    pos = ItemPos.TOP,
                    colors = colors,
                    onCheckedChange = { state = state.copy(strokeEnabled = it) }
                )
                if (state.strokeEnabled) {
                    DesignerColorRow(
                        title = "Border Stroke Color",
                        color = state.strokeColor,
                        pos = ItemPos.MIDDLE,
                        colors = colors,
                        onReset = if (state.strokeColor != defState.strokeColor) {
                            { state = state.copy(strokeColor = defState.strokeColor) }
                        } else null,
                        onClick = {
                            activeColorRequest = ActiveColorRequest(
                                title = "Border Stroke Color",
                                initial = state.strokeColor,
                                onPick = { state = state.copy(strokeColor = it) }
                            )
                        }
                    )
                    DesignerSliderRow(
                        title = "Border Width",
                        valueLabel = String.format("%.2f dp", state.strokeWidthDp),
                        value = state.strokeWidthDp,
                        valueRange = 0.25f..3.0f,
                        pos = ItemPos.MIDDLE,
                        colors = colors,
                        onReset = if (abs(state.strokeWidthDp - defState.strokeWidthDp) > 0.05f) {
                            { state = state.copy(strokeWidthDp = defState.strokeWidthDp) }
                        } else null,
                        onValueChange = { state = state.copy(strokeWidthDp = it) }
                    )
                    DesignerSliderRow(
                        title = "Border Opacity",
                        valueLabel = "${(state.strokeAlpha * 100).roundToInt()}%",
                        value = state.strokeAlpha,
                        valueRange = 0.1f..1.0f,
                        pos = ItemPos.MIDDLE,
                        colors = colors,
                        onReset = if (abs(state.strokeAlpha - defState.strokeAlpha) > 0.02f) {
                            { state = state.copy(strokeAlpha = defState.strokeAlpha) }
                        } else null,
                        onValueChange = { state = state.copy(strokeAlpha = it) }
                    )
                }

                DesignerToggleRow(
                    title = "Key Drop Shadow",
                    subtitle = "Subtle elevation beneath keycaps",
                    checked = state.shadowEnabled,
                    pos = if (state.shadowEnabled) ItemPos.MIDDLE else ItemPos.BOTTOM,
                    colors = colors,
                    onCheckedChange = { state = state.copy(shadowEnabled = it) }
                )
                if (state.shadowEnabled) {
                    DesignerColorRow(
                        title = "Shadow Color",
                        color = state.shadowColor,
                        pos = ItemPos.MIDDLE,
                        colors = colors,
                        onReset = if (state.shadowColor != defState.shadowColor) {
                            { state = state.copy(shadowColor = defState.shadowColor) }
                        } else null,
                        onClick = {
                            activeColorRequest = ActiveColorRequest(
                                title = "Shadow Color",
                                initial = state.shadowColor,
                                onPick = { state = state.copy(shadowColor = it) }
                            )
                        }
                    )
                    DesignerSliderRow(
                        title = "Shadow Elevation",
                        valueLabel = String.format("%.1f dp", state.shadowElevationDp),
                        value = state.shadowElevationDp,
                        valueRange = 0.5f..6.0f,
                        pos = ItemPos.BOTTOM,
                        colors = colors,
                        onReset = if (abs(state.shadowElevationDp - defState.shadowElevationDp) > 0.1f) {
                            { state = state.copy(shadowElevationDp = defState.shadowElevationDp) }
                        } else null,
                        onValueChange = { state = state.copy(shadowElevationDp = it) }
                    )
                }
            }

            // --- SECTION: TOOLBAR, ACCENTS & OVERLAYS ---
            SectionHeader(
                title = "Toolbar, Accents & Overlays",
                colors = colors,
                onReset = { state = state.resetAccentsOverlaysSection() }
            )
            DesignerGroup {
                DesignerColorRow(
                    title = "Accent Color",
                    subtitle = "Primary theme highlight & selection tint",
                    color = state.accentColor,
                    pos = ItemPos.TOP,
                    colors = colors,
                    onReset = if (state.accentColor != defState.accentColor) {
                        { state = state.copy(accentColor = defState.accentColor) }
                    } else null,
                    onClick = {
                        activeColorRequest = ActiveColorRequest(
                            title = "Accent Color",
                            initial = state.accentColor,
                            onPick = { state = state.copy(accentColor = it) }
                        )
                    }
                )
                DesignerColorRow(
                    title = "Active Option Highlight",
                    subtitle = "Toolbar & tools drawer active toggle highlight",
                    color = state.activeOptionHighlightColor,
                    pos = ItemPos.MIDDLE,
                    colors = colors,
                    onReset = if (state.activeOptionHighlightColor != defState.activeOptionHighlightColor) {
                        { state = state.copy(activeOptionHighlightColor = defState.activeOptionHighlightColor) }
                    } else null,
                    onClick = {
                        activeColorRequest = ActiveColorRequest(
                            title = "Active Option Highlight",
                            initial = state.activeOptionHighlightColor,
                            onPick = { state = state.copy(activeOptionHighlightColor = it) }
                        )
                    }
                )
                DesignerColorRow(
                    title = "Suggestion Bar Background",
                    color = state.suggestionBackground,
                    pos = ItemPos.MIDDLE,
                    colors = colors,
                    onReset = if (state.suggestionBackground != defState.suggestionBackground) {
                        { state = state.copy(suggestionBackground = defState.suggestionBackground) }
                    } else null,
                    onClick = {
                        activeColorRequest = ActiveColorRequest(
                            title = "Suggestion Bar Background",
                            initial = state.suggestionBackground,
                            onPick = { state = state.copy(suggestionBackground = it) }
                        )
                    }
                )
                DesignerColorRow(
                    title = "Suggestion Text & Icons",
                    color = state.suggestionForeground,
                    pos = ItemPos.MIDDLE,
                    colors = colors,
                    onReset = if (state.suggestionForeground != defState.suggestionForeground) {
                        { state = state.copy(suggestionForeground = defState.suggestionForeground) }
                    } else null,
                    onClick = {
                        activeColorRequest = ActiveColorRequest(
                            title = "Suggestion Text & Icons",
                            initial = state.suggestionForeground,
                            onPick = { state = state.copy(suggestionForeground = it) }
                        )
                    }
                )
                DesignerColorRow(
                    title = "MoreKeys Popup Background",
                    subtitle = "Long-press popup menu surface color",
                    color = state.moreKeysBackground,
                    pos = ItemPos.BOTTOM,
                    colors = colors,
                    onReset = if (state.moreKeysBackground != defState.moreKeysBackground) {
                        { state = state.copy(moreKeysBackground = defState.moreKeysBackground) }
                    } else null,
                    onClick = {
                        activeColorRequest = ActiveColorRequest(
                            title = "MoreKeys Popup Background",
                            initial = state.moreKeysBackground,
                            onPick = { state = state.copy(moreKeysBackground = it) }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Save & Apply Button
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            saver.save(state, isEditing = editingThemeId != null)
                        }
                        isSaving = false
                        result.onSuccess { savedId ->
                            Toast.makeText(
                                context,
                                "Saved & applied \"${state.name.ifBlank { "My Theme" }}\"",
                                Toast.LENGTH_SHORT
                            ).show()
                            onThemeSaved(savedId)
                        }.onFailure { err ->
                            Toast.makeText(
                                context,
                                "Could not save theme: ${err.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (colors.isDark) Color(0xFF0F141C) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingThemeId != null) "Update & Apply Theme" else "Save & Apply Theme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color(0xFF0F141C) else Color.White
                )
            }
        }
    }

    activeColorRequest?.let { req ->
        KiebordColorPickerSheet(
            title = req.title,
            initialColor = req.initial,
            allowAlpha = req.allowAlpha,
            colors = colors,
            onDismiss = { activeColorRequest = null },
            onColorSelected = req.onPick
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    colors: ThemeDesignerColors,
    onReset: (() -> Unit)? = null,
    resetLabel: String = "Reset"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent
        )
        if (onReset != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = resetLabel,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = resetLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DesignerGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        content = content
    )
}

@Composable
private fun PreviewChip(
    label: String,
    active: Boolean,
    colors: ThemeDesignerColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (active) colors.accent.copy(alpha = 0.18f) else colors.inputBackground
    val border = if (active) colors.accent else colors.cardBorder
    val fg = if (active) colors.accent else colors.textSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentPill(
    text: String,
    selected: Boolean,
    colors: ThemeDesignerColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) colors.accent else colors.inputBackground
    val fg = if (selected) (if (colors.isDark) Color(0xFF0F141C) else Color.White) else colors.textPrimary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun DesignerColorRow(
    title: String,
    subtitle: String? = null,
    color: Color,
    pos: ItemPos,
    colors: ThemeDesignerColors,
    onReset: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = remember(pos) { itemShape(pos) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
            if (onReset != null) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset $title",
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = ThemeColorUtils.colorToHex(color),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textSecondary,
                modifier = Modifier.padding(end = 10.dp)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.5.dp, colors.cardBorder, CircleShape)
            )
        }
    }
}

@Composable
private fun DesignerToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    pos: ItemPos,
    colors: ThemeDesignerColors,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = remember(pos) { itemShape(pos) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onCheckedChange(!checked) },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (colors.isDark) Color(0xFF0F141C) else Color.White,
                    checkedTrackColor = colors.accent
                )
            )
        }
    }
}

@Composable
private fun DesignerSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    pos: ItemPos,
    colors: ThemeDesignerColors,
    onReset: (() -> Unit)? = null,
    onValueChange: (Float) -> Unit
) {
    val shape = remember(pos) { itemShape(pos) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (onReset != null) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reset $title",
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = valueLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent
                )
            )
        }
    }
}

@Composable
private fun PresetPaletteStripRow(
    title: String,
    presets: List<ThemeStudioPreset>,
    selectedPresetId: String?,
    pos: ItemPos,
    colors: ThemeDesignerColors,
    onSelectPreset: (ThemeStudioPreset) -> Unit
) {
    val shape = remember(pos) { itemShape(pos) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScrollWithFadingEdge(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { preset ->
                    val isSelected = preset.id == selectedPresetId
                    PresetThreeCirclesChip(
                        preset = preset,
                        selected = isSelected,
                        colors = colors,
                        onClick = { onSelectPreset(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetThreeCirclesChip(
    preset: ThemeStudioPreset,
    selected: Boolean,
    colors: ThemeDesignerColors,
    onClick: () -> Unit
) {
    val containerBg = if (selected) colors.accent.copy(alpha = 0.16f) else colors.inputBackground
    val borderColor = if (selected) colors.accent else colors.cardBorder
    val borderWidth = if (selected) 1.5.dp else 1.dp

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 3 horizontally stacked overlapping color circles
        Row(
            horizontalArrangement = Arrangement.spacedBy((-7).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(preset.circle1, preset.circle2, preset.circle3).forEach { swatchColor ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.5.dp, colors.cardBackground, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = preset.name,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.accent else colors.textPrimary,
            maxLines = 1
        )
    }
}
