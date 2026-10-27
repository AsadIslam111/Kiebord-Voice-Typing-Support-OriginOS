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
        assertEquals("অভ্র", PredefinedLayouts.displayName("bn_avro"))
        assertEquals("প্রভাত", PredefinedLayouts.displayName("bn_probhat"))
        assertEquals("Arabic", PredefinedLayouts.displayName("ar_arabic"))
    }

    @Test
    fun testAvroLayoutResolution() {
        val layout = PredefinedLayouts.fromId("bn_avro")
        assertEquals("bn_avro", layout.id)
        assertEquals("অভ্র", layout.name)
        assertEquals(4, layout.rows.size)

        // Key 'a' in home row has no ঁ
        val keyA = layout.rows[1].keys[0]
        assertEquals("a", keyA.label)
        assertEquals("@", keyA.flickUp)
        assertEquals(listOf("@"), keyA.moreKeys)

        // Key 'l' in home row has no ঃ
        val keyL = layout.rows[1].keys[8]
        assertEquals("l", keyL.label)
        assertEquals(")", keyL.flickUp)
        assertEquals(listOf(")"), keyL.moreKeys)
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
        assertEquals(emptyList<String>(), topRowKeys[0].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(listOf("ঙ্ক"), topRowKeys[0].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))
        assertEquals("ঙ্ক", topRowKeys[0].flickUp(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("য", topRowKeys[1].label)
        assertEquals("য়", topRowKeys[1].shiftedLabel)
        assertEquals(emptyList<String>(), topRowKeys[1].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(listOf("ঞ্ঝ"), topRowKeys[1].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        // Home row
        val homeRowKeys = layout.rows[1].keys
        assertEquals(9, homeRowKeys.size)
        assertEquals("ৃ", homeRowKeys[0].label)
        assertEquals("র্", homeRowKeys[0].shiftedLabel)
        assertEquals(listOf("ঋ"), homeRowKeys[0].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(emptyList<String>(), homeRowKeys[0].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("ত", homeRowKeys[7].label)
        assertEquals("থ", homeRowKeys[7].shiftedLabel)
        assertEquals(listOf("ৎ"), homeRowKeys[7].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(emptyList<String>(), homeRowKeys[7].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        // Bottom row
        val bottomRowKeys = layout.rows[2].keys
        assertEquals(9, bottomRowKeys.size)
        assertEquals(KeyType.SHIFT, bottomRowKeys[0].type)
        assertEquals("্য", bottomRowKeys[1].label)
        assertEquals("্য", bottomRowKeys[1].shiftedLabel)

        assertEquals("ো", bottomRowKeys[2].label)
        assertEquals("ৌ", bottomRowKeys[2].shiftedLabel)
        assertEquals(listOf("ও"), bottomRowKeys[2].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(listOf("ঔ"), bottomRowKeys[2].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("ে", bottomRowKeys[3].label)
        assertEquals("ৈ", bottomRowKeys[3].shiftedLabel)
        assertEquals(listOf("এ"), bottomRowKeys[3].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(listOf("ঐ"), bottomRowKeys[3].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("র", bottomRowKeys[4].label)
        assertEquals("ল", bottomRowKeys[4].shiftedLabel)
        assertEquals(listOf("ব়্য"), bottomRowKeys[4].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("ন", bottomRowKeys[5].label)
        assertEquals("ণ", bottomRowKeys[5].shiftedLabel)
        assertEquals(listOf("ঞ্চ"), bottomRowKeys[5].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("স", bottomRowKeys[6].label)
        assertEquals("ষ", bottomRowKeys[6].shiftedLabel)
        assertEquals(listOf("ষ্ণ"), bottomRowKeys[6].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals("ম", bottomRowKeys[7].label)
        assertEquals("শ", bottomRowKeys[7].shiftedLabel)
        assertEquals(listOf("ঃ"), bottomRowKeys[7].moreKeys(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals(listOf("হ্ম"), bottomRowKeys[7].moreKeys(io.kiebord.common.model.ShiftState.ONE_SHOT))

        assertEquals(KeyType.DELETE, bottomRowKeys[8].type)

        // Row 4
        val row4Keys = layout.rows[3].keys
        assertEquals(6, row4Keys.size)
        assertEquals(KeyType.SYMBOLS, row4Keys[0].type)
        assertEquals(",", row4Keys[1].label)
        assertTrue(row4Keys[1].moreKeys.contains("ACTION_SETTINGS"))
        assertTrue(row4Keys[1].moreKeys.contains("ACTION_ONE_HANDED"))
        assertEquals(KeyType.SWITCH_LANGUAGE, row4Keys[2].type)
        assertEquals(KeyType.SPACE, row4Keys[3].type)
        assertEquals("।", row4Keys[4].label)
        assertEquals(KeyType.ACTION, row4Keys[5].type)

        // Verify national layout key hints match first moreKeys and dummy secondary hints are removed for each shift state
        for (state in listOf(io.kiebord.common.model.ShiftState.UNSHIFTED, io.kiebord.common.model.ShiftState.ONE_SHOT)) {
            for (row in layout.rows) {
                for (key in row.keys) {
                    val mk = key.moreKeys(state)
                    val hint = key.flickUp(state)
                    if (mk.isEmpty() || mk.first().startsWith("ACTION_") || key.label == "।") {
                        org.junit.Assert.assertNull("Key ${key.label} in state $state without character moreKeys should not have flickUp", hint)
                    } else {
                        assertEquals("Key ${key.label} in state $state flickUp must match moreKeys.first()", mk.first(), hint)
                    }
                }
            }
        }
    }

    @Test
    fun testSymbolsLayoutsWithAndWithoutNumberRow() {
        val standard = PredefinedLayouts.getSymbolsLayout("symbols_1", hasNumberRow = false)
        assertEquals("symbols_1", standard.id)
        assertEquals(4, standard.rows.size)
        // Row 0 is numbers
        assertEquals("1", standard.rows[0].keys[0].label)
        assertEquals("0", standard.rows[0].keys[9].label)
        // Row 1 is @, #, $, %, etc.
        assertEquals("@", standard.rows[1].keys[0].label)
        assertEquals("ZWNJ", standard.rows[1].keys[9].label)
        assertEquals("\u200C", standard.rows[1].keys[9].textToCommit(io.kiebord.common.model.ShiftState.UNSHIFTED))
        // Row 2 is =<, *, ", etc.
        assertEquals("=\\<", standard.rows[2].keys[0].label)
        // Row 3 is ABC, comma, underscore, space, slash, period, enter
        assertEquals("ABC", standard.rows[3].keys[0].label)
        assertEquals(",", standard.rows[3].keys[1].label)
        assertEquals("_", standard.rows[3].keys[2].label)
        assertEquals(KeyType.SPACE, standard.rows[3].keys[3].type)
        assertEquals("/", standard.rows[3].keys[4].label)
        assertEquals(".", standard.rows[3].keys[5].label)

        // With extra row (number row enabled)
        val withExtra = PredefinedLayouts.getSymbolsLayout("symbols_1", hasNumberRow = true)
        assertEquals(5, withExtra.rows.size)
        // Row 0 is numbers
        assertEquals("1", withExtra.rows[0].keys[0].label)
        // Row 1 is the EXTRA ROW from Borno XML reference: ~ < > _ ` º \ = { }
        val extraRow = withExtra.rows[1].keys
        assertEquals(10, extraRow.size)
        assertEquals("~", extraRow[0].label)
        assertEquals("<", extraRow[1].label)
        assertEquals(">", extraRow[2].label)
        assertEquals("_", extraRow[3].label)
        assertEquals("`", extraRow[4].label)
        assertEquals("º", extraRow[5].label)
        assertEquals("\\", extraRow[6].label)
        assertEquals("=", extraRow[7].label)
        assertEquals("{", extraRow[8].label)
        assertEquals("}", extraRow[9].label)
        // Row 2 is @, #, $...
        assertEquals("@", withExtra.rows[2].keys[0].label)
        // Row 3 is =<...
        assertEquals("=\\<", withExtra.rows[3].keys[0].label)
        // Row 4 is ABC...
        assertEquals("ABC", withExtra.rows[4].keys[0].label)
    }

    @Test
    fun testSymbolsMoreLayoutsWithAndWithoutNumberRow() {
        val standard = PredefinedLayouts.getSymbolsLayout("symbols_2", hasNumberRow = false)
        assertEquals("symbols_2", standard.id)
        assertEquals(4, standard.rows.size)
        assertEquals("~", standard.rows[0].keys[0].label)
        assertEquals("∆", standard.rows[0].keys[9].label)
        assertEquals("£", standard.rows[1].keys[0].label)
        assertEquals("?123", standard.rows[2].keys[0].label)
        assertEquals("ABC", standard.rows[3].keys[0].label)

        // With extra shift row (number row enabled)
        val withExtra = PredefinedLayouts.getSymbolsLayout("symbols_2", hasNumberRow = true)
        assertEquals(5, withExtra.rows.size)
        assertEquals("~", withExtra.rows[0].keys[0].label)
        // Row 1 is the EXTRA SHIFT ROW from Borno XML reference: ≠ ≤ ≥ ∫ Σ ≈ ± ∓ ½ ∞
        val extraShiftRow = withExtra.rows[1].keys
        assertEquals(10, extraShiftRow.size)
        assertEquals("≠", extraShiftRow[0].label)
        assertEquals("≤", extraShiftRow[1].label)
        assertEquals("≥", extraShiftRow[2].label)
        assertEquals("∫", extraShiftRow[3].label)
        assertEquals("Σ", extraShiftRow[4].label)
        assertEquals("≈", extraShiftRow[5].label)
        assertEquals("±", extraShiftRow[6].label)
        assertEquals("∓", extraShiftRow[7].label)
        assertEquals("½", extraShiftRow[8].label)
        assertEquals("∞", extraShiftRow[9].label)
        // Row 2 is £, ¢...
        assertEquals("£", withExtra.rows[2].keys[0].label)
        // Row 3 is ?123...
        assertEquals("?123", withExtra.rows[3].keys[0].label)
        // Row 4 is ABC...
        assertEquals("ABC", withExtra.rows[4].keys[0].label)
    }

    @Test
    fun testNumberRowsHaveNoSecondaryFlickUp() {
        val bnRow = PredefinedLayouts.createNumberRow("bn")
        assertEquals(10, bnRow.keys.size)
        for (k in bnRow.keys) {
            org.junit.Assert.assertNull("Bengali number row key ${k.label} should not have flickUp", k.flickUp)
        }

        val enRow = PredefinedLayouts.createNumberRow("en")
        assertEquals(10, enRow.keys.size)
        for (k in enRow.keys) {
            org.junit.Assert.assertNull("English number row key ${k.label} should not have flickUp", k.flickUp)
        }
    }

    @Test
    fun testCustomLayoutResolverAndDisplayName() {
        val originalResolver = PredefinedLayouts.customLayoutResolver
        try {
            PredefinedLayouts.customLayoutResolver = { id ->
                if (id == "custom_layout_1") "Custom Layout" else null
            }
            assertEquals("Custom Layout", PredefinedLayouts.displayName("custom_layout_1"))
            assertEquals("English", PredefinedLayouts.displayName("en_qwerty"))
        } finally {
            PredefinedLayouts.customLayoutResolver = originalResolver
        }
    }

    @Test
    fun testNumberRowShiftedSymbols() {
        val enRow = PredefinedLayouts.NumberRowEn
        assertEquals("1", enRow.keys[0].textToCommit(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals("!", enRow.keys[0].textToCommit(io.kiebord.common.model.ShiftState.ONE_SHOT))
        assertEquals("!", enRow.keys[0].textToCommit(io.kiebord.common.model.ShiftState.CAPS_LOCK))

        val bnRow = PredefinedLayouts.NumberRowBn
        assertEquals("৪", bnRow.keys[3].textToCommit(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals("৳", bnRow.keys[3].textToCommit(io.kiebord.common.model.ShiftState.ONE_SHOT))

        val arRow = PredefinedLayouts.NumberRowAr
        assertEquals("٥", arRow.keys[4].textToCommit(io.kiebord.common.model.ShiftState.UNSHIFTED))
        assertEquals("٪", arRow.keys[4].textToCommit(io.kiebord.common.model.ShiftState.ONE_SHOT))
    }

    @Test
    fun testKeyboardLayoutDefEffectiveDisplayName() {
        val withDisplay = io.kiebord.common.model.KeyboardLayoutDef(
            id = "test_custom",
            name = "Test Custom Full",
            displayName = "Custom Short",
            language = "en",
            rows = emptyList()
        )
        assertEquals("Custom Short", withDisplay.effectiveDisplayName())

        val withoutDisplay = io.kiebord.common.model.KeyboardLayoutDef(
            id = "test_custom_2",
            name = "Test Custom 2",
            language = "en",
            rows = emptyList()
        )
        assertEquals("Test Custom 2", withoutDisplay.effectiveDisplayName())
    }
}
