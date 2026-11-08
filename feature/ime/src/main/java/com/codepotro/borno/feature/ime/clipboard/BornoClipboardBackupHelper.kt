package com.codepotro.borno.feature.ime.clipboard

import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

data class BornoImportItem(
    val text: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class BornoImportResult(
    val totalFound: Int,
    val importedCount: Int,
    val updatedPinnedCount: Int,
    val skippedDuplicates: Int
)

object BornoClipboardBackupHelper {

    /**
     * Parses a Borno Keyboard clipboard backup (.clip / JSON stream).
     * Supports:
     * - Standard Borno structure: { "db": { "version": 6, "data": [ { "id": 1, "text": "...", "isPinned": 1, "time": "..." } ] } }
     * - Alternative root data: { "data": [ ... ] }
     * - Direct JSON array: [ { ... } ]
     * - Flexible type conversions for isPinned (integer or boolean) and time (string or number).
     */
    fun parseBornoBackup(inputStream: InputStream): List<BornoImportItem> {
        val items = mutableListOf<BornoImportItem>()
        val contentBytes = try {
            inputStream.readBytes()
        } catch (_: Exception) {
            return emptyList()
        }

        if (contentBytes.isEmpty()) return emptyList()

        val textContent = try {
            String(contentBytes, StandardCharsets.UTF_8).trim()
        } catch (_: Exception) {
            return emptyList()
        }

        if (textContent.isEmpty()) return emptyList()

        try {
            val root = MiniJson.parse(textContent) ?: return emptyList()

            @Suppress("UNCHECKED_CAST")
            val dataList: List<Any?> = when (root) {
                is List<*> -> root
                is Map<*, *> -> {
                    val db = root["db"] as? Map<*, *>
                    (db?.get("data") as? List<*>) ?: (root["data"] as? List<*>) ?: emptyList<Any?>()
                }
                else -> emptyList()
            }

            for (element in dataList) {
                val itemMap = element as? Map<*, *> ?: continue
                val text = (itemMap["text"] as? String)?.trim()
                if (text.isNullOrEmpty()) continue

                val isPinned = when (val pinVal = itemMap["isPinned"]) {
                    is Boolean -> pinVal
                    is Number -> pinVal.toInt() != 0
                    is String -> pinVal == "1" || pinVal.equals("true", ignoreCase = true)
                    else -> false
                }

                val timestamp = when (val timeVal = itemMap["time"] ?: itemMap["timestamp"]) {
                    is Number -> timeVal.toLong()
                    is String -> timeVal.trim().toLongOrNull() ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                }

                items.add(
                    BornoImportItem(
                        text = text,
                        isPinned = isPinned,
                        timestamp = if (timestamp > 0L) timestamp else System.currentTimeMillis()
                    )
                )
            }
        } catch (_: Exception) {}

        return items
    }

    /**
     * Exports clipboard entries into standard Borno Keyboard format (.clip / JSON),
     * ensuring full bidirectional compatibility with Borno Keyboard and Kiebord.
     */
    fun writeBornoBackup(clips: List<ClipboardEntry>, outputStream: OutputStream): Boolean {
        return try {
            OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write("{\n")
                writer.write("  \"db\": {\n")
                writer.write("    \"version\": 6,\n")
                writer.write("    \"data\": [\n")

                for (i in clips.indices) {
                    val clip = clips[i]
                    val isLast = i == clips.size - 1
                    writer.write("      {\n")
                    writer.write("        \"id\": ${i + 1},\n")
                    writer.write("        \"text\": \"${escapeJson(clip.text)}\",\n")
                    writer.write("        \"isPinned\": ${if (clip.isPinned) 1 else 0},\n")
                    writer.write("        \"time\": \"${clip.timestamp}\"\n")
                    writer.write("      }${if (!isLast) "," else ""}\n")
                }

                writer.write("    ]\n")
                writer.write("  }\n")
                writer.write("}\n")
                writer.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    internal fun escapeJson(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Lightweight, self-contained JSON parser in pure Kotlin.
     * Avoids stub issues in unit tests and runs reliably across all Android platforms.
     */
    internal object MiniJson {
        fun parse(json: String): Any? {
            val src = json.trim()
            if (src.isEmpty()) return null
            val parser = StringParser(src)
            return parser.parseValue()
        }

        private class StringParser(private val s: String) {
            private var idx = 0

            private fun skipWhitespace() {
                while (idx < s.length && s[idx].isWhitespace()) {
                    idx++
                }
            }

            fun parseValue(): Any? {
                skipWhitespace()
                if (idx >= s.length) return null
                return when (s[idx]) {
                    '{' -> parseObject()
                    '[' -> parseArray()
                    '"' -> parseString()
                    't', 'f' -> parseBoolean()
                    'n' -> parseNull()
                    '-', in '0'..'9' -> parseNumber()
                    else -> null
                }
            }

            private fun parseObject(): Map<String, Any?> {
                val map = mutableMapOf<String, Any?>()
                idx++ // skip '{'
                skipWhitespace()
                if (idx < s.length && s[idx] == '}') {
                    idx++
                    return map
                }
                while (idx < s.length) {
                    skipWhitespace()
                    if (idx >= s.length || s[idx] != '"') break
                    val key = parseString()
                    skipWhitespace()
                    if (idx < s.length && s[idx] == ':') {
                        idx++
                    }
                    val value = parseValue()
                    map[key] = value
                    skipWhitespace()
                    if (idx < s.length && s[idx] == ',') {
                        idx++
                    } else if (idx < s.length && s[idx] == '}') {
                        idx++
                        break
                    } else {
                        idx++
                    }
                }
                return map
            }

            private fun parseArray(): List<Any?> {
                val list = mutableListOf<Any?>()
                idx++ // skip '['
                skipWhitespace()
                if (idx < s.length && s[idx] == ']') {
                    idx++
                    return list
                }
                while (idx < s.length) {
                    skipWhitespace()
                    val value = parseValue()
                    list.add(value)
                    skipWhitespace()
                    if (idx < s.length && s[idx] == ',') {
                        idx++
                    } else if (idx < s.length && s[idx] == ']') {
                        idx++
                        break
                    } else {
                        idx++
                    }
                }
                return list
            }

            private fun parseString(): String {
                idx++ // skip opening '"'
                val sb = StringBuilder()
                while (idx < s.length) {
                    val c = s[idx++]
                    if (c == '"') return sb.toString()
                    if (c == '\\' && idx < s.length) {
                        when (val esc = s[idx++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (idx + 4 <= s.length) {
                                    val hex = s.substring(idx, idx + 4)
                                    idx += 4
                                    hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                                }
                            }
                            else -> sb.append(esc)
                        }
                    } else {
                        sb.append(c)
                    }
                }
                return sb.toString()
            }

            private fun parseBoolean(): Boolean {
                return if (s.startsWith("true", idx)) {
                    idx += 4
                    true
                } else if (s.startsWith("false", idx)) {
                    idx += 5
                    false
                } else {
                    idx++
                    false
                }
            }

            private fun parseNull(): Any? {
                if (s.startsWith("null", idx)) {
                    idx += 4
                }
                return null
            }

            private fun parseNumber(): Number {
                val start = idx
                if (idx < s.length && s[idx] == '-') idx++
                while (idx < s.length && (s[idx].isDigit() || s[idx] == '.' || s[idx] == 'e' || s[idx] == 'E' || s[idx] == '+' || s[idx] == '-')) {
                    idx++
                }
                val numStr = s.substring(start, idx)
                return numStr.toLongOrNull() ?: numStr.toDoubleOrNull() ?: 0L
            }
        }
    }
}
