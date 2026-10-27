package io.kiebord.theme

import android.content.Context
import io.kiebord.theme.ThemeColorUtils.toCustomThemeDef
import io.kiebord.theme.ThemeColorUtils.toKeyboardColorScheme
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class ThemeCategory(val title: String) {
    ORIGINALS("Kiebord Originals"),
    BORNO_AMOLED("Borno Classics & Dark"),
    BORNO_AESTHETIC("Borno Aesthetic & Vibrant"),
    BORNO_COLORS("Borno Colors & Pastels"),
    CUSTOM("Custom Imported Themes")
}

data class ThemeOptionItem(
    val id: String,
    val name: String,
    val description: String,
    val scheme: KeyboardColorScheme,
    val isCustom: Boolean = false,
    val category: ThemeCategory = ThemeCategory.BORNO_COLORS
)

class ThemeRepository private constructor(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val customThemesDir: File
        get() = File(context.filesDir, "themes").apply { if (!exists()) mkdirs() }

    private val originalThemes = linkedMapOf<String, ThemeOptionItem>(
        "default_dark" to ThemeOptionItem(
            id = "default_dark",
            name = "Default Dark",
            description = "Material dark charcoal with Google pastel blue accents",
            scheme = KeyboardColorScheme.DefaultDark,
            category = ThemeCategory.ORIGINALS
        ),
        "amoled_black" to ThemeOptionItem(
            id = "amoled_black",
            name = "AMOLED Black",
            description = "Pitch black with high-contrast keys and blue pill accents",
            scheme = KeyboardColorScheme.AmoledBlack,
            category = ThemeCategory.ORIGINALS
        ),
        "tokyo_night" to ThemeOptionItem(
            id = "tokyo_night",
            name = "Tokyo Night",
            description = "Deep indigo twilight inspired by neon cityscapes",
            scheme = KeyboardColorScheme.TokyoNight,
            category = ThemeCategory.ORIGINALS
        ),
        "nord" to ThemeOptionItem(
            id = "nord",
            name = "Nord Frost",
            description = "Arctic slate blue and cool glacial hues",
            scheme = KeyboardColorScheme.Nord,
            category = ThemeCategory.ORIGINALS
        ),
        "default_light" to ThemeOptionItem(
            id = "default_light",
            name = "Default Light",
            description = "Crisp white floating keycaps on light grey with Google Blue accents",
            scheme = KeyboardColorScheme.DefaultLight,
            category = ThemeCategory.ORIGINALS
        )
    )

    private val bundledThemes = ConcurrentHashMap<String, CustomThemeDef>()
    private val customThemes = ConcurrentHashMap<String, CustomThemeDef>()

    init {
        loadBundledThemes()
        loadCustomThemes()
    }

    private fun loadBundledThemes() {
        try {
            val assetStream = context.assets.open("themes/borno_themes.json")
            val content = assetStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val bornoFile = json.decodeFromString<BornoThemeFile>(content)
            for (group in bornoFile.themes) {
                for (colorTheme in group.colors) {
                    val customDef = colorTheme.toCustomThemeDef()
                    bundledThemes[customDef.id] = customDef
                    if (colorTheme.id != null) {
                        bundledThemes["borno_${colorTheme.id}"] = customDef
                        bundledThemes["${colorTheme.id}"] = customDef
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadCustomThemes() {
        try {
            val files = customThemesDir.listFiles { _, name ->
                name.endsWith(".json") || name.endsWith(".kbtheme")
            } ?: emptyArray()
            for (file in files) {
                try {
                    val content = file.readText(Charsets.UTF_8)
                    parseThemeFile(content).onSuccess { list ->
                        list.forEach { def ->
                            customThemes[def.id] = def
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun getColorScheme(id: String): KeyboardColorScheme {
        originalThemes[id]?.let { return it.scheme }
        bundledThemes[id]?.let { return it.toKeyboardColorScheme() }
        customThemes[id]?.let { return it.toKeyboardColorScheme() }
        return KeyboardColorScheme.DefaultDark
    }

    fun getThemeOption(id: String): ThemeOptionItem {
        originalThemes[id]?.let { return it }
        customThemes[id]?.let { def ->
            return ThemeOptionItem(
                id = def.id,
                name = def.name,
                description = "Imported custom theme",
                scheme = def.toKeyboardColorScheme(),
                isCustom = true,
                category = ThemeCategory.CUSTOM
            )
        }
        bundledThemes[id]?.let { def ->
            val cat = categorizeBornoTheme(def.id)
            return ThemeOptionItem(
                id = def.id,
                name = def.name,
                description = "Borno color theme",
                scheme = def.toKeyboardColorScheme(),
                isCustom = false,
                category = cat
            )
        }
        return originalThemes["default_dark"]!!
    }

    fun getAvailableThemes(): List<ThemeOptionItem> {
        val result = mutableListOf<ThemeOptionItem>()
        val seenIds = mutableSetOf<String>()

        for (item in originalThemes.values) {
            if (seenIds.add(item.id)) {
                result.add(item)
            }
        }

        val orderedBornoIds = listOf(
            1001, 1002, 1003, 1004, 1005, 1006,
            1008, 1009,
            1010, 1011,
            1012, 1013, 1014, 1015, 1016, 1017, 1018, 1023, 1025, 1028, 1029,
            1034, 1038, 1040, 1041, 1042,
            1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058
        )

        for (numId in orderedBornoIds) {
            val key = "borno_$numId"
            bundledThemes[key]?.let { def ->
                if (seenIds.add(def.id)) {
                    val cat = categorizeBornoTheme(def.id)
                    result.add(
                        ThemeOptionItem(
                            id = def.id,
                            name = def.name,
                            description = "Borno color theme",
                            scheme = def.toKeyboardColorScheme(),
                            isCustom = false,
                            category = cat
                        )
                    )
                }
            }
        }

        for (def in customThemes.values) {
            if (seenIds.add(def.id)) {
                result.add(
                    ThemeOptionItem(
                        id = def.id,
                        name = def.name,
                        description = "Custom imported theme",
                        scheme = def.toKeyboardColorScheme(),
                        isCustom = true,
                        category = ThemeCategory.CUSTOM
                    )
                )
            }
        }

        return result
    }

    private fun categorizeBornoTheme(id: String): ThemeCategory {
        val num = id.removePrefix("borno_").toIntOrNull() ?: return ThemeCategory.BORNO_COLORS
        return when (num) {
            in 1001..1011 -> ThemeCategory.BORNO_AMOLED
            in 1012..1035 -> ThemeCategory.BORNO_AESTHETIC
            else -> ThemeCategory.BORNO_COLORS
        }
    }

    fun parseThemeFile(jsonContent: String): Result<List<CustomThemeDef>> {
        return runCatching {
            val trimmed = jsonContent.trim()
            if (trimmed.contains("\"themes\"") && trimmed.contains("\"colors\"")) {
                val bundle = json.decodeFromString<BornoThemeFile>(trimmed)
                val list = mutableListOf<CustomThemeDef>()
                for (group in bundle.themes) {
                    for (color in group.colors) {
                        val def = color.toCustomThemeDef("custom_${System.currentTimeMillis()}_${color.id ?: color.name.hashCode()}")
                        list.add(def)
                    }
                }
                if (list.isNotEmpty()) return@runCatching list
            }

            if (trimmed.contains("\"backgroundColor\"") && trimmed.contains("\"keyColor\"")) {
                val color = json.decodeFromString<BornoColorTheme>(trimmed)
                val def = color.toCustomThemeDef("custom_${System.currentTimeMillis()}_${color.id ?: color.name.hashCode()}")
                return@runCatching listOf(def)
            }

            val customDef = json.decodeFromString<CustomThemeDef>(trimmed)
            listOf(customDef)
        }
    }

    fun importTheme(jsonContent: String): Result<List<ThemeOptionItem>> {
        val parseResult = parseThemeFile(jsonContent)
        return parseResult.map { list ->
            val importedItems = mutableListOf<ThemeOptionItem>()
            for (def in list) {
                val file = File(customThemesDir, "${def.id}.json")
                file.writeText(json.encodeToString(CustomThemeDef.serializer(), def), Charsets.UTF_8)
                customThemes[def.id] = def
                importedItems.add(
                    ThemeOptionItem(
                        id = def.id,
                        name = def.name,
                        description = "Imported custom theme",
                        scheme = def.toKeyboardColorScheme(),
                        isCustom = true,
                        category = ThemeCategory.CUSTOM
                    )
                )
            }
            importedItems
        }
    }

    fun deleteCustomTheme(id: String): Boolean {
        val file = File(customThemesDir, "$id.json")
        val deleted = file.delete()
        customThemes.remove(id)
        return deleted || !file.exists()
    }

    companion object {
        @Volatile
        private var instance: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository {
            return instance ?: synchronized(this) {
                instance ?: ThemeRepository(context.applicationContext ?: context).also { instance = it }
            }
        }
    }
}
