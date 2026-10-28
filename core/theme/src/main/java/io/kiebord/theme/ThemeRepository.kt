package io.kiebord.theme

import android.content.Context
import io.kiebord.theme.ThemeColorUtils.toCustomThemeDef
import io.kiebord.theme.ThemeColorUtils.toKeyboardColorScheme
import io.kiebord.theme.ThemeColorUtils.toKeyboardThemeSpec
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class ThemeCategory(val title: String) {
    ORIGINALS("Originals"),
    BORNO_GRADIENT("Gradients"),
    BORNO_AMOLED("Dark & AMOLED"),
    BORNO_AESTHETIC("Aesthetic"),
    BORNO_COLORS("Colors"),
    CUSTOM("Custom Themes")
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
        "default_light" to ThemeOptionItem(
            id = "default_light",
            name = "Default Light",
            description = "Crisp white floating keycaps on cool alpine grey with vibrant blue accents",
            scheme = KeyboardColorScheme.DefaultLight,
            category = ThemeCategory.ORIGINALS
        ),
        "default_dark" to ThemeOptionItem(
            id = "default_dark",
            name = "Default Dark",
            description = "Velvet graphite dark keycaps with vibrant indigo-blue accents",
            scheme = KeyboardColorScheme.DefaultDark,
            category = ThemeCategory.ORIGINALS
        )
    )

    private val bundledThemes = ConcurrentHashMap<String, CustomThemeDef>()
    private val customThemes = ConcurrentHashMap<String, CustomThemeDef>()
    private val richThemes = ConcurrentHashMap<String, KeyboardThemeSpec>()

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
                    val richSpec = colorTheme.toKeyboardThemeSpec()
                    bundledThemes[customDef.id] = customDef
                    richThemes[customDef.id] = richSpec
                    if (colorTheme.id != null) {
                        bundledThemes["borno_${colorTheme.id}"] = customDef
                        bundledThemes["${colorTheme.id}"] = customDef
                        richThemes["borno_${colorTheme.id}"] = richSpec
                        richThemes["${colorTheme.id}"] = richSpec
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
        richThemes[id]?.let { return it.toKeyboardColorScheme() }
        bundledThemes[id]?.let { return it.toKeyboardColorScheme() }
        customThemes[id]?.let { return it.toKeyboardColorScheme() }
        return KeyboardColorScheme.DefaultDark
    }

    fun getRichTheme(id: String): KeyboardThemeSpec? {
        richThemes[id]?.let { return it }
        bundledThemes[id]?.let { return it.toKeyboardThemeSpec() }
        customThemes[id]?.let { return it.toKeyboardThemeSpec() }
        return null
    }

    fun getThemeOption(id: String): ThemeOptionItem {
        originalThemes[id]?.let { return it }
        richThemes[id]?.let { spec ->
            return ThemeOptionItem(
                id = spec.id,
                name = spec.name,
                description = "Rich theme (${spec.background.gradient?.type?.name?.lowercase() ?: if (spec.background.image != null) "image" else "solid"})",
                scheme = spec.toKeyboardColorScheme(),
                isCustom = customThemes.containsKey(spec.id) || !spec.id.startsWith("borno_"),
                category = if (customThemes.containsKey(spec.id)) ThemeCategory.CUSTOM else categorizeBornoTheme(spec.id)
            )
        }
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

        val orderedBornoIds = (1..42).toList()

        for (numId in orderedBornoIds) {
            val key = "borno_$numId"
            bundledThemes[key]?.let { def ->
                if (seenIds.add(def.id)) {
                    val cat = categorizeBornoTheme(def.id)
                    val richSpec = richThemes[def.id]
                    val scheme = richSpec?.toKeyboardColorScheme() ?: def.toKeyboardColorScheme()
                    result.add(
                        ThemeOptionItem(
                            id = def.id,
                            name = def.name,
                            description = if (cat == ThemeCategory.BORNO_GRADIENT) "Gradient theme with translucent keys" else "Borno color theme",
                            scheme = scheme,
                            isCustom = false,
                            category = cat
                        )
                    )
                }
            }
        }

        for (def in customThemes.values) {
            if (seenIds.add(def.id)) {
                val richSpec = richThemes[def.id]
                val scheme = richSpec?.toKeyboardColorScheme() ?: def.toKeyboardColorScheme()
                result.add(
                    ThemeOptionItem(
                        id = def.id,
                        name = def.name,
                        description = "Custom imported theme",
                        scheme = scheme,
                        isCustom = true,
                        category = ThemeCategory.CUSTOM
                    )
                )
            }
        }

        return result
    }

    private fun categorizeBornoTheme(id: String): ThemeCategory {
        val rich = richThemes[id]
        if (rich?.background?.gradient != null) {
            return ThemeCategory.BORNO_GRADIENT
        }
        val numId = id.removePrefix("borno_").toIntOrNull()
        if (numId != null && numId in 1..12) {
            return ThemeCategory.BORNO_GRADIENT
        }
        return ThemeCategory.BORNO_COLORS
    }

    fun parseThemeFile(jsonContent: String): Result<List<CustomThemeDef>> {
        return runCatching {
            val trimmed = jsonContent.trim()

            if (trimmed.contains("\"characterKey\"") || trimmed.contains("\"perKeyOverrides\"") || (trimmed.contains("\"background\"") && trimmed.contains("\"actionKey\""))) {
                val spec = json.decodeFromString<KeyboardThemeSpec>(trimmed)
                richThemes[spec.id] = spec
                val def = CustomThemeDef(
                    id = spec.id,
                    name = spec.name,
                    isDark = spec.isDark,
                    containerBackground = spec.background.solidColor ?: spec.background.gradient?.colors?.firstOrNull() ?: "#1B1B1F",
                    keyBackground = spec.characterKey.background ?: "#2C2C30",
                    keyForeground = spec.characterKey.foreground ?: "#E3E2E6",
                    keyBorder = spec.characterKey.stroke?.color ?: "#00000000",
                    keyShadow = spec.characterKey.shadow?.color ?: "#00000000",
                    actionKeyBackground = spec.functionalKey.background ?: "#242428",
                    actionKeyForeground = spec.actionKey.foreground ?: "#04294E",
                    shiftActiveColor = spec.shiftActiveColor,
                    accentColor = spec.accentColor,
                    moreKeysBackground = spec.moreKeysBackground,
                    guidelineColor = spec.guidelineColor,
                    suggestionBackground = spec.suggestionBackground?.solidColor ?: (spec.background.solidColor ?: "#1B1B1F"),
                    suggestionForeground = spec.suggestionForeground,
                    keyPressedBackground = spec.characterKey.pressedBackground ?: (spec.functionalKey.background ?: "#242428"),
                    keyPressedForeground = spec.characterKey.pressedForeground ?: (spec.characterKey.foreground ?: "#E3E2E6")
                )
                return@runCatching listOf(def)
            }

            if (trimmed.contains("\"themes\"") && trimmed.contains("\"colors\"")) {
                val bundle = json.decodeFromString<BornoThemeFile>(trimmed)
                val list = mutableListOf<CustomThemeDef>()
                for (group in bundle.themes) {
                    for (color in group.colors) {
                        val def = color.toCustomThemeDef("custom_${System.currentTimeMillis()}_${color.id ?: color.name.hashCode()}")
                        val richSpec = color.toKeyboardThemeSpec(def.id)
                        richThemes[def.id] = richSpec
                        list.add(def)
                    }
                }
                if (list.isNotEmpty()) return@runCatching list
            }

            if (trimmed.contains("\"backgroundColor\"") && trimmed.contains("\"keyColor\"")) {
                val color = json.decodeFromString<BornoColorTheme>(trimmed)
                val def = color.toCustomThemeDef("custom_${System.currentTimeMillis()}_${color.id ?: color.name.hashCode()}")
                val richSpec = color.toKeyboardThemeSpec(def.id)
                richThemes[def.id] = richSpec
                return@runCatching listOf(def)
            }

            val customDef = json.decodeFromString<CustomThemeDef>(trimmed)
            val richSpec = customDef.toKeyboardThemeSpec()
            richThemes[customDef.id] = richSpec
            listOf(customDef)
        }
    }

    fun importTheme(jsonContent: String): Result<List<ThemeOptionItem>> {
        val parseResult = parseThemeFile(jsonContent)
        return parseResult.map { list ->
            val importedItems = mutableListOf<ThemeOptionItem>()
            for (def in list) {
                val richSpec = richThemes[def.id]
                val file = File(customThemesDir, "${def.id}.json")
                if (richSpec != null) {
                    file.writeText(json.encodeToString(KeyboardThemeSpec.serializer(), richSpec), Charsets.UTF_8)
                } else {
                    file.writeText(json.encodeToString(CustomThemeDef.serializer(), def), Charsets.UTF_8)
                }
                customThemes[def.id] = def
                val scheme = richSpec?.toKeyboardColorScheme() ?: def.toKeyboardColorScheme()
                importedItems.add(
                    ThemeOptionItem(
                        id = def.id,
                        name = def.name,
                        description = "Imported custom theme",
                        scheme = scheme,
                        isCustom = true,
                        category = ThemeCategory.CUSTOM
                    )
                )
            }
            importedItems
        }
    }

    fun importRichTheme(spec: KeyboardThemeSpec): ThemeOptionItem {
        richThemes[spec.id] = spec
        val file = File(customThemesDir, "${spec.id}.json")
        file.writeText(json.encodeToString(KeyboardThemeSpec.serializer(), spec), Charsets.UTF_8)
        val scheme = spec.toKeyboardColorScheme()
        val def = CustomThemeDef(
            id = spec.id,
            name = spec.name,
            isDark = spec.isDark,
            containerBackground = spec.background.solidColor ?: spec.background.gradient?.colors?.firstOrNull() ?: "#1B1B1F",
            keyBackground = spec.characterKey.background ?: "#2C2C30",
            keyForeground = spec.characterKey.foreground ?: "#E3E2E6",
            keyBorder = spec.characterKey.stroke?.color ?: "#00000000",
            keyShadow = spec.characterKey.shadow?.color ?: "#00000000",
            actionKeyBackground = spec.functionalKey.background ?: "#242428",
            actionKeyForeground = spec.actionKey.foreground ?: "#04294E",
            shiftActiveColor = spec.shiftActiveColor,
            accentColor = spec.accentColor,
            moreKeysBackground = spec.moreKeysBackground,
            guidelineColor = spec.guidelineColor,
            suggestionBackground = spec.suggestionBackground?.solidColor ?: (spec.background.solidColor ?: "#1B1B1F"),
            suggestionForeground = spec.suggestionForeground,
            keyPressedBackground = spec.characterKey.pressedBackground ?: (spec.functionalKey.background ?: "#242428"),
            keyPressedForeground = spec.characterKey.pressedForeground ?: (spec.characterKey.foreground ?: "#E3E2E6")
        )
        customThemes[spec.id] = def
        return ThemeOptionItem(
            id = spec.id,
            name = spec.name,
            description = "Imported rich theme",
            scheme = scheme,
            isCustom = true,
            category = ThemeCategory.CUSTOM
        )
    }

    fun deleteCustomTheme(id: String): Boolean {
        val file = File(customThemesDir, "$id.json")
        val deleted = file.delete()
        customThemes.remove(id)
        richThemes.remove(id)
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
