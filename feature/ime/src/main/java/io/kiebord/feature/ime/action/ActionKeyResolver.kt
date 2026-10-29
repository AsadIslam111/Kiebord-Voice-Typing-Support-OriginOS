package io.kiebord.feature.ime.action

import android.text.InputType
import android.view.inputmethod.EditorInfo
import io.kiebord.common.model.ShiftState

enum class ActionKeyType {
    ENTER,
    SEARCH,
    SEND,
    GO,
    NEXT,
    DONE,
    PREVIOUS,
    CUSTOM_LABEL
}

data class ActionKeyVisual(
    val type: ActionKeyType,
    val isAction: Boolean,
    val actionId: Int,
    val customLabel: String? = null,
    val contentDescription: String,
    val isAccentStyle: Boolean
) {
    companion object {
        val DefaultEnter = ActionKeyVisual(
            type = ActionKeyType.ENTER,
            isAction = false,
            actionId = EditorInfo.IME_ACTION_NONE,
            customLabel = null,
            contentDescription = "New line",
            isAccentStyle = false
        )
    }
}

object ActionKeyResolver {
    fun resolve(editorInfo: EditorInfo?, shiftState: ShiftState = ShiftState.UNSHIFTED): ActionKeyVisual {
        if (editorInfo == null) return ActionKeyVisual.DefaultEnter

        val imeOptions = editorInfo.imeOptions
        val rawAction = imeOptions and EditorInfo.IME_MASK_ACTION
        val hasNoEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val inputType = editorInfo.inputType
        val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        // Shift + Enter override: When user presses Shift in a field that has an action (like Send in chat),
        // pressing Enter inserts a newline instead of sending the message.
        if (shiftState != ShiftState.UNSHIFTED && !hasNoEnterAction) {
            return ActionKeyVisual.DefaultEnter
        }

        // Multi-line and NO_ENTER_ACTION rule:
        // Android sets IME_FLAG_NO_ENTER_ACTION for multiline inputs (like Facebook comment boxes, notes).
        // In these fields, the key must act as a newline return button.
        if (hasNoEnterAction || (isMultiLine && (rawAction == EditorInfo.IME_ACTION_UNSPECIFIED || rawAction == EditorInfo.IME_ACTION_NONE))) {
            return ActionKeyVisual.DefaultEnter
        }

        // Custom action label specified by target application (e.g. "Post", "Search")
        val label = editorInfo.actionLabel?.toString()
        if (!label.isNullOrBlank()) {
            val actionId = if (editorInfo.actionId != 0) editorInfo.actionId else rawAction
            return ActionKeyVisual(
                type = ActionKeyType.CUSTOM_LABEL,
                isAction = true,
                actionId = actionId,
                customLabel = label,
                contentDescription = label,
                isAccentStyle = true
            )
        }

        return when (rawAction) {
            EditorInfo.IME_ACTION_SEARCH -> ActionKeyVisual(
                type = ActionKeyType.SEARCH,
                isAction = true,
                actionId = EditorInfo.IME_ACTION_SEARCH,
                contentDescription = "Search",
                isAccentStyle = true
            )
            EditorInfo.IME_ACTION_SEND -> ActionKeyVisual(
                type = ActionKeyType.SEND,
                isAction = true,
                actionId = EditorInfo.IME_ACTION_SEND,
                contentDescription = "Send",
                isAccentStyle = true
            )
            EditorInfo.IME_ACTION_GO -> ActionKeyVisual(
                type = ActionKeyType.GO,
                isAction = true,
                actionId = EditorInfo.IME_ACTION_GO,
                contentDescription = "Go",
                isAccentStyle = true
            )
            EditorInfo.IME_ACTION_NEXT -> ActionKeyVisual(
                type = ActionKeyType.NEXT,
                isAction = true,
                actionId = EditorInfo.IME_ACTION_NEXT,
                contentDescription = "Next field",
                isAccentStyle = true
            )
            EditorInfo.IME_ACTION_DONE -> ActionKeyVisual(
                type = ActionKeyType.DONE,
                isAction = true,
                actionId = EditorInfo.IME_ACTION_DONE,
                contentDescription = "Done",
                isAccentStyle = true
            )
            EditorInfo.IME_ACTION_PREVIOUS -> ActionKeyVisual(
                type = ActionKeyType.PREVIOUS,
                isAction = true,
                actionId = EditorInfo.IME_ACTION_PREVIOUS,
                contentDescription = "Previous field",
                isAccentStyle = true
            )
            else -> ActionKeyVisual.DefaultEnter
        }
    }
}

