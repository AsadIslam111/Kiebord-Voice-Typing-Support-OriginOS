package com.codepotro.borno.data.langpack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScriptRulesJson(
    @SerialName("primary_script") val primaryScript: String = "LATIN",
    @SerialName("allowed_characters") val allowedCharacters: String = "",
    @SerialName("word_connectors") val wordConnectors: List<String> = emptyList()
)

data class ScriptRules(
    val primaryScript: String,
    val allowedCharacters: Set<Char>,
    val wordConnectors: Set<Char>
)

