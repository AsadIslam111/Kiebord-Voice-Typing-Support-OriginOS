package io.kiebord.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.kiebord.theme.ThemeColorUtils.toCustomThemeDef
import io.kiebord.theme.ThemeColorUtils.toKeyboardColorScheme
import io.kiebord.theme.ThemeColorUtils.toKeyboardThemeSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.io.FileOutputStream
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

data class ParsedThemeBundle(
    val definitions: List<CustomThemeDef>,
    val stagingDir: File? = null
)

class ThemeRepository private constructor(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
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
    private val schemeCache = ConcurrentHashMap<String, KeyboardColorScheme>()

    init {
        loadBundledThemes()
        loadCustomThemes()
    }

    private fun canonicalId(id: String): String {
        return if (id.isNotEmpty() && id.all { it.isDigit() }) "borno_$id" else id
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
                    val key = canonicalId(customDef.id)
                    bundledThemes[key] = customDef
                    richThemes[key] = richSpec
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadCustomThemes() {
        try {
            val items = customThemesDir.listFiles() ?: emptyArray()
            for (item in items) {
                try {
                    if (item.isDirectory) {
                        val spec = ThemeZipUtils.discoverAndNormalizeThemePackage(item, json)
                        val def = spec.toCustomThemeDef()
                        customThemes[def.id] = def
                        richThemes[def.id] = spec
                    } else if (item.isFile) {
                        if (item.name.endsWith(".json") || item.name.endsWith(".kbtheme") || item.name.endsWith(".bornotheme")) {
                            val bytes = item.readBytes()
                            if (ThemeZipUtils.isZipBytes(bytes)) {
                                val targetDir = File(customThemesDir, item.nameWithoutExtension)
                                ThemeZipUtils.extractZipSafe(bytes, targetDir)
                                val spec = ThemeZipUtils.discoverAndNormalizeThemePackage(targetDir, json)
                                val def = spec.toCustomThemeDef()
                                customThemes[def.id] = def
                                richThemes[def.id] = spec
                            } else {
                                val content = bytes.toString(Charsets.UTF_8)
                                parseThemeFile(content).onSuccess { list ->
                                    list.forEach { def ->
                                        customThemes[def.id] = def
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun getColorScheme(id: String): KeyboardColorScheme {
        val cid = canonicalId(id)
        originalThemes[cid]?.let { return it.scheme }
        schemeCache[cid]?.let { return it }
        val resolved = richThemes[cid]?.toKeyboardColorScheme()
            ?: bundledThemes[cid]?.toKeyboardColorScheme()
            ?: customThemes[cid]?.toKeyboardColorScheme()
        if (resolved != null) {
            schemeCache[cid] = resolved
            return resolved
        }
        return KeyboardColorScheme.DefaultDark
    }

    fun getRichTheme(id: String): KeyboardThemeSpec? {
        val cid = canonicalId(id)
        richThemes[cid]?.let { return it }
        bundledThemes[cid]?.let { return it.toKeyboardThemeSpec() }
        customThemes[cid]?.let { return it.toKeyboardThemeSpec() }
        return null
    }

    fun getThemeOption(id: String): ThemeOptionItem {
        val cid = canonicalId(id)
        originalThemes[cid]?.let { return it }
        richThemes[cid]?.let { spec ->
            val isCustomTheme = customThemes.containsKey(spec.id) || !spec.id.startsWith("borno_")
            val versionTag = if (spec.schemaVersion == 2) "v2" else "v1"
            val desc = if (isCustomTheme) {
                "Custom Borno theme ($versionTag)"
            } else {
                "Rich theme (${spec.background.gradient?.type?.name?.lowercase() ?: if (spec.background.image != null) "image" else "solid"})"
            }
            return ThemeOptionItem(
                id = spec.id,
                name = spec.name,
                description = desc,
                scheme = getColorScheme(cid),
                isCustom = isCustomTheme,
                category = if (isCustomTheme) ThemeCategory.CUSTOM else categorizeBornoTheme(spec.id)
            )
        }
        customThemes[cid]?.let { def ->
            return ThemeOptionItem(
                id = def.id,
                name = def.name,
                description = "Imported custom theme",
                scheme = getColorScheme(cid),
                isCustom = true,
                category = ThemeCategory.CUSTOM
            )
        }
        bundledThemes[cid]?.let { def ->
            val cat = categorizeBornoTheme(def.id)
            return ThemeOptionItem(
                id = def.id,
                name = def.name,
                description = "Borno color theme",
                scheme = getColorScheme(cid),
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

    private fun saveBackgroundImage(ctx: Context?, base64: String): String? {
        if (ctx == null) return null
        return try {
            val bytes = ThemeColorUtils.decodeBase64Safe(base64)
            if (bytes.isEmpty()) return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            val reqDim = 1280
            var inSample = 1
            if (opts.outHeight > reqDim || opts.outWidth > reqDim) {
                val halfH = opts.outHeight / 2
                val halfW = opts.outWidth / 2
                while ((halfH / inSample) >= reqDim && (halfW / inSample) >= reqDim) {
                    inSample *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            val imagesDir = File(ctx.filesDir, "custom_themes").apply { if (!exists()) mkdirs() }
            val destFile = File(imagesDir, "theme_bg_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { fos ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                fos.flush()
            }
            bmp.recycle()
            destFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    fun parseThemeFile(jsonContent: String): Result<List<CustomThemeDef>> {
        return runCatching {
            var trimmed = jsonContent.trim().removePrefix("\uFEFF").trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                val decoded = ThemeColorUtils.decodeBase64ToStringSafe(trimmed)
                if (decoded != null && (decoded.trim().startsWith("{") || decoded.trim().startsWith("["))) {
                    trimmed = decoded.trim().removePrefix("\uFEFF").trim()
                }
            }

            val jsonElement = try {
                json.parseToJsonElement(trimmed)
            } catch (_: Exception) {
                null
            }

            if (jsonElement is JsonObject) {
                val schemaVersion = jsonElement["schemaVersion"]?.let {
                    runCatching { it.jsonPrimitive.intOrNull }.getOrNull()
                }

                val hasV2Keys = jsonElement.containsKey("characterKey") ||
                    jsonElement.containsKey("perKeyOverrides") ||
                    (jsonElement.containsKey("background") && jsonElement["background"] is JsonObject)

                val hasV1Keys = jsonElement.containsKey("backgroundType") ||
                    jsonElement.containsKey("keyColor") ||
                    jsonElement.containsKey("backgroundColor") ||
                    jsonElement.containsKey("builder") ||
                    jsonElement.containsKey("enterColor")

                // If schemaVersion is explicitly 2 or contains V2 specific structure:
                if (schemaVersion == 2 || (schemaVersion == null && hasV2Keys && !hasV1Keys)) {
                    try {
                        var spec = json.decodeFromJsonElement<KeyboardThemeSpec>(jsonElement)
                        if (!spec.backgroundImageBase64.isNullOrBlank()) {
                            val savedImagePath = saveBackgroundImage(context, spec.backgroundImageBase64)
                            if (savedImagePath != null) {
                                val currentImg = spec.background.image
                                val updatedImg = currentImg?.copy(imagePath = savedImagePath)
                                    ?: ImageBackgroundSpec(imagePath = savedImagePath)
                                spec = spec.copy(
                                    background = spec.background.copy(image = updatedImg)
                                )
                            }
                        }
                        richThemes[spec.id] = spec
                        return@runCatching listOf(spec.toCustomThemeDef())
                    } catch (_: Exception) {}
                }

                // If schemaVersion is 1 or contains V1 keys:
                if (schemaVersion == 1 || hasV1Keys) {
                    try {
                        val exported = json.decodeFromJsonElement<BornoExportedTheme>(jsonElement)
                        val imagePath = if (!exported.backgroundImageBase64.isNullOrBlank()) {
                            saveBackgroundImage(context, exported.backgroundImageBase64)
                        } else exported.backgroundPath
                        val def = exported.toCustomThemeDef(resolvedImagePath = imagePath)
                        val richSpec = exported.toKeyboardThemeSpec(customId = def.id, resolvedImagePath = imagePath)
                        richThemes[def.id] = richSpec
                        return@runCatching listOf(def)
                    } catch (_: Exception) {}
                }

                // Try V2 again if V1 didn't match or failed
                try {
                    var spec = json.decodeFromJsonElement<KeyboardThemeSpec>(jsonElement)
                    if (!spec.backgroundImageBase64.isNullOrBlank()) {
                        val savedImagePath = saveBackgroundImage(context, spec.backgroundImageBase64)
                        if (savedImagePath != null) {
                            val currentImg = spec.background.image
                            val updatedImg = currentImg?.copy(imagePath = savedImagePath)
                                ?: ImageBackgroundSpec(imagePath = savedImagePath)
                            spec = spec.copy(
                                background = spec.background.copy(image = updatedImg)
                            )
                        }
                    }
                    richThemes[spec.id] = spec
                    return@runCatching listOf(spec.toCustomThemeDef())
                } catch (_: Exception) {}

                // Try BornoExportedTheme again as fallback
                try {
                    val exported = json.decodeFromJsonElement<BornoExportedTheme>(jsonElement)
                    val imagePath = if (!exported.backgroundImageBase64.isNullOrBlank()) {
                        saveBackgroundImage(context, exported.backgroundImageBase64)
                    } else exported.backgroundPath
                    val def = exported.toCustomThemeDef(resolvedImagePath = imagePath)
                    val richSpec = exported.toKeyboardThemeSpec(customId = def.id, resolvedImagePath = imagePath)
                    richThemes[def.id] = richSpec
                    return@runCatching listOf(def)
                } catch (_: Exception) {}

                // Try Standalone Borno color theme object
                if (jsonElement.containsKey("backgroundColor") && jsonElement.containsKey("keyColor")) {
                    try {
                        val color = json.decodeFromJsonElement<BornoColorTheme>(jsonElement)
                        val def = color.toCustomThemeDef("custom_${System.currentTimeMillis()}_${color.id ?: color.name.hashCode()}")
                        val richSpec = color.toKeyboardThemeSpec(def.id)
                        richThemes[def.id] = richSpec
                        return@runCatching listOf(def)
                    } catch (_: Exception) {}
                }

                // Try flat CustomThemeDef
                try {
                    val customDef = json.decodeFromJsonElement<CustomThemeDef>(jsonElement)
                    val richSpec = customDef.toKeyboardThemeSpec()
                    richThemes[customDef.id] = richSpec
                    return@runCatching listOf(customDef)
                } catch (_: Exception) {}
            }

            // Multi-theme bundle (borno_themes.json)
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

            throw IllegalArgumentException("Unsupported or corrupted theme file format")
        }
    }

    fun importTheme(jsonContent: String): Result<List<ThemeOptionItem>> {
        val parseResult = parseThemeFile(jsonContent)
        return parseResult.map { list ->
            val importedItems = mutableListOf<ThemeOptionItem>()
            for (def in list) {
                val richSpec = richThemes[def.id]
                val versionTag = if (richSpec?.schemaVersion == 2) "v2" else "v1"
                val file = File(customThemesDir, "${def.id}.bornotheme")
                if (richSpec != null) {
                    file.writeText(json.encodeToString(KeyboardThemeSpec.serializer(), richSpec.copy(schemaVersion = 2)), Charsets.UTF_8)
                } else {
                    file.writeText(json.encodeToString(CustomThemeDef.serializer(), def), Charsets.UTF_8)
                }
                customThemes[def.id] = def
                val scheme = richSpec?.toKeyboardColorScheme() ?: def.toKeyboardColorScheme()
                schemeCache[def.id] = scheme
                importedItems.add(
                    ThemeOptionItem(
                        id = def.id,
                        name = def.name,
                        description = "Custom Borno theme ($versionTag)",
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
        val versionedSpec = spec.copy(schemaVersion = 2)
        richThemes[versionedSpec.id] = versionedSpec
        val file = File(customThemesDir, "${versionedSpec.id}.bornotheme")
        file.writeText(json.encodeToString(KeyboardThemeSpec.serializer(), versionedSpec), Charsets.UTF_8)
        val scheme = versionedSpec.toKeyboardColorScheme()
        val def = versionedSpec.toCustomThemeDef()
        customThemes[versionedSpec.id] = def
        schemeCache[versionedSpec.id] = scheme
        return ThemeOptionItem(
            id = versionedSpec.id,
            name = versionedSpec.name,
            description = "Custom Borno theme (v2)",
            scheme = scheme,
            isCustom = true,
            category = ThemeCategory.CUSTOM
        )
    }

    /**
     * Exports a theme by ID as a modern .bornotheme (Version 2) string.
     * If the theme has a local background image, it is automatically embedded
     * as Base64 for 100% portable single-file distribution.
     */
    fun exportTheme(id: String, asBase64: Boolean = false): Result<String> {
        return runCatching {
            val cid = canonicalId(id)
            val richSpec = richThemes[cid]
                ?: customThemes[cid]?.toKeyboardThemeSpec()
                ?: bundledThemes[cid]?.toKeyboardThemeSpec()
                ?: throw IllegalArgumentException("Theme not found: $id")

            var exportSpec = richSpec.copy(schemaVersion = 2)
            val imagePath = richSpec.background.image?.imagePath
            if (!imagePath.isNullOrBlank() && richSpec.backgroundImageBase64.isNullOrBlank()) {
                val file = File(imagePath)
                if (file.exists() && file.canRead()) {
                    val bytes = file.readBytes()
                    val b64 = ThemeColorUtils.encodeBase64Safe(bytes)
                    exportSpec = exportSpec.copy(backgroundImageBase64 = b64)
                }
            }

            val jsonStr = json.encodeToString(KeyboardThemeSpec.serializer(), exportSpec)
            if (asBase64) {
                ThemeColorUtils.encodeBase64Safe(jsonStr)
            } else {
                jsonStr
            }
        }
    }

    fun parseThemeBytes(bytes: ByteArray): Result<ParsedThemeBundle> {
        if (ThemeZipUtils.isZipBytes(bytes)) {
            return runCatching {
                val stagingDir = File(context.cacheDir, "theme_staging_${System.currentTimeMillis()}").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }
                ThemeZipUtils.extractZipSafe(bytes, stagingDir)
                val spec = ThemeZipUtils.discoverAndNormalizeThemePackage(stagingDir, json)
                richThemes[spec.id] = spec
                val def = spec.toCustomThemeDef()
                ParsedThemeBundle(definitions = listOf(def), stagingDir = stagingDir)
            }
        }
        val text = bytes.toString(Charsets.UTF_8)
        return parseThemeFile(text).map { ParsedThemeBundle(definitions = it, stagingDir = null) }
    }

    fun importStagedTheme(themeId: String, stagingDir: File): Result<ThemeOptionItem> {
        return runCatching {
            val destDir = File(customThemesDir, themeId)
            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()
            stagingDir.copyRecursively(destDir, overwrite = true)
            try { stagingDir.deleteRecursively() } catch (_: Exception) {}

            val spec = ThemeZipUtils.discoverAndNormalizeThemePackage(destDir, json)
            val versionedSpec = spec.copy(schemaVersion = 2)
            richThemes[versionedSpec.id] = versionedSpec
            val def = versionedSpec.toCustomThemeDef()
            customThemes[versionedSpec.id] = def
            val scheme = versionedSpec.toKeyboardColorScheme()
            schemeCache[versionedSpec.id] = scheme

            val themeJsonFile = File(destDir, "theme.json")
            themeJsonFile.writeText(json.encodeToString(KeyboardThemeSpec.serializer(), versionedSpec), Charsets.UTF_8)

            ThemeOptionItem(
                id = versionedSpec.id,
                name = versionedSpec.name,
                description = "Custom Borno theme (v2)",
                scheme = scheme,
                isCustom = true,
                category = ThemeCategory.CUSTOM
            )
        }
    }

    fun importThemeBytes(bytes: ByteArray): Result<List<ThemeOptionItem>> {
        if (ThemeZipUtils.isZipBytes(bytes)) {
            return runCatching {
                val tempDir = File(context.cacheDir, "theme_temp_${System.currentTimeMillis()}").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }
                ThemeZipUtils.extractZipSafe(bytes, tempDir)
                val parsedSpec = ThemeZipUtils.discoverAndNormalizeThemePackage(tempDir, json)
                val item = importStagedTheme(parsedSpec.id, tempDir).getOrThrow()
                listOf(item)
            }
        }
        val text = bytes.toString(Charsets.UTF_8)
        return importTheme(text)
    }

    fun exportThemeZip(id: String, outputFile: File): Result<File> {
        return runCatching {
            val cid = canonicalId(id)
            val richSpec = richThemes[cid]
                ?: customThemes[cid]?.toKeyboardThemeSpec()
                ?: bundledThemes[cid]?.toKeyboardThemeSpec()
                ?: throw IllegalArgumentException("Theme not found: $id")
            ThemeZipUtils.packageThemeZip(richSpec, outputFile, json)
        }
    }

    fun deleteCustomTheme(id: String): Boolean {
        var anyDeleted = false
        val themeDir = File(customThemesDir, id)
        if (themeDir.exists()) {
            if (themeDir.deleteRecursively()) anyDeleted = true
        }

        val files = listOf(
            File(customThemesDir, "$id.bornotheme"),
            File(customThemesDir, "$id.kbtheme"),
            File(customThemesDir, "$id.json")
        )
        val spec = richThemes[id]
        val imgPath = spec?.background?.image?.imagePath
        if (!imgPath.isNullOrBlank()) {
            try {
                val imgFile = File(imgPath)
                if (imgFile.exists() && imgFile.absolutePath.contains(context.filesDir.absolutePath)) {
                    imgFile.delete()
                }
            } catch (_: Exception) {}
        }
        for (f in files) {
            if (f.exists()) {
                if (f.delete()) anyDeleted = true
            }
        }
        customThemes.remove(id)
        richThemes.remove(id)
        schemeCache.remove(id)
        return anyDeleted || files.none { it.exists() }
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
