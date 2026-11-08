package com.codepotro.borno.data.dictionary

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a custom vocabulary entry or shortcut-to-text expansion mapping in Kiebord.
 *
 * @param id Unique identifier.
 * @param word The expanded text or custom word to commit (e.g., "আসসালামু আলাইকুম", "Antigravity").
 * @param shortcut Optional shortcut key sequence used to trigger this expansion (e.g., "asm").
 * @param language Target layout language: [LANG_ALL] ("all"), [LANG_BN] ("bn"), or [LANG_EN] ("en").
 * @param timestamp Epoch creation or update millisecond.
 */
@Serializable
data class PersonalDictionaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val shortcut: String? = null,
    val language: String = LANG_ALL,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val LANG_ALL = "all"
        const val LANG_BN = "bn"
        const val LANG_EN = "en"

        fun normalizeShortcut(shortcut: String?): String? {
            val trimmed = shortcut?.trim()
            return if (trimmed.isNullOrEmpty()) null else trimmed.lowercase()
        }

        fun normalizeWord(word: String): String {
            return word.trim()
        }
    }

    /**
     * Checks if this entry applies to the given layout language code.
     * Universal ([LANG_ALL]) entries match any keyboard layout.
     */
    fun matchesLanguage(layoutLang: String): Boolean {
        if (language.equals(LANG_ALL, ignoreCase = true) || layoutLang.equals(LANG_ALL, ignoreCase = true)) {
            return true
        }
        val normalizedEntry = language.lowercase().trim()
        val normalizedTarget = layoutLang.lowercase().trim()
        return normalizedEntry == normalizedTarget
    }
}

/**
 * Container model used for JSON export/import backups of personal dictionary entries.
 */
@Serializable
data class PersonalDictionaryDbPayload(
    val version: Int = 1,
    val data: List<PersonalDictionaryEntry> = emptyList()
)

@Serializable
data class PersonalDictionaryBackup(
    val format: String = FORMAT_BORNOPDB,
    val schemaVersion: Int = 1,
    val type: String = TYPE_PERSONAL_DICTIONARY,
    val generator: String = GENERATOR_KIEBORD,
    val exportedAt: Long = System.currentTimeMillis(),
    val count: Int = 0,
    val db: PersonalDictionaryDbPayload = PersonalDictionaryDbPayload(),
    val entries: List<PersonalDictionaryEntry> = emptyList(),
    val version: Int = 1,
    val exportTime: Long = exportedAt
) {
    companion object {
        const val FORMAT_BORNOPDB = "bornopdb"
        const val TYPE_PERSONAL_DICTIONARY = "personal_dictionary"
        const val GENERATOR_KIEBORD = "kiebord"
        const val EXTENSION_BORNOPDB = "bornopdb"
    }
}
