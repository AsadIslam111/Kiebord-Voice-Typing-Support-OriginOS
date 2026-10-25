package io.kiebord.feature.ime.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncFromSystemClipboard()
    }

    init {
        loadClipsFromDisk()
        try {
            systemClipboard?.addPrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}
        syncFromSystemClipboard()
    }

    fun syncFromSystemClipboard() {
        purgeExpiredOtps()
        val clipData = systemClipboard?.primaryClip ?: return
        if (clipData.itemCount > 0) {
            val text = clipData.getItemAt(0)?.coerceToText(appContext)?.toString()
            if (!text.isNullOrBlank()) {
                addClip(text)
            }
        }
    }

    fun addClip(text: String, notifyQuickPaste: Boolean = true): ClipboardEntry? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val existingIndex = clips.indexOfFirst { it.text == trimmed }
        val entry = if (existingIndex >= 0) {
            val existing = clips[existingIndex]
            clips.removeAt(existingIndex)
            val updated = existing.copy(timestamp = System.currentTimeMillis())
            clips.add(0, updated)
            updated
        } else {
            val isOtp = isPotentialOtp(trimmed)
            val newEntry = ClipboardEntry(
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
        try {
            val jsonArray = JSONArray()
            for (entry in clips) {
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

    private fun loadClipsFromDisk() {
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
