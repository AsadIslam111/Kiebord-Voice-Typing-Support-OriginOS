package io.kiebord.data.dictionary

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class PersonalDictionaryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAddAndQueryExactShortcut() {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        // Add Bengali shortcut: asm -> আসসালামু আলাইকুম
        val bnEntry = repo.addOrUpdate(
            word = "আসসালামু আলাইকুম",
            shortcut = "asm",
            language = PersonalDictionaryEntry.LANG_BN
        )

        // Query with matching language
        val bnMatches = repo.getShortcutMatches("asm", "bn")
        assertEquals(1, bnMatches.size)
        assertEquals("আসসালামু আলাইকুম", bnMatches[0].word)

        // Query with English layout -> shouldn't match bn-only entry
        val enMatches = repo.getShortcutMatches("asm", "en")
        assertTrue(enMatches.isEmpty())

        // Query with "all" layout
        val allMatches = repo.getShortcutMatches("asm", "all")
        assertEquals(1, allMatches.size)
    }

    @Test
    fun testUniversalAllLanguagesShortcut() {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        // Universal shortcut (All languages)
        repo.addOrUpdate(
            word = "আসসালামু আলাইকুম",
            shortcut = "asm",
            language = PersonalDictionaryEntry.LANG_ALL
        )

        // Universal shortcut matches in both bn and en layouts
        val bnMatches = repo.getShortcutMatches("asm", "bn")
        assertEquals(1, bnMatches.size)
        assertEquals("আসসালামু আলাইকুম", bnMatches[0].word)

        val enMatches = repo.getShortcutMatches("asm", "en")
        assertEquals(1, enMatches.size)
        assertEquals("আসসালামু আলাইকুম", enMatches[0].word)

        // Word prefix lookup with "all" language for English layout
        repo.addOrUpdate(word = "UniversalWord", shortcut = null, language = PersonalDictionaryEntry.LANG_ALL)
        val enPrefixMatches = repo.getPrefixMatches("Univ", "en")
        assertEquals(1, enPrefixMatches.size)
        assertEquals("UniversalWord", enPrefixMatches[0].word)
    }

    @Test
    fun testPrefixMatches() {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        repo.addOrUpdate(word = "আসসালামু আলাইকুম", shortcut = "asm", language = "all")
        repo.addOrUpdate(word = "আমাদের বিদ্যালয়", shortcut = null, language = "bn")
        repo.addOrUpdate(word = "Antigravity", shortcut = "ag", language = "en")
        repo.addOrUpdate(word = "Hello World Extra", shortcut = "helloworld", language = "en")

        // 1. Shorter shortcuts (length 2-4) require full shortcut typing:
        // Typing "a" or "as" must NOT trigger "asm"
        val prefixSingleChar = repo.getPrefixMatches("a", "bn")
        assertFalse(prefixSingleChar.any { it.word == "আসসালামু আলাইকুম" })

        val prefixPartial = repo.getPrefixMatches("as", "bn")
        assertFalse(prefixPartial.any { it.word == "আসসালামু আলাইকুম" })

        // Typing full shortcut "asm" triggers it
        val fullShortcut = repo.getPrefixMatches("asm", "bn")
        assertTrue(fullShortcut.any { it.word == "আসসালামু আলাইকুম" })

        // 2. Word prefix lookup "আম" -> suggests আমাদের বিদ্যালয়
        val prefixWord = repo.getPrefixMatches("আম", "bn")
        assertTrue(prefixWord.any { it.word == "আমাদের বিদ্যালয়" })

        // 3. English word prefix "Anti" -> suggests Antigravity
        val prefixEn = repo.getPrefixMatches("Anti", "en")
        assertTrue(prefixEn.any { it.word == "Antigravity" })

        // 4. Longer shortcuts (> 4 chars): typing 4+ characters matches as prefix
        val prefixLong = repo.getPrefixMatches("hell", "en")
        assertTrue(prefixLong.any { it.word == "Hello World Extra" })
    }

    @Test
    fun testUpdateAndDelete() {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        val entry = repo.addOrUpdate(word = "Hello", shortcut = "hl", language = "en")
        assertEquals("Hello", repo.getShortcutMatches("hl", "en").first().word)

        // Update word and shortcut
        repo.update(entry.copy(word = "Hello World", shortcut = "hw"))
        assertTrue(repo.getShortcutMatches("hl", "en").isEmpty())
        assertEquals("Hello World", repo.getShortcutMatches("hw", "en").first().word)

        // Delete
        val deleted = repo.delete(entry.id)
        assertTrue(deleted)
        assertTrue(repo.getShortcutMatches("hw", "en").isEmpty())
        assertTrue(repo.getAllEntries().isEmpty())
    }

    @Test
    fun testSearchFiltering() {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        repo.addOrUpdate(word = "আসসালামু আলাইকুম", shortcut = "asm", language = "bn")
        repo.addOrUpdate(word = "শুভ সকাল", shortcut = "ss", language = "bn")
        repo.addOrUpdate(word = "Good Morning", shortcut = "gm", language = "en")
        repo.addOrUpdate(word = "Universal Note", shortcut = "un", language = "all")

        // Search by word query
        val queryResult = repo.searchEntries("শুভ")
        assertEquals(1, queryResult.size)
        assertEquals("শুভ সকাল", queryResult[0].word)

        // Search by shortcut query
        val shortcutResult = repo.searchEntries("gm")
        assertEquals(1, shortcutResult.size)
        assertEquals("Good Morning", shortcutResult[0].word)

        // Filter by language: bn
        val bnOnly = repo.searchEntries("", languageFilter = "bn")
        assertEquals(2, bnOnly.size)

        // Filter by language: en
        val enOnly = repo.searchEntries("", languageFilter = "en")
        assertEquals(1, enOnly.size)

        // Filter by language: all (returns all)
        val all = repo.searchEntries("", languageFilter = "all")
        assertEquals(4, all.size)
    }

    @Test
    fun testExportAndImportJsonRoundTrip() = runBlocking {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        repo.addOrUpdate(word = "আসসালামু আলাইকুম", shortcut = "asm", language = "bn")
        repo.addOrUpdate(word = "On my way", shortcut = "omw", language = "en")
        repo.addOrUpdate(word = "Thank you", shortcut = "ty", language = "all")

        val out = ByteArrayOutputStream()
        val exportResult = repo.exportToJson(out)
        assertTrue(exportResult.isSuccess)
        assertEquals(3, exportResult.getOrNull())

        // Create new clean repository in a second folder
        val folder2 = tempFolder.newFolder("repo2")
        val repo2 = PersonalDictionaryRepository(folder2)
        assertTrue(repo2.getAllEntries().isEmpty())

        // Import the exported stream into repo2
        val inStream = ByteArrayInputStream(out.toByteArray())
        val importResult = repo2.importFromJson(inStream)
        assertTrue(importResult.isSuccess)

        val res = importResult.getOrNull()!!
        assertEquals(3, res.totalFound)
        assertEquals(3, res.importedCount)
        assertEquals(0, res.skippedDuplicates)

        val entries = repo2.getAllEntries()
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.word == "আসসালামু আলাইকুম" && it.shortcut == "asm" && it.language == "bn" })
        assertTrue(entries.any { it.word == "On my way" && it.shortcut == "omw" && it.language == "en" })
        assertTrue(entries.any { it.word == "Thank you" && it.shortcut == "ty" && it.language == "all" })

        // Re-importing same stream without replaceExisting should skip duplicates
        val inStream2 = ByteArrayInputStream(out.toByteArray())
        val importResult2 = repo2.importFromJson(inStream2, replaceExisting = false)
        val res2 = importResult2.getOrNull()!!
        assertEquals(3, res2.totalFound)
        assertEquals(0, res2.importedCount)
        assertEquals(3, res2.skippedDuplicates)
    }

    @Test
    fun testImportTsvFormat() = runBlocking {
        val repo = PersonalDictionaryRepository(tempFolder.root)

        val tsv = """
            আসসালামু আলাইকুম	asm	bn
            On my way	omw	en
            Cheers	ch	all
        """.trimIndent()

        val inStream = ByteArrayInputStream(tsv.toByteArray(StandardCharsets.UTF_8))
        val importResult = repo.importFromJson(inStream)
        assertTrue(importResult.isSuccess)

        val res = importResult.getOrNull()!!
        assertEquals(3, res.totalFound)
        assertEquals(3, res.importedCount)

        val entries = repo.getAllEntries()
        assertEquals(3, entries.size)
        assertEquals(1, repo.getShortcutMatches("asm", "bn").size)
        assertEquals(1, repo.getShortcutMatches("omw", "en").size)
    }
}

