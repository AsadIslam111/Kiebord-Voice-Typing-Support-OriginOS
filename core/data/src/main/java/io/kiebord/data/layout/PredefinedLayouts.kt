package io.kiebord.data.layout

import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyRow
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef

object PredefinedLayouts {

    val EnglishQwerty = KeyboardLayoutDef(
        id = "en_qwerty",
        name = "English (QWERTY)",
        language = "en",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("q", "Q", flickUp = "1", moreKeys = listOf("1")),
                    KeyDef("w", "W", flickUp = "2", moreKeys = listOf("2")),
                    KeyDef("e", "E", flickUp = "3", moreKeys = listOf("3", "é", "è", "ê", "ë", "ē")),
                    KeyDef("r", "R", flickUp = "4", moreKeys = listOf("4")),
                    KeyDef("t", "T", flickUp = "5", moreKeys = listOf("5")),
                    KeyDef("y", "Y", flickUp = "6", moreKeys = listOf("6")),
                    KeyDef("u", "U", flickUp = "7", moreKeys = listOf("7", "ú", "ù", "û", "ü", "ū")),
                    KeyDef("i", "I", flickUp = "8", moreKeys = listOf("8", "í", "ì", "î", "ï", "ī")),
                    KeyDef("o", "O", flickUp = "9", moreKeys = listOf("9", "ó", "ò", "ô", "ö", "œ", "ø", "ō", "õ")),
                    KeyDef("p", "P", flickUp = "0", moreKeys = listOf("0"))
                )
            ),
            KeyRow(
                keys = listOf(
                    KeyDef("a", "A", flickUp = "@", moreKeys = listOf("@", "à", "á", "â", "ä", "æ", "ã", "å", "ā")),
                    KeyDef("s", "S", flickUp = "#", moreKeys = listOf("#", "ß")),
                    KeyDef("d", "D", flickUp = "$", moreKeys = listOf("$")),
                    KeyDef("f", "F", flickUp = "%", moreKeys = listOf("%")),
                    KeyDef("g", "G", flickUp = "&", moreKeys = listOf("&")),
                    KeyDef("h", "H", flickUp = "-", moreKeys = listOf("-", "_")),
                    KeyDef("j", "J", flickUp = "+", moreKeys = listOf("+")),
                    KeyDef("k", "K", flickUp = "(", moreKeys = listOf("(")),
                    KeyDef("l", "L", flickUp = ")", moreKeys = listOf(")"))
                ),
                startPaddingWeight = 0.5f,
                endPaddingWeight = 0.5f
            ),
            KeyRow(
                listOf(
                    KeyDef(type = KeyType.SHIFT, icon = "shift", widthWeight = 1.5f),
                    KeyDef("z", "Z", flickUp = "*", moreKeys = listOf("ACTION_SELECT_ALL", "*")),
                    KeyDef("x", "X", flickUp = "\"", moreKeys = listOf("ACTION_CUT", "\"")),
                    KeyDef("c", "C", flickUp = "'", moreKeys = listOf("ACTION_COPY", "'", "ç")),
                    KeyDef("v", "V", flickUp = ":", moreKeys = listOf("ACTION_PASTE", ":")),
                    KeyDef("b", "B", flickUp = ";", moreKeys = listOf(";")),
                    KeyDef("n", "N", flickUp = "!", moreKeys = listOf("!", "ñ")),
                    KeyDef("m", "M", flickUp = "?", moreKeys = listOf("?")),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.5f, slidingInput = true),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", flickUp = null, moreKeys = listOf("<", ">", "[", "]", "{", "}"), widthWeight = 1.0f),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef(".", flickUp = ">", widthWeight = 1.0f, moreKeysColumns = 8, moreKeys = listOf(",", "?", "!", "#", ")", "(", "/", ";", "'", "@", ":", "-", "\"", "+", "%", "&")),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val BengaliBorno = KeyboardLayoutDef(
        id = "bn_borno",
        name = "Bengali (Borno Phonetic)",
        language = "bn",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("q", "Q", flickUp = "1", moreKeys = listOf("1")),
                    KeyDef("w", "W", flickUp = "2", moreKeys = listOf("2")),
                    KeyDef("e", "E", flickUp = "3", moreKeys = listOf("3")),
                    KeyDef("r", "R", flickUp = "4", moreKeys = listOf("4")),
                    KeyDef("t", "T", flickUp = "5", moreKeys = listOf("ৎ", "5")),
                    KeyDef("y", "Y", flickUp = "6", moreKeys = listOf("`", "6")),
                    KeyDef("u", "U", flickUp = "7", moreKeys = listOf("7")),
                    KeyDef("i", "I", flickUp = "8", moreKeys = listOf("8")),
                    KeyDef("o", "O", flickUp = "9", moreKeys = listOf("9")),
                    KeyDef("p", "P", flickUp = "0", moreKeys = listOf("0"))
                )
            ),
            KeyRow(
                keys = listOf(
                    KeyDef("a", "A", flickUp = "ঁ", moreKeys = listOf("ঁ", "@")),
                    KeyDef("s", "S", flickUp = "#", moreKeys = listOf("#")),
                    KeyDef("d", "D", flickUp = "$", moreKeys = listOf("$")),
                    KeyDef("f", "F", flickUp = "%", moreKeys = listOf("%")),
                    KeyDef("g", "G", flickUp = "&", moreKeys = listOf("্", "্\u200c", "়", "&")),
                    KeyDef("h", "H", flickUp = "-", moreKeys = listOf("-")),
                    KeyDef("j", "J", flickUp = "+", moreKeys = listOf("+")),
                    KeyDef("k", "K", flickUp = "(", moreKeys = listOf("(")),
                    KeyDef("l", "L", flickUp = ")", moreKeys = listOf(")", "ঃ"))
                ),
                startPaddingWeight = 0.5f,
                endPaddingWeight = 0.5f
            ),
            KeyRow(
                listOf(
                    KeyDef(type = KeyType.SHIFT, icon = "shift", widthWeight = 1.5f),
                    KeyDef("z", "Z", flickUp = "*", moreKeys = listOf("ACTION_SELECT_ALL", "*")),
                    KeyDef("x", "X", flickUp = "\"", moreKeys = listOf("ACTION_CUT", "\"")),
                    KeyDef("c", "C", flickUp = "'", moreKeys = listOf("ACTION_COPY", "'")),
                    KeyDef("v", "V", flickUp = ":", moreKeys = listOf("ACTION_PASTE", ":")),
                    KeyDef("b", "B", flickUp = ";", moreKeys = listOf(";")),
                    KeyDef("n", "N", flickUp = "!", moreKeys = listOf("!")),
                    KeyDef("m", "M", flickUp = "?", moreKeys = listOf("?")),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?১২৩", type = KeyType.SYMBOLS, widthWeight = 1.5f, slidingInput = true),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", flickUp = null, moreKeys = listOf("<", ">", "[", "]", "{", "}"), widthWeight = 1.0f),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef("।", flickUp = ">", widthWeight = 1.0f, moreKeysColumns = 9, moreKeys = listOf(",", ".", "?", "!", "#", ")", "(", "/", ";", "'", "@", ":", "-", "\"", "+", "%", "&")),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val BengaliProbhat = KeyboardLayoutDef(
        id = "bn_probhat",
        name = "Bengali (Probhat)",
        language = "bn",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("দ", "ধ", flickUp = "ধ", moreKeys = listOf("ধ")),
                    KeyDef("ূ", "ঊ", flickUp = "ঊ", moreKeys = listOf("ঊ")),
                    KeyDef("ী", "ঈ", flickUp = "ঈ", moreKeys = listOf("ঈ")),
                    KeyDef("র", "ড়", flickUp = "ড়", moreKeys = listOf("ড়", "র্", "্র", "র\u200d্য")),
                    KeyDef("ট", "ঠ", flickUp = "ঠ", moreKeys = listOf("ঠ", "৳")),
                    KeyDef("এ", "ঐ", flickUp = "ঐ", moreKeys = listOf("ঐ", "ঞ")),
                    KeyDef("ু", "উ", flickUp = "উ", moreKeys = listOf("উ", "ৎ")),
                    KeyDef("ি", "ই", flickUp = "ই", moreKeys = listOf("ই")),
                    KeyDef("ও", "ঔ", flickUp = "ঔ", moreKeys = listOf("ঔ")),
                    KeyDef("প", "ফ", flickUp = "ফ", moreKeys = listOf("ফ")),
                    KeyDef("ে", "ৈ", flickUp = "ৈ", moreKeys = listOf("ৈ")),
                    KeyDef("ো", "ৌ", flickUp = "ৌ", moreKeys = listOf("ৌ"))
                )
            ),
            KeyRow(
                keys = listOf(
                    KeyDef("া", "অ", flickUp = "অ", moreKeys = listOf("অ")),
                    KeyDef("স", "ষ", flickUp = "ষ", moreKeys = listOf("ষ")),
                    KeyDef("ড", "ঢ", flickUp = "ঢ", moreKeys = listOf("ঢ")),
                    KeyDef("ত", "থ", flickUp = "থ", moreKeys = listOf("থ", "ৎ")),
                    KeyDef("গ", "ঘ", flickUp = "ঘ", moreKeys = listOf("ঘ")),
                    KeyDef("হ", "ঃ", flickUp = "ঃ", moreKeys = listOf("ঃ")),
                    KeyDef("জ", "ঝ", flickUp = "ঝ", moreKeys = listOf("ঝ")),
                    KeyDef("ক", "খ", flickUp = "খ", moreKeys = listOf("খ")),
                    KeyDef("ল", "ং", flickUp = "ং", moreKeys = listOf("ং"))
                ),
                startPaddingWeight = 0.5f,
                endPaddingWeight = 0.5f
            ),
            KeyRow(
                listOf(
                    KeyDef(type = KeyType.SHIFT, icon = "shift", widthWeight = 1.25f),
                    KeyDef("য়", "য", flickUp = "য", moreKeys = listOf("য", "্য")),
                    KeyDef("শ", "ঢ়", flickUp = "ঢ়", moreKeys = listOf("ACTION_SELECT_ALL", "ঢ়")),
                    KeyDef("চ", "ছ", flickUp = "ছ", moreKeys = listOf("ACTION_CUT", "ছ")),
                    KeyDef("আ", "ঋ", flickUp = "ঋ", moreKeys = listOf("ACTION_COPY", "ঋ")),
                    KeyDef("ব", "ভ", flickUp = "ভ", moreKeys = listOf("ACTION_PASTE", "ভ")),
                    KeyDef("ন", "ণ", flickUp = "ণ", moreKeys = listOf("ণ")),
                    KeyDef("ম", "ঙ", flickUp = "ঙ", moreKeys = listOf("ঙ")),
                    KeyDef("ৃ", "।", flickUp = "।", moreKeys = listOf("।")),
                    KeyDef("্", "ঁ", flickUp = "ঁ", moreKeys = listOf("ঁ", "়")),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.25f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?১২৩", type = KeyType.SYMBOLS, widthWeight = 1.5f, slidingInput = true),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", widthWeight = 1.0f, moreKeys = listOf(";", ":", "<", ">")),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef("।", flickUp = ">", widthWeight = 1.0f, moreKeysColumns = 9, moreKeys = listOf(",", ".", "?", "!", "#", ")", "(", "/", ";", "'", "@", ":", "-", "\"", "+", "%", "&")),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val BengaliNational = KeyboardLayoutDef(
        id = "bn_national",
        name = "Bengali (Jatiyo / BSTI)",
        language = "bn",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("ঙ", "ং", flickUp = "ং", moreKeys = listOf("ঙ্ক")),
                    KeyDef("য", "য়", flickUp = "য়", moreKeys = listOf("ঞ্ঝ")),
                    KeyDef("ড", "ঢ", flickUp = "ঢ"),
                    KeyDef("প", "ফ", flickUp = "ফ"),
                    KeyDef("ট", "ঠ", flickUp = "ঠ"),
                    KeyDef("চ", "ছ", flickUp = "ছ", moreKeys = listOf("ঞ্ছ")),
                    KeyDef("জ", "ঝ", flickUp = "ঝ", moreKeys = listOf("জ্ঞ")),
                    KeyDef("হ", "ঞ", flickUp = "ঞ", moreKeys = listOf("ঞ্জ")),
                    KeyDef("গ", "ঘ", flickUp = "ঘ", moreKeys = listOf("ঙ্গ")),
                    KeyDef("ড়", "ঢ়", flickUp = "ঢ়")
                )
            ),
            KeyRow(
                keys = listOf(
                    KeyDef("ৃ", "র্", flickUp = "র্", moreKeys = listOf("ঋ")),
                    KeyDef("ু", "ূ", flickUp = "ূ", moreKeys = listOf("উ", "ঊ")),
                    KeyDef("ি", "ী", flickUp = "ী", moreKeys = listOf("ই", "ঈ")),
                    KeyDef("া", "অ", flickUp = "অ", moreKeys = listOf("আ")),
                    KeyDef("্", "ঁ", flickUp = "ঁ", moreKeys = listOf("়")),
                    KeyDef("ব", "ভ", flickUp = "ভ"),
                    KeyDef("ক", "খ", flickUp = "খ", moreKeys = listOf("ক্ষ")),
                    KeyDef("ত", "থ", flickUp = "থ", moreKeys = listOf("ৎ")),
                    KeyDef("দ", "ধ", flickUp = "ধ")
                ),
                startPaddingWeight = 0.5f,
                endPaddingWeight = 0.5f
            ),
            KeyRow(
                listOf(
                    KeyDef(type = KeyType.SHIFT, icon = "shift", widthWeight = 1.5f),
                    KeyDef("্য", "্য", flickUp = "্য", moreKeys = listOf("্র", "ACTION_SELECT_ALL")),
                    KeyDef("ো", "ৌ", flickUp = "ৌ", moreKeys = listOf("ও", "ঔ", "ACTION_CUT")),
                    KeyDef("ে", "ৈ", flickUp = "ৈ", moreKeys = listOf("এ", "ঐ", "ACTION_COPY")),
                    KeyDef("র", "ল", flickUp = "ল", moreKeys = listOf("ব়্য", "র\u200d্য", "ACTION_PASTE")),
                    KeyDef("ন", "ণ", flickUp = "ণ", moreKeys = listOf("ঞ্চ")),
                    KeyDef("স", "ষ", flickUp = "ষ", moreKeys = listOf("ষ্ণ")),
                    KeyDef("ম", "শ", flickUp = "শ", moreKeys = listOf("ঃ", "হ্ম")),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?১২৩", type = KeyType.SYMBOLS, widthWeight = 1.5f, slidingInput = true),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", widthWeight = 1.0f, moreKeys = listOf(";", ":", "<", ">")),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef("।", flickUp = ">", widthWeight = 1.0f, moreKeysColumns = 9, moreKeys = listOf(",", ".", "?", "!", "#", ")", "(", "/", ";", "'", "@", ":", "-", "\"", "+", "%", "&")),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val Symbols = KeyboardLayoutDef(
        id = "symbols_1",
        name = "Symbols",
        language = "sym",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("1", moreKeys = listOf("1", "¹", "½", "⅓", "¼", "⅛", "১")),
                    KeyDef("2", moreKeys = listOf("2", "²", "⅔", "২")),
                    KeyDef("3", moreKeys = listOf("3", "³", "¾", "⅜", "৩")),
                    KeyDef("4", moreKeys = listOf("4", "⁴", "৪")),
                    KeyDef("5", moreKeys = listOf("5", "⅝", "৫")),
                    KeyDef("6", moreKeys = listOf("6", "৬")),
                    KeyDef("7", moreKeys = listOf("7", "⅞", "৭")),
                    KeyDef("8", moreKeys = listOf("8", "৮")),
                    KeyDef("9", moreKeys = listOf("9", "৯")),
                    KeyDef("0", moreKeys = listOf("0", "ⁿ", "∅", "০"))
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("@"),
                    KeyDef("#"),
                    KeyDef("$", moreKeys = listOf("$", "৳", "₹", "€", "£", "¥", "¢")),
                    KeyDef("%", moreKeys = listOf("%", "‰")),
                    KeyDef("&"),
                    KeyDef("-", moreKeys = listOf("-", "_", "–", "—", "·", "⁻")),
                    KeyDef("+", moreKeys = listOf("+", "±", "⁺")),
                    KeyDef("(", moreKeys = listOf("(", "[", "{", "<")),
                    KeyDef(")", moreKeys = listOf(")", "]", "}", ">")),
                    KeyDef("ZWNJ", commitText = "\u200C", moreKeys = listOf("ZWJ", "/"))
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("=\\<", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef("*", moreKeys = listOf("*", "★", "†", "‡")),
                    KeyDef("\"", moreKeys = listOf("\"", "”", "“", "„", "»", "«")),
                    KeyDef("'", moreKeys = listOf("'", "’", "‘", "‚", "›", "‹")),
                    KeyDef(":", moreKeys = listOf(":", "ঃ")),
                    KeyDef(";", moreKeys = listOf(";")),
                    KeyDef("!", moreKeys = listOf("!", "¡")),
                    KeyDef("?", moreKeys = listOf("?", "¿")),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("ABC", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef(",", widthWeight = 1.0f),
                    KeyDef("_", widthWeight = 1.0f),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 3.0f),
                    KeyDef("/", widthWeight = 1.0f),
                    KeyDef(".", widthWeight = 1.0f, moreKeys = listOf(".", "…")),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val SymbolsWithNumberRow = KeyboardLayoutDef(
        id = "symbols_1",
        name = "Symbols",
        language = "sym",
        rows = listOf(
            Symbols.rows[0],
            KeyRow(
                listOf(
                    KeyDef("~"),
                    KeyDef("<", moreKeys = listOf("<", "«", "≤")),
                    KeyDef(">", moreKeys = listOf(">", "»", "≥")),
                    KeyDef("_"),
                    KeyDef("`"),
                    KeyDef("º", moreKeys = listOf("º", "°")),
                    KeyDef("\\"),
                    KeyDef("=", moreKeys = listOf("=", "≠", "≈", "∞")),
                    KeyDef("{"),
                    KeyDef("}")
                )
            ),
            Symbols.rows[1],
            Symbols.rows[2],
            Symbols.rows[3]
        )
    )

    val SymbolsMore = KeyboardLayoutDef(
        id = "symbols_2",
        name = "More Symbols",
        language = "sym",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("~"),
                    KeyDef("`"),
                    KeyDef("|"),
                    KeyDef("•", moreKeys = listOf("•", "♪", "♥", "♠", "♦", "♣")),
                    KeyDef("√"),
                    KeyDef("π", moreKeys = listOf("π", "Π")),
                    KeyDef("÷"),
                    KeyDef("×"),
                    KeyDef("¶", moreKeys = listOf("¶", "§")),
                    KeyDef("∆")
                )
            ),
            KeyRow(
                keys = listOf(
                    KeyDef("£"),
                    KeyDef("¢"),
                    KeyDef("€"),
                    KeyDef("¥"),
                    KeyDef("^", moreKeys = listOf("^", "↑", "↓", "←", "→")),
                    KeyDef("°", moreKeys = listOf("°", "′", "″")),
                    KeyDef("=", moreKeys = listOf("=", "≠", "≈", "∞")),
                    KeyDef("{"),
                    KeyDef("}")
                ),
                startPaddingWeight = 0.5f,
                endPaddingWeight = 0.5f
            ),
            KeyRow(
                listOf(
                    KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef("\\"),
                    KeyDef("©"),
                    KeyDef("®"),
                    KeyDef("™"),
                    KeyDef("℅"),
                    KeyDef("["),
                    KeyDef("]"),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("ABC", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef(",", widthWeight = 1.0f),
                    KeyDef("<", widthWeight = 1.0f, moreKeys = listOf("<", "«", "≤")),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 3.0f),
                    KeyDef(">", widthWeight = 1.0f, moreKeys = listOf(">", "»", "≥")),
                    KeyDef(".", widthWeight = 1.0f, moreKeys = listOf(".", "…")),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val SymbolsMoreWithNumberRow = KeyboardLayoutDef(
        id = "symbols_2",
        name = "More Symbols",
        language = "sym",
        rows = listOf(
            SymbolsMore.rows[0],
            KeyRow(
                listOf(
                    KeyDef("≠"),
                    KeyDef("≤"),
                    KeyDef("≥"),
                    KeyDef("∫"),
                    KeyDef("Σ"),
                    KeyDef("≈"),
                    KeyDef("±"),
                    KeyDef("∓"),
                    KeyDef("½"),
                    KeyDef("∞")
                )
            ),
            SymbolsMore.rows[1],
            SymbolsMore.rows[2],
            SymbolsMore.rows[3]
        )
    )

    fun getSymbolsLayout(id: String, hasNumberRow: Boolean): KeyboardLayoutDef {
        return when (id) {
            "symbols_2" -> if (hasNumberRow) SymbolsMoreWithNumberRow else SymbolsMore
            else -> if (hasNumberRow) SymbolsWithNumberRow else Symbols
        }
    }

    fun fromId(id: String): KeyboardLayoutDef {
        return when (id) {
            "bn_borno" -> BengaliBorno
            "bn_probhat" -> BengaliProbhat
            "bn_national" -> BengaliNational
            "symbols_1" -> Symbols
            "symbols_2" -> SymbolsMore
            else -> EnglishQwerty
        }
    }

    fun displayName(id: String): String {
        return when (id) {
            "bn_borno" -> "বর্ণ ফোনেটিক"
            "bn_probhat" -> "প্রভাত"
            "bn_national" -> "জাতীয় (BSTI)"
            else -> "English"
        }
    }
}
