package io.kiebord.data.layout

import io.kiebord.common.model.KeyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredefinedLayoutsTest {

    @Test
    fun testLayoutDisplayNames() {
        assertEquals("English", PredefinedLayouts.displayName("en_qwerty"))
        assertEquals("জাতীয় (BSTI)", PredefinedLayouts.displayName("bn_national"))
        assertEquals("বর্ণ ফোনেটিক", PredefinedLayouts.displayName("bn_borno"))
        assertEquals("প্রভাত", PredefinedLayouts.displayName("bn_probhat"))
    }

    @Test
    fun testNationalLayoutResolution() {
        val layout = PredefinedLayouts.fromId("bn_national")
        assertEquals("bn_national", layout.id)
        assertEquals(4, layout.rows.size)

        // Top row
        val topRowKeys = layout.rows[0].keys
        assertEquals(10, topRowKeys.size)
        assertEquals("ঙ", topRowKeys[0].label)
        assertEquals("ং", topRowKeys[0].shiftedLabel)
        assertEquals(listOf("ঙ্ক"), topRowKeys[0].moreKeys)

        assertEquals("য", topRowKeys[1].label)
        assertEquals("য়", topRowKeys[1].shiftedLabel)
        assertEquals(listOf("ঞ্ঝ"), topRowKeys[1].moreKeys)

        // Home row
        val homeRowKeys = layout.rows[1].keys
        assertEquals(9, homeRowKeys.size)
        assertEquals("ৃ", homeRowKeys[0].label)
        assertEquals("র্", homeRowKeys[0].shiftedLabel)
        assertEquals(listOf("ঋ"), homeRowKeys[0].moreKeys)

        assertEquals("ত", homeRowKeys[7].label)
        assertEquals("থ", homeRowKeys[7].shiftedLabel)
        assertEquals(listOf("ৎ"), homeRowKeys[7].moreKeys)

        // Bottom row
        val bottomRowKeys = layout.rows[2].keys
        assertEquals(9, bottomRowKeys.size)
        assertEquals(KeyType.SHIFT, bottomRowKeys[0].type)
        assertEquals("্য", bottomRowKeys[1].label)
        assertEquals("্য", bottomRowKeys[1].shiftedLabel)
        assertTrue(bottomRowKeys[1].moreKeys.contains("ACTION_SELECT_ALL"))

        assertEquals("ো", bottomRowKeys[2].label)
        assertEquals("ৌ", bottomRowKeys[2].shiftedLabel)
        assertTrue(bottomRowKeys[2].moreKeys.contains("ও"))
        assertTrue(bottomRowKeys[2].moreKeys.contains("ঔ"))

        assertEquals("ে", bottomRowKeys[3].label)
        assertEquals("ৈ", bottomRowKeys[3].shiftedLabel)
        assertTrue(bottomRowKeys[3].moreKeys.contains("এ"))
        assertTrue(bottomRowKeys[3].moreKeys.contains("ঐ"))

        assertEquals("র", bottomRowKeys[4].label)
        assertEquals("ল", bottomRowKeys[4].shiftedLabel)
        assertTrue(bottomRowKeys[4].moreKeys.contains("ব়্য"))

        assertEquals("ন", bottomRowKeys[5].label)
        assertEquals("ণ", bottomRowKeys[5].shiftedLabel)
        assertEquals(listOf("ঞ্চ"), bottomRowKeys[5].moreKeys)

        assertEquals("স", bottomRowKeys[6].label)
        assertEquals("ষ", bottomRowKeys[6].shiftedLabel)
        assertEquals(listOf("ষ্ণ"), bottomRowKeys[6].moreKeys)

        assertEquals("ম", bottomRowKeys[7].label)
        assertEquals("শ", bottomRowKeys[7].shiftedLabel)
        assertEquals(listOf("ঃ", "হ্ম"), bottomRowKeys[7].moreKeys)

        assertEquals(KeyType.DELETE, bottomRowKeys[8].type)

        // Row 4
        val row4Keys = layout.rows[3].keys
        assertEquals(6, row4Keys.size)
        assertEquals(KeyType.SYMBOLS, row4Keys[0].type)
        assertEquals(KeyType.SWITCH_LANGUAGE, row4Keys[1].type)
        assertEquals(",", row4Keys[2].label)
        assertEquals(KeyType.SPACE, row4Keys[3].type)
        assertEquals("।", row4Keys[4].label)
        assertEquals(KeyType.ACTION, row4Keys[5].type)
    }
}
