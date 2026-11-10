package com.codepotro.borno.data.langpack

import android.content.Context
import com.codepotro.borno.data.layout.LayoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

class LanguagePackRepository internal constructor(
    private val filesDirProvider: () -> File,
    private val onInvalidateLayouts: () -> Unit = {}
) {
    private constructor(context: Context) : this(
        filesDirProvider = { context.filesDir },
        onInvalidateLayouts = { LayoutRepository.getInstance(context).invalidateCache() }
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val metadataCache = ConcurrentHashMap<String, LanguagePackMetadata>()
    private val scriptRulesCache = ConcurrentHashMap<String, ScriptRules>()
    private val charFoldingCache = ConcurrentHashMap<String, String>()

    val langpacksDir: File
        get() = File(filesDirProvider(), "langpacks").apply { if (!exists()) mkdirs() }

    val customLayoutsDir: File
        get() = File(filesDirProvider(), "layouts").apply { if (!exists()) mkdirs() }

    val customRulesDir: File
        get() = File(filesDirProvider(), "rules").apply { if (!exists()) mkdirs() }

    init {
        reloadInstalledCache()
    }

    @Synchronized
    fun reloadInstalledCache() {
        metadataCache.clear()
        scriptRulesCache.clear()
        charFoldingCache.clear()

        val dirs = langpacksDir.listFiles { f -> f.isDirectory && !f.name.startsWith(".") } ?: emptyArray()
        for (dir in dirs) {
            val manifestFile = File(dir, "pack.json")
            if (manifestFile.exists()) {
                runCatching {
                    val meta = json.decodeFromString<LanguagePackMetadata>(manifestFile.readText(Charsets.UTF_8))
                    metadataCache[meta.id] = meta
                    metadataCache[meta.language] = meta

                    meta.scriptRulesFile?.let { relPath ->
                        val rulesFile = File(dir, relPath)
                        if (rulesFile.exists()) {
                            val rulesJson = json.decodeFromString<ScriptRulesJson>(rulesFile.readText(Charsets.UTF_8))
                            val scriptRules = ScriptRules(
                                primaryScript = rulesJson.primaryScript,
                                allowedCharacters = rulesJson.allowedCharacters.toSet(),
                                wordConnectors = rulesJson.wordConnectors.mapNotNull { it.firstOrNull() }.toSet()
                            )
                            scriptRulesCache[meta.id] = scriptRules
                            scriptRulesCache[meta.language] = scriptRules
                        }
                    }

                    meta.charFoldingFile?.let { relPath ->
                        val foldingFile = File(dir, relPath)
                        if (foldingFile.exists()) {
                            val content = foldingFile.readText(Charsets.UTF_8)
                            charFoldingCache[meta.id] = content
                            charFoldingCache[meta.language] = content
                        }
                    }
                }
            }
        }
    }

    fun getInstalledPacks(): List<LanguagePackMetadata> {
        return metadataCache.values.distinctBy { it.id }
    }

    fun getPack(idOrLang: String): LanguagePackMetadata? {
        metadataCache[idOrLang]?.let { return it }
        val base = idOrLang.substringBefore('_')
        return metadataCache[base]
    }

    fun getPackForLayout(layoutId: String): LanguagePackMetadata? {
        val baseId = layoutId.removePrefix("bn_")
        for (pack in metadataCache.values) {
            if (pack.layouts.contains(layoutId) || pack.layouts.contains(baseId)) {
                return pack
            }
        }
        val prefix = layoutId.substringBefore('_')
        return getPack(prefix)
    }

    fun getDictionaryFile(idOrLang: String): File? {
        val pack = getPack(idOrLang) ?: return null
        val dictPath = pack.dictionaryFile ?: return null
        val file = File(File(langpacksDir, pack.id), dictPath)
        return if (file.exists()) file else null
    }

    fun getScriptRules(idOrLang: String): ScriptRules? {
        scriptRulesCache[idOrLang]?.let { return it }
        val base = idOrLang.substringBefore('_')
        return scriptRulesCache[base]
    }

    fun getCharFoldingJson(idOrLang: String): String? {
        charFoldingCache[idOrLang]?.let { return it }
        val base = idOrLang.substringBefore('_')
        return charFoldingCache[base]
    }

    fun installPack(inputStream: InputStream): Result<LanguagePackMetadata> {
        return runCatching {
            val tempDir = File(langpacksDir, ".tmp_install_${UUID.randomUUID()}").apply { mkdirs() }
            try {
                unzipWithZipSlipProtection(inputStream, tempDir)

                val manifestFile = File(tempDir, "pack.json")
                require(manifestFile.exists()) { "Invalid .bornolang pack: pack.json is missing" }

                val meta = json.decodeFromString<LanguagePackMetadata>(manifestFile.readText(Charsets.UTF_8))
                require(meta.id.isNotBlank()) { "Pack id must not be blank" }

                val targetPackDir = File(langpacksDir, meta.id)
                if (targetPackDir.exists()) {
                    targetPackDir.deleteRecursively()
                }

                if (!tempDir.renameTo(targetPackDir)) {
                    targetPackDir.mkdirs()
                    tempDir.copyRecursively(targetPackDir, overwrite = true)
                    tempDir.deleteRecursively()
                }

                val layoutsSourceDir = File(targetPackDir, "layouts")
                if (layoutsSourceDir.exists() && layoutsSourceDir.isDirectory) {
                    val layoutFiles = layoutsSourceDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
                    for (layoutFile in layoutFiles) {
                        val destLayout = File(customLayoutsDir, layoutFile.name)
                        layoutFile.copyTo(destLayout, overwrite = true)
                    }
                }

                val rulesSourceDir = File(targetPackDir, "rules")
                if (rulesSourceDir.exists() && rulesSourceDir.isDirectory) {
                    val ruleFiles = rulesSourceDir.listFiles() ?: emptyArray()
                    for (ruleFile in ruleFiles) {
                        val destRule = File(customRulesDir, ruleFile.name)
                        ruleFile.copyTo(destRule, overwrite = true)
                    }
                }

                reloadInstalledCache()
                onInvalidateLayouts()

                meta
            } catch (t: Throwable) {
                if (tempDir.exists()) {
                    tempDir.deleteRecursively()
                }
                throw t
            }
        }
    }

    fun uninstallPack(packId: String): Boolean {
        val pack = metadataCache[packId] ?: metadataCache.values.firstOrNull { it.id == packId }
        val targetPackDir = File(langpacksDir, packId)

        if (pack != null) {
            for (layoutName in pack.layouts) {
                val layoutFile = File(customLayoutsDir, "$layoutName.json")
                if (layoutFile.exists()) {
                    layoutFile.delete()
                }
            }

            val targetRulesDir = File(targetPackDir, "rules")
            if (targetRulesDir.exists()) {
                val ruleFiles = targetRulesDir.listFiles() ?: emptyArray()
                for (ruleFile in ruleFiles) {
                    val destRule = File(customRulesDir, ruleFile.name)
                    if (destRule.exists()) {
                        destRule.delete()
                    }
                }
            }
        }

        val deleted = if (targetPackDir.exists()) targetPackDir.deleteRecursively() else false

        reloadInstalledCache()
        onInvalidateLayouts()
        return deleted
    }

    suspend fun fetchCatalog(catalogUrl: String = DEFAULT_CATALOG_URL): Result<List<PackCatalogEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(catalogUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Kiebord-Android")
            }

            connection.inputStream.use { stream ->
                val body = stream.bufferedReader(Charsets.UTF_8).readText()
                json.decodeFromString<List<PackCatalogEntry>>(body)
            }
        }
    }

    suspend fun downloadAndInstall(
        entry: PackCatalogEntry,
        onProgress: (Float) -> Unit = {}
    ): Result<LanguagePackMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val tempDownloadFile = File(langpacksDir, ".tmp_dl_${UUID.randomUUID()}.bornolang")
            try {
                val url = URL(entry.downloadUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Kiebord-Android")
                }

                val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else entry.sizeBytes
                val digest = MessageDigest.getInstance("SHA-256")

                connection.inputStream.use { rawInput ->
                    BufferedInputStream(rawInput).use { input ->
                        FileOutputStream(tempDownloadFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var downloaded: Long = 0L

                            while (input.read(buffer).also { read = it } != -1) {
                                digest.update(buffer, 0, read)
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0) {
                                    onProgress(downloaded.toFloat() / totalBytes)
                                }
                            }
                        }
                    }
                }

                val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
                val expectedHash = entry.sha256.trim().lowercase()

                if (expectedHash.isNotEmpty() && !computedHash.equals(expectedHash, ignoreCase = true)) {
                    throw SecurityException("SHA-256 mismatch for ${entry.name}. Expected $expectedHash but got $computedHash")
                }

                tempDownloadFile.inputStream().use { stream ->
                    installPack(stream).getOrThrow()
                }
            } finally {
                if (tempDownloadFile.exists()) {
                    tempDownloadFile.delete()
                }
            }
        }
    }

    fun installBundledExtensionsIfNeeded(context: Context) {
        try {
            val extensionFiles = context.assets.list("extensions") ?: emptyArray()
            for (fileName in extensionFiles) {
                if (!fileName.endsWith(".bornolang")) continue
                val assetPath = "extensions/$fileName"
                try {
                    val (packId, version) = context.assets.open(assetPath).use { stream ->
                        ZipInputStream(BufferedInputStream(stream)).use { zipIn ->
                            var entry = zipIn.nextEntry
                            var id: String? = null
                            var ver = 1
                            while (entry != null) {
                                if (entry.name == "pack.json") {
                                    val content = zipIn.bufferedReader(Charsets.UTF_8).readText()
                                    val parsed = json.decodeFromString<LanguagePackMetadata>(content)
                                    id = parsed.id
                                    ver = parsed.version
                                    break
                                }
                                zipIn.closeEntry()
                                entry = zipIn.nextEntry
                            }
                            Pair(id, ver)
                        }
                    }

                    if (packId != null) {
                        val installed = metadataCache[packId]
                        if (installed == null || installed.version < version) {
                            context.assets.open(assetPath).use { stream ->
                                installPack(stream)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun unzipWithZipSlipProtection(inputStream: InputStream, targetDir: File) {
        val canonicalTarget = targetDir.canonicalPath
        ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val destFile = File(targetDir, entry.name)
                val canonicalDest = destFile.canonicalPath
                if (!canonicalDest.startsWith(canonicalTarget + File.separator) && canonicalDest != canonicalTarget) {
                    throw SecurityException("Zip Slip directory traversal detected: ${entry.name}")
                }

                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    companion object {
        const val DEFAULT_CATALOG_URL =
            "https://raw.githubusercontent.com/JayedAhsan/Kiebord/main/language-packs/index.json"

        @Volatile
        private var instance: LanguagePackRepository? = null

        fun getInstance(context: Context): LanguagePackRepository {
            return instance ?: synchronized(this) {
                instance ?: LanguagePackRepository(context.applicationContext ?: context).also {
                    instance = it
                    it.installBundledExtensionsIfNeeded(context)
                }
            }
        }
    }
}
