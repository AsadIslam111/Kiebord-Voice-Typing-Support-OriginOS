package io.kiebord.feature.ime.action

import io.kiebord.common.model.ShiftState

/**
 * Formats suggestion candidate text casing based on typed prefix and shift state.
 * Follows Borno Keyboard's approach:
 * - If Caps Lock is active, or user typed an acronym (>1 uppercase letter), format suggestions in ALL UPPERCASE (e.g. "USA").
 * - If only the first character is capitalized (e.g. at sentence start or Shift ONE_SHOT), format suggestions in Title Case ("Very", "Via", "The"), NOT all uppercase.
 * - Otherwise preserve the word's casing as stored in the dictionary / user dictionary (e.g. "Ahsan", "Saad", "very").
 * - Non-Latin text (e.g. Bengali) is preserved untouched.
 */
object SuggestionCasingUtils {

    fun formatCandidate(
        word: String,
        typedPrefix: String,
        shiftState: ShiftState = ShiftState.UNSHIFTED
    ): String {
        if (word.isEmpty() || !containsLatinText(word)) return word

        val isAllUpper = when {
            shiftState == ShiftState.CAPS_LOCK -> true
            typedPrefix.length > 1 && typedPrefix.all { it.isUpperCase() } -> true
            else -> false
        }
        val isFirstUpper = when {
            isAllUpper -> false
            typedPrefix.isNotEmpty() -> typedPrefix[0].isUpperCase()
            shiftState == ShiftState.ONE_SHOT -> true
            else -> false
        }

        return when {
            isAllUpper -> word.uppercase()
            isFirstUpper -> word.replaceFirstChar { it.uppercase() }
            else -> word
        }
    }

    private fun containsLatinText(text: String): Boolean =
        text.any { (it in 'a'..'z') || (it in 'A'..'Z') }
}

