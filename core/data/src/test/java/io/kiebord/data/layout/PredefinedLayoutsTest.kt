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
        assertEquals("বর্ণ ইজি", PredefinedLayouts.displayName("bn_easy"))
        assertEquals("বর্ণ ইজি", PredefinedLayouts.displayName("borno_easy"))
    }

    @Test
    fun testBengaliBornoEasyLayoutResolution() {
        val layout = PredefinedLayouts.fromId("bn_easy")
        assertEquals("bn_easy", layout.id)
        assertEquals("বর্ণ ইজি", layout.name)
        assertEquals(6, layout.rows.size)

        // Row 0: Vowels (11 keys)
        val r0 = layout.rows[0].keys
        assertEquals(11, r0.size)
        assertEquals("অ", r0[0].label)
        assertEquals("আ", r0[1].label)
        assertEquals("া", r0[1].flickUp)
        assertTrue(r0[1].moreKeys.isEmpty())
        assertEquals("ঔ", r0[10].label)
        assertEquals("ৌ", r0[10].flickUp)

        // Rows 1-3: Consonants (10 keys each)
        assertEquals(10, layout.rows[1].keys.size)
        assertEquals("ক", layout.rows[1].keys[0].label)
        assertEquals("ঞ", layout.rows[1].keys[9].label)

        assertEquals(10, layout.rows[2].keys.size)
        assertEquals("ট", layout.rows[2].keys[0].label)
        assertEquals("ন", layout.rows[2].keys[9].label)

        assertEquals(10, layout.rows[3].keys.size)
        assertEquals("প", layout.rows[3].keys[0].label)
        assertEquals("ষ", layout.rows[3].keys[9].label)

        // Row 4: স..্ + Delete (11 keys)
        val r4 = layout.rows[4].keys
        assertEquals(11, r4.size)
        assertEquals("স", r4[0].label)
        assertEquals("্", r4[9].label)
        assertEquals(listOf("্য", "্র", "়"), r4[9].moreKeys)
        assertEquals(KeyType.DELETE, r4[10].type)

        // Row 5: Function & spacebar row (7 keys including ZWNJ)
        val r5 = layout.rows[5].keys
        assertEquals(7, r5.size)
        assertEquals(KeyType.SYMBOLS, r5[0].type)
        assertEquals(KeyType.SPACE, r5[3].type)
        assertEquals(3.0f, r5[3].widthWeight, 0.01f)
        assertEquals("‌", r5[4].commitText)
        assertEquals(listOf("ZWJ"), r5[4].moreKeys)
        assertEquals(1.0f, r5[4].widthWeight, 0.01f)
        assertEquals("।", r5[5].label)
        assertEquals(KeyType.ACTION, r5[6].type)

        // Test dynamic first row: Vowel Mode
        val vowelRow = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = false)
        assertEquals(11, vowelRow.keys.size)
        assertEquals("অ", vowelRow.keys[0].label)
        assertEquals("আ", vowelRow.keys[1].label)

        // Test dynamic first row: Kar Mode with consonant "ক"
        val karRowKo = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = true, cluster = "ক")
        assertEquals(11, karRowKo.keys.size)
        assertEquals("ক", karRowKo.keys[0].label)
        assertEquals("", karRowKo.keys[0].commitText) // confirms inherent vowel, no kar appended
        assertEquals("কা", karRowKo.keys[1].label)
        assertEquals("া", karRowKo.keys[1].commitText)
        assertEquals("কি", karRowKo.keys[2].label)
        assertEquals("ি", karRowKo.keys[2].commitText)
        assertEquals("কৌ", karRowKo.keys[10].label)
        assertEquals("ৌ", karRowKo.keys[10].commitText)

        // Test dynamic first row: Kar Mode with conjunct "ক্ত"
        val karRowKto = PredefinedLayouts.getBornoEasyFirstRow(isKarMode = true, cluster = "ক্ত")
        assertEquals(11, karRowKto.keys.size)
        assertEquals("ক্ত", karRowKto.keys[0].label)
        assertEquals("", karRowKto.keys[0].commitText)
        assertEquals("ক্তা", karRowKto.keys[1].label)
        assertEquals("া", karRowKto.keys[1].commitText)
        assertEquals("ক্তি", karRowKto.keys[2].label)
        assertEquals("ি", karRowKto.keys[2].commitText)
        assertEquals("ক্তৌ", karRowKto.keys[10].label)
        assertEquals("ৌ", karRowKto.keys[10].commitText)
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
        assertEquals("ZW\nNJ", standard.rows[1].keys[9].label)
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

    @Test
    fun testSymbolsLayoutResolutionAndFallback() {
        val customSym = io.kiebord.common.model.KeyboardLayoutDef(
            id = "custom_sym",
            name = "Custom Symbols",
            language = "sym",
            rows = listOf(
                io.kiebord.common.model.KeyRow(listOf(io.kiebord.common.model.KeyDef("1"))),
                io.kiebord.common.model.KeyRow(listOf(io.kiebord.common.model.KeyDef("@"))),
                io.kiebord.common.model.KeyRow(listOf(io.kiebord.common.model.KeyDef("*"))),
                io.kiebord.common.model.KeyRow(listOf(io.kiebord.common.model.KeyDef("ABC"))),
                io.kiebord.common.model.KeyRow(listOf(io.kiebord.common.model.KeyDef("extra")))
            )
        )
        val customWithSym = io.kiebord.common.model.KeyboardLayoutDef(
            id = "custom_with_sym",
            name = "Custom With Symbols",
            language = "custom",
            symbolsLayout = customSym,
            rows = emptyList()
        )
        assertEquals(customSym, customWithSym.effectiveSymbolsLayout())
        assertEquals(5, customWithSym.effectiveSymbolsLayout()!!.rows.size)
        assertTrue(customSym.isSymbolsLayout())

        // Default / predefined layouts without symbolsLayout safely return null and fall back
        val borno = PredefinedLayouts.BengaliBorno
        org.junit.Assert.assertNull(borno.effectiveSymbolsLayout())
        org.junit.Assert.assertFalse(borno.isSymbolsLayout())
        val fallback = borno.effectiveSymbolsLayout() ?: PredefinedLayouts.Symbols
        assertEquals("symbols_1", fallback.id)
    }

    @Test
    fun testJsonSerializationWithCustomSymbolsLayout() {
        val jsonString = """
            {
              "id": "test_layout",
              "name": "Test Layout",
              "language": "xx",
              "rows": [
                { "keys": [{ "label": "a" }] }
              ],
              "symbolsLayout": {
                "id": "test_symbols",
                "name": "Test Symbols",
                "rows": [
                  { "keys": [{ "label": "1" }, { "label": "2" }] },
                  { "keys": [{ "label": "@" }, { "label": "#" }] },
                  { "keys": [{ "label": "!" }, { "label": "?" }] },
                  { "keys": [{ "label": "ABC" }] },
                  { "keys": [{ "label": "extra_row" }] }
                ]
              }
            }
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val parsed = json.decodeFromString<io.kiebord.common.model.KeyboardLayoutDef>(jsonString)

        assertEquals("test_layout", parsed.id)
        assertNotNull(parsed.effectiveSymbolsLayout())
        val sym = parsed.effectiveSymbolsLayout()!!
        assertEquals("test_symbols", sym.id)
        assertEquals(5, sym.rows.size)
        assertEquals("1", sym.rows[0].keys[0].label)
        assertEquals("extra_row", sym.rows[4].keys[0].label)
        assertTrue(sym.isSymbolsLayout())
    }

    @Test
    fun testDuplicateRowDetection() {
        val extraRowNums = io.kiebord.common.model.LayoutExtraRow(
            id = "extra_nums",
            title = "Numbers",
            keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { io.kiebord.common.model.KeyDef(label = it) }
        )
        val symRowNums = io.kiebord.common.model.KeyRow(
            keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { io.kiebord.common.model.KeyDef(label = it) }
        )
        val symRowSymbols = io.kiebord.common.model.KeyRow(
            keys = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/").map { io.kiebord.common.model.KeyDef(label = it) }
        )

        // Numbers duplicate spotted
        assertTrue(io.kiebord.common.model.KeyboardLayoutDef.isDuplicateRow(extraRowNums, listOf(symRowNums, symRowSymbols)))

        // Non-duplicate Harakat row
        val harakatRow = io.kiebord.common.model.LayoutExtraRow(
            id = "ar_harakat",
            title = "Harakat",
            keys = listOf("\u064e", "\u064b", "\u064f", "\u064c", "\u0650", "\u064d", "\u0652", "\u0651", "\u0640", "\u061f").map { io.kiebord.common.model.KeyDef(label = it) }
        )
        org.junit.Assert.assertFalse(io.kiebord.common.model.KeyboardLayoutDef.isDuplicateRow(harakatRow, listOf(symRowNums, symRowSymbols)))

        // Row ID matching existing extra row
        val existingExtra = listOf(
            io.kiebord.common.model.LayoutExtraRow(
                id = "ar_harakat",
                title = "Existing Harakat",
                keys = emptyList()
            )
        )
        assertTrue(io.kiebord.common.model.KeyboardLayoutDef.isDuplicateRow(harakatRow, listOf(symRowSymbols), existingExtra))
    }

    @Test
    fun testResolveExtraRowsForSymbolsCarriesOverWhenRowCountEquals() {
        val harakatRow = io.kiebord.common.model.LayoutExtraRow(
            id = "ar_harakat",
            title = "Harakat",
            defaultEnabled = true,
            keys = listOf("\u064e", "\u064b", "\u064f", "\u064c", "\u0650", "\u064d", "\u0652", "\u0651", "\u0640", "\u061f").map { io.kiebord.common.model.KeyDef(label = it) }
        )

        val alphaLayout = io.kiebord.common.model.KeyboardLayoutDef(
            id = "ar_arabic",
            name = "Arabic",
            extraRows = listOf(harakatRow),
            rows = List(4) { io.kiebord.common.model.KeyRow(keys = listOf(io.kiebord.common.model.KeyDef("a"))) }
        )

        val symbolsLayout = io.kiebord.common.model.KeyboardLayoutDef(
            id = "symbols",
            name = "Symbols",
            rows = List(4) { io.kiebord.common.model.KeyRow(keys = listOf(io.kiebord.common.model.KeyDef("1"))) }
        )

        // Alpha has 4 + 1 = 5 rows, symbols has 4 rows -> extra row moved to symbols
        val extraForSymbols = symbolsLayout.resolveExtraRowsForSymbols(alphaLayout)
        assertEquals(1, extraForSymbols.size)
        assertEquals(10, extraForSymbols[0].keys.size)
        assertEquals("\u064e", extraForSymbols[0].keys[0].label)

        // If Harakat is disabled via override, no extra row moved
        val overrides = mapOf("ar_arabic_ar_harakat" to false)
        val extraWhenDisabled = symbolsLayout.resolveExtraRowsForSymbols(alphaLayout, overrides)
        assertEquals(0, extraWhenDisabled.size)
    }

    @Test
    fun testResolveExtraRowsForSymbolsUsesCorrespondingRowWhenFound() {
        val alphaHarakat = io.kiebord.common.model.LayoutExtraRow(
            id = "harakat",
            title = "Harakat",
            defaultEnabled = true,
            keys = listOf(io.kiebord.common.model.KeyDef("ـَ"))
        )
        val alphaLayout = io.kiebord.common.model.KeyboardLayoutDef(
            id = "ar_arabic",
            name = "Arabic",
            extraRows = listOf(alphaHarakat),
            rows = List(4) { io.kiebord.common.model.KeyRow(keys = listOf(io.kiebord.common.model.KeyDef("a"))) }
        )

        val symbolQuranic = io.kiebord.common.model.LayoutExtraRow(
            id = "harakat",
            title = "Quranic",
            defaultEnabled = true,
            keys = listOf(io.kiebord.common.model.KeyDef("ۖ"))
        )
        val symbolsLayout = io.kiebord.common.model.KeyboardLayoutDef(
            id = "ar_symbols_1",
            name = "Symbols",
            extraRows = listOf(symbolQuranic),
            rows = List(4) { io.kiebord.common.model.KeyRow(keys = listOf(io.kiebord.common.model.KeyDef("1"))) }
        )

        // Found matching id -> uses corresponding row from symbols layout
        val extra = symbolsLayout.resolveExtraRowsForSymbols(alphaLayout)
        assertEquals(1, extra.size)
        assertEquals("ۖ", extra[0].keys[0].label)
    }

    @Test
    fun testResolveExtraRowsForSymbolsMovesExtraRowWhenNotFound() {
        val alphaExtra = io.kiebord.common.model.LayoutExtraRow(
            id = "custom_feature",
            title = "Custom",
            defaultEnabled = true,
            keys = listOf(io.kiebord.common.model.KeyDef("★"))
        )
        val alphaLayout = io.kiebord.common.model.KeyboardLayoutDef(
            id = "custom_lang",
            name = "Custom",
            extraRows = listOf(alphaExtra),
            rows = List(4) { io.kiebord.common.model.KeyRow(keys = listOf(io.kiebord.common.model.KeyDef("a"))) }
        )

        val symbolsLayout = io.kiebord.common.model.KeyboardLayoutDef(
            id = "custom_symbols",
            name = "Symbols",
            extraRows = emptyList(),
            rows = List(4) { io.kiebord.common.model.KeyRow(keys = listOf(io.kiebord.common.model.KeyDef("1"))) }
        )

        // Not found in symbol page -> moves extra row to symbol page
        val extra = symbolsLayout.resolveExtraRowsForSymbols(alphaLayout)
        assertEquals(1, extra.size)
        assertEquals("★", extra[0].keys[0].label)
    }

    @Test
    fun testRealArabicJsonRowCountMatching() {
        val jsonFile = java.io.File("../../feature/ime/src/main/assets/layouts/ar_arabic.json")
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val arabic = json.decodeFromString<io.kiebord.common.model.KeyboardLayoutDef>(jsonFile.readText())
        val sym = arabic.effectiveSymbolsLayout()!!
        val moreSym = arabic.effectiveMoreSymbolsLayout()!!

        // Base layout has 4 rows, 1 extra row (Harakat) and 1 numberRow
        assertEquals(4, arabic.rows.size)
        assertEquals(1, arabic.extraRows.size)
        assertEquals("harakat", arabic.extraRows[0].id)
        org.junit.Assert.assertFalse(arabic.extraRows[0].defaultEnabled)
        assertNotNull(arabic.numberRow)

        // Symbols (?123) and More Symbols (=\<) have 4 core rows and matching extra row for Harakat (Quranic marks)
        assertEquals(4, sym.rows.size)
        assertEquals(1, sym.extraRows.size)
        assertEquals("harakat", sym.extraRows[0].id)
        assertEquals("ۖ", sym.extraRows[0].keys[0].label)
        assertEquals("١", sym.rows[0].keys[0].label)

        assertEquals(4, moreSym.rows.size)
        assertEquals(1, moreSym.extraRows.size)
        assertEquals("harakat", moreSym.extraRows[0].id)
        assertEquals("ۖ", moreSym.extraRows[0].keys[0].label)
        assertEquals("~", moreSym.rows[0].keys[0].label)
    }

    @Test
    fun testArabicLayoutRowCountsInAllPermutations() {
        val jsonFile = java.io.File("../../feature/ime/src/main/assets/layouts/ar_arabic.json")
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val arabic = json.decodeFromString<io.kiebord.common.model.KeyboardLayoutDef>(jsonFile.readText())
        val sym = arabic.effectiveSymbolsLayout()!!
        val moreSym = arabic.effectiveMoreSymbolsLayout()!!

        // Helper simulating KeyboardRootView row construction
        fun buildRows(
            display: io.kiebord.common.model.KeyboardLayoutDef,
            baseAlpha: io.kiebord.common.model.KeyboardLayoutDef,
            showNumberRow: Boolean,
            overrides: Map<String, Boolean>
        ): List<io.kiebord.common.model.KeyRow> {
            val isSymbol = display.isSymbolsLayout()
            val numRow: List<io.kiebord.common.model.KeyRow> = if (showNumberRow) {
                listOf(display.numberRow ?: baseAlpha.numberRow ?: PredefinedLayouts.createNumberRow(baseAlpha.language))
            } else {
                emptyList()
            }
            val extraRows: List<io.kiebord.common.model.KeyRow> = if (isSymbol) {
                display.resolveExtraRowsForSymbols(baseAlpha, overrides)
            } else {
                display.extraRows.filter { extra ->
                    overrides["${display.id}_${extra.id}"] ?: extra.defaultEnabled
                }.map { io.kiebord.common.model.KeyRow(it.keys) }
            }
            val coreRows: List<io.kiebord.common.model.KeyRow> = if (isSymbol) {
                display.resolveCoreRowsForSymbols(showNumberRow)
            } else {
                display.rows
            }
            return numRow + extraRows + coreRows
        }

        // Permutation 1: Both number row and harakat enabled
        val p1Overrides = mapOf("ar_arabic_harakat" to true)
        val p1Alpha = buildRows(arabic, arabic, showNumberRow = true, p1Overrides)
        val p1Sym = buildRows(sym, arabic, showNumberRow = true, p1Overrides)
        val p1More = buildRows(moreSym, arabic, showNumberRow = true, p1Overrides)
        assertEquals(6, p1Alpha.size)
        assertEquals(6, p1Sym.size)
        assertEquals(6, p1More.size)
        assertEquals(p1Alpha.size, p1Sym.size)
        assertEquals(p1Alpha.size, p1More.size)

        // Permutation 2: Number row enabled, harakat disabled
        val p2Overrides = mapOf("ar_arabic_harakat" to false)
        val p2Alpha = buildRows(arabic, arabic, showNumberRow = true, p2Overrides)
        val p2Sym = buildRows(sym, arabic, showNumberRow = true, p2Overrides)
        val p2More = buildRows(moreSym, arabic, showNumberRow = true, p2Overrides)
        assertEquals(5, p2Alpha.size)
        assertEquals(5, p2Sym.size)
        assertEquals(5, p2More.size)
        assertEquals(p2Alpha.size, p2Sym.size)
        assertEquals(p2Alpha.size, p2More.size)

        // Permutation 3: Number row disabled, harakat enabled
        val p3Overrides = mapOf("ar_arabic_harakat" to true)
        val p3Alpha = buildRows(arabic, arabic, showNumberRow = false, p3Overrides)
        val p3Sym = buildRows(sym, arabic, showNumberRow = false, p3Overrides)
        val p3More = buildRows(moreSym, arabic, showNumberRow = false, p3Overrides)
        assertEquals(5, p3Alpha.size)
        assertEquals(5, p3Sym.size)
        assertEquals(5, p3More.size)
        assertEquals(p3Alpha.size, p3Sym.size)
        assertEquals(p3Alpha.size, p3More.size)

        // Permutation 4: Both number row and harakat disabled
        val p4Overrides = mapOf("ar_arabic_harakat" to false)
        val p4Alpha = buildRows(arabic, arabic, showNumberRow = false, p4Overrides)
        val p4Sym = buildRows(sym, arabic, showNumberRow = false, p4Overrides)
        val p4More = buildRows(moreSym, arabic, showNumberRow = false, p4Overrides)
        assertEquals(4, p4Alpha.size)
        assertEquals(4, p4Sym.size)
        assertEquals(4, p4More.size)
        assertEquals(p4Alpha.size, p4Sym.size)
        assertEquals(p4Alpha.size, p4More.size)
    }

    @Test
    fun testAllPredefinedLayoutsRowCountsMatchAcrossAllStates() {
        fun buildRows(
            display: io.kiebord.common.model.KeyboardLayoutDef,
            baseAlpha: io.kiebord.common.model.KeyboardLayoutDef,
            showNumberRow: Boolean
        ): List<io.kiebord.common.model.KeyRow> {
            val isSymbol = display.isSymbolsLayout()
            val numRow: List<io.kiebord.common.model.KeyRow> = if (showNumberRow) {
                listOf(display.numberRow ?: baseAlpha.numberRow ?: PredefinedLayouts.createNumberRow(baseAlpha.language))
            } else {
                emptyList()
            }
            val extraRows: List<io.kiebord.common.model.KeyRow> = if (isSymbol) {
                display.resolveExtraRowsForSymbols(baseAlpha, emptyMap())
            } else {
                display.extraRows.filter { it.defaultEnabled }.map { io.kiebord.common.model.KeyRow(it.keys) }
            }
            val coreRows: List<io.kiebord.common.model.KeyRow> = if (isSymbol) {
                display.resolveCoreRowsForSymbols(showNumberRow)
            } else {
                display.rows
            }
            return numRow + extraRows + coreRows
        }

        for (layout in PredefinedLayouts.allPredefined) {
            val is6Row = layout.rows.size >= 6
            val expectedWithNum = if (is6Row) 7 else 5
            val expectedNoNum = if (is6Row) 6 else 4

            val sym = layout.effectiveSymbolsLayout() ?: PredefinedLayouts.Symbols
            val moreSym = layout.effectiveMoreSymbolsLayout() ?: PredefinedLayouts.SymbolsMore

            // 1. With number row enabled: ABC, ?123, and =< must match
            val rowsAlphaWithNum = buildRows(layout, layout, showNumberRow = true)
            val rowsSymWithNum = buildRows(sym, layout, showNumberRow = true)
            val rowsMoreWithNum = buildRows(moreSym, layout, showNumberRow = true)

            assertEquals("${layout.id} ABC row count with number row must be $expectedWithNum", expectedWithNum, rowsAlphaWithNum.size)
            assertEquals("${layout.id} Symbols (?123) row count with number row must be $expectedWithNum", expectedWithNum, rowsSymWithNum.size)
            assertEquals("${layout.id} More Symbols (=<) row count with number row must be $expectedWithNum", expectedWithNum, rowsMoreWithNum.size)
            assertEquals("${layout.id} row counts must match across ABC and ?123", rowsAlphaWithNum.size, rowsSymWithNum.size)
            assertEquals("${layout.id} row counts must match across ABC and =<", rowsAlphaWithNum.size, rowsMoreWithNum.size)

            // 2. With number row disabled: ABC, ?123, and =< must match
            val rowsAlphaNoNum = buildRows(layout, layout, showNumberRow = false)
            val rowsSymNoNum = buildRows(sym, layout, showNumberRow = false)
            val rowsMoreNoNum = buildRows(moreSym, layout, showNumberRow = false)

            assertEquals("${layout.id} ABC row count without number row must be $expectedNoNum", expectedNoNum, rowsAlphaNoNum.size)
            assertEquals("${layout.id} Symbols (?123) row count without number row must be $expectedNoNum", expectedNoNum, rowsSymNoNum.size)
            assertEquals("${layout.id} More Symbols (=<) row count without number row must be $expectedNoNum", expectedNoNum, rowsMoreNoNum.size)
            assertEquals("${layout.id} row counts must match across ABC and ?123", rowsAlphaNoNum.size, rowsSymNoNum.size)
            assertEquals("${layout.id} row counts must match across ABC and =<", rowsAlphaNoNum.size, rowsMoreNoNum.size)
        }
    }

    @Test
    fun testResolveCoreRowsForSymbolsAlwaysReturnsFourCoreRows() {
        // 4-row layouts
        assertEquals(4, PredefinedLayouts.Symbols.resolveCoreRowsForSymbols(showNumberRow = true).size)
        assertEquals(4, PredefinedLayouts.Symbols.resolveCoreRowsForSymbols(showNumberRow = false).size)
        assertEquals(4, PredefinedLayouts.SymbolsMore.resolveCoreRowsForSymbols(showNumberRow = true).size)
        assertEquals(4, PredefinedLayouts.SymbolsMore.resolveCoreRowsForSymbols(showNumberRow = false).size)

        // 5-row layouts (such as SymbolsWithNumberRow and SymbolsMoreWithNumberRow)
        assertEquals(4, PredefinedLayouts.SymbolsWithNumberRow.resolveCoreRowsForSymbols(showNumberRow = true).size)
        assertEquals(4, PredefinedLayouts.SymbolsWithNumberRow.resolveCoreRowsForSymbols(showNumberRow = false).size)
        assertEquals(4, PredefinedLayouts.SymbolsMoreWithNumberRow.resolveCoreRowsForSymbols(showNumberRow = true).size)
        assertEquals(4, PredefinedLayouts.SymbolsMoreWithNumberRow.resolveCoreRowsForSymbols(showNumberRow = false).size)
    }
}
