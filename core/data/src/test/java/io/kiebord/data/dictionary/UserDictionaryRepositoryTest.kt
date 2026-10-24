package io.kiebord.data.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserDictionaryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testLearnedBengaliBigrams() {
        val repo = UserDictionaryRepository(tempFolder.root)

        // Record "নাম" following "আমার"
        repo.recordWord("নাম", prevWord = "আমার")

        val nextWords = repo.getNextWords("আমার")
        assertEquals(listOf("নাম"), nextWords)
    }

    @Test
    fun testLearnedBigramRankingByFrequency() {
        val repo = UserDictionaryRepository(tempFolder.root)

        // "আমার দেশ" typed once, "আমার নাম" typed twice
        repo.recordWord("দেশ", prevWord = "আমার")
        repo.recordWord("নাম", prevWord = "আমার")
        repo.recordWord("নাম", prevWord = "আমার")

        val nextWords = repo.getNextWords("আমার")
        assertEquals("নাম", nextWords.firstOrNull())
        assertTrue(nextWords.contains("দেশ"))
    }

    @Test
    fun testReduplicatedWordsBigram() {
        val repo = UserDictionaryRepository(tempFolder.root)

        // "খুব খুব" (khub khub)
        repo.recordWord("খুব", prevWord = "খুব")

        val nextWords = repo.getNextWords("খুব")
        assertEquals(listOf("খুব"), nextWords)
    }

    @Test
    fun testCompletionsForLearnedWords() {
        val repo = UserDictionaryRepository(tempFolder.root)

        repo.recordWord("বাংলাদেশ")
        repo.recordWord("বাঙালি")

        val completions = repo.getCompletions("বা")
        assertTrue(completions.contains("বাংলাদেশ"))
        assertTrue(completions.contains("বাঙালি"))
    }
}
