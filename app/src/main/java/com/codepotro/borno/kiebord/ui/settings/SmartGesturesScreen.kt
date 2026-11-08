package com.codepotro.borno.kiebord.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codepotro.borno.data.prefs.KiebordPreferences

@Composable
fun SmartGesturesScreen(
    preferences: KiebordPreferences,
    onUpdateFastDeleteEnabled: (Boolean) -> Unit,
    onUpdateSelectiveDeleteEnabled: (Boolean) -> Unit,
    onUpdateSelectiveDeleteMode: (String) -> Unit,
    onUpdateCursorTrackpad: (Boolean) -> Unit,
    onUpdateSpacebarSwipeMode: (String) -> Unit,
    onUpdateVolumeKeyGesturesEnabled: (Boolean) -> Unit,
    onUpdateVolumeKeyGestureAction: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Delete Gestures")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Fast Delete",
                subtitle = "Delete all text before cursor by sliding up from the backspace key to the top row",
                checked = preferences.fastDeleteEnabled,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateFastDeleteEnabled
            )

            SettingsToggleItem(
                title = "Selective Delete",
                subtitle = "Swiping left from backspace selects and deletes text",
                checked = preferences.selectiveDeleteEnabled,
                position = if (preferences.selectiveDeleteEnabled) GroupPosition.MIDDLE else GroupPosition.BOTTOM,
                onCheckedChange = onUpdateSelectiveDeleteEnabled
            )

            if (preferences.selectiveDeleteEnabled) {
                SettingsRadioOption(
                    title = "By Word",
                    subtitle = "Swiping left from backspace selects and deletes text word-by-word",
                    selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD,
                    position = GroupPosition.MIDDLE,
                    onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_WORD) }
                )

                SettingsRadioOption(
                    title = "By Character",
                    subtitle = "Swiping left from backspace selects and deletes text character-by-character",
                    selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_CHARACTER,
                    position = GroupPosition.BOTTOM,
                    onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_CHARACTER) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Spacebar Gestures")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Spacebar Cursor Trackpad",
                subtitle = "Slide thumb horizontally across spacebar to position cursor",
                checked = preferences.cursorTrackpadEnabled,
                position = if (preferences.cursorTrackpadEnabled) GroupPosition.TOP else GroupPosition.ALONE,
                onCheckedChange = onUpdateCursorTrackpad
            )

            if (preferences.cursorTrackpadEnabled) {
                SettingsRadioOption(
                    title = "Swipe to switch layout, hold & slide to move cursor",
                    subtitle = "Immediate swipe cycles layouts; holding then sliding moves cursor",
                    selected = preferences.spacebarSwipeMode == KiebordPreferences.SPACEBAR_MODE_SWIPE_LAYOUT_HOLD_CURSOR,
                    position = GroupPosition.MIDDLE,
                    onSelect = { onUpdateSpacebarSwipeMode(KiebordPreferences.SPACEBAR_MODE_SWIPE_LAYOUT_HOLD_CURSOR) }
                )

                SettingsRadioOption(
                    title = "Swipe to move cursor, hold & slide to switch layout",
                    subtitle = "Immediate swipe moves cursor; holding then sliding cycles layouts",
                    selected = preferences.spacebarSwipeMode == KiebordPreferences.SPACEBAR_MODE_SWIPE_CURSOR_HOLD_LAYOUT,
                    position = GroupPosition.BOTTOM,
                    onSelect = { onUpdateSpacebarSwipeMode(KiebordPreferences.SPACEBAR_MODE_SWIPE_CURSOR_HOLD_LAYOUT) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Volume Key Gestures")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Volume Key Gestures",
                subtitle = "Use hardware volume keys for typing shortcuts while keyboard is visible",
                checked = preferences.volumeKeyGesturesEnabled,
                position = if (preferences.volumeKeyGesturesEnabled) GroupPosition.TOP else GroupPosition.ALONE,
                onCheckedChange = onUpdateVolumeKeyGesturesEnabled
            )

            if (preferences.volumeKeyGesturesEnabled) {
                SettingsRadioOption(
                    title = "Move Cursor",
                    subtitle = "Volume Down moves cursor left, Volume Up moves cursor right",
                    selected = preferences.volumeKeyGestureAction == KiebordPreferences.VOLUME_ACTION_MOVE_CURSOR,
                    position = GroupPosition.MIDDLE,
                    onSelect = { onUpdateVolumeKeyGestureAction(KiebordPreferences.VOLUME_ACTION_MOVE_CURSOR) }
                )

                SettingsRadioOption(
                    title = "Switch Layout",
                    subtitle = "Volume Down switches to previous layout, Volume Up to next layout",
                    selected = preferences.volumeKeyGestureAction == KiebordPreferences.VOLUME_ACTION_SWITCH_LAYOUT,
                    position = GroupPosition.BOTTOM,
                    onSelect = { onUpdateVolumeKeyGestureAction(KiebordPreferences.VOLUME_ACTION_SWITCH_LAYOUT) }
                )
            }
        }
    }
}

