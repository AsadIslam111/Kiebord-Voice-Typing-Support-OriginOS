package io.kiebord.data.langpack

import kotlinx.serialization.Serializable

@Serializable
data class PackCatalogEntry(
    val id: String,
    val version: Int = 1,
    val name: String,
    val author: String = "",
    val description: String = "",
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long = 0L,
    val hasDictionary: Boolean = false,
    val layouts: List<String> = emptyList()
)

