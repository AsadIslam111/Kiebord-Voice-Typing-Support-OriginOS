package io.kiebord.feature.ime.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SandboxedClipboardManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAddClipAndQuickPaste() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        val entry = manager.addClip("First clip", notifyQuickPaste = true)
        assertNotNull(entry)
        assertEquals(1, manager.clips.size)
        assertEquals("First clip", manager.clips[0].text)
        assertEquals(entry, manager.quickPasteClip)

        // Adding second clip
        val entry2 = manager.addClip("Second clip", notifyQuickPaste = true)
        assertEquals(2, manager.clips.size)
        assertEquals("Second clip", manager.clips[0].text)
        assertEquals("First clip", manager.clips[1].text)
        assertEquals(entry2, manager.quickPasteClip)
    }

    @Test
    fun testDeleteClipRemovesFromClipsAndClearsQuickPaste() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        val entry1 = manager.addClip("Clip 1", notifyQuickPaste = true)!!
        val entry2 = manager.addClip("Clip 2", notifyQuickPaste = true)!!

        assertEquals(2, manager.clips.size)
        assertEquals(entry2, manager.quickPasteClip)

        // Delete the latest copied clip
        manager.deleteClip(entry2)

        assertEquals(1, manager.clips.size)
        assertEquals(entry1.id, manager.clips[0].id)
        assertNull(manager.quickPasteClip)

        // Syncing must NOT resurrect the deleted clip
        manager.syncFromSystemClipboard()
        assertEquals(1, manager.clips.size)
        assertEquals("Clip 1", manager.clips[0].text)
    }

    @Test
    fun testDeleteClipsBatch() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        val entry1 = manager.addClip("Item 1")!!
        val entry2 = manager.addClip("Item 2")!!
        val entry3 = manager.addClip("Item 3")!!

        assertEquals(3, manager.clips.size)

        manager.deleteClips(listOf(entry1.id, entry3.id))
        assertEquals(1, manager.clips.size)
        assertEquals(entry2.id, manager.clips[0].id)
    }

    @Test
    fun testTogglePin() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        val entry = manager.addClip("Pinnable clip")!!
        assertFalse(manager.clips[0].isPinned)

        manager.togglePin(entry)
        assertTrue(manager.clips[0].isPinned)

        manager.togglePin(manager.clips[0])
        assertFalse(manager.clips[0].isPinned)
    }

    @Test
    fun testClearAllUnpinnedKeepsPinned() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        val entry1 = manager.addClip("Unpinned 1")!!
        val entry2 = manager.addClip("Pinned item")!!
        manager.togglePin(entry2)
        val entry3 = manager.addClip("Unpinned 2")!!

        assertEquals(3, manager.clips.size)

        manager.clearAllUnpinned()
        assertEquals(1, manager.clips.size)
        assertEquals("Pinned item", manager.clips[0].text)
        assertTrue(manager.clips[0].isPinned)
    }

    @Test
    fun testClearAll() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        manager.addClip("A")
        val b = manager.addClip("B")!!
        manager.togglePin(b)

        assertEquals(2, manager.clips.size)

        manager.clearAll()
        assertEquals(0, manager.clips.size)
        assertNull(manager.quickPasteClip)
    }

    @Test
    fun testDuplicateClipMovedToTop() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        manager.addClip("Alpha")
        manager.addClip("Beta")
        assertEquals(2, manager.clips.size)
        assertEquals("Beta", manager.clips[0].text)
        assertEquals("Alpha", manager.clips[1].text)

        // Adding "Alpha" again should move it to index 0
        manager.addClip("Alpha")
        assertEquals(2, manager.clips.size)
        assertEquals("Alpha", manager.clips[0].text)
        assertEquals("Beta", manager.clips[1].text)
    }

    @Test
    fun testPersistenceSaveAndReload() {
        val manager1 = SandboxedClipboardManager(filesDir = tempFolder.root)
        val entry1 = manager1.addClip("Saved clip 1")!!
        manager1.togglePin(entry1)
        manager1.addClip("Saved clip 2")

        // Wait/force synchronous write for test
        kotlinx.coroutines.runBlocking {
            manager1.saveClipsToDiskAwait()
        }

        // New instance loaded from same folder
        val manager2 = SandboxedClipboardManager(filesDir = tempFolder.root)
        assertEquals(2, manager2.clips.size)
        val loaded1 = manager2.clips.firstOrNull { it.text == "Saved clip 1" }
        assertNotNull(loaded1)
        assertTrue(loaded1!!.isPinned)

        val loaded2 = manager2.clips.firstOrNull { it.text == "Saved clip 2" }
        assertNotNull(loaded2)
        assertFalse(loaded2!!.isPinned)
    }

    @Test
    fun testUnpinnedClipsLimitIsEnforced() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)

        for (i in 1..55) {
            manager.addClip("Clip number $i")
        }

        val unpinned = manager.clips.filter { !it.isPinned }
        assertEquals(50, unpinned.size)
        assertEquals("Clip number 55", unpinned[0].text)
        assertEquals("Clip number 6", unpinned.last().text)
    }

    @Test
    fun testPurgeExpiredRecentClips() {
        val manager = SandboxedClipboardManager(filesDir = tempFolder.root)
        val now = System.currentTimeMillis()

        val freshClip = manager.addClip("Fresh clip")!!
        val oldClip = manager.addClip("Old clip")!!
        val pinnedOldClip = manager.addClip("Pinned old clip")!!
        manager.togglePin(pinnedOldClip)

        // Mock timestamps: 5 hours ago for old clips
        val fiveHoursAgo = now - (5 * 3600_000L)
        val idxOld = manager.clips.indexOfFirst { it.id == oldClip.id }
        manager.clips[idxOld] = manager.clips[idxOld].copy(timestamp = fiveHoursAgo)

        val idxPinned = manager.clips.indexOfFirst { it.id == pinnedOldClip.id }
        manager.clips[idxPinned] = manager.clips[idxPinned].copy(timestamp = fiveHoursAgo)

        assertEquals(3, manager.clips.size)

        // Purge clips older than 2 hours
        manager.purgeExpiredRecentClips(durationHours = 2)

        assertEquals(2, manager.clips.size)
        assertTrue(manager.clips.any { it.text == "Fresh clip" })
        assertTrue(manager.clips.any { it.text == "Pinned old clip" })
        assertFalse(manager.clips.any { it.text == "Old clip" })
    }
}

