package io.kiebord.data.layout

import android.content.Context
import io.kiebord.common.model.KeyboardLayoutDef
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class LayoutItemMeta(
    val id: String,
    val name: String,
    val displayName: String = name,
    val language: String,
    val direction: String,
    val isCustom: Boolean,
    val hasExtraRows: Boolean
)

class LayoutRepository private constructor(private val context: Context) {

    private val cache = ConcurrentHashMap<String, KeyboardLayoutDef>()
    @Volatile
    private var cachedAvailableLayouts: List<LayoutItemMeta>? = null
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val customLayoutsDir: File
        get() = File(context.filesDir, "layouts").apply { if (!exists()) mkdirs() }

    private val bundledAssetIds: List<String> by lazy {
        try {
            (context.assets.list("layouts") ?: emptyArray()).filter { it.endsWith(".json") }.map { it.removeSuffix(".json") }
        } catch (_: Exception) {
            emptyList()
        }
    }

    init {
        for (layout in PredefinedLayouts.allPredefined) {
            cache[layout.id] = layout
        }
        PredefinedLayouts.customLayoutResolver = { id ->
            cache[id]?.displayName ?: cache[id]?.name
        }
    }

    fun preload() {
        for (id in bundledAssetIds) {
            getLayout(id)
        }
        getAvailableLayouts()
    }

    fun getCachedLayout(id: String): KeyboardLayoutDef? = cache[id]

    fun getCachedAvailableLayouts(): List<LayoutItemMeta>? = cachedAvailableLayouts

    fun getLayout(id: String): KeyboardLayoutDef {
        cache[id]?.let { return it }

        // 1. Check user custom files
        val customFile = File(customLayoutsDir, "$id.json")
        if (customFile.exists()) {
            try {
                val layout = json.decodeFromString<KeyboardLayoutDef>(customFile.readText(Charsets.UTF_8))
                cache[id] = layout
                layout.effectiveSymbolsLayout()?.let { cache[it.id] = it }
                layout.effectiveMoreSymbolsLayout()?.let { cache[it.id] = it }
                return layout
            } catch (_: Exception) {}
        }

        // 2. Check bundled assets
        val candidates = listOf(id, id.removePrefix("bn_"), "bn_$id").distinct()
        for (name in candidates) {
            try {
                val assetPath = "layouts/$name.json"
                context.assets.open(assetPath).use { stream ->
                    val content = stream.bufferedReader(Charsets.UTF_8).readText()
                    val layout = json.decodeFromString<KeyboardLayoutDef>(content)
                    cache[id] = layout
                    cache[layout.id] = layout
                    cache[name] = layout
                    layout.effectiveSymbolsLayout()?.let { cache[it.id] = it }
                    layout.effectiveMoreSymbolsLayout()?.let { cache[it.id] = it }
                    return layout
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback to PredefinedLayouts
        val fallback = PredefinedLayouts.fromId(id)
        cache[id] = fallback
        return fallback
    }

    fun getAvailableLayouts(): List<LayoutItemMeta> {
        cachedAvailableLayouts?.let { return it }
        val result = mutableListOf<LayoutItemMeta>()
        val seenIds = mutableSetOf<String>()

        // Predefined
        for (layout in PredefinedLayouts.allPredefined) {
            if (seenIds.add(layout.id)) {
                result.add(
                    LayoutItemMeta(
                        id = layout.id,
                        name = layout.name,
                        displayName = layout.displayName ?: PredefinedLayouts.displayName(layout.id),
                        language = layout.language,
                        direction = layout.direction,
                        isCustom = false,
                        hasExtraRows = layout.extraRows.isNotEmpty()
                    )
                )
            }
        }

        // Bundled assets
        for (id in bundledAssetIds) {
            if (id !in seenIds) {
                try {
                    val layout = getLayout(id)
                    if (seenIds.add(layout.id)) {
                        result.add(
                            LayoutItemMeta(
                                id = layout.id,
                                name = layout.name,
                                displayName = layout.displayName ?: layout.name,
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
                                    displayName = layout.displayName ?: layout.name,
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

        cachedAvailableLayouts = result
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
            layout.effectiveSymbolsLayout()?.let { cache[it.id] = it }
            layout.effectiveMoreSymbolsLayout()?.let { cache[it.id] = it }
            cachedAvailableLayouts = null
            layout
        }
    }

    fun deleteCustomLayout(id: String): Boolean {
        val file = File(customLayoutsDir, "$id.json")
        val deleted = file.delete()
        if (deleted) {
            val removed = cache.remove(id)
            removed?.effectiveSymbolsLayout()?.let { cache.remove(it.id) }
            removed?.effectiveMoreSymbolsLayout()?.let { cache.remove(it.id) }
            cachedAvailableLayouts = null
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
