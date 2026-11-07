package io.kiebord.data.langpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LanguagePackRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSampleZip(includeMaliciousPath: Boolean = false): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            if (includeMaliciousPath) {
                zos.putNextEntry(ZipEntry("../evil.txt"))
                zos.write("malicious payload".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            } else {
                zos.putNextEntry(ZipEntry("pack.json"))
                val manifest = """
                    {
                      "id": "tr_sample",
                      "version": 1,
                      "language": "tr",
                      "displayName": "Turkish Sample",
                      "author": "Tester",
                      "layouts": ["tr_sample_q"],
                      "dictionaryFile": "dictionaries/tr.dict",
                      "scriptRulesFile": "rules/script_rules.json",
                      "charFoldingFile": "gesture/char_folding.json"
                    }
                """.trimIndent()
                zos.write(manifest.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("layouts/tr_sample_q.json"))
                val layout = """
                    {
                      "id": "tr_sample_q",
                      "name": "Turkish Sample Q",
                      "language": "tr",
                      "rows": []
                    }
                """.trimIndent()
                zos.write(layout.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("dictionaries/tr.dict"))
                zos.write(byteArrayOf(0x01, 0x02, 0x03, 0x04))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("rules/script_rules.json"))
                val rules = """
                    {
                      "primary_script": "LATIN",
                      "allowed_characters": "abcçdefgğhıijklmnoöprsştuüvyz",
                      "word_connectors": ["'"]
                    }
                """.trimIndent()
                zos.write(rules.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("gesture/char_folding.json"))
                val folding = """
                    {
                      "version": 1,
                      "fold_to_base": { "ğ": "g" }
                    }
                """.trimIndent()
                zos.write(folding.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun testInstallPackAndQuery() {
        var invalidated = false
        val repo = LanguagePackRepository(
            filesDirProvider = { tempFolder.root },
            onInvalidateLayouts = { invalidated = true }
        )

        val zipBytes = createSampleZip(includeMaliciousPath = false)
        val result = repo.installPack(ByteArrayInputStream(zipBytes))
        assertTrue(result.isSuccess)
        assertTrue(invalidated)

        val meta = result.getOrThrow()
        assertEquals("tr_sample", meta.id)
        assertEquals("tr", meta.language)
        assertEquals("Turkish Sample", meta.displayName)

        val installed = repo.getInstalledPacks()
        assertEquals(1, installed.size)
        assertEquals("tr_sample", installed[0].id)

        val packForLayout = repo.getPackForLayout("tr_sample_q")
        assertNotNull(packForLayout)
        assertEquals("tr_sample", packForLayout?.id)

        val dictFile = repo.getDictionaryFile("tr_sample")
        assertNotNull(dictFile)
        assertTrue(dictFile?.exists() == true)

        val rules = repo.getScriptRules("tr_sample")
        assertNotNull(rules)
        assertTrue(rules?.allowedCharacters?.contains('ç') == true)
        assertTrue(rules?.allowedCharacters?.contains('ğ') == true)
        assertTrue(rules?.allowedCharacters?.contains('ı') == true)
        assertTrue(rules?.wordConnectors?.contains('\'') == true)

        val foldingJson = repo.getCharFoldingJson("tr_sample")
        assertNotNull(foldingJson)
        assertTrue(foldingJson?.contains("\"ğ\": \"g\"") == true)

        val layoutFile = File(repo.customLayoutsDir, "tr_sample_q.json")
        assertTrue(layoutFile.exists())
    }

    @Test
    fun testZipSlipProtection() {
        val repo = LanguagePackRepository(
            filesDirProvider = { tempFolder.root },
            onInvalidateLayouts = {}
        )

        val maliciousZip = createSampleZip(includeMaliciousPath = true)
        val result = repo.installPack(ByteArrayInputStream(maliciousZip))
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is SecurityException)
        assertTrue(ex?.message?.contains("Zip Slip") == true)
    }

    @Test
    fun testUninstallPack() {
        var invalidatedCount = 0
        val repo = LanguagePackRepository(
            filesDirProvider = { tempFolder.root },
            onInvalidateLayouts = { invalidatedCount++ }
        )

        val zipBytes = createSampleZip(includeMaliciousPath = false)
        repo.installPack(ByteArrayInputStream(zipBytes))

        val layoutFile = File(repo.customLayoutsDir, "tr_sample_q.json")
        assertTrue(layoutFile.exists())

        val uninstalled = repo.uninstallPack("tr_sample")
        assertTrue(uninstalled)
        assertTrue(repo.getInstalledPacks().isEmpty())
        assertFalse(layoutFile.exists())
        assertEquals(2, invalidatedCount)
    }
}

