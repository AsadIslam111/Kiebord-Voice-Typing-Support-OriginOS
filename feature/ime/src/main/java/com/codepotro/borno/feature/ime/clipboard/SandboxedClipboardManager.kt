package com.codepotro.borno.feature.ime.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

data class ClipboardEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isOtp: Boolean = false
)

class SandboxedClipboardManager internal constructor(
    private val filesDir: File,
    private val systemClipboard: ClipboardManager? = null,
    context: Context? = null
) {
    private val appContext = context?.applicationContext

    constructor(context: Context) : this(
        filesDir = context.applicationContext.filesDir,
        systemClipboard = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager,
        context = context
    )

    val clips = mutableStateListOf<ClipboardEntry>()

    var quickPasteClip by mutableStateOf<ClipboardEntry?>(null)

    private val otpPattern = Regex("^\\b\\d{4,8}\\b$")
    private val storageFile by lazy { File(filesDir, "kiebord_clipboard_v1.json") }
    private val ioScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val fileWriteMutex = Mutex()

    private var lastSyncedText: String? = null
    private var lastSyncedTimestamp: Long = 0L

    init {
        loadClipsFromDiskSync()
        purgeExpiredRecentClipsFromPrefs()
        if (appContext != null) {
            ioScope.launch {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    syncFromSystemClipboard()
                }
            }
        }
    }

    private fun extractPrimaryClipText(cm: ClipboardManager, callingContext: Context? = null): String? {
        if (!cm.hasPrimaryClip()) return null
        val clipData = cm.primaryClip ?: return null
        val targetContext = callingContext ?: appContext
        for (i in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(i) ?: continue
            val text = item.text?.toString()
                ?: runCatching { item.coerceToText(targetContext)?.toString() }.getOrNull()
                ?: item.htmlText?.let {
                    runCatching {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            android.text.Html.fromHtml(it, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
                        } else {
                            @Suppress("DEPRECATION")
                            android.text.Html.fromHtml(it).toString()
                        }
                    }.getOrNull()
                }
                ?: item.htmlText

            if (!text.isNullOrBlank()) {
                return text
            }
        }
        return null
    }

    private fun clearSystemClipboardIfMatching(predicate: (String) -> Boolean) {
        try {
            val cm = (appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                ?: systemClipboard
                ?: return
            val currentText = extractPrimaryClipText(cm)?.trim() ?: return
            if (predicate(currentText)) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    cm.clearPrimaryClip()
                } else {
                    cm.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                lastSyncedText = null
                lastSyncedTimestamp = 0L
            }
        } catch (_: Exception) {}
    }

    fun syncFromSystemClipboard(callingContext: Context? = null, forceNotify: Boolean = false) {
        try {
            purgeExpiredOtps()
            purgeExpiredRecentClipsFromPrefs()
            val cm = (callingContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                ?: (appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                ?: systemClipboard
                ?: return

            val foundText = extractPrimaryClipText(cm, callingContext) ?: return
            val trimmed = foundText.trim()
            if (trimmed.isEmpty()) return

            val clipTimestamp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    cm.primaryClipDescription?.timestamp ?: 0L
                } catch (_: Exception) { 0L }
            } else {
                0L
            }

            val isNewCopy = forceNotify || (trimmed != lastSyncedText) || (clipTimestamp > 0L && clipTimestamp != lastSyncedTimestamp)

            if (clipTimestamp > 0L) {
                lastSyncedTimestamp = clipTimestamp
            }
            lastSyncedText = trimmed

            if (isNewCopy) {
                addClip(trimmed, notifyQuickPaste = true)
            }
        } catch (_: Exception) {}
    }

    fun addClip(text: String, notifyQuickPaste: Boolean = true): ClipboardEntry? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val existingIndex = clips.indexOfFirst { it.text == trimmed }
        val entry = if (existingIndex >= 0) {
            val existing = clips[existingIndex]
            clips.removeAt(existingIndex)
            val updated = existing.copy(id = java.util.UUID.randomUUID().toString(), timestamp = System.currentTimeMillis())
            clips.add(0, updated)
            updated
        } else {
            val isOtp = isPotentialOtp(trimmed)
            val newEntry = ClipboardEntry(
                id = java.util.UUID.randomUUID().toString(),
                text = trimmed,
                timestamp = System.currentTimeMillis(),
                isOtp = isOtp
            )
            clips.add(0, newEntry)
            newEntry
        }

        // Limit unpinned clips to 50
        val unpinned = clips.filter { !it.isPinned }
        if (unpinned.size > 50) {
            val lastUnpinned = unpinned.last()
            clips.remove(lastUnpinned)
        }

        if (notifyQuickPaste) {
            quickPasteClip = entry
        }

        saveClipsToDisk()
        return entry
    }

    fun clearQuickPaste() {
        quickPasteClip = null
    }

    fun togglePin(entry: ClipboardEntry) {
        val index = clips.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            val updated = entry.copy(isPinned = !entry.isPinned)
            clips[index] = updated
            saveClipsToDisk()
        }
    }

    fun deleteClip(entry: ClipboardEntry) {
        clips.removeAll { it.id == entry.id }
        if (quickPasteClip?.id == entry.id) {
            quickPasteClip = null
        }
        clearSystemClipboardIfMatching { it == entry.text.trim() }
        saveClipsToDisk()
    }

    fun deleteClips(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val deletedTexts = clips.filter { it.id in idSet }.map { it.text.trim() }.toSet()
        clips.removeAll { it.id in idSet }
        if (quickPasteClip?.id in idSet) {
            quickPasteClip = null
        }
        clearSystemClipboardIfMatching { it in deletedTexts }
        saveClipsToDisk()
    }

    fun clearAllUnpinned() {
        val unpinnedTexts = clips.filter { !it.isPinned }.map { it.text.trim() }.toSet()
        clips.removeAll { !it.isPinned }
        if (quickPasteClip?.isPinned == false) {
            quickPasteClip = null
        }
        clearSystemClipboardIfMatching { it in unpinnedTexts }
        saveClipsToDisk()
    }

    fun clearAll() {
        clips.clear()
        quickPasteClip = null
        clearSystemClipboardIfMatching { true }
        saveClipsToDisk()
    }

    suspend fun importParsedClips(
        items: List<BornoImportItem>,
        replaceExisting: Boolean
    ): BornoImportResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        if (replaceExisting) {
            clips.clear()
            quickPasteClip = null
            var pinnedCount = 0
            val deduplicated = mutableListOf<ClipboardEntry>()
            val seenTexts = mutableSetOf<String>()

            for (item in items) {
                val trimmed = item.text.trim()
                if (trimmed.isEmpty()) continue
                if (seenTexts.add(trimmed)) {
                    if (item.isPinned) pinnedCount++
                    deduplicated.add(
                        ClipboardEntry(
                            text = trimmed,
                            timestamp = if (item.timestamp > 0) item.timestamp else System.currentTimeMillis(),
                            isPinned = item.isPinned,
                            isOtp = false
                        )
                    )
                } else if (item.isPinned) {
                    val idx = deduplicated.indexOfFirst { it.text == trimmed }
                    if (idx >= 0 && !deduplicated[idx].isPinned) {
                        deduplicated[idx] = deduplicated[idx].copy(isPinned = true)
                        pinnedCount++
                    }
                }
            }
            clips.addAll(deduplicated)
            saveClipsToDiskAwait()
            BornoImportResult(
                totalFound = items.size,
                importedCount = deduplicated.size,
                updatedPinnedCount = 0,
                skippedDuplicates = items.size - deduplicated.size
            )
        } else {
            var importedCount = 0
            var updatedPinnedCount = 0
            var skippedDuplicates = 0
            val toAdd = mutableListOf<ClipboardEntry>()

            for (item in items) {
                val trimmed = item.text.trim()
                if (trimmed.isEmpty()) continue

                val existingIndex = clips.indexOfFirst { it.text == trimmed }
                if (existingIndex >= 0) {
                    val existing = clips[existingIndex]
                    if (item.isPinned && !existing.isPinned) {
                        clips[existingIndex] = existing.copy(isPinned = true)
                        updatedPinnedCount++
                    } else {
                        skippedDuplicates++
                    }
                } else {
                    toAdd.add(
                        ClipboardEntry(
                            text = trimmed,
                            timestamp = if (item.timestamp > 0) item.timestamp else System.currentTimeMillis(),
                            isPinned = item.isPinned,
                            isOtp = false
                        )
                    )
                    importedCount++
                }
            }
            clips.addAll(0, toAdd)
            saveClipsToDiskAwait()
            BornoImportResult(
                totalFound = items.size,
                importedCount = importedCount,
                updatedPinnedCount = updatedPinnedCount,
                skippedDuplicates = skippedDuplicates
            )
        }
    }

    suspend fun importBornoClipboardBackup(
        inputStream: java.io.InputStream,
        replaceExisting: Boolean = false
    ): Result<BornoImportResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val items = BornoClipboardBackupHelper.parseBornoBackup(inputStream)
            if (items.isEmpty()) {
                throw IllegalArgumentException("No valid clipboard items found in backup file.")
            }
            importParsedClips(items, replaceExisting)
        }
    }

    suspend fun exportClipboardBackup(
        outputStream: java.io.OutputStream
    ): Result<Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val snapshot = clips.toList()
            val success = BornoClipboardBackupHelper.writeBornoBackup(snapshot, outputStream)
            if (!success) {
                throw java.io.IOException("Failed to write clipboard backup to output stream.")
            }
            snapshot.size
        }
    }

    fun purgeOldClips() {
        val pinned = clips.filter { it.isPinned }
        val recentUnpinned = clips.filter { !it.isPinned }.take(5)
        clips.clear()
        clips.addAll((pinned + recentUnpinned).distinctBy { it.id })
        saveClipsToDisk()
    }

    private fun purgeExpiredOtps() {
        val now = System.currentTimeMillis()
        val fiveMinutes = 5 * 60 * 1000L
        val removed = clips.removeAll { it.isOtp && !it.isPinned && (now - it.timestamp > fiveMinutes) }
        if (removed) {
            saveClipsToDisk()
        }
    }

    fun purgeExpiredRecentClips(durationHours: Int) {
        val now = System.currentTimeMillis()
        val cutoff = now - (durationHours.coerceIn(1, 48) * 3600_000L)
        val removed = clips.removeAll { !it.isPinned && it.timestamp < cutoff }
        if (removed) {
            saveClipsToDisk()
        }
    }

    private fun purgeExpiredRecentClipsFromPrefs() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("kiebord_fast_cache", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("auto_delete_recent_clips", false)
            if (enabled) {
                val hours = prefs.getInt("auto_delete_recent_clips_duration_hours", 24)
                purgeExpiredRecentClips(hours)
            }
        } catch (_: Exception) {}
    }

    private fun isPotentialOtp(text: String): Boolean {
        return otpPattern.matches(text) || (text.length in 4..8 && text.all { it.isDigit() })
    }

    private fun writeClipsToFileDirect(snapshot: List<ClipboardEntry>) {
        try {
            val sb = StringBuilder()
            sb.append("[\n")
            for (i in snapshot.indices) {
                val entry = snapshot[i]
                sb.append("  {\n")
                sb.append("    \"id\": \"").append(BornoClipboardBackupHelper.escapeJson(entry.id)).append("\",\n")
                sb.append("    \"text\": \"").append(BornoClipboardBackupHelper.escapeJson(entry.text)).append("\",\n")
                sb.append("    \"timestamp\": ").append(entry.timestamp).append(",\n")
                sb.append("    \"isPinned\": ").append(entry.isPinned).append(",\n")
                sb.append("    \"isOtp\": ").append(entry.isOtp).append("\n")
                sb.append("  }")
                if (i < snapshot.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("]\n")

            val parent = storageFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tempFile = File(filesDir, "kiebord_clipboard_v1.tmp")
            tempFile.writeText(sb.toString())
            if (tempFile.exists()) {
                if (storageFile.exists()) {
                    storageFile.delete()
                }
                tempFile.renameTo(storageFile)
            }
        } catch (_: Exception) {}
    }

    private fun saveClipsToDisk() {
        val snapshot = clips.toList()
        ioScope.launch {
            fileWriteMutex.withLock {
                writeClipsToFileDirect(snapshot)
            }
        }
    }

    suspend fun saveClipsToDiskAwait() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val snapshot = clips.toList()
        fileWriteMutex.withLock {
            writeClipsToFileDirect(snapshot)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadClipsFromDiskSync() {
        try {
            if (!storageFile.exists()) return
            val jsonStr = storageFile.readText()
            if (jsonStr.isBlank()) return
            val parsed = BornoClipboardBackupHelper.MiniJson.parse(jsonStr) as? List<Map<String, Any?>> ?: return
            val loaded = mutableListOf<ClipboardEntry>()
            for (map in parsed) {
                val id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                val text = map["text"] as? String ?: ""
                val timestamp = when (val t = map["timestamp"]) {
                    is Number -> t.toLong()
                    is String -> t.toLongOrNull() ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                }
                val isPinned = map["isPinned"] as? Boolean ?: (map["isPinned"] == 1 || map["isPinned"] == "1")
                val isOtp = map["isOtp"] as? Boolean ?: (map["isOtp"] == 1 || map["isOtp"] == "1")
                if (text.isNotBlank()) {
                    loaded.add(
                        ClipboardEntry(
                            id = id,
                            text = text,
                            timestamp = timestamp,
                            isPinned = isPinned,
                            isOtp = isOtp
                        )
                    )
                }
            }
            clips.clear()
            clips.addAll(loaded)
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: SandboxedClipboardManager? = null

        fun getInstance(context: Context): SandboxedClipboardManager {
            return instance ?: synchronized(this) {
                instance ?: SandboxedClipboardManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
