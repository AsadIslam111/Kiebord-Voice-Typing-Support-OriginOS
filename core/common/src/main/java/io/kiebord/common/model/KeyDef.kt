package io.kiebord.common.model

data class KeyDef(
    val label: String = "",
    val shiftedLabel: String? = null,
    val type: KeyType = KeyType.CHARACTER,
    val icon: String? = null,
    val widthWeight: Float = 1.0f,
    val flickUp: String? = null,
    val moreKeys: List<String> = emptyList(),
    val slidingInput: Boolean = false
) {
    fun displayLabel(shiftState: ShiftState): String {
        return when (shiftState) {
            ShiftState.UNSHIFTED -> label
            ShiftState.ONE_SHOT, ShiftState.CAPS_LOCK -> shiftedLabel ?: label.uppercase()
        }
    }
}

data class KeyRow(
    val keys: List<KeyDef>
)

data class KeyboardLayoutDef(
    val id: String,
    val name: String,
    val language: String,
    val rows: List<KeyRow>
)
