package io.kiebord.data.langpack

import kotlinx.serialization.Serializable

@Serializable
data class LanguagePackMetadata(
    val id: String,
    val version: Int = 1,
    val language: String,
    val locale: String = language,
    val displayName: String,
    val author: String = "",
    val license: String = "",
    val layouts: List<String> = emptyList(),
    val dictionaryFile: String? = null,
    val scriptRulesFile: String? = null,
    val charFoldingFile: String? = null,
    val currencySymbol: String? = null
)

