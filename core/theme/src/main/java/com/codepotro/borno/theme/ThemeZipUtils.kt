package com.codepotro.borno.theme

import com.codepotro.borno.theme.ThemeColorUtils.toKeyboardThemeSpec
import kotlinx.serialization.json.Json
import com.codepotro.borno.theme.ThemeColorUtils.toKeyboardThemeSpec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utilities for detecting, inspecting, extracting, and packaging full `.bornotheme`
 * ZIP archives containing companion assets (background images, custom fonts, key icons).
 */
object ThemeZipUtils {

    /**
     * Inspects the leading bytes to determine whether the content is a ZIP archive.
     * Standard ZIP archives begin with magic bytes 0x50 0x4B 0x03 0x04 ("PK\u0003\u0004").
     */
    fun isZipBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && (
            (bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) ||
            (bytes[2] == 0x05.toByte() && bytes[3] == 0x06.toByte()) ||
            (bytes[2] == 0x07.toByte() && bytes[3] == 0x08.toByte())
        )
    }

    /**
     * Safely unpacks a ZIP archive from a stream or byte array into [destDir],
     * with strict ZipSlip validation to prevent directory traversal vulnerabilities.
     */
    fun extractZipSafe(zipBytes: ByteArray, destDir: File): List<File> {
        return extractZipSafe(ByteArrayInputStream(zipBytes), destDir)
    }

    fun extractZipSafe(inputStream: InputStream, destDir: File): List<File> {
        if (!destDir.exists()) destDir.mkdirs()
        val destCanonical = destDir.canonicalPath
        val extractedFiles = mutableListOf<File>()

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = File(destDir, entry.name)
                val canonicalPath = targetFile.canonicalPath
                if (!canonicalPath.startsWith(destCanonical + File.separator) && canonicalPath != destCanonical) {
                    throw SecurityException("ZipSlip security violation: entry ${entry.name} escapes destination directory")
                }

                // Ignore macOS metadata and hidden files (e.g. .DS_Store, __MACOSX)
                val isHiddenOrMeta = entry.name.startsWith("__MACOSX") ||
                    entry.name.split("/").any { it.startsWith(".") && it != "." && it != ".." }

                if (!isHiddenOrMeta) {
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        extractedFiles.add(targetFile)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return extractedFiles
    }

    /**
     * Analyzes an unpacked theme folder [themeDir], discovers specification files,
     * auto-detects companion assets (wallpaper, font, and custom key icons),
     * normalizes all relative paths to absolute disk paths, and returns a ready-to-render
     * [KeyboardThemeSpec].
     */
    fun discoverAndNormalizeThemePackage(themeDir: File, json: Json): KeyboardThemeSpec {
        require(themeDir.exists() && themeDir.isDirectory) {
            "Theme directory does not exist or is not a directory: ${themeDir.absolutePath}"
        }

        // 1. Locate theme specification file (theme.json preferred, or *.bornotheme, *.json)
        val candidateSpecFiles = listOf(
            File(themeDir, "theme.json"),
            File(themeDir, "spec.json"),
            File(themeDir, "borno.json")
        )
        val specFile = candidateSpecFiles.firstOrNull { it.exists() && it.isFile }
            ?: themeDir.listFiles { _, name ->
                name.endsWith(".json") || name.endsWith(".bornotheme") || name.endsWith(".kbtheme")
            }?.firstOrNull()
            ?: throw IllegalArgumentException("No theme specification file (theme.json) found in package")

        var rawContent = specFile.readText(Charsets.UTF_8).trim().removePrefix("\uFEFF").trim()
        if (!rawContent.startsWith("{") && !rawContent.startsWith("[")) {
            val decoded = ThemeColorUtils.decodeBase64ToStringSafe(rawContent)
            if (decoded != null && (decoded.trim().startsWith("{") || decoded.trim().startsWith("["))) {
                rawContent = decoded.trim().removePrefix("\uFEFF").trim()
            }
        }

        val jsonElement = json.parseToJsonElement(rawContent) as? JsonObject
            ?: throw IllegalArgumentException("Invalid JSON root object in ${specFile.name}")

        val schemaVersion = jsonElement["schemaVersion"]?.let {
            runCatching { it.jsonPrimitive.intOrNull }.getOrNull()
        }
        val hasV2Keys = jsonElement.containsKey("characterKey") ||
            jsonElement.containsKey("perKeyOverrides") ||
            (jsonElement.containsKey("background") && jsonElement["background"] is JsonObject)

        var spec: KeyboardThemeSpec = if (schemaVersion == 2 || hasV2Keys) {
            json.decodeFromJsonElement<KeyboardThemeSpec>(jsonElement)
        } else {
            val v1Exported = json.decodeFromJsonElement<BornoExportedTheme>(jsonElement)
            val fallbackId = v1Exported.name?.lowercase()?.replace(" ", "_")?.ifBlank { null }
                ?: "custom_${System.currentTimeMillis()}"
            with(ThemeColorUtils) {
                v1Exported.toKeyboardThemeSpec(customId = fallbackId)
            }
        }

        // Ensure schemaVersion is 2
        spec = spec.copy(schemaVersion = 2)

        // 2. Auto-discover background wallpaper if not already set or if path is relative
        val currentImgPath = spec.background.image?.imagePath
        val resolvedImgPath: String? = if (!currentImgPath.isNullOrBlank()) {
            resolvePathInDir(themeDir, currentImgPath)
        } else {
            findFirstMatchingFile(themeDir, listOf("background", "wallpaper", "bg"), listOf("jpg", "jpeg", "png", "webp"))?.absolutePath
        }

        if (!resolvedImgPath.isNullOrBlank()) {
            val currentImgSpec = spec.background.image ?: ImageBackgroundSpec(imagePath = resolvedImgPath)
            spec = spec.copy(
                background = spec.background.copy(image = currentImgSpec.copy(imagePath = resolvedImgPath))
            )
        }

        // 3. Auto-discover typography font if not already set or if path is relative
        val currentFontPath = spec.fontPath
        val resolvedFontPath: String? = if (!currentFontPath.isNullOrBlank()) {
            resolvePathInDir(themeDir, currentFontPath)
        } else {
            findFirstMatchingFile(themeDir, listOf("font", "keyboard_font", "typeface"), listOf("ttf", "otf"))?.absolutePath
                ?: findAnyFileWithExtensions(themeDir, listOf("ttf", "otf"))?.absolutePath
        }

        if (!resolvedFontPath.isNullOrBlank()) {
            spec = spec.copy(fontPath = resolvedFontPath)
        }

        // 4. Auto-discover and resolve keycap icons
        val iconsDir = File(themeDir, "icons").takeIf { it.exists() && it.isDirectory }
        val iconSearchDirs = listOfNotNull(iconsDir, themeDir)

        fun resolveIcon(declared: String?, fallbackNames: List<String>): String? {
            if (!declared.isNullOrBlank()) {
                val resolved = resolvePathInDir(themeDir, declared)
                if (resolved != null) return resolved
                // If it's not a file on disk, return declared string (could be an Android drawable resource ID)
                return declared
            }
            for (dir in iconSearchDirs) {
                val found = findFirstMatchingFile(dir, fallbackNames, listOf("png", "webp", "jpg", "xml"))
                if (found != null) return found.absolutePath
            }
            return null
        }

        val resolvedIcons = ThemeIconsSpec(
            shift = resolveIcon(spec.icons.shift, listOf("shift", "ic_shift")),
            shiftOneShot = resolveIcon(spec.icons.shiftOneShot, listOf("shift_active", "shift_oneshot", "ic_shift_active")),
            capsLock = resolveIcon(spec.icons.capsLock, listOf("caps_lock", "capslock", "ic_caps_lock")),
            backspace = resolveIcon(spec.icons.backspace, listOf("backspace", "delete", "ic_delete")),
            action = resolveIcon(spec.icons.action, listOf("action", "enter", "return", "ic_action")),
            language = resolveIcon(spec.icons.language, listOf("language", "globe", "switch_language", "ic_language")),
            emoji = resolveIcon(spec.icons.emoji, listOf("emoji", "ic_emoji")),
            settings = resolveIcon(spec.icons.settings, listOf("settings", "ic_settings")),
            voice = resolveIcon(spec.icons.voice, listOf("voice", "mic", "ic_voice")),
            search = resolveIcon(spec.icons.search, listOf("search", "ic_search")),
            moreKeys = resolveIcon(spec.icons.moreKeys, listOf("more_keys", "morekeys", "ic_more_keys"))
        )

        spec = spec.copy(icons = resolvedIcons)
        return spec
    }

    /**
     * Resolves a relative path inside [rootDir], returning the absolute path if found.
     */
    private fun resolvePathInDir(rootDir: File, path: String): String? {
        if (path.isBlank()) return null
        val directFile = File(path)
        if (directFile.isAbsolute && directFile.exists()) return directFile.absolutePath

        val relFile = File(rootDir, path)
        if (relFile.exists()) return relFile.absolutePath

        // Check if inside fonts/ or icons/ or images/
        val subDirs = listOf("fonts", "icons", "images", "assets")
        for (sub in subDirs) {
            val subFile = File(File(rootDir, sub), path)
            if (subFile.exists()) return subFile.absolutePath
        }
        return null
    }

    private fun findFirstMatchingFile(dir: File, baseNames: List<String>, extensions: List<String>): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        val files = dir.listFiles() ?: return null
        for (f in files) {
            if (f.isFile) {
                val nameWithoutExt = f.nameWithoutExtension.lowercase()
                val ext = f.extension.lowercase()
                if (baseNames.any { it.equals(nameWithoutExt, ignoreCase = true) } && extensions.contains(ext)) {
                    return f
                }
            }
        }
        return null
    }

    private fun findAnyFileWithExtensions(dir: File, extensions: List<String>): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        val files = dir.listFiles() ?: return null
        for (f in files) {
            if (f.isFile && extensions.contains(f.extension.lowercase())) {
                return f
            }
            if (f.isDirectory) {
                val nested = findAnyFileWithExtensions(f, extensions)
                if (nested != null) return nested
            }
        }
        return null
    }

    /**
     * Creates a portable `.bornotheme` ZIP package from a [KeyboardThemeSpec] and companion files.
     * Rewrites paths in `theme.json` to be clean relative paths.
     */
    fun packageThemeZip(
        spec: KeyboardThemeSpec,
        outputZipFile: File,
        json: Json
    ): File {
        outputZipFile.parentFile?.mkdirs()
        val filesToZip = mutableListOf<Pair<File, String>>() // local File to zip entry path

        var cleanSpec = spec.copy(schemaVersion = 2)

        // Wallpaper
        val bgPath = spec.background.image?.imagePath
        if (!bgPath.isNullOrBlank()) {
            val bgFile = File(bgPath)
            if (bgFile.exists() && bgFile.isFile) {
                val entryName = "background.${bgFile.extension.ifBlank { "jpg" }}"
                filesToZip.add(bgFile to entryName)
                cleanSpec = cleanSpec.copy(
                    background = cleanSpec.background.copy(
                        image = cleanSpec.background.image?.copy(imagePath = entryName)
                    ),
                    backgroundImageBase64 = null
                )
            }
        }

        // Font
        val fontPath = spec.fontPath
        if (!fontPath.isNullOrBlank()) {
            val fontFile = File(fontPath)
            if (fontFile.exists() && fontFile.isFile) {
                val entryName = "font.${fontFile.extension.ifBlank { "ttf" }}"
                filesToZip.add(fontFile to entryName)
                cleanSpec = cleanSpec.copy(fontPath = entryName)
            }
        }

        // Icons
        fun packageIcon(iconPath: String?, defaultName: String): String? {
            if (iconPath.isNullOrBlank()) return null
            val file = File(iconPath)
            return if (file.exists() && file.isFile) {
                val entryName = "icons/$defaultName.${file.extension.ifBlank { "png" }}"
                filesToZip.add(file to entryName)
                entryName
            } else {
                iconPath
            }
        }

        val packagedIcons = ThemeIconsSpec(
            shift = packageIcon(spec.icons.shift, "shift"),
            shiftOneShot = packageIcon(spec.icons.shiftOneShot, "shift_active"),
            capsLock = packageIcon(spec.icons.capsLock, "caps_lock"),
            backspace = packageIcon(spec.icons.backspace, "backspace"),
            action = packageIcon(spec.icons.action, "action"),
            language = packageIcon(spec.icons.language, "language"),
            emoji = packageIcon(spec.icons.emoji, "emoji"),
            settings = packageIcon(spec.icons.settings, "settings"),
            voice = packageIcon(spec.icons.voice, "voice"),
            search = packageIcon(spec.icons.search, "search"),
            moreKeys = packageIcon(spec.icons.moreKeys, "more_keys")
        )
        cleanSpec = cleanSpec.copy(icons = packagedIcons)

        val themeJsonBytes = json.encodeToString(KeyboardThemeSpec.serializer(), cleanSpec).toByteArray(Charsets.UTF_8)

        ZipOutputStream(FileOutputStream(outputZipFile)).use { zos ->
            // 1. theme.json entry
            val jsonEntry = ZipEntry("theme.json")
            zos.putNextEntry(jsonEntry)
            zos.write(themeJsonBytes)
            zos.closeEntry()

            // 2. Companion files
            for ((file, entryPath) in filesToZip) {
                val entry = ZipEntry(entryPath)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        return outputZipFile
    }
}

