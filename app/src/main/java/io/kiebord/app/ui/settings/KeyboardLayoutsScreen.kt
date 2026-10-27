package io.kiebord.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyRow
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.data.layout.LayoutItemMeta
import io.kiebord.data.layout.LayoutRepository
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LayoutCardData(
    val id: String,
    val name: String,
    val badge: String,
    val description: String,
    val layoutDef: KeyboardLayoutDef
)

@Composable
fun KeyboardLayoutsScreen(
    preferences: KiebordPreferences,
    onUpdatePrimaryLayout: (String) -> Unit,
    onToggleLayoutEnabled: (String, Boolean) -> Unit,
    onToggleExtraRow: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val layoutRepo = remember(context) { LayoutRepository.getInstance(context) }
    var reloadTrigger by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()
    val colors = LocalSettingsColors.current

    val englishCard = remember {
        LayoutCardData(
            id = KiebordPreferences.LAYOUT_EN_QWERTY,
            name = "English",
            badge = "EN",
            description = "Standard Latin layout with gesture typing",
            layoutDef = PredefinedLayouts.EnglishQwerty
        )
    }

    val banglaCards = remember {
        listOf(
            LayoutCardData(
                id = KiebordPreferences.LAYOUT_BN_BORNO,
                name = "বর্ণ ফোনেটিক",
                badge = "বর্ণ",
                description = "ami -> আমি",
                layoutDef = PredefinedLayouts.BengaliBorno
            ),
            LayoutCardData(
                id = KiebordPreferences.LAYOUT_BN_AVRO,
                name = "অভ্র",
                badge = "অভ্র",
                description = "ami -> আমি",
                layoutDef = PredefinedLayouts.BengaliAvro
            ),
            LayoutCardData(
                id = KiebordPreferences.LAYOUT_BN_NATIONAL,
                name = "জাতীয় (BSTI)",
                badge = "জাতীয়",
                description = "National BSTI layout",
                layoutDef = PredefinedLayouts.BengaliNational
            ),
            LayoutCardData(
                id = KiebordPreferences.LAYOUT_BN_PROBHAT,
                name = "প্রভাত",
                badge = "প্রভাত",
                description = "Fixed layout",
                layoutDef = PredefinedLayouts.BengaliProbhat
            )
        )
    }

    var arabicLayout by remember {
        mutableStateOf(
            layoutRepo.getCachedLayout(KiebordPreferences.LAYOUT_AR_ARABIC)
                ?: PredefinedLayouts.fromId(KiebordPreferences.LAYOUT_AR_ARABIC)
        )
    }
    val arabicCard = remember(arabicLayout) {
        LayoutCardData(
            id = KiebordPreferences.LAYOUT_AR_ARABIC,
            name = "Arabic",
            badge = "عربي",
            description = "Standard Arabic with Harakat diacritics",
            layoutDef = arabicLayout
        )
    }

    var availableMeta by remember {
        mutableStateOf(layoutRepo.getCachedAvailableLayouts() ?: emptyList())
    }

    LaunchedEffect(reloadTrigger) {
        val loadedArabic = withContext(Dispatchers.IO) {
            layoutRepo.getLayout(KiebordPreferences.LAYOUT_AR_ARABIC)
        }
        val loadedMeta = withContext(Dispatchers.IO) {
            layoutRepo.getAvailableLayouts()
        }
        arabicLayout = loadedArabic
        availableMeta = loadedMeta
    }

    val customMeta = remember(availableMeta) { availableMeta.filter { it.isCustom } }
    var customCards by remember { mutableStateOf<List<LayoutCardData>>(emptyList()) }

    LaunchedEffect(customMeta, reloadTrigger) {
        val cards = withContext(Dispatchers.IO) {
            customMeta.map { meta ->
                val def = layoutRepo.getLayout(meta.id)
                LayoutCardData(
                    id = meta.id,
                    name = meta.displayName ?: meta.name,
                    badge = meta.language.uppercase(),
                    description = "Custom imported layout",
                    layoutDef = def
                )
            }
        }
        customCards = cards
    }

    val isEnglishEnabled = preferences.enabledLayoutIds.contains(englishCard.id)
    val isEnglishLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnglishEnabled
    val isAnyBanglaEnabled = preferences.enabledLayoutIds.any { it.startsWith("bn") }
    val isArabicEnabled = preferences.enabledLayoutIds.contains(arabicCard.id)

    var isEnglishExpanded by remember { mutableStateOf(isEnglishEnabled) }
    var isBanglaExpanded by remember { mutableStateOf(isAnyBanglaEnabled) }
    var isArabicExpanded by remember { mutableStateOf(isArabicEnabled) }
    var isArabicOptionsOpen by remember { mutableStateOf(false) }
    var selectedCustomSettingsId by remember { mutableStateOf<String?>(null) }
    var isCustomExpanded by remember { mutableStateOf(customCards.any { preferences.enabledLayoutIds.contains(it.id) }) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (jsonContent != null) {
                    val result = layoutRepo.importLayout(jsonContent)
                    result.onSuccess { newLayout ->
                        onToggleLayoutEnabled(newLayout.id, true)
                        reloadTrigger++
                        Toast.makeText(context, "Imported " + newLayout.name, Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Import failed: " + (err.message ?: "Invalid JSON"), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Available Keyboard Layouts")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            // 1. English Item
            SettingsItemCard(
                position = GroupPosition.TOP,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEnglishExpanded = !isEnglishExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "English",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Standard Latin layout with gesture typing",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isEnglishExpanded) 90f else 0f,
                            label = "EnglishChevronRotation"
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isEnglishExpanded) "Collapse" else "Expand",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }

                    AnimatedVisibility(
                        visible = isEnglishExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            KeyboardLayoutPreviewItem(
                                cardData = englishCard,
                                preferences = preferences,
                                isEnabled = isEnglishEnabled,
                                onToggle = {
                                    if (!isEnglishLastRemaining || !isEnglishEnabled) {
                                        onToggleLayoutEnabled(englishCard.id, !isEnglishEnabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Bangla Item with Horizontal Carousel
            SettingsItemCard(
                position = GroupPosition.MIDDLE,
                modifier = Modifier.fillMaxWidth()
            ) {
                val activeBanglaCount = banglaCards.count { preferences.enabledLayoutIds.contains(it.id) }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBanglaExpanded = !isBanglaExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bangla",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isBanglaExpanded) "বর্ণ ফোনেটিক, অভ্র, জাতীয় (BSTI), প্রভাত" else "$activeBanglaCount active layout(s) - tap to expand",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isBanglaExpanded) 90f else 0f,
                            label = "BanglaChevronRotation"
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isBanglaExpanded) "Collapse" else "Expand",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }

                    AnimatedVisibility(
                        visible = isBanglaExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp, top = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = banglaCards,
                                    key = { it.id }
                                ) { cardData ->
                                    val isEnabled = preferences.enabledLayoutIds.contains(cardData.id)
                                    val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled

                                    KeyboardLayoutPreviewItem(
                                        cardData = cardData,
                                        preferences = preferences,
                                        isEnabled = isEnabled,
                                        onToggle = {
                                            if (!isLastRemaining || !isEnabled) {
                                                onToggleLayoutEnabled(cardData.id, !isEnabled)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Arabic Item
            val hasCustom = customCards.isNotEmpty()
            SettingsItemCard(
                position = if (hasCustom) GroupPosition.MIDDLE else GroupPosition.BOTTOM,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isArabicExpanded = !isArabicExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Arabic",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Standard Arabic with Harakat diacritics",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isArabicExpanded) 90f else 0f,
                            label = "ArabicChevronRotation"
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isArabicExpanded) "Collapse" else "Expand",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }

                    AnimatedVisibility(
                        visible = isArabicExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            KeyboardLayoutPreviewItem(
                                cardData = arabicCard,
                                preferences = preferences,
                                isEnabled = isArabicEnabled,
                                onToggle = {
                                    onToggleLayoutEnabled(arabicCard.id, !isArabicEnabled)
                                }
                            )

                            // Dynamic Extra Rows options
                            if (arabicCard.layoutDef.extraRows.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isArabicOptionsOpen = !isArabicOptionsOpen }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Options",
                                        tint = if (isArabicOptionsOpen) colors.accent else colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Layout Options",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isArabicOptionsOpen) colors.accent else colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val chevronRotation by animateFloatAsState(
                                        targetValue = if (isArabicOptionsOpen) 90f else 0f,
                                        label = "ArabicChevronRotation"
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = if (isArabicOptionsOpen) colors.accent else colors.textSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .graphicsLayer { rotationZ = chevronRotation }
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isArabicOptionsOpen,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        for (extraRow in arabicCard.layoutDef.extraRows) {
                                            val isRowEnabled = preferences.isExtraRowEnabled(
                                                arabicCard.id,
                                                extraRow.id,
                                                extraRow.defaultEnabled
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(colors.cardBackground.copy(alpha = 0.5f))
                                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = extraRow.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.textPrimary
                                                    )
                                                    Text(
                                                        text = "Show extra diacritics row above keyboard",
                                                        fontSize = 11.sp,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                Switch(
                                                    checked = isRowEnabled,
                                                    onCheckedChange = { checked ->
                                                        onToggleExtraRow(arabicCard.id, extraRow.id, checked)
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = colors.accent
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Custom Imported Layouts (if present)
            if (hasCustom) {
                SettingsItemCard(
                    position = GroupPosition.BOTTOM,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCustomExpanded = !isCustomExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Layouts",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${customCards.size} custom layout(s) installed",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            val chevronRotation by animateFloatAsState(
                                targetValue = if (isCustomExpanded) 90f else 0f,
                                label = "CustomChevronRotation"
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isCustomExpanded) "Collapse" else "Expand",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = chevronRotation }
                            )
                        }

                        AnimatedVisibility(
                            visible = isCustomExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp, top = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(
                                        items = customCards,
                                        key = { it.id }
                                    ) { cardData ->
                                        val isEnabled = preferences.enabledLayoutIds.contains(cardData.id)
                                        val hasExtraRows = cardData.layoutDef.extraRows.isNotEmpty()
                                        val isCardSettingsOpen = selectedCustomSettingsId == cardData.id

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            KeyboardLayoutPreviewItem(
                                                cardData = cardData,
                                                preferences = preferences,
                                                isEnabled = isEnabled,
                                                onToggle = {
                                                    onToggleLayoutEnabled(cardData.id, !isEnabled)
                                                }
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasExtraRows) {
                                                    IconButton(
                                                        onClick = {
                                                            selectedCustomSettingsId = if (selectedCustomSettingsId == cardData.id) null else cardData.id
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Settings,
                                                            contentDescription = "Options",
                                                            tint = if (isCardSettingsOpen) colors.accent else colors.textSecondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        layoutRepo.deleteCustomLayout(cardData.id)
                                                        onToggleLayoutEnabled(cardData.id, false)
                                                        reloadTrigger++
                                                        Toast.makeText(context, "Deleted ${cardData.name}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete",
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val activeCustomSettingsCard = customCards.find { it.id == selectedCustomSettingsId }
                            AnimatedVisibility(
                                visible = activeCustomSettingsCard != null && activeCustomSettingsCard.layoutDef.extraRows.isNotEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                if (activeCustomSettingsCard != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = "Options",
                                                tint = colors.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${activeCustomSettingsCard.name} Options",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.accent
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        for (extraRow in activeCustomSettingsCard.layoutDef.extraRows) {
                                            val isRowEnabled = preferences.isExtraRowEnabled(
                                                activeCustomSettingsCard.id,
                                                extraRow.id,
                                                extraRow.defaultEnabled
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(colors.cardBackground.copy(alpha = 0.5f))
                                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = extraRow.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.textPrimary
                                                    )
                                                    Text(
                                                        text = "Show optional extra row above keyboard",
                                                        fontSize = 11.sp,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                Switch(
                                                    checked = isRowEnabled,
                                                    onCheckedChange = { checked ->
                                                        onToggleExtraRow(activeCustomSettingsCard.id, extraRow.id, checked)
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = colors.accent
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Import Layout Button
        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsItemCard(
                position = GroupPosition.ALONE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Import",
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Custom Layout (.json)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Load external keyboard layout file from storage",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardLayoutPreviewItem(
    cardData: LayoutCardData,
    preferences: KiebordPreferences,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSettingsColors.current
    val context = LocalContext.current
    val currentScheme = remember(context, preferences.theme) {
        io.kiebord.theme.ThemeRepository.getInstance(context).getColorScheme(preferences.theme)
    }

    val itemWidth = 164.dp
    val thumbnailHeight = 120.dp

    Column(
        modifier = modifier
            .width(itemWidth)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(thumbnailHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(currentScheme.containerBackground)
                .border(
                    width = if (isEnabled) 1.5.dp else 1.dp,
                    color = if (isEnabled) colors.accent.copy(alpha = 0.5f) else colors.cardBorder,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            KeyboardMiniPreview(
                layout = cardData.layoutDef,
                colorScheme = currentScheme,
                extraRowOverrides = preferences.extraRowOverrides,
                showKeyBorders = preferences.showKeyBorders,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.width(itemWidth),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isEnabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enabled",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .border(1.5.dp, colors.textSecondary.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }

            Text(
                text = cardData.name,
                fontSize = 13.sp,
                fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isEnabled) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (cardData.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = cardData.description,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KeyboardMiniPreview(
    layout: KeyboardLayoutDef,
    colorScheme: KeyboardColorScheme,
    extraRowOverrides: Map<String, Boolean>,
    showKeyBorders: Boolean = true,
    modifier: Modifier = Modifier
) {
    val enabledExtra = remember(layout, extraRowOverrides) {
        layout.extraRows.filter { extraRow ->
            extraRowOverrides["${layout.id}_${extraRow.id}"] ?: extraRow.defaultEnabled
        }.map { KeyRow(it.keys) }
    }
    val allRows = remember(layout, enabledExtra) {
        enabledExtra + layout.rows
    }

    val context = LocalContext.current
    val typeface = remember(context) {
        androidx.core.content.res.ResourcesCompat.getFont(context, io.kiebord.core.theme.R.font.keyboard_font)
            ?: android.graphics.Typeface.DEFAULT
    }

    val textPaint = remember(typeface) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            this.typeface = typeface
        }
    }

    val shiftPainter = painterResource(io.kiebord.feature.ime.R.drawable.ic_shift)
    val deletePainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Backspace)
    val actionPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.KeyboardReturn)
    val languagePainter = rememberVectorPainter(Icons.Outlined.Language)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val numRows = allRows.size
        if (numRows == 0 || totalWidth <= 0f || totalHeight <= 0f) return@Canvas

        val rowSpacingPx = 3.dp.toPx()
        val keySpacingPx = 2.dp.toPx()
        val cornerRadiusPx = 3.dp.toPx()

        val totalRowSpacing = (numRows - 1) * rowSpacingPx
        val rowHeight = ((totalHeight - totalRowSpacing) / numRows).coerceAtLeast(0f)

        for ((rowIndex, row) in allRows.withIndex()) {
            val rowY = rowIndex * (rowHeight + rowSpacingPx)
            val hasStartPadding = row.startPaddingWeight > 0f
            val hasEndPadding = row.endPaddingWeight > 0f
            val totalItemCount = (if (hasStartPadding) 1 else 0) + row.keys.size + (if (hasEndPadding) 1 else 0)
            val totalSpacing = if (totalItemCount > 1) (totalItemCount - 1) * keySpacingPx else 0f
            val availableKeySpace = (totalWidth - totalSpacing).coerceAtLeast(0f)

            val totalWeight = (if (hasStartPadding) row.startPaddingWeight else 0f) +
                    (if (hasEndPadding) row.endPaddingWeight else 0f) +
                    row.keys.sumOf { it.widthWeight.toDouble() }.toFloat()

            var currentX = 0f
            if (hasStartPadding && totalWeight > 0f) {
                val startPadWidth = (row.startPaddingWeight / totalWeight) * availableKeySpace
                currentX += startPadWidth + keySpacingPx
            }

            for (key in row.keys) {
                val keyWidth = if (totalWeight > 0f) {
                    (key.widthWeight / totalWeight) * availableKeySpace
                } else 0f

                val isComma = key.type == KeyType.CHARACTER && (key.label == "," || key.label == "،")
                val isPill = key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || (!showKeyBorders && key.type == KeyType.SPACE)
                val bg = when {
                    key.type == KeyType.ACTION -> colorScheme.accentColor
                    key.type == KeyType.SYMBOLS || isComma -> colorScheme.actionKeyBackground
                    key.type == KeyType.SPACE || key.type == KeyType.SWITCH_LANGUAGE -> colorScheme.keyBackground
                    key.type == KeyType.SHIFT || key.type == KeyType.DELETE -> colorScheme.actionKeyBackground
                    showKeyBorders -> colorScheme.keyBackground
                    else -> colorScheme.containerBackground
                }
                val fg = when {
                    key.type == KeyType.ACTION -> colorScheme.actionKeyForeground
                    else -> colorScheme.keyForeground
                }

                val keyCornerRadius = if (isPill) {
                    CornerRadius(rowHeight / 2f, rowHeight / 2f)
                } else {
                    CornerRadius(cornerRadiusPx, cornerRadiusPx)
                }

                drawRoundRect(
                    color = bg,
                    topLeft = Offset(currentX, rowY),
                    size = Size(keyWidth, rowHeight),
                    cornerRadius = keyCornerRadius
                )

                if (key.type == KeyType.CHARACTER || key.type == KeyType.SPACE || key.type == KeyType.SYMBOLS) {
                    val label = if (key.type == KeyType.SPACE) "" else key.label
                    if (label.isNotEmpty()) {
                        textPaint.textSize = (if (label.length > 2) 6.sp else 8.sp).toPx()
                        textPaint.color = fg.toArgb()
                        val fontMetrics = textPaint.fontMetrics
                        val baselineY = rowY + (rowHeight - fontMetrics.ascent - fontMetrics.descent) / 2f
                        val centerX = currentX + keyWidth / 2f
                        drawContext.canvas.nativeCanvas.drawText(label, centerX, baselineY, textPaint)
                    }
                } else if (key.type == KeyType.SHIFT) {
                    val iconSize = 9.5.dp.toPx()
                    val iconLeft = currentX + (keyWidth - iconSize) / 2f
                    val iconTop = rowY + (rowHeight - iconSize) / 2f
                    translate(left = iconLeft, top = iconTop) {
                        with(shiftPainter) {
                            draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(fg))
                        }
                    }
                } else if (key.type == KeyType.DELETE) {
                    val iconSize = 9.dp.toPx()
                    val iconLeft = currentX + (keyWidth - iconSize) / 2f
                    val iconTop = rowY + (rowHeight - iconSize) / 2f
                    translate(left = iconLeft, top = iconTop) {
                        with(deletePainter) {
                            draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(fg))
                        }
                    }
                } else if (key.type == KeyType.ACTION) {
                    val iconSize = 9.dp.toPx()
                    val iconLeft = currentX + (keyWidth - iconSize) / 2f
                    val iconTop = rowY + (rowHeight - iconSize) / 2f
                    translate(left = iconLeft, top = iconTop) {
                        with(actionPainter) {
                            draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(fg))
                        }
                    }
                } else if (key.type == KeyType.SWITCH_LANGUAGE) {
                    val iconSize = 9.dp.toPx()
                    val iconLeft = currentX + (keyWidth - iconSize) / 2f
                    val iconTop = rowY + (rowHeight - iconSize) / 2f
                    translate(left = iconLeft, top = iconTop) {
                        with(languagePainter) {
                            draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(fg))
                        }
                    }
                }

                currentX += keyWidth + keySpacingPx
            }
        }
    }
}
