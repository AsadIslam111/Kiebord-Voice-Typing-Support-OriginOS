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
    val shiftedCommitText: String? = null,
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
        return when (shiftState) {
            ShiftState.UNSHIFTED -> commitText ?: label
            ShiftState.ONE_SHOT, ShiftState.CAPS_LOCK -> shiftedCommitText ?: shiftedLabel ?: (commitText ?: label).uppercase()
        }
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

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class KeyboardLayoutDef(
    val id: String = "symbols",
    val name: String = "Symbols",
    val displayName: String? = null,
    val language: String = "",
    val direction: String = "LTR",
    @kotlinx.serialization.json.JsonNames("gesture_mode", "gestureMode")
    val gestureMode: String? = null,
    val processor: String? = null,
    val rules: String? = null,
    val extraRows: List<LayoutExtraRow> = emptyList(),
    val numberRow: KeyRow? = null,
    val shiftedNumberRow: KeyRow? = null,
    val symbolsLayout: KeyboardLayoutDef? = null,
    val symbols: KeyboardLayoutDef? = null,
    val moreSymbolsLayout: KeyboardLayoutDef? = null,
    val rows: List<KeyRow> = emptyList()
) {
    fun effectiveDisplayName(): String = displayName ?: name.ifEmpty { id }
    fun effectiveSymbolsLayout(): KeyboardLayoutDef? = symbolsLayout ?: symbols
    fun effectiveMoreSymbolsLayout(): KeyboardLayoutDef? = moreSymbolsLayout
    fun isSymbolsLayout(): Boolean = id.startsWith("symbols") || id.contains("_symbols") || language == "sym"

    fun isQwertyBased(): Boolean {
        if (isSymbolsLayout()) return false
        if (gestureMode?.equals("qwerty", ignoreCase = true) == true) return true
        if (id == "en_qwerty" || id == "bn_borno" || id == "bn_avro") return true
        return rows.any { row ->
            val charLabels = row.keys.filter { it.type == KeyType.CHARACTER }.map { it.label.lowercase() }
            charLabels.size >= 6 && charLabels.take(6) == listOf("q", "w", "e", "r", "t", "y")
        }
    }

    /**
     * Resolves extra rows for a symbols layout based on the active alphabet layout.
     * For each enabled extra row in the alphabet layout, uses the corresponding row in this symbols layout
     * if found, otherwise moves the alphabet extra row to this symbols layout.
     */
    fun resolveExtraRowsForSymbols(
        alphaLayout: KeyboardLayoutDef,
        extraRowOverrides: Map<String, Boolean> = emptyMap()
    ): List<KeyRow> {
        if (alphaLayout === this || alphaLayout.isSymbolsLayout()) {
            return this.extraRows.filter { extraRow ->
                val primaryId = this.id.removeSuffix("_symbols").removeSuffix("_symbols_more").removeSuffix("_1").removeSuffix("_2")
                extraRowOverrides["${this.id}_${extraRow.id}"]
                    ?: extraRowOverrides["${primaryId}_${extraRow.id}"]
                    ?: extraRowOverrides[extraRow.id]
                    ?: extraRow.defaultEnabled
            }.map { KeyRow(it.keys) }
        }

        val enabledAlphaExtra = alphaLayout.extraRows.filter { extraRow ->
            val primaryId = alphaLayout.id.removeSuffix("_symbols").removeSuffix("_symbols_more").removeSuffix("_1").removeSuffix("_2")
            extraRowOverrides["${alphaLayout.id}_${extraRow.id}"]
                ?: extraRowOverrides["${primaryId}_${extraRow.id}"]
                ?: extraRowOverrides[extraRow.id]
                ?: extraRow.defaultEnabled
        }

        return enabledAlphaExtra.map { alphaExtra ->
            val corresponding = this.extraRows.find { it.id == alphaExtra.id }
            if (corresponding != null) {
                KeyRow(corresponding.keys)
            } else {
                KeyRow(alphaExtra.keys)
            }
        }
    }

    /**
     * Resolves the 4 core rows for a symbols layout based on whether the top number row is enabled.
     * When showNumberRow is true and the first core row consists of digits (or shift 1 symbols),
     * it is replaced with the alternate symbols row so numbers are not duplicated across rows.
     */
    fun resolveCoreRowsForSymbols(showNumberRow: Boolean): List<KeyRow> {
        if (this.rows.isEmpty()) return emptyList()

        // 6-row layouts (e.g. Borno Easy symbols) preserve all 6 core rows across both modes
        if (this.rows.size >= 6) {
            return this.rows
        }

        val isMoreSymbols = this.id.contains("symbols_2") || this.id.contains("more")

        if (!showNumberRow) {
            return if (this.rows.size >= 5) {
                if (isDigitsRow(this.rows[0]) || isMoreSymbols) {
                    listOf(this.rows[0]) + this.rows.drop(2)
                } else {
                    this.rows
                }
            } else {
                this.rows
            }
        }

        // When showNumberRow is true:
        if (this.rows.size >= 5) {
            if (isDigitsRow(this.rows[0]) || isMoreSymbols) {
                return this.rows.drop(1)
            }
        }

        if (this.rows.size == 4) {
            if (isMoreSymbols) {
                return listOf(SymbolsMoreAlternateRow) + this.rows.drop(1)
            } else if (isDigitsRow(this.rows[0])) {
                return listOf(SymbolsAlternateRow) + this.rows.drop(1)
            }
        }

        return this.rows
    }

    companion object {
        val SymbolsAlternateRow = KeyRow(
            listOf(
                KeyDef("~"),
                KeyDef("<", moreKeys = listOf("<", "«", "≤")),
                KeyDef(">", moreKeys = listOf(">", "»", "≥")),
                KeyDef("_"),
                KeyDef("`"),
                KeyDef("º", moreKeys = listOf("º", "°")),
                KeyDef("\\"),
                KeyDef("=", moreKeys = listOf("=", "≠", "≈", "∞")),
                KeyDef("{"),
                KeyDef("}")
            )
        )

        val SymbolsMoreAlternateRow = KeyRow(
            listOf(
                KeyDef("≠"),
                KeyDef("≤"),
                KeyDef("≥"),
                KeyDef("∫"),
                KeyDef("Σ"),
                KeyDef("≈"),
                KeyDef("±"),
                KeyDef("∓"),
                KeyDef("½"),
                KeyDef("∞")
            )
        )

        fun isDigitsRow(row: KeyRow): Boolean {
            val charKeys = row.keys.filter { it.type == KeyType.CHARACTER }
            if (charKeys.isEmpty()) return false
            val digitCount = charKeys.count { key ->
                val text = key.label.trim()
                text.isNotEmpty() && text.all { ch ->
                    ch.isDigit() || ch in '٠'..'٩' || ch in '۰'..'۹' || ch in '০'..'৯'
                }
            }
            return digitCount >= 7 || (digitCount >= charKeys.size / 2 && digitCount >= 4)
        }

        fun isDuplicateRow(
            extraRow: LayoutExtraRow,
            candidateRows: List<KeyRow>,
            candidateExtra: List<LayoutExtraRow> = emptyList()
        ): Boolean {
            if (candidateExtra.any { it.id == extraRow.id }) return true

            val extraLabels = extraRow.keys
                .filter { it.type == KeyType.CHARACTER }
                .mapNotNull { key ->
                    val text = key.label.ifBlank { key.commitText ?: "" }.trim()
                    text.ifEmpty { null }
                }
                .toSet()
            if (extraLabels.isEmpty()) return false

            for (extra in candidateExtra) {
                val candidateLabels = extra.keys
                    .filter { it.type == KeyType.CHARACTER }
                    .mapNotNull { key ->
                        val text = key.label.ifBlank { key.commitText ?: "" }.trim()
                        text.ifEmpty { null }
                    }
                    .toSet()
                if (candidateLabels.isEmpty()) continue
                val intersection = extraLabels.intersect(candidateLabels)
                val minThreshold = minOf(extraLabels.size, candidateLabels.size)
                if (intersection.size >= minThreshold || (minThreshold > 2 && intersection.size >= (minThreshold + 1) / 2)) {
                    return true
                }
            }

            for (row in candidateRows) {
                val candidateLabels = row.keys
                    .filter { it.type == KeyType.CHARACTER }
                    .mapNotNull { key ->
                        val text = key.label.ifBlank { key.commitText ?: "" }.trim()
                        text.ifEmpty { null }
                    }
                    .toSet()
                if (candidateLabels.isEmpty()) continue
                val intersection = extraLabels.intersect(candidateLabels)
                val minThreshold = minOf(extraLabels.size, candidateLabels.size)
                if (intersection.size >= minThreshold || (minThreshold > 2 && intersection.size >= (minThreshold + 1) / 2)) {
                    return true
                }
            }
            return false
        }
    }
}
