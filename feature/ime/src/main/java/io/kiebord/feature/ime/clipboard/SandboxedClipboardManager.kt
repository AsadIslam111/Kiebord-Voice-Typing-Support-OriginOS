package io.kiebord.feature.ime.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.mutableStateListOf

data class ClipboardEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isOtp: Boolean = false
)

class SandboxedClipboardManager(context: Context) {
    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clips = mutableStateListOf<ClipboardEntry>()

    private val otpPattern = Regex("^\\b\\d{4,8}\\b$")

    fun syncFromSystemClipboard() {
        purgeExpiredOtps()
        val clipData = systemClipboard?.primaryClip ?: return
        if (clipData.itemCount > 0) {
            val text = clipData.getItemAt(0)?.coerceToText(null)?.toString()
            if (!text.isNullOrBlank()) {
                addClip(text)
            }
        }
    }

    fun addClip(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Check if clip already exists
        val existingIndex = clips.indexOfFirst { it.text == trimmed }
        if (existingIndex >= 0) {
            val existing = clips[existingIndex]
            clips.removeAt(existingIndex)
            clips.add(0, existing.copy(timestamp = System.currentTimeMillis()))
            return
        }

        val isOtp = isPotentialOtp(trimmed)
        clips.add(
            0,
            ClipboardEntry(
                text = trimmed,
                timestamp = System.currentTimeMillis(),
                isOtp = isOtp
            )
        )

        // Keep maximum 30 unpinned clips
        val unpinned = clips.filter { !it.isPinned }
        if (unpinned.size > 30) {
            val lastUnpinned = unpinned.last()
            clips.remove(lastUnpinned)
        }
    }

    fun togglePin(entry: ClipboardEntry) {
        val index = clips.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            clips[index] = entry.copy(isPinned = !entry.isPinned)
        }
    }

    fun deleteClip(entry: ClipboardEntry) {
        clips.removeAll { it.id == entry.id }
    }

    fun clearAllUnpinned() {
        clips.removeAll { !it.isPinned }
    }

    /**
     * Purges excess clips when system is under memory pressure, keeping only pinned clips and top 5 recent clips.
     */
    fun purgeOldClips() {
        val pinned = clips.filter { it.isPinned }
        val recentUnpinned = clips.filter { !it.isPinned }.take(5)
        clips.clear()
        clips.addAll((pinned + recentUnpinned).distinctBy { it.id })
    }

    private fun purgeExpiredOtps() {
        val now = System.currentTimeMillis()
        val fiveMinutes = 5 * 60 * 1000L
        clips.removeAll { it.isOtp && !it.isPinned && (now - it.timestamp > fiveMinutes) }
    }

    private fun isPotentialOtp(text: String): Boolean {
        return otpPattern.matches(text) || (text.length in 4..8 && text.all { it.isDigit() })
    }
}
