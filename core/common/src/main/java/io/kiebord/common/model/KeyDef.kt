package io.kiebord.common.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyDef(
    val label: String = "",
    val shiftedLabel: String? = null,
    val type: KeyType = KeyType.CHARACTER,
    val icon: String? = null,
    val widthWeight: Float = 1.0f,
    val flickUp: String? = null,
    val moreKeys: List<String> = emptyList(),
    val moreKeysColumns: Int? = null,
    val slidingInput: Boolean = false,
    val commitText: String? = null,
    val shiftedFlickUp: String? = null,
    val shiftedMoreKeys: List<String>? = null
) {
    fun displayLabel(shiftState: ShiftState): String {
        return when (shiftState) {
            ShiftState.UNSHIFTED -> label
            ShiftState.ONE_SHOT, ShiftState.CAPS_LOCK -> shiftedLabel ?: label.uppercase()
        }
    }

    fun textToCommit(shiftState: ShiftState): String {
        return commitText ?: displayLabel(shiftState)
    }

    fun moreKeys(shiftState: ShiftState): List<String> {
        return when (shiftState) {
            ShiftState.UNSHIFTED -> moreKeys
            ShiftState.ONE_SHOT, ShiftState.CAPS_LOCK -> shiftedMoreKeys ?: moreKeys
        }
    }

    fun flickUp(shiftState: ShiftState): String? {
        return when (shiftState) {
            ShiftState.UNSHIFTED -> flickUp
            ShiftState.ONE_SHOT, ShiftState.CAPS_LOCK -> shiftedFlickUp ?: (if (shiftedMoreKeys != null) shiftedMoreKeys.firstOrNull() else flickUp)
        }
    }
}

@Serializable
data class KeyRow(
    val keys: List<KeyDef>,
    val startPaddingWeight: Float = 0f,
    val endPaddingWeight: Float = 0f,
    val startPaddingDp: Float = 0f,
    val endPaddingDp: Float = 0f
) {
    constructor(
        keys: List<KeyDef>,
        sidePaddingWeight: Float
    ) : this(
        keys = keys,
        startPaddingWeight = sidePaddingWeight,
        endPaddingWeight = sidePaddingWeight
    )
}

@Serializable
data class LayoutExtraRow(
    val id: String,
    val title: String,
    val defaultEnabled: Boolean = true,
    val keys: List<KeyDef>
)

@Serializable
data class KeyboardLayoutDef(
    val id: String,
    val name: String,
    val language: String,
    val direction: String = "LTR",
    val extraRows: List<LayoutExtraRow> = emptyList(),
    val numberRow: KeyRow? = null,
    val rows: List<KeyRow>
)
