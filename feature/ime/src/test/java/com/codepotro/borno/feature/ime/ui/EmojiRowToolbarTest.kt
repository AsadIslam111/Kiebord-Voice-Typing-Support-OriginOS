package com.codepotro.borno.feature.ime.ui

import com.codepotro.borno.data.prefs.KiebordPreferences
import com.codepotro.borno.feature.ime.ui.emoji.DEFAULT_EMOJI_ROW_ITEMS
import com.codepotro.borno.feature.ime.ui.toolbar.ToolbarAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiRowToolbarTest {

    @Test
    fun testToolbarActionEmojiRowToggleExists() {
        assertNotNull(ToolbarAction.EMOJI_ROW_TOGGLE)
        assertEquals(ToolbarAction.EMOJI_ROW_TOGGLE, ToolbarAction.valueOf("EMOJI_ROW_TOGGLE"))
    }

    @Test
    fun testKiebordPreferencesContainsEmojiRowToggleInDrawerTools() {
        val prefs = KiebordPreferences()
        assertTrue(
            "drawerTools must contain EMOJI_ROW_TOGGLE by default",
            prefs.drawerTools.contains("EMOJI_ROW_TOGGLE")
        )
    }

    @Test
    fun testDefaultEmojiRowItems() {
        val expected = listOf("😊", "😁", "😂", "❤️", "😍", "🙁", "😎", "👍", "🔥", "🎉")
        assertEquals(10, DEFAULT_EMOJI_ROW_ITEMS.size)
        assertEquals(expected, DEFAULT_EMOJI_ROW_ITEMS)
    }

    @Test
    fun testRecentEmojiPriority() {
        val recents = listOf("🔥", "🎉", "🚀")
        val combined = (recents + DEFAULT_EMOJI_ROW_ITEMS).distinct()

        // Most recent should appear first
        assertEquals("🔥", combined[0])
        assertEquals("🎉", combined[1])
        assertEquals("🚀", combined[2])

        // No duplicates
        assertEquals(combined.size, combined.distinct().size)
    }

    @Test
    fun testDeferredReorderMaintainsStableOrderWhileTapping() {
        val initialDisplay = DEFAULT_EMOJI_ROW_ITEMS.toList()
        val tapped = mutableListOf<String>()
        tapped.add("😂")
        tapped.add("😂")

        assertEquals("😂", initialDisplay[2])

        val distinctTapped = tapped.distinct()
        val reordered = (distinctTapped + initialDisplay).distinct()
        assertEquals("😂", reordered[0])
        assertEquals(initialDisplay.size, reordered.size)
    }
}

