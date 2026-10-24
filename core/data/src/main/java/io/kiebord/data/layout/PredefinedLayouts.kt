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
                    KeyDef("q", "Q", flickUp = "1", moreKeys = listOf("1", "!", "`", "¬")),
                    KeyDef("w", "W", flickUp = "2", moreKeys = listOf("2", "@", "₩", "§")),
                    KeyDef("e", "E", flickUp = "3", moreKeys = listOf("3", "é", "è", "ê", "ë", "€")),
                    KeyDef("r", "R", flickUp = "4", moreKeys = listOf("4", "$", "₹", "®")),
                    KeyDef("t", "T", flickUp = "5", moreKeys = listOf("5", "%", "™", "þ")),
                    KeyDef("y", "Y", flickUp = "6", moreKeys = listOf("6", "^", "¥", "ý")),
                    KeyDef("u", "U", flickUp = "7", moreKeys = listOf("7", "ú", "ù", "û", "ü")),
                    KeyDef("i", "I", flickUp = "8", moreKeys = listOf("8", "í", "ì", "î", "ï")),
                    KeyDef("o", "O", flickUp = "9", moreKeys = listOf("9", "ó", "ò", "ô", "ö")),
                    KeyDef("p", "P", flickUp = "0", moreKeys = listOf("0", "*", "π", "¶"))
                )
            ),
            KeyRow(
                keys = listOf(
                    KeyDef("a", "A", flickUp = "@", moreKeys = listOf("@", "á", "à", "â", "ä")),
                    KeyDef("s", "S", flickUp = "#", moreKeys = listOf("#", "ß", "š", "ś", "$")),
                    KeyDef("d", "D", flickUp = "$", moreKeys = listOf("&", "ð", "ď")),
                    KeyDef("f", "F", flickUp = "%", moreKeys = listOf("_", "ſ", "ƒ")),
                    KeyDef("g", "G", flickUp = "&", moreKeys = listOf("-", "ğ")),
                    KeyDef("h", "H", flickUp = "*", moreKeys = listOf("+", "ħ")),
                    KeyDef("j", "J", flickUp = "-", moreKeys = listOf("=", "ĵ")),
                    KeyDef("k", "K", flickUp = "+", moreKeys = listOf("(", "ķ")),
                    KeyDef("l", "L", flickUp = "=", moreKeys = listOf(")", "ł", "ľ"))
                ),
                startPaddingWeight = 0.5f,
                endPaddingWeight = 0.5f
            ),
            KeyRow(
                listOf(
                    KeyDef(type = KeyType.SHIFT, icon = "shift", widthWeight = 1.5f),
                    KeyDef("z", "Z", flickUp = "_", moreKeys = listOf("/", "ž", "ź")),
                    KeyDef("x", "X", flickUp = "\"", moreKeys = listOf("\\", "×")),
                    KeyDef("c", "C", flickUp = "'", moreKeys = listOf("ç", "ć", "©")),
                    KeyDef("v", "V", flickUp = ":", moreKeys = listOf("\"", "✓")),
                    KeyDef("b", "B", flickUp = ";", moreKeys = listOf("[", "•")),
                    KeyDef("n", "N", flickUp = "!", moreKeys = listOf("]", "ñ", "ń")),
                    KeyDef("m", "M", flickUp = "?", moreKeys = listOf("¿", "µ")),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.5f, slidingInput = true),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", flickUp = null, moreKeys = listOf("<", ">", "[", "]", "{", "}"), widthWeight = 1.0f),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef(".", flickUp = ">", moreKeys = listOf("?", "!", ",", "-", "/", "@", ":", "'", "\"", "_", ";"), widthWeight = 1.0f),
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
                    KeyDef("দ", "ধ", moreKeys = listOf("দ্ধ")),
                    KeyDef("ড", "ঢ", moreKeys = listOf("ড্ড")),
                    KeyDef("প", "ফ", moreKeys = listOf("প্ত")),
                    KeyDef("ট", "ঠ", moreKeys = listOf("ট্ট")),
                    KeyDef("চ", "ছ", moreKeys = listOf("চ্চ")),
                    KeyDef("ত", "থ", moreKeys = listOf("ত্ত", "ত্র")),
                    KeyDef("জ", "ঝ", moreKeys = listOf("জ্জ", "জ্ঞ")),
                    KeyDef("ক", "খ", moreKeys = listOf("ক্ক", "ক্ত", "ক্ষ")),
                    KeyDef("স", "ষ", moreKeys = listOf("স্ত", "স্থ")),
                    KeyDef("হ", "ঃ", moreKeys = listOf("হ্ন", "হ্ম"))
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("ি", "ই"),
                    KeyDef("ু", "উ"),
                    KeyDef("ী", "ঈ"),
                    KeyDef("ূ", "ঊ"),
                    KeyDef("া", "আ"),
                    KeyDef("ে", "এ"),
                    KeyDef("ো", "ও"),
                    KeyDef("য়", "য়"),
                    KeyDef("র", "র্"),
                    KeyDef("ল", "৲", moreKeys = listOf("ল্প", "ল্ক"))
                )
            ),
            KeyRow(
                listOf(
                    KeyDef(type = KeyType.SHIFT, icon = "shift", widthWeight = 1.5f),
                    KeyDef("্", "ৎ"),
                    KeyDef("ং", "ঙ"),
                    KeyDef("ঃ", "ঢ়"),
                    KeyDef("ঁ", "ড়"),
                    KeyDef("ব", "ভ", moreKeys = listOf("ব্ব")),
                    KeyDef("ম", "্ম", moreKeys = listOf("ম্ম")),
                    KeyDef("ন", "ঞ", moreKeys = listOf("ন্ত", "ন্দ")),
                    KeyDef("ণ", "ণ", moreKeys = listOf("ণ্ট", "ণ্ঠ")),
                    KeyDef("শ", "ষ্", moreKeys = listOf("শ্চ")),
                    KeyDef("য", "্য"),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.5f, slidingInput = true),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", widthWeight = 1.0f, moreKeys = listOf(";", ":", "<", ">")),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef("।", moreKeys = listOf(".", "!", "?", ",", "-", "/", "—"), widthWeight = 1.0f),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    val BengaliBorno = KeyboardLayoutDef(
        id = "bn_borno",
        name = "Bengali (Borno Phonetic)",
        language = "bn",
        rows = EnglishQwerty.rows
    )

    val Symbols = KeyboardLayoutDef(
        id = "symbols_1",
        name = "Symbols",
        language = "sym",
        rows = listOf(
            KeyRow(
                listOf(
                    KeyDef("1", moreKeys = listOf("1", "১")),
                    KeyDef("2", moreKeys = listOf("2", "২")),
                    KeyDef("3", moreKeys = listOf("3", "৩")),
                    KeyDef("4", moreKeys = listOf("4", "৪")),
                    KeyDef("5", moreKeys = listOf("5", "৫")),
                    KeyDef("6", moreKeys = listOf("6", "৬")),
                    KeyDef("7", moreKeys = listOf("7", "৭")),
                    KeyDef("8", moreKeys = listOf("8", "৮")),
                    KeyDef("9", moreKeys = listOf("9", "৯")),
                    KeyDef("0", moreKeys = listOf("0", "০"))
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("@"),
                    KeyDef("#"),
                    KeyDef("$", moreKeys = listOf("$", "৳", "₹", "€", "£", "¥")),
                    KeyDef("_"),
                    KeyDef("&"),
                    KeyDef("-"),
                    KeyDef("+"),
                    KeyDef("("),
                    KeyDef(")"),
                    KeyDef("/")
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("=\\<", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef("*"),
                    KeyDef("\""),
                    KeyDef("'"),
                    KeyDef(":"),
                    KeyDef(";"),
                    KeyDef("!"),
                    KeyDef("?"),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("ABC", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef(",", widthWeight = 1.0f),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef(".", widthWeight = 1.0f),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
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
                    KeyDef("•"),
                    KeyDef("√"),
                    KeyDef("π"),
                    KeyDef("÷"),
                    KeyDef("×"),
                    KeyDef("§"),
                    KeyDef("Δ")
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("£"),
                    KeyDef("€"),
                    KeyDef("¥"),
                    KeyDef("¢"),
                    KeyDef("^"),
                    KeyDef("°"),
                    KeyDef("="),
                    KeyDef("{"),
                    KeyDef("}"),
                    KeyDef("\\")
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("?123", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef("%"),
                    KeyDef("©"),
                    KeyDef("®"),
                    KeyDef("™"),
                    KeyDef("✓"),
                    KeyDef("["),
                    KeyDef("]"),
                    KeyDef(type = KeyType.DELETE, icon = "backspace", widthWeight = 1.5f)
                )
            ),
            KeyRow(
                listOf(
                    KeyDef("ABC", type = KeyType.SYMBOLS, widthWeight = 1.5f),
                    KeyDef(type = KeyType.SWITCH_LANGUAGE, icon = "globe", widthWeight = 1.0f),
                    KeyDef("<", widthWeight = 1.0f),
                    KeyDef(" ", type = KeyType.SPACE, icon = "space", widthWeight = 4.0f),
                    KeyDef(">", widthWeight = 1.0f),
                    KeyDef(type = KeyType.ACTION, icon = "enter", widthWeight = 1.5f, moreKeys = listOf("ACTION_EMOJI"))
                )
            )
        )
    )

    fun fromId(id: String): KeyboardLayoutDef {
        return when (id) {
            "bn_borno" -> BengaliBorno
            "bn_probhat" -> BengaliProbhat
            else -> EnglishQwerty
        }
    }

    fun displayName(id: String): String {
        return when (id) {
            "bn_borno" -> "বাংলা (বর্ণ ফোনেটিক)"
            "bn_probhat" -> "বাংলা (प्रभात)"
            else -> "English (QWERTY)"
        }
    }
}
