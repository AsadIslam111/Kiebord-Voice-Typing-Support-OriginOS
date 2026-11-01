package io.kiebord.feature.ime.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ClipboardEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isOtp: Boolean = false
)

class SandboxedClipboardManager(context: Context) {
    private val appContext = context.applicationContext
    private val systemClipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clips = mutableStateListOf<ClipboardEntry>()

    var quickPasteClip by mutableStateOf<ClipboardEntry?>(null)

    private val otpPattern = Regex("^\\b\\d{4,8}\\b$")
    private val storageFile by lazy { File(appContext.filesDir, "kiebord_clipboard_v1.json") }
    private val ioScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private var lastSyncedText: String? = null
    private var lastSyncedTimestamp: Long = 0L

    init {
        ioScope.launch {
            loadClipsFromDisk()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                syncFromSystemClipboard()
            }
        }
    }

    fun syncFromSystemClipboard(callingContext: Context? = null, forceNotify: Boolean = false) {
        try {
            purgeExpiredOtps()
            val cm = (callingContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                ?: (appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                ?: systemClipboard
                ?: return

            if (!cm.hasPrimaryClip()) return

            val clipData = cm.primaryClip ?: return
            var foundText: String? = null
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i) ?: continue
                val text = item.text?.toString()
                    ?: runCatching { item.coerceToText(callingContext ?: appContext)?.toString() }.getOrNull()
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
                    foundText = text
                    break
                }
            }

            if (!foundText.isNullOrBlank()) {
                val trimmed = foundText.trim()
                if (trimmed.isNotEmpty()) {
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
                    } else {
                        val existing = clips.firstOrNull { it.text == trimmed }
                        if (existing == null) {
                            addClip(trimmed, notifyQuickPaste = false)
                        }
                    }
                }
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
        saveClipsToDisk()
    }

    fun deleteClips(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        clips.removeAll { it.id in idSet }
        if (quickPasteClip?.id in idSet) {
            quickPasteClip = null
        }
        saveClipsToDisk()
    }

    fun clearAllUnpinned() {
        clips.removeAll { !it.isPinned }
        if (quickPasteClip?.isPinned == false) {
            quickPasteClip = null
        }
        saveClipsToDisk()
    }

    fun clearAll() {
        clips.clear()
        quickPasteClip = null
        saveClipsToDisk()
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

    private fun isPotentialOtp(text: String): Boolean {
        return otpPattern.matches(text) || (text.length in 4..8 && text.all { it.isDigit() })
    }

    private fun saveClipsToDisk() {
        val snapshot = clips.toList()
        ioScope.launch {
            try {
                val jsonArray = JSONArray()
                for (entry in snapshot) {
                    val obj = JSONObject().apply {
                        put("id", entry.id)
                        put("text", entry.text)
                        put("timestamp", entry.timestamp)
                        put("isPinned", entry.isPinned)
                        put("isOtp", entry.isOtp)
                    }
                    jsonArray.put(obj)
                }
                storageFile.writeText(jsonArray.toString())
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadClipsFromDisk() {
        try {
            if (!storageFile.exists()) return
            val jsonStr = storageFile.readText()
            if (jsonStr.isBlank()) return
            val jsonArray = JSONArray(jsonStr)
            val loaded = mutableListOf<ClipboardEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val text = obj.optString("text", "")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val isPinned = obj.optBoolean("isPinned", false)
                val isOtp = obj.optBoolean("isOtp", false)
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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val currentIds = clips.map { it.id }.toSet()
                val currentTexts = clips.map { it.text }.toSet()
                for (item in loaded) {
                    if (item.id !in currentIds && item.text !in currentTexts) {
                        clips.add(item)
                    }
                }
            }
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
