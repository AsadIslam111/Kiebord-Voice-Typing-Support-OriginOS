package com.codepotro.borno.feature.ime.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class BornoClipboardBackupHelperTest {

    @Test
    fun testParseStandardBornoBackup() {
        val json = """
            {
              "db": {
                "version": 6,
                "data": [
                  {
                    "id": 1,
                    "text": "First copied item",
                    "isPinned": 1,
                    "time": "1730000000000"
                  },
                  {
                    "id": 2,
                    "text": "Second unpinned item",
                    "isPinned": 0,
                    "time": "1730000010000"
                  }
                ]
              }
            }
        """.trimIndent()

        val items = BornoClipboardBackupHelper.parseBornoBackup(
            ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8))
        )

        assertEquals(2, items.size)
        assertEquals("First copied item", items[0].text)
        assertTrue(items[0].isPinned)
        assertEquals(1730000000000L, items[0].timestamp)

        assertEquals("Second unpinned item", items[1].text)
        assertFalse(items[1].isPinned)
        assertEquals(1730000010000L, items[1].timestamp)
    }

    @Test
    fun testParseAlternativeVariantsAndTypes() {
        val json = """
            {
              "data": [
                {
                  "text": "Boolean pin",
                  "isPinned": true,
                  "time": 1730000020000
                },
                {
                  "text": "String boolean pin",
                  "isPinned": "true",
                  "timestamp": "1730000030000"
                },
                {
                  "text": "   ",
                  "isPinned": 1
                }
              ]
            }
        """.trimIndent()

        val items = BornoClipboardBackupHelper.parseBornoBackup(
            ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8))
        )

        assertEquals(2, items.size)
        assertEquals("Boolean pin", items[0].text)
        assertTrue(items[0].isPinned)
        assertEquals(1730000020000L, items[0].timestamp)

        assertEquals("String boolean pin", items[1].text)
        assertTrue(items[1].isPinned)
        assertEquals(1730000030000L, items[1].timestamp)
    }

    @Test
    fun testParseDirectJsonArray() {
        val json = """
            [
              {
                "text": "Direct array item",
                "isPinned": 0
              }
            ]
        """.trimIndent()

        val items = BornoClipboardBackupHelper.parseBornoBackup(
            ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8))
        )

        assertEquals(1, items.size)
        assertEquals("Direct array item", items[0].text)
        assertFalse(items[0].isPinned)
    }

    @Test
    fun testRoundTripExportAndImport() {
        val originalClips = listOf(
            ClipboardEntry(
                text = "Pinned clip 1",
                timestamp = 1730100000000L,
                isPinned = true
            ),
            ClipboardEntry(
                text = "Unpinned clip 2",
                timestamp = 1730200000000L,
                isPinned = false
            )
        )

        val outStream = ByteArrayOutputStream()
        val success = BornoClipboardBackupHelper.writeBornoBackup(originalClips, outStream)
        assertTrue(success)

        val exportedBytes = outStream.toByteArray()
        val inStream = ByteArrayInputStream(exportedBytes)
        val imported = BornoClipboardBackupHelper.parseBornoBackup(inStream)

        assertEquals(2, imported.size)
        assertEquals("Pinned clip 1", imported[0].text)
        assertTrue(imported[0].isPinned)
        assertEquals(1730100000000L, imported[0].timestamp)

        assertEquals("Unpinned clip 2", imported[1].text)
        assertFalse(imported[1].isPinned)
        assertEquals(1730200000000L, imported[1].timestamp)
    }

    @Test
    fun testCorruptedJsonReturnsEmptyList() {
        val corrupted = "{ this is not valid json : [[["
        val items = BornoClipboardBackupHelper.parseBornoBackup(
            ByteArrayInputStream(corrupted.toByteArray(StandardCharsets.UTF_8))
        )
        assertTrue(items.isEmpty())
    }
}

