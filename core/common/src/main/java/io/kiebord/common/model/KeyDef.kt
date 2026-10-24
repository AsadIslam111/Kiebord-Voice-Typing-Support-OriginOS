package io.kiebord.common.model

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
    val commitText: String? = null
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
}

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

data class KeyboardLayoutDef(
    val id: String,
    val name: String,
    val language: String,
    val rows: List<KeyRow>
)
