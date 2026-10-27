package io.kiebord.data.layout

import android.content.Context
import io.kiebord.common.model.KeyboardLayoutDef
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class LayoutItemMeta(
    val id: String,
    val name: String,
    val language: String,
    val direction: String,
    val isCustom: Boolean,
    val hasExtraRows: Boolean
)

class LayoutRepository private constructor(private val context: Context) {

    private val cache = ConcurrentHashMap<String, KeyboardLayoutDef>()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val customLayoutsDir: File
        get() = File(context.filesDir, "layouts").apply { if (!exists()) mkdirs() }

    init {
        for (layout in PredefinedLayouts.allPredefined) {
            cache[layout.id] = layout
        }
    }

    fun getLayout(id: String): KeyboardLayoutDef {
        cache[id]?.let { return it }

        // 1. Check user custom files
        val customFile = File(customLayoutsDir, "$id.json")
        if (customFile.exists()) {
            try {
                val layout = json.decodeFromString<KeyboardLayoutDef>(customFile.readText(Charsets.UTF_8))
                cache[id] = layout
                return layout
            } catch (_: Exception) {}
        }

        // 2. Check bundled assets
        try {
            val assetPath = "layouts/$id.json"
            context.assets.open(assetPath).use { stream ->
                val content = stream.bufferedReader(Charsets.UTF_8).readText()
                val layout = json.decodeFromString<KeyboardLayoutDef>(content)
                cache[id] = layout
                return layout
            }
        } catch (_: Exception) {}

        // 3. Fallback to PredefinedLayouts
        val fallback = PredefinedLayouts.fromId(id)
        cache[id] = fallback
        return fallback
    }

    fun getAvailableLayouts(): List<LayoutItemMeta> {
        val result = mutableListOf<LayoutItemMeta>()
        val seenIds = mutableSetOf<String>()

        // Predefined
        for (layout in PredefinedLayouts.allPredefined) {
            if (seenIds.add(layout.id)) {
                result.add(
                    LayoutItemMeta(
                        id = layout.id,
                        name = layout.name,
                        language = layout.language,
                        direction = layout.direction,
                        isCustom = false,
                        hasExtraRows = layout.extraRows.isNotEmpty()
                    )
                )
            }
        }

        // Bundled assets
        try {
            val list = context.assets.list("layouts") ?: emptyArray()
            for (filename in list) {
                if (filename.endsWith(".json")) {
                    val id = filename.removeSuffix(".json")
                    if (id !in seenIds) {
                        try {
                            val layout = getLayout(id)
                            if (seenIds.add(layout.id)) {
                                result.add(
                                    LayoutItemMeta(
                                        id = layout.id,
                                        name = layout.name,
                                        language = layout.language,
                                        direction = layout.direction,
                                        isCustom = false,
                                        hasExtraRows = layout.extraRows.isNotEmpty()
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        // Custom imported files
        try {
            val files = customLayoutsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            for (file in files) {
                val id = file.name.removeSuffix(".json")
                if (id !in seenIds) {
                    try {
                        val layout = getLayout(id)
                        if (seenIds.add(layout.id)) {
                            result.add(
                                LayoutItemMeta(
                                    id = layout.id,
                                    name = layout.name,
                                    language = layout.language,
                                    direction = layout.direction,
                                    isCustom = true,
                                    hasExtraRows = layout.extraRows.isNotEmpty()
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        return result
    }

    fun importLayout(jsonContent: String): Result<KeyboardLayoutDef> {
        return runCatching {
            val layout = json.decodeFromString<KeyboardLayoutDef>(jsonContent)
            require(layout.id.isNotBlank()) { "Layout id must not be blank" }
            require(layout.rows.isNotEmpty()) { "Layout must contain at least one row" }
            val file = File(customLayoutsDir, "${layout.id}.json")
            file.writeText(jsonContent, Charsets.UTF_8)
            cache[layout.id] = layout
            layout
        }
    }

    fun deleteCustomLayout(id: String): Boolean {
        val file = File(customLayoutsDir, "$id.json")
        val deleted = file.delete()
        if (deleted) {
            cache.remove(id)
        }
        return deleted
    }

    companion object {
        @Volatile
        private var instance: LayoutRepository? = null

        fun getInstance(context: Context): LayoutRepository {
            return instance ?: synchronized(this) {
                instance ?: LayoutRepository(context.applicationContext ?: context).also { instance = it }
            }
        }
    }
}
