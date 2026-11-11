package com.codepotro.borno.feature.ime.ui.emoji

import com.codepotro.borno.feature.ime.ui.suggestion.SuggestionCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class EmojiSearchCatalogTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val candidatePaths = listOf(
                File("src/main/assets/emoji/emoji-en-US.json"),
                File("feature/ime/src/main/assets/emoji/emoji-en-US.json")
            )
            val file = candidatePaths.firstOrNull { it.exists() }
                ?: error("Could not find emoji-en-US.json for test")
            FileInputStream(file).use { stream ->
                EmojiSearchCatalog.loadFromStream(stream)
            }
        }
    }

    @Test
    fun testExactMatches() {
        assertEquals("👋", EmojiSearchCatalog.findExact("hi"))
        assertEquals("🇧🇩", EmojiSearchCatalog.findExact("bangladesh"))
        assertEquals("🇧🇩", EmojiSearchCatalog.findExact("Bangladesh"))
        assertEquals("🛌", EmojiSearchCatalog.findExact("bed"))
        assertEquals("😀", EmojiSearchCatalog.findExact("happy"))
        assertEquals("😆", EmojiSearchCatalog.findExact("lol"))

        val bedMatches = EmojiSearchCatalog.findExactMatches("bed", limit = 2)
        assertEquals(2, bedMatches.size)
        assertEquals("🛌", bedMatches[0])
        assertEquals("🛏️", bedMatches[1])

        val hiMatches = EmojiSearchCatalog.findExactMatches("hi", limit = 2)
        assertEquals(1, hiMatches.size)
        assertEquals("👋", hiMatches[0])

        assertEquals("👨‍🏫", EmojiSearchCatalog.findExact("teacher"))
    }

    @Test
    fun testExactMatchUnknownReturnsNull() {
        assertNull(EmojiSearchCatalog.findExact("nonexistentkeyword12345"))
        assertNull(EmojiSearchCatalog.findExact(""))
        assertNull(EmojiSearchCatalog.findExact("gay"))
        assertNull(EmojiSearchCatalog.findExact("lesbian"))
        assertNull(EmojiSearchCatalog.findExact("lgbt"))
        assertNull(EmojiSearchCatalog.findExact("lgbtq"))
        assertTrue(EmojiSearchCatalog.findExactMatches("gay", limit = 2).isEmpty())
        assertTrue(EmojiSearchCatalog.findExactMatches("lesbian", limit = 2).isEmpty())
        assertNull(EmojiSearchCatalog.findExact("a"))
        assertNull(EmojiSearchCatalog.findExact("i"))
        assertNull(EmojiSearchCatalog.findExact("x"))
        assertNull(EmojiSearchCatalog.findExact("c"))
        assertTrue(EmojiSearchCatalog.findExactMatches("i", limit = 2).isEmpty())
        assertTrue(EmojiSearchCatalog.findExactMatches("a", limit = 2).isEmpty())
    }

    @Test
    fun testSearchPrefixAndKeywords() {
        val catResults = EmojiSearchCatalog.search("cat")
        assertTrue(catResults.contains("🐱"))

        val smileResults = EmojiSearchCatalog.search("smile")
        assertTrue(smileResults.isNotEmpty())
        assertTrue(smileResults.contains("😀") || smileResults.contains("😊"))

        // Length < 2 returns default candidates
        val emptyResults = EmojiSearchCatalog.search("")
        assertTrue(emptyResults.isNotEmpty())
        val singleCharResults = EmojiSearchCatalog.search("s")
        assertEquals(emptyResults, singleCharResults)
    }

    @Test
    fun testIsEmoji() {
        assertTrue(EmojiSearchCatalog.isEmoji("👋"))
        assertTrue(EmojiSearchCatalog.isEmoji("🇧🇩"))
        assertTrue(EmojiSearchCatalog.isEmoji("😀"))
        assertFalse(EmojiSearchCatalog.isEmoji("hello"))
        assertFalse(EmojiSearchCatalog.isEmoji("123"))
    }

    @Test
    fun testSuggestionStripPlacementNormalModeOneEmoji() {
        val candidates = mutableListOf(
            SuggestionCandidate("hi"),
            SuggestionCandidate("him"),
            SuggestionCandidate("his"),
            SuggestionCandidate("high")
        )
        EmojiSearchCatalog.insertEmojiSuggestions(
            candidateList = candidates,
            rawPrefix = "hi",
            isScrollable = false
        )

        assertEquals(5, candidates.size)
        val candidateTexts = candidates.map { it.text }
        // 4 candidates: word, word, word, emoji
        assertEquals(listOf("hi", "him", "his", "👋"), candidateTexts.take(4))
        assertEquals("high", candidateTexts[4])
    }

    @Test
    fun testSuggestionStripPlacementNormalModeTwoEmojis() {
        val candidates = mutableListOf(
            SuggestionCandidate("bed"),
            SuggestionCandidate("beds"),
            SuggestionCandidate("bedroom"),
            SuggestionCandidate("bedtime")
        )
        EmojiSearchCatalog.insertEmojiSuggestions(
            candidateList = candidates,
            rawPrefix = "bed",
            isScrollable = false
        )

        assertEquals(6, candidates.size)
        val candidateTexts = candidates.map { it.text }
        // If 4 candidates fit: word, word, emoji, emoji
        assertEquals(listOf("bed", "beds", "🛌", "🛏️"), candidateTexts.take(4))
        // If 3 candidates fit: word, word, emoji (at least 2 words before emoji, never word, emoji, emoji)
        assertEquals(listOf("bed", "beds", "🛌"), candidateTexts.take(3))
    }

    @Test
    fun testSuggestionStripPlacementScrollableMode() {
        val candidates = mutableListOf(
            SuggestionCandidate("hi"),
            SuggestionCandidate("him"),
            SuggestionCandidate("his"),
            SuggestionCandidate("high")
        )
        EmojiSearchCatalog.insertEmojiSuggestions(
            candidateList = candidates,
            rawPrefix = "hi",
            isScrollable = true
        )

        val candidateTexts = candidates.map { it.text }
        assertEquals("hi", candidateTexts[0])
        assertEquals("him", candidateTexts[1])
        assertEquals("👋", candidateTexts[2])
        assertEquals("his", candidateTexts[3])
        assertEquals("high", candidateTexts[4])
    }

    @Test
    fun testSuggestionStripPlacementScrollableModeTwoEmojis() {
        val candidates = mutableListOf(
            SuggestionCandidate("bed"),
            SuggestionCandidate("beds"),
            SuggestionCandidate("bedroom")
        )
        EmojiSearchCatalog.insertEmojiSuggestions(
            candidateList = candidates,
            rawPrefix = "bed",
            isScrollable = true
        )

        val candidateTexts = candidates.map { it.text }
        assertEquals("bed", candidateTexts[0])
        assertEquals("beds", candidateTexts[1])
        assertEquals("🛌", candidateTexts[2])
        assertEquals("🛏️", candidateTexts[3])
        assertEquals("bedroom", candidateTexts[4])
    }

    @Test
    fun testBornoPhoneticRawInputEmojiMatch() {
        // In Borno Phonetic, raw input is English while transliteration is Bengali
        val raw = "bangladesh"
        val translitCandidates = mutableListOf(
            SuggestionCandidate("বাংলাদেশ"),
            SuggestionCandidate("bangladesh"),
            SuggestionCandidate("বাংলা")
        )
        EmojiSearchCatalog.insertEmojiSuggestions(
            candidateList = translitCandidates,
            rawPrefix = raw,
            isScrollable = false
        )

        val candidateTexts = translitCandidates.map { it.text }
        // 4 candidates: word, word, word, emoji
        assertEquals(listOf("বাংলাদেশ", "bangladesh", "বাংলা", "🇧🇩"), candidateTexts.take(4))
    }

    @Test
    fun testBornoPhoneticTwoWordCandidatesWithEmoji() {
        val raw = "bangladesh"
        val translitCandidates = mutableListOf(
            SuggestionCandidate("বাংলাদেশ"),
            SuggestionCandidate("bangladesh")
        )
        EmojiSearchCatalog.insertEmojiSuggestions(
            candidateList = translitCandidates,
            rawPrefix = raw,
            isScrollable = false
        )

        val candidateTexts = translitCandidates.map { it.text }
        assertEquals(listOf("বাংলাদেশ", "bangladesh", "🇧🇩"), candidateTexts.take(3))
    }
}

