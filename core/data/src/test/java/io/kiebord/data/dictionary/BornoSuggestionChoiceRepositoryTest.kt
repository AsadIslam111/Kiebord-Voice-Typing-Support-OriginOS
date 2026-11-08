package io.kiebord.data.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BornoSuggestionChoiceRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInputLengthGreaterThanOneConstraint() {
        val repo = BornoSuggestionChoiceRepository(tempFolder.root)

        // Length 1 (e.g. "a", "k") should NOT be recorded
        repo.recordChoice("a", "আমাদের")
        assertNull(repo.getChoice("a"))
        assertFalse(repo.hasChoice("a"))
        assertFalse(repo.isChoiceWord("আমাদের"))

        // Length > 1 (e.g. "am") should be recorded
        repo.recordChoice("am", "আমাদের")
        assertEquals("আমাদের", repo.getChoice("am"))
        assertTrue(repo.hasChoice("am"))
        assertTrue(repo.isChoiceWord("আমাদের"))
    }

    @Test
    fun testOverwriteChoice() {
        val repo = BornoSuggestionChoiceRepository(tempFolder.root)

        repo.recordChoice("am", "আমাদের")
        assertEquals("আমাদের", repo.getChoice("am"))

        // Overwrite mapping
        repo.recordChoice("am", "আমার")
        assertEquals("আমার", repo.getChoice("am"))
        assertTrue(repo.isChoiceWord("আমার"))
        assertFalse(repo.isChoiceWord("আমাদের"))
    }

    @Test
    fun testUnlearnWordRemovesMapping() {
        val repo = BornoSuggestionChoiceRepository(tempFolder.root)

        repo.recordChoice("am", "আমাদের")
        repo.recordChoice("amd", "আমাদের")
        assertTrue(repo.isChoiceWord("আমাদের"))
        assertEquals("আমাদের", repo.getChoice("am"))
        assertEquals("আমাদের", repo.getChoice("amd"))

        // Unlearning word removes all its mappings
        val removed = repo.unlearnWord("আমাদের")
        assertTrue(removed)
        assertFalse(repo.isChoiceWord("আমাদের"))
        assertNull(repo.getChoice("am"))
        assertNull(repo.getChoice("amd"))
    }

    @Test
    fun testUnlearnChoiceRemovesSingleMapping() {
        val repo = BornoSuggestionChoiceRepository(tempFolder.root)

        repo.recordChoice("am", "আমাদের")
        repo.recordChoice("amd", "আমাদের")

        val removed = repo.unlearnChoice("am")
        assertTrue(removed)
        assertNull(repo.getChoice("am"))
        assertEquals("আমাদের", repo.getChoice("amd"))
        assertTrue(repo.isChoiceWord("আমাদের"))
    }

    @Test
    fun testDiskPersistence() {
        val dir = tempFolder.root
        val repo1 = BornoSuggestionChoiceRepository(dir)
        repo1.recordChoice("am", "আমাদের")
        repo1.recordChoice("tumi", "তুমি")
        repo1.saveToDiskNow()

        // Create new instance from same directory to verify persistence
        val repo2 = BornoSuggestionChoiceRepository(dir)
        assertEquals("আমাদের", repo2.getChoice("am"))
        assertEquals("তুমি", repo2.getChoice("tumi"))
        assertTrue(repo2.isChoiceWord("আমাদের"))
        assertTrue(repo2.isChoiceWord("তুমি"))
    }

    @Test
    fun testClearAll() {
        val repo = BornoSuggestionChoiceRepository(tempFolder.root)
        repo.recordChoice("am", "আমাদের")
        repo.recordChoice("tumi", "তুমি")

        repo.clearAll()
        assertNull(repo.getChoice("am"))
        assertNull(repo.getChoice("tumi"))
        assertFalse(repo.isChoiceWord("আমাদের"))
    }
}

