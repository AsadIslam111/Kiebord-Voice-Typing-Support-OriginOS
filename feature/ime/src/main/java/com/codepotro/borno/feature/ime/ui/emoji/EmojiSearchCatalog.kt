package com.codepotro.borno.feature.ime.ui.emoji

import android.content.Context
import com.codepotro.borno.feature.ime.ui.suggestion.SuggestionCandidate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.nio.charset.StandardCharsets

object EmojiSearchCatalog {
    private class EmojiEntry(
        val emoji: String,
        val keywords: Array<String>
    )

    @Volatile
    private var isLoaded = false
    private val lock = Any()
    private var appContext: Context? = null

    private var allEntries: Array<EmojiEntry> = emptyArray()
    private var exactMap: Map<String, List<String>> = emptyMap()
    private var allEmojiSet: Set<String> = emptySet()

    private val searchCache = object : LinkedHashMap<String, List<String>>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean {
            return size > 128
        }
    }

    private val defaultCandidates = listOf(
        "😊", "❤️", "🔥", "👍", "🎉", "😂", "✨", "😍", "🙌", "😎", "🥳", "🍕",
        "👏", "💯", "💖", "🥰", "😭", "🥺", "😉", "🤩", "⭐", "💪", "🚀", "👌",
        "🤤", "😜", "😴", "🤔", "👀", "🤝", "🎶", "☕", "🤣", "💀", "😇", "😋",
        "🤗", "🤭", "🤫", "😏", "😬", "🙄", "😱", "🤠", "🤑", "👋", "🙏", "🍻"
    )

    fun init(context: Context) {
        if (isLoaded) return
        appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            ensureLoadedInternal()
        }
    }

    fun loadFromStream(inputStream: InputStream) {
        synchronized(lock) {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            parseAndPopulate(content)
        }
    }

    private fun ensureLoadedInternal() {
        if (isLoaded) return
        synchronized(lock) {
            if (isLoaded) return
            val ctx = appContext ?: return
            try {
                ctx.assets.open("emoji/emoji-en-US.json").use { stream ->
                    val content = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    parseAndPopulate(content)
                }
            } catch (_: Exception) {}
        }
    }

    private fun parseAndPopulate(content: String) {
        val n = content.length
        var i = 0
        val entries = ArrayList<EmojiEntry>(1950)
        val exact = HashMap<String, MutableList<String>>(6500)
        val emojiSet = HashSet<String>(1950)

        while (i < n) {
            while (i < n && content[i] != '"') i++
            if (i >= n) break
            i++

            val emojiSb = StringBuilder()
            while (i < n && content[i] != '"') {
                if (content[i] == '\\' && i + 1 < n) {
                    i++
                    if (content[i] == 'u' && i + 4 < n) {
                        val hex = content.substring(i + 1, i + 5).toIntOrNull(16) ?: 0
                        emojiSb.append(hex.toChar())
                        i += 4
                    } else {
                        emojiSb.append(content[i])
                    }
                } else {
                    emojiSb.append(content[i])
                }
                i++
            }
            val emoji = emojiSb.toString()
            i++

            while (i < n && content[i] != '[') i++
            i++

            val keywords = ArrayList<String>(12)
            while (i < n) {
                while (i < n && content[i] != '"' && content[i] != ']') i++
                if (i >= n || content[i] == ']') {
                    i++
                    break
                }
                i++

                val kwSb = StringBuilder()
                while (i < n && content[i] != '"') {
                    if (content[i] == '\\' && i + 1 < n) {
                        i++
                        if (content[i] == 'u' && i + 4 < n) {
                            val hex = content.substring(i + 1, i + 5).toIntOrNull(16) ?: 0
                            kwSb.append(hex.toChar())
                            i += 4
                        } else {
                            kwSb.append(content[i])
                        }
                    } else {
                        kwSb.append(content[i])
                    }
                    i++
                }
                val kw = kwSb.toString().lowercase().trim()
                if (kw.isNotEmpty()) {
                    keywords.add(kw)
                    val list = exact.getOrPut(kw) { ArrayList(2) }
                    if (!list.contains(emoji)) {
                        list.add(emoji)
                    }
                }
                i++
            }

            if (emoji.isNotEmpty()) {
                entries.add(EmojiEntry(emoji, keywords.toTypedArray()))
                emojiSet.add(emoji)
            }
        }

        allEntries = entries.toTypedArray()
        exactMap = exact
        allEmojiSet = emojiSet
        synchronized(searchCache) {
            searchCache.clear()
        }
        isLoaded = true
    }

    fun isEmoji(text: String): Boolean {
        if (!isLoaded) ensureLoadedInternal()
        return allEmojiSet.contains(text)
    }

    fun insertEmojiSuggestions(
        candidateList: MutableList<SuggestionCandidate>,
        rawPrefix: String,
        isScrollable: Boolean,
        emojiSuggestionsEnabled: Boolean = true
    ) {
        val trimmed = rawPrefix.trim()
        if (!emojiSuggestionsEnabled || trimmed.length < 2 || candidateList.isEmpty()) return
        val exactEmojis = findExactMatches(trimmed, limit = 2)
        if (exactEmojis.isEmpty()) return

        val wordsCount = candidateList.size
        val targetPos = if (isScrollable || exactEmojis.size >= 2) {
            minOf(2, wordsCount)
        } else {
            minOf(3, wordsCount)
        }

        var addedCount = 0
        val maxToAdd = minOf(exactEmojis.size, 2)
        for (i in 0 until maxToAdd) {
            val emoji = exactEmojis[i]
            var alreadyPresent = false
            for (j in 0 until candidateList.size) {
                if (candidateList[j].text == emoji) {
                    alreadyPresent = true
                    break
                }
            }
            if (!alreadyPresent) {
                val insertIndex = minOf(targetPos + addedCount, candidateList.size)
                candidateList.add(
                    insertIndex,
                    SuggestionCandidate(text = emoji, isPrimary = false, rawInput = rawPrefix, isEmoji = true)
                )
                addedCount++
            }
        }
    }

    fun findExactMatches(keyword: String, limit: Int = 2): List<String> {
        if (!isLoaded) ensureLoadedInternal()
        val q = keyword.trim().lowercase()
        if (q.length < 2) return emptyList()
        val matches = exactMap[q] ?: return emptyList()
        return matches.take(limit)
    }

    fun findExact(keyword: String): String? {
        return findExactMatches(keyword, 1).firstOrNull()
    }

    fun search(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.length < 2) return defaultCandidates

        if (!isLoaded) ensureLoadedInternal()
        if (allEntries.isEmpty()) return emptyList()

        synchronized(searchCache) {
            val cached = searchCache[q]
            if (cached != null) return cached
        }

        val exactMatches = ArrayList<String>()
        val prefixMatches = ArrayList<String>()
        val substringMatches = ArrayList<String>()

        for (entry in allEntries) {
            val emoji = entry.emoji
            var matchedTier = 0
            for (kw in entry.keywords) {
                if (kw == q) {
                    matchedTier = 1
                    break
                } else if (matchedTier < 2 && (kw.startsWith(q) || kw.split('_', ' ').any { it.startsWith(q) })) {
                    matchedTier = 2
                } else if (matchedTier == 0 && kw.contains(q)) {
                    matchedTier = 3
                }
            }
            when (matchedTier) {
                1 -> exactMatches.add(emoji)
                2 -> prefixMatches.add(emoji)
                3 -> substringMatches.add(emoji)
            }
        }

        val result = ArrayList<String>(exactMatches.size + prefixMatches.size + substringMatches.size)
        val seen = HashSet<String>()
        for (e in exactMatches) {
            if (seen.add(e)) result.add(e)
        }
        for (e in prefixMatches) {
            if (seen.add(e)) result.add(e)
        }
        for (e in substringMatches) {
            if (seen.add(e)) result.add(e)
        }

        synchronized(searchCache) {
            searchCache[q] = result
        }
        return result
    }

    fun getDeleteLengthBeforeCursor(text: CharSequence?): Int {
        if (text == null || text.isEmpty()) return 0

        val len = text.length
        val end = len
        var i = len

        val lastCodePoint = Character.codePointBefore(text, i)
        val lastCharCount = Character.charCount(lastCodePoint)
        i -= lastCharCount

        if (lastCodePoint < 0x2000 && lastCodePoint != 0x00A9 && lastCodePoint != 0x00AE) {
            return 1
        }

        if (lastCodePoint == 0x20E3 && i > 0) {
            var prevCp = Character.codePointBefore(text, i)
            if (prevCp == 0xFE0F) {
                i -= Character.charCount(prevCp)
                if (i > 0) {
                    prevCp = Character.codePointBefore(text, i)
                }
            }
            if ((prevCp in '0'.code..'9'.code) || prevCp == '#'.code || prevCp == '*'.code) {
                i -= Character.charCount(prevCp)
                return end - i
            }
        }

        if (lastCodePoint == 0xE007F && i > 0) {
            while (i > 0) {
                val cp = Character.codePointBefore(text, i)
                if (cp in 0xE0020..0xE007E) {
                    i -= Character.charCount(cp)
                } else if (cp == 0x1F3F4) {
                    i -= Character.charCount(cp)
                    return end - i
                } else {
                    break
                }
            }
        }

        if (lastCodePoint in 0x1F1E6..0x1F1FF) {
            var riCount = 1
            var tempI = i
            while (tempI > 0) {
                val prevCp = Character.codePointBefore(text, tempI)
                if (prevCp in 0x1F1E6..0x1F1FF) {
                    riCount++
                    tempI -= Character.charCount(prevCp)
                } else {
                    break
                }
            }
            return if (riCount % 2 == 0) {
                Character.charCount(lastCodePoint) * 2
            } else {
                Character.charCount(lastCodePoint)
            }
        }

        var currentCp = lastCodePoint
        while (i > 0) {
            val cp = Character.codePointBefore(text, i)
            val cpChars = Character.charCount(cp)

            if (cp in 0x1F3FB..0x1F3FF) {
                i -= cpChars
                currentCp = cp
                continue
            }

            if (cp == 0xFE0F || cp == 0xFE0E) {
                i -= cpChars
                currentCp = cp
                continue
            }

            if (cp == 0x200D) {
                i -= cpChars
                if (i > 0) {
                    val prevCp = Character.codePointBefore(text, i)
                    i -= Character.charCount(prevCp)
                    currentCp = prevCp
                    continue
                } else {
                    break
                }
            }

            if (currentCp in 0x1F3FB..0x1F3FF || currentCp == 0xFE0F || currentCp == 0xFE0E) {
                i -= cpChars
                currentCp = cp
                continue
            }

            break
        }

        return (end - i).coerceAtLeast(1)
    }
}

