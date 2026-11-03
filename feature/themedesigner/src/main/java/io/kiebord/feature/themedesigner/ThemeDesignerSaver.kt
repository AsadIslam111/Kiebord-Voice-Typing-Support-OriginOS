package io.kiebord.feature.themedesigner

import android.content.Context
import android.net.Uri
import io.kiebord.theme.KeyboardThemeSpec
import io.kiebord.theme.ThemeIconsSpec
import io.kiebord.theme.ThemeRepository
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists designer output through the shared [ThemeRepository] pipeline so themes created in
 * Theme Studio behave exactly like imported `.bornotheme` files (listing, apply, delete, export).
 */
internal class ThemeDesignerSaver(private val context: Context) {

    private val repo = ThemeRepository.getInstance(context)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    /** Scratch folder for assets picked during an editing session (wallpaper, etc.). */
    private val sessionDir: File
        get() = File(context.cacheDir, "theme_studio_session").apply { if (!exists()) mkdirs() }

    /** Copies a picked content URI into the session folder and returns its absolute path. */
    fun importAsset(uri: Uri, baseName: String, fallbackExt: String): String? = runCatching {
        val ext = context.contentResolver.getType(uri)?.substringAfterLast('/')?.let {
            when (it) {
                "jpeg" -> "jpg"
                "x-font-ttf", "font-sfnt", "ttf" -> "ttf"
                "x-font-otf", "otf" -> "otf"
                "png", "webp", "jpg" -> it
                else -> null
            }
        } ?: uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.length in 2..4 }
            ?: fallbackExt
        val dest = File(sessionDir, "${baseName}_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: return@runCatching null
        dest.absolutePath
    }.getOrNull()

    fun loadExisting(themeId: String): KeyboardThemeSpec? = repo.getRichTheme(themeId)

    /**
     * Saves the theme. Color/gradient-only themes are stored as a single `.bornotheme` JSON file;
     * themes with a wallpaper or bundled font/icons are stored as an unpacked package folder (`theme.json` + assets).
     */
    fun save(state: ThemeDesignerState, isEditing: Boolean): Result<String> = runCatching {
        val spec = state.toSpec()
        val bgImage = spec.background.image?.imagePath?.let(::File)?.takeIf { it.exists() }
        val font = spec.fontPath?.let(::File)?.takeIf { it.exists() }
        val hasLocalIcons = listOfNotNull(
            spec.icons.shift,
            spec.icons.shiftOneShot,
            spec.icons.capsLock,
            spec.icons.backspace,
            spec.icons.action,
            spec.icons.language,
            spec.icons.emoji,
            spec.icons.settings,
            spec.icons.voice,
            spec.icons.search,
            spec.icons.moreKeys
        ).any { File(it).exists() }

        if (bgImage == null && font == null && !hasLocalIcons) {
            if (isEditing) repo.deleteCustomTheme(spec.id)
            repo.importRichTheme(spec.copy(fontPath = null)).id
        } else {
            // Stage the package first (assets may currently live inside the theme's own folder,
            // which gets replaced on save).
            val staging = File(context.cacheDir, "theme_studio_stage_${System.currentTimeMillis()}").apply {
                deleteRecursively()
                mkdirs()
            }
            var packaged = spec
            if (bgImage != null) {
                val name = "background.${bgImage.extension.ifBlank { "jpg" }}"
                bgImage.copyTo(File(staging, name), overwrite = true)
                packaged = packaged.copy(
                    background = packaged.background.copy(
                        image = packaged.background.image?.copy(imagePath = name)
                    )
                )
            }
            if (font != null) {
                val name = "font.${font.extension.ifBlank { "ttf" }}"
                font.copyTo(File(staging, name), overwrite = true)
                packaged = packaged.copy(fontPath = name)
            } else {
                packaged = packaged.copy(fontPath = null)
            }
            if (hasLocalIcons) {
                val iconsDir = File(staging, "icons").apply { mkdirs() }
                fun copyIcon(path: String?): String? {
                    if (path.isNullOrBlank()) return null
                    val f = File(path)
                    if (!f.exists()) return path
                    val rel = "icons/${f.name}"
                    f.copyTo(File(iconsDir, f.name), overwrite = true)
                    return rel
                }
                packaged = packaged.copy(
                    icons = ThemeIconsSpec(
                        shift = copyIcon(spec.icons.shift),
                        shiftOneShot = copyIcon(spec.icons.shiftOneShot),
                        capsLock = copyIcon(spec.icons.capsLock),
                        backspace = copyIcon(spec.icons.backspace),
                        action = copyIcon(spec.icons.action),
                        language = copyIcon(spec.icons.language),
                        emoji = copyIcon(spec.icons.emoji),
                        settings = copyIcon(spec.icons.settings),
                        voice = copyIcon(spec.icons.voice),
                        search = copyIcon(spec.icons.search),
                        moreKeys = copyIcon(spec.icons.moreKeys)
                    )
                )
            }
            File(staging, "theme.json").writeText(
                json.encodeToString(KeyboardThemeSpec.serializer(), packaged),
                Charsets.UTF_8
            )
            if (isEditing) repo.deleteCustomTheme(spec.id)
            repo.importStagedTheme(spec.id, staging).getOrThrow().id
        }.also {
            clearSession()
        }
    }

    fun clearSession() {
        runCatching { File(context.cacheDir, "theme_studio_session").deleteRecursively() }
    }
}
