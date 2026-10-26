package io.kiebord.feature.ime.ui.emoji

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import io.kiebord.feature.ime.R
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme

/**
 * Attaches Back-to-Keyboard and Emoji Search buttons to the category tab row
 * right before the AndroidX EmojiPicker categories.
 */
private fun attachHeaderControls(
    picker: EmojiPickerView,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    colorScheme: KeyboardColorScheme
) {
    try {
        val header = picker.findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_header) ?: return
        val parent = header.parent as? ViewGroup ?: return
        if (parent.findViewWithTag<View>("kiebord_emoji_nav") != null) return

        val headerIdx = parent.indexOfChild(header)
        parent.removeView(header)

        val context = picker.context
        val density = context.resources.displayMetrics.density
        val btnSizePx = (38 * density).toInt()
        val iconPadPx = (8 * density).toInt()

        val topNavRow = LinearLayout(context).apply {
            tag = "kiebord_emoji_nav"
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                header.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        val selectableBackgroundRes = outValue.resourceId

        // 1. Back to keyboard button
        val backBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_emoji_arrow_back)
            imageTintList = ColorStateList.valueOf(colorScheme.keyForeground.toArgb())
            contentDescription = "Back to keyboard"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(iconPadPx, iconPadPx, iconPadPx, iconPadPx)
            if (selectableBackgroundRes != 0) {
                setBackgroundResource(selectableBackgroundRes)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClose() }
        }

        // 2. Search icon button
        val searchBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_emoji_search)
            imageTintList = ColorStateList.valueOf(colorScheme.keyForeground.toArgb())
            contentDescription = "Search emojis"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(iconPadPx, iconPadPx, iconPadPx, iconPadPx)
            if (selectableBackgroundRes != 0) {
                setBackgroundResource(selectableBackgroundRes)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onOpenSearch() }
        }

        val btnParams = LinearLayout.LayoutParams(btnSizePx, btnSizePx).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        topNavRow.addView(backBtn, btnParams)
        topNavRow.addView(searchBtn, btnParams)

        header.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
        topNavRow.addView(header)

        parent.addView(topNavRow, headerIdx)
    } catch (_: Exception) {}
}

@Composable
private fun EmojiBottomKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseBackground: Color,
    keyShape: Shape,
    verticalPadding: Dp,
    showShadow: Boolean,
    colorScheme: KeyboardColorScheme,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val activeBg = if (isPressed) {
        if (colorScheme.keyPressedBackground != Color.Unspecified) colorScheme.keyPressedBackground
        else colorScheme.accentColor.copy(alpha = 0.28f)
    } else {
        baseBackground
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 3.dp, vertical = verticalPadding)
            .then(
                if (showShadow && colorScheme.keyShadow.alpha > 0.01f) {
                    Modifier.shadow(
                        elevation = 1.5.dp,
                        shape = keyShape,
                        clip = false,
                        ambientColor = colorScheme.keyShadow,
                        spotColor = colorScheme.keyShadow
                    )
                } else Modifier
            )
            .clip(keyShape)
            .background(activeBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun EmojiPanelView(
    onEmojiSelected: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 290.dp,
    showKeyBorders: Boolean = true
) {
    val colorScheme = KiebordTheme.colorScheme
    val context = LocalContext.current

    val isDark = colorScheme.containerBackground.luminance() < 0.5f
    val themeResId = if (isDark) R.style.Theme_Kiebord_EmojiPicker_Dark else R.style.Theme_Kiebord_EmojiPicker_Light

    val themedContext = remember(context, themeResId) {
        ContextThemeWrapper(context, themeResId)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
    ) {
        // 1. AndroidX EmojiPicker View (smooth 120fps RecyclerView with official category icons)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalWidthDp = maxWidth
            // Dynamic column calculation:
            // Standard emoji cell is ~38-42dp wide.
            // In portrait (~360-412dp), this yields 9-10 columns.
            // In landscape (~700-1000dp), this yields 17-25 columns, keeping emojis at normal size!
            val calculatedColumns = remember(totalWidthDp) {
                (totalWidthDp / 40.dp).toInt().coerceIn(9, 25)
            }

            AndroidView(
                factory = {
                    EmojiFontManager.clearEmojiPickerCache(themedContext)
                    EmojiPickerView(themedContext).apply {
                        emojiGridColumns = calculatedColumns
                        setOnEmojiPickedListener { item ->
                            onEmojiSelected(item.emoji)
                        }

                        setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                            override fun onChildViewAdded(parent: View?, child: View?) {
                                attachHeaderControls(this@apply, onClose, onOpenSearch, colorScheme)
                            }
                            override fun onChildViewRemoved(parent: View?, child: View?) {}
                        })

                        post {
                            attachHeaderControls(this, onClose, onOpenSearch, colorScheme)
                        }
                    }
                },
                update = { picker ->
                    if (picker.emojiGridColumns != calculatedColumns) {
                        picker.emojiGridColumns = calculatedColumns
                    }
                    attachHeaderControls(picker, onClose, onOpenSearch, colorScheme)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        }

        // 2. Bottom Action Bar: [ABC], [Spacebar], [Delete] following keyboard key colors and shapes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "ABC" return key (left)
            val abcShape = RoundedCornerShape(percent = 50)
            EmojiBottomKey(
                onClick = onClose,
                baseBackground = colorScheme.actionKeyBackground,
                keyShape = abcShape,
                verticalPadding = if (showKeyBorders) 3.dp else 6.dp,
                showShadow = showKeyBorders,
                colorScheme = colorScheme,
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    text = "ABC",
                    color = colorScheme.keyForeground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Spacebar keycap (center)
            val spaceShape = if (showKeyBorders) RoundedCornerShape(8.dp) else RoundedCornerShape(percent = 50)
            EmojiBottomKey(
                onClick = onSpace,
                baseBackground = colorScheme.keyBackground,
                keyShape = spaceShape,
                verticalPadding = if (showKeyBorders) 3.dp else 6.dp,
                showShadow = showKeyBorders,
                colorScheme = colorScheme,
                modifier = Modifier.weight(1f)
            ) {}

            // Delete / Backspace keycap (right)
            val deleteShape = RoundedCornerShape(8.dp)
            EmojiBottomKey(
                onClick = onDelete,
                baseBackground = if (showKeyBorders) colorScheme.actionKeyBackground else colorScheme.containerBackground,
                keyShape = deleteShape,
                verticalPadding = 3.dp,
                showShadow = showKeyBorders,
                colorScheme = colorScheme,
                modifier = Modifier.width(64.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = colorScheme.keyForeground,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
