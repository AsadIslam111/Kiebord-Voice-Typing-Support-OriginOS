//! Complete, clean-room port of Jayed Ahsan's BornoPhonetic transliterator from BornoTSF.
//! Reference: https://github.com/JayedAhsan/BornoTSF/blob/main/layoutParsers/Transliterators/BornoPhonetic.hpp

use std::collections::{HashMap, HashSet};
use std::sync::OnceLock;

const HOSHONTO: &str = "\u{09CD}";
const ZWJ: &str = "\u{200D}";

#[derive(Clone, Copy, PartialEq, Eq)]
enum PrevTokenType {
    StartOfWord,
    Consonant,
    Vowel,
    Symbol,
    Other,
}

pub struct BornoPhonetic;

impl BornoPhonetic {
    pub fn convert(query: &str) -> String {
        Self::convert_mode(query, false)
    }

    pub fn convert_avro(query: &str) -> String {
        Self::convert_mode(query, true)
    }

    pub fn convert_mode(query: &str, is_avro: bool) -> String {
        Self::parse_mode(query, is_avro)
    }

    pub fn parse(input: &str) -> String {
        Self::parse_mode(input, false)
    }

    pub fn parse_mode(input: &str, is_avro: bool) -> String {
        if input.is_empty() {
            return String::new();
        }

        // Handle escape backslashes
        if let Some(pos) = input.find('\\') {
            let mut k = 0;
            let bytes = input.as_bytes();
            while pos + k < input.len() && bytes[pos + k] == b'\\' {
                k += 1;
            }

            let left_text = &input[0..pos];
            let right_text = &input[pos + k..];

            if right_text.is_empty() {
                match k {
                    1 => format!("{}\\", Self::parse_segment_mode(left_text, is_avro)),
                    2 => left_text.to_string(),
                    3 => format!("{}\\", left_text),
                    4 => format!("{}\\\\", Self::parse_segment_mode(left_text, is_avro)),
                    _ if k % 2 == 1 => format!("{}{}", left_text, "\\".repeat((k - 1) / 2)),
                    _ => format!("{}{}", Self::parse_segment_mode(left_text, is_avro), "\\".repeat(k / 2)),
                }
            } else {
                match k {
                    1 => format!("{}\\{}", Self::parse_segment_mode(left_text, is_avro), Self::parse_mode(right_text, is_avro)),
                    2 => format!("{}{}", left_text, Self::parse_mode(right_text, is_avro)),
                    3 => format!("{}\\{}", left_text, Self::parse_mode(right_text, is_avro)),
                    4 => format!("{}\\\\{}", Self::parse_segment_mode(left_text, is_avro), Self::parse_mode(right_text, is_avro)),
                    _ if k % 2 == 1 => format!("{}{}{}", left_text, "\\".repeat((k - 1) / 2), Self::parse_mode(right_text, is_avro)),
                    _ => format!("{}{}{}", Self::parse_segment_mode(left_text, is_avro), "\\".repeat(k / 2), Self::parse_mode(right_text, is_avro)),
                }
            }
        } else {
            Self::parse_segment_mode(input, is_avro)
        }
    }

    pub fn parse_segment(input: &str) -> String {
        Self::parse_segment_mode(input, false)
    }

    pub fn parse_segment_mode(input: &str, is_avro: bool) -> String {
        if input.is_empty() {
            return String::new();
        }

        let corrected = Self::case_handle(input);
        let tokens = Self::tokenize_mode(&corrected, is_avro);
        let mut output = String::with_capacity(corrected.len() * 3);
        let mut last_type = PrevTokenType::StartOfWord;
        let mut last_vowel_token: Option<&str> = None;
        let mut prev_vowel_token: Option<&str> = None;

        let phola_map = Self::get_phola_map();
        let consonants_map = Self::get_consonants_map();
        let conjuncts_map = Self::get_conjuncts_map();
        let symbol_map = Self::get_symbol_map(is_avro);
        let diacritics_map = Self::get_diacritics_map(is_avro);
        let punctuations_map = Self::get_punctuations_map();
        let digits_map = Self::get_digits_map();
        let initial_map = Self::get_initial_map();
        let vowel_signs_map = Self::get_vowel_signs_map();
        let after_vowel_map = Self::get_after_vowel_map();
        let reph_map = Self::get_reph_map();

        let mut i = 0;
        let token_count = tokens.len();

        while i < token_count {
            let token = tokens[i];

            if token == " " {
                output.push(' ');
                last_type = PrevTokenType::StartOfWord;
                i += 1;
                continue;
            }

            // Special token 'w'
            if token == "w" {
                if last_type == PrevTokenType::Consonant {
                    output.push_str(HOSHONTO);
                    output.push_str(phola_map.get("w").unwrap_or(&"ব"));
                    last_type = PrevTokenType::Consonant;
                } else if last_type == PrevTokenType::Symbol {
                    output.push_str(phola_map.get("w").unwrap_or(&"ব"));
                    last_type = PrevTokenType::Consonant;
                } else {
                    if i + 1 < token_count && tokens[i + 1] == "a" {
                        output.push_str("ওয়া");
                        i += 1;
                    } else if i + 1 < token_count && tokens[i + 1] == "e" {
                        output.push_str("ওয়ে");
                        i += 1;
                    } else {
                        output.push_str("ও");
                    }
                    last_type = PrevTokenType::Vowel;
                }
                i += 1;
                continue;
            }

            // Special token 'x'
            if token == "x" {
                if last_type == PrevTokenType::StartOfWord {
                    output.push_str("এক্স");
                } else {
                    output.push_str("ক্স");
                }
                last_type = PrevTokenType::Consonant;
                i += 1;
                continue;
            }

            // Special token 'rri'
            if token == "rri" {
                let next_is_backtick = i + 1 < token_count && tokens[i + 1] == "`";
                if next_is_backtick {
                    output.push_str("ৃ");
                } else if last_type != PrevTokenType::Consonant || last_type == PrevTokenType::StartOfWord {
                    output.push_str("ঋ");
                } else {
                    output.push_str("ৃ");
                }
                last_type = PrevTokenType::Vowel;
                i += 1;
                continue;
            }

            // In Borno Phonetic (!is_avro), capital 'A' after a non-start token produces chandrabindu ('ঁ')
            if !is_avro && token == "A" {
                if last_type != PrevTokenType::StartOfWord {
                    output.push_str("ঁ");
                    last_type = PrevTokenType::Symbol;
                    i += 1;
                    continue;
                }
            }

            if let Some(val) = symbol_map.get(token) {
                output.push_str(val);
                last_type = PrevTokenType::Symbol;
                i += 1;
                continue;
            }

            if let Some(val) = digits_map.get(token) {
                output.push_str(val);
                last_type = PrevTokenType::Other;
                i += 1;
                continue;
            }

            if let Some(val) = punctuations_map.get(token) {
                output.push_str(val);
                last_type = PrevTokenType::Other;
                i += 1;
                continue;
            }

            if let Some(val) = reph_map.get(token) {
                if last_type == PrevTokenType::Consonant {
                    output.push_str(HOSHONTO);
                    output.push_str("রর");
                    last_type = PrevTokenType::Consonant;
                } else {
                    let mut cond_reph = false;
                    if i + 1 < token_count {
                        let next = tokens[i + 1];
                        let next_is_vowel = initial_map.contains_key(next);
                        if !next_is_vowel && next != "r" && next != "R" {
                            cond_reph = true;
                        }
                    }
                    if cond_reph {
                        output.push_str(val);
                        last_type = PrevTokenType::Other;
                    } else {
                        output.push_str("রর");
                        last_type = PrevTokenType::Consonant;
                    }
                }
                i += 1;
                continue;
            }

            if let Some(val) = diacritics_map.get(token) {
                output.push_str(val);
                if token == ",," || token == ",." || token == ",.." || token == "hs" {
                    last_type = PrevTokenType::Consonant;
                } else {
                    last_type = PrevTokenType::Vowel;
                }
                i += 1;
                continue;
            }

            if let Some(val) = conjuncts_map.get(token) {
                output.push_str(val);
                last_type = PrevTokenType::Consonant;
                i += 1;
                continue;
            }

            if let Some(val) = phola_map.get(token) {
                if last_type == PrevTokenType::Consonant {
                    if token == "y" || token == "z" || token == "Z" {
                        let prev_is_r = i > 0 && tokens[i - 1] == "r";
                        if prev_is_r {
                            let mut prev2_is_special = false;
                            if i >= 2 {
                                let prev2 = tokens[i - 2];
                                if (consonants_map.contains_key(prev2) || conjuncts_map.contains_key(prev2))
                                    && prev2 != "r" && prev2 != "y" && prev2 != "w" && prev2 != "x"
                                {
                                    prev2_is_special = true;
                                }
                            }
                            if prev2_is_special {
                                output.push_str(HOSHONTO);
                                output.push_str(val);
                            } else {
                                output.push_str(ZWJ);
                                output.push_str(HOSHONTO);
                                output.push_str(val);
                            }
                        } else {
                            output.push_str(HOSHONTO);
                            output.push_str(val);
                        }
                    } else if token == "r" {
                        if output.ends_with("\u{09CD}র") {
                            output.push_str("র");
                        } else {
                            output.push_str(HOSHONTO);
                            output.push_str(val);
                        }
                    } else {
                        output.push_str(HOSHONTO);
                        output.push_str(val);
                    }
                } else {
                    if token == "r" { output.push_str("র"); }
                    else if token == "R" { output.push_str("ড়"); }
                    else if token == "y" || token == "Y" { output.push_str("য়"); }
                    else if token == "z" || token == "Z" { output.push_str("য"); }
                }
                last_type = PrevTokenType::Consonant;
                i += 1;
                continue;
            }

            if let Some(val) = consonants_map.get(token) {
                output.push_str(val);
                last_type = PrevTokenType::Consonant;
                i += 1;
                continue;
            }

            if let Some(val) = initial_map.get(token) {
                if last_type == PrevTokenType::Consonant {
                    if token == "o" {
                        // Inherent 'o' elision: empty string
                    } else if let Some(sign) = vowel_signs_map.get(token) {
                        output.push_str(sign);
                    }
                    prev_vowel_token = None;
                    last_vowel_token = Some(token);
                } else if last_type == PrevTokenType::Vowel {
                    if token == "o" {
                        output.push_str("ও");
                    } else if is_avro && (token == "a" || token == "A") && last_vowel_token != Some("a") && last_vowel_token != Some("A") {
                        output.push_str("য়া");
                    } else if !is_avro && (token == "a" || token == "A") && (last_vowel_token == Some("i") || last_vowel_token == Some("I")) && (prev_vowel_token == Some("a") || prev_vowel_token == Some("A")) {
                        output.push_str("য়া");
                    } else if let Some(after) = after_vowel_map.get(token) {
                        output.push_str(after);
                    }
                    prev_vowel_token = last_vowel_token;
                    last_vowel_token = Some(token);
                } else {
                    output.push_str(val);
                    prev_vowel_token = None;
                    last_vowel_token = Some(token);
                }
                last_type = PrevTokenType::Vowel;
                i += 1;
                continue;
            }

            output.push_str(token);
            last_type = PrevTokenType::Other;
            i += 1;
        }

        Self::remove_consecutive_kar_signs(output)
    }

    fn case_handle(input: &str) -> String {
        let mut result = String::with_capacity(input.len());
        for c in input.chars() {
            match c {
                'a' | 'A' | 'o' | 'O' | 'i' | 'I' | 'u' | 'U' | 'd' | 'D' | 'g' | 'G' | 'j' | 'J'
                | 'n' | 'N' | 'r' | 'R' | 's' | 'S' | 't' | 'T' | 'y' | 'Y' | 'z' | 'Z' | 'h'
                | 'H' | 'x' | 'X' => result.push(c),
                other => {
                    if other.is_ascii_uppercase() {
                        result.push(other.to_ascii_lowercase());
                    } else {
                        result.push(other);
                    }
                }
            }
        }
        result
    }

    pub fn tokenize(text: &str) -> Vec<&str> {
        Self::tokenize_mode(text, false)
    }

    pub fn tokenize_mode(text: &str, is_avro: bool) -> Vec<&str> {
        let mut tokens = Vec::with_capacity(text.len());
        if text.is_empty() {
            return tokens;
        }

        let keys_set = Self::get_global_keys_set(is_avro);
        let max_len = 5;
        let mut i = 0;
        let len = text.len();

        while i < len {
            let mut matched = false;
            let remaining = len - i;
            let check_len = max_len.min(remaining);

            for l in (1..=check_len).rev() {
                if let Some(segment) = text.get(i..i + l) {
                    if keys_set.contains(segment) {
                        // Lookahead collision resolver for 'r' followed by 'ri'
                        if l > 2 && segment.ends_with('r') && i + l + 2 <= len {
                            if let Some(next_two) = text.get(i + l..i + l + 2) {
                                if next_two == "ri" {
                                    if let Some(shorter) = segment.get(0..l - 1) {
                                        if keys_set.contains(shorter) {
                                            tokens.push(shorter);
                                            i += l - 1;
                                            matched = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        // Lookahead collision resolver for 'T' followed by 'H'
                        if l > 1 && segment.ends_with('T') && i + l < len {
                            if let Some(next_one) = text.get(i + l..i + l + 1) {
                                if next_one == "H" {
                                    if let Some(shorter) = segment.get(0..l - 1) {
                                        if keys_set.contains(shorter) {
                                            tokens.push(shorter);
                                            i += l - 1;
                                            matched = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        tokens.push(segment);
                        i += l;
                        matched = true;
                        break;
                    }
                }
            }

            if !matched {
                if let Some(ch) = text[i..].chars().next() {
                    let ch_len = ch.len_utf8();
                    tokens.push(&text[i..i + ch_len]);
                    i += ch_len;
                } else {
                    i += 1;
                }
            }
        }

        tokens
    }

    fn remove_consecutive_kar_signs(s: String) -> String {
        let mut prev_kar = false;
        let mut has_consecutive = false;
        for c in s.chars() {
            let is_kar = Self::is_kar_sign(c);
            if prev_kar && is_kar {
                has_consecutive = true;
                break;
            }
            prev_kar = is_kar;
        }
        if !has_consecutive {
            return s;
        }

        let mut chars: Vec<char> = s.chars().collect();
        for i in 1..chars.len() {
            if Self::is_kar_sign(chars[i - 1]) && Self::is_kar_sign(chars[i]) {
                chars[i] = Self::get_corresponding_vowel(chars[i]);
            }
        }

        chars.into_iter().collect()
    }

    fn is_kar_sign(c: char) -> bool {
        let code = c as u32;
        (0x09BE..=0x09C4).contains(&code)
            || (0x09C7..=0x09C8).contains(&code)
            || (0x09CB..=0x09CC).contains(&code)
            || code == 0x09D7
    }

    fn get_corresponding_vowel(sign: char) -> char {
        match sign as u32 {
            0x09BE => '\u{0986}', // আ
            0x09BF => '\u{0987}', // ই
            0x09C0 => '\u{0988}', // ঈ
            0x09C1 => '\u{0989}', // উ
            0x09C2 => '\u{098A}', // ঊ
            0x09C3 => '\u{098B}', // ঋ
            0x09C7 => '\u{098F}', // এ
            0x09C8 => '\u{0990}', // ঐ
            0x09CB => '\u{0993}', // ও
            0x09CC | 0x09D7 => '\u{0994}', // ঔ
            _ => sign,
        }
    }

    fn get_symbol_map(is_avro: bool) -> &'static HashMap<&'static str, &'static str> {
        if is_avro {
            static AVRO_MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
            AVRO_MAP.get_or_init(|| {
                let mut map = Self::build_symbol_map();
                map.insert("cb", "ঁ");
                map.insert("qq", "ঁ");
                map.insert("HH", "ঃ");
                map
            })
        } else {
            static BORNO_MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
            BORNO_MAP.get_or_init(Self::build_symbol_map)
        }
    }

    fn get_initial_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_initial_map)
    }

    fn get_after_vowel_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_after_vowel_map)
    }

    fn get_vowel_signs_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_vowel_signs_map)
    }

    fn get_consonants_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_consonants_map)
    }

    fn get_conjuncts_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_conjuncts_map)
    }

    fn get_phola_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_phola_map)
    }

    fn get_reph_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_reph_map)
    }

    fn get_diacritics_map(is_avro: bool) -> &'static HashMap<&'static str, &'static str> {
        if is_avro {
            static AVRO_MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
            AVRO_MAP.get_or_init(|| {
                let mut map = Self::build_diacritics_map();
                map.insert("hs", HOSHONTO);
                map
            })
        } else {
            static BORNO_MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
            BORNO_MAP.get_or_init(Self::build_diacritics_map)
        }
    }

    fn get_punctuations_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_punctuations_map)
    }

    fn get_digits_map() -> &'static HashMap<&'static str, &'static str> {
        static MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
        MAP.get_or_init(Self::build_digits_map)
    }

    fn get_global_keys_set(is_avro: bool) -> &'static HashSet<&'static str> {
        if is_avro {
            static AVRO_KEYS_SET: OnceLock<HashSet<&'static str>> = OnceLock::new();
            AVRO_KEYS_SET.get_or_init(|| {
                let mut set = Self::get_global_keys_set(false).clone();
                set.insert("cb");
                set.insert("qq");
                set.insert("HH");
                set.insert("hs");
                set
            })
        } else {
            static BORNO_KEYS_SET: OnceLock<HashSet<&'static str>> = OnceLock::new();
            BORNO_KEYS_SET.get_or_init(|| {
                let mut set = HashSet::new();
                for k in Self::get_symbol_map(false).keys() { set.insert(*k); }
                for k in Self::get_initial_map().keys() { set.insert(*k); }
                for k in Self::get_after_vowel_map().keys() { set.insert(*k); }
                for k in Self::get_vowel_signs_map().keys() { set.insert(*k); }
                for k in Self::get_consonants_map().keys() { set.insert(*k); }
                for k in Self::get_conjuncts_map().keys() { set.insert(*k); }
                for k in Self::get_diacritics_map(false).keys() { set.insert(*k); }
                for k in Self::get_punctuations_map().keys() { set.insert(*k); }
                for k in Self::get_phola_map().keys() { set.insert(*k); }
                for k in Self::get_reph_map().keys() { set.insert(*k); }
                for k in Self::get_digits_map().keys() { set.insert(*k); }
                set.insert("rri");
                set
            })
        }
    }

    fn build_symbol_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("ng", "ং"), ("TH", "ৎ"), ("X", "ৎ"), ("t``", "ৎ"),
            ("xx", "ঁ"), ("::", "ঃ"), ("ff", "ঃ")
        ])
    }

    fn build_initial_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("a", "আ"), ("A", "আ"), ("i", "ই"), ("I", "ঈ"), ("u", "উ"), ("U", "ঊ"),
            ("e", "এ"), ("E", "এ"), ("o", "অ"), ("O", "ও"), ("OI", "ঐ"), ("OU", "ঔ")
        ])
    }

    fn build_after_vowel_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("a", "আ"), ("A", "আ"), ("i", "ই"), ("I", "ঈ"), ("u", "উ"), ("U", "ঊ"),
            ("e", "এ"), ("E", "এ"), ("o", "অ"), ("O", "ও"), ("OI", "ঐ"), ("OU", "ঔ")
        ])
    }

    fn build_vowel_signs_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("a", "া"), ("A", "া"), ("i", "ি"), ("I", "ী"), ("u", "ু"), ("U", "ূ"),
            ("e", "ে"), ("E", "ে"), ("o", ""),  ("O", "ো"), ("OI", "ৈ"), ("OU", "ৌ")
        ])
    }

    fn build_consonants_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("k", "ক"), ("kh", "খ"), ("g", "গ"), ("G", "গ"), ("gh", "ঘ"), ("Gh", "ঘ"),
            ("Ng", "ঙ"), ("NG", "ঞ"), ("c", "চ"), ("ch", "ছ"), ("j", "জ"), ("J", "জ"),
            ("jh", "ঝ"), ("t", "ত"), ("th", "থ"), ("T", "ট"), ("Th", "ঠ"), ("d", "দ"),
            ("dh", "ধ"), ("D", "ড"), ("Dh", "ঢ"), ("n", "ন"), ("N", "ণ"), ("p", "প"),
            ("ph", "ফ"), ("f", "ফ"), ("b", "ব"), ("bh", "ভ"), ("v", "ভ"), ("m", "ম"),
            ("z", "য"), ("y", "য়"), ("Y", "য়"), ("r", "র"), ("R", "ড়"), ("Rh", "ঢ়"),
            ("l", "ল"), ("s", "স"), ("S", "শ"), ("sh", "শ"), ("Sh", "ষ"), ("SH", "ষ"),
            ("h", "হ"), ("H", "হ"), ("q", "ক"), ("x", "ক্স")
        ])
    }

    fn build_conjuncts_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("kkhN", "ক্ষ্ণ"), ("kShN", "ক্ষ্ণ"), ("kkhm", "ক্ষ্ম"), ("kShm", "ক্ষ্ম"),
            ("kxN", "ক্ষ্ণ"), ("kxm", "ক্ষ্ম"), ("kkh", "ক্ষ"), ("kSh", "ক্ষ"),
            ("ksh", "ক্শ"), ("kx", "ক্ষ"), ("kk", "ক্ক"), ("kT", "ক্ট"),
            ("kt", "ক্ত"), ("km", "ক্ম"), ("kl", "ক্ল"), ("ks", "ক্স"),
            ("ghn", "ঘ্ন"), ("Ghn", "ঘ্ন"), ("gdh", "গ্ধ"), ("Gdh", "গ্ধ"),
            ("gN", "গ্ণ"), ("GN", "গ্ণ"), ("gn", "গ্ন"), ("Gn", "গ্ন"),
            ("gm", "গ্ম"), ("Gm", "গ্ম"), ("gl", "গ্ল"), ("Gl", "গ্ল"),
            ("gg", "জ্ঞ"), ("GG", "জ্ঞ"), ("Gg", "জ্ঞ"), ("gG", "জ্ঞ"),
            ("nch", "ঞ্ছ"), ("njh", "ঞ্ঝ"), ("ndh", "ন্ধ"), ("nTh", "ন্ঠ"),
            ("nth", "ন্থ"), ("nj", "ঞ্জ"), ("nn", "ন্ন"), ("nm", "ন্ম"),
            ("nd", "ন্দ"), ("nT", "ন্ট"), ("nD", "ন্ড"), ("nt", "ন্ত"),
            ("ns", "ন্স"), ("nc", "ঞ্চ"),
            ("NgkSh", "ঙ্ক্ষ"), ("Ngkkh", "ঙ্ক্ষ"), ("NGch", "ঞ্ছ"), ("Nggh", "ঙ্ঘ"),
            ("Ngkh", "ঙ্খ"), ("NGjh", "ঞ্ঝ"), ("ngOI", "ঙ্গৈ"), ("ngOU", "ঙ্গৌ"),
            ("Ngkx", "ঙ্ক্ষ"), ("NGc", "ঞ্চ"), ("ngh", "ঙ্ঘ"), ("ngH", "ংহ"), ("Ngk", "ঙ্ক"),
            ("Ngx", "ঙ্ক্ষ"), ("Ngg", "ঙ্গ"), ("Ngm", "ঙ্ম"), ("NGj", "ঞ্জ"),
            ("NTh", "ণ্ঠ"), ("nkh", "ঙ্খ"), ("ngo", "ঙ্গ"), ("nga", "ঙ্গা"),
            ("ngi", "ঙ্গি"), ("ngI", "ঙ্গী"), ("ngu", "ঙ্গু"), ("ngU", "ঙ্গূ"),
            ("nge", "ঙ্গে"), ("ngO", "ঙ্গো"), ("NDh", "ণ্ঢ"), ("nsh", "নশ"),
            ("Ngr", "ঙ্র"), ("NGr", "ঞ্র"), ("Ng", "ঙ"),
            ("NG", "ঞ"), ("nk", "ঙ্ক"), ("NN", "ণ্ণ"),
            ("Nn", "ণ্ণ"), ("Nm", "ণ্ম"), ("NT", "ণ্ট"), ("ND", "ণ্ড"),
            ("cNG", "চ্ঞ"), ("cch", "চ্ছ"), ("cc", "চ্চ"),
            ("jjh", "জ্ঝ"), ("jNG", "জ্ঞ"), ("jj", "জ্জ"),
            ("tth", "ত্থ"), ("tn", "ত্ন"), ("tm", "ত্ম"), ("tt", "ত্ত"),
            ("TT", "ট্ট"), ("Tm", "ট্ম"),
            ("dhn", "ধ্ন"), ("dhm", "ধ্ম"), ("dgh", "দ্ঘ"), ("ddh", "দ্ধ"),
            ("dbh", "দ্ভ"), ("dv", "দ্ভ"), ("dm", "দ্ম"), ("dg", "দ্গ"),
            ("dd", "দ্দ"), ("DD", "ড্ড"),
            ("phl", "ফ্ল"), ("pT", "প্ট"), ("pt", "প্ত"), ("pn", "প্ন"),
            ("pp", "প্প"), ("pl", "প্ল"), ("ps", "প্স"), ("fl", "ফ্ল"),
            ("bdh", "ব্ধ"), ("bhl", "ভ্ল"), ("bj", "ব্জ"), ("bd", "ব্দ"),
            ("bb", "ব্ব"), ("bl", "ব্ল"), ("vl", "ভ্ল"),
            ("mth", "ম্থ"), ("mph", "ম্ফ"), ("mbh", "ম্ভ"), ("mpl", "মপ্ল"),
            ("mn", "ম্ন"), ("mp", "ম্প"), ("mv", "ম্ভ"), ("mm", "ম্ম"),
            ("ml", "ম্ল"), ("mb", "ম্ব"), ("mf", "ম্ফ"),
            ("Rg", "ড়্গ"),
            ("oZ", "অ্য"),
            ("lbh", "ল্ভ"), ("ldh", "ল্ধ"), ("lkh", "ল্খ"), ("lgh", "ল্ঘ"),
            ("lph", "ল্ফ"), ("lk", "ল্ক"), ("lg", "ল্গ"), ("lT", "ল্ট"),
            ("lD", "ল্ড"), ("lp", "ল্প"), ("lv", "ল্ভ"), ("lm", "ল্ম"),
            ("ll", "ল্ল"), ("lb", "ল্ব"),
            ("shch", "শ্ছ"), ("Sch", "শ্ছ"), ("skl", "স্ক্ল"), ("skh", "স্খ"),
            ("sth", "স্থ"), ("sph", "স্ফ"), ("shc", "শ্চ"), ("sht", "শ্ত"),
            ("shn", "শ্ন"), ("shm", "শ্ম"), ("spl", "স্প্ল"), ("shl", "শ্ল"),
            ("sk", "স্ক"), ("Sc", "শ্চ"), ("sT", "স্ট"), ("st", "স্ত"),
            ("sn", "স্ন"), ("sp", "স্প"), ("sf", "স্ফ"), ("sm", "স্ম"),
            ("sl", "স্ল"),
            ("ShTh", "ষ্ঠ"), ("Shph", "ষ্ফ"), ("Shk", "ষ্ক"), ("ShT", "ষ্ট"),
            ("ShN", "ষ্ণ"), ("Shp", "ষ্প"), ("Shf", "ষ্ফ"), ("Shm", "ষ্ম"),
            ("SHTh", "ষ্ঠ"), ("SHph", "ষ্ফ"), ("SHk", "ষ্ক"), ("SHT", "ষ্ট"),
            ("SHN", "ষ্ণ"), ("SHp", "ষ্প"), ("SHf", "ষ্ফ"), ("SHm", "ষ্ম"),
            ("Sn", "শ্ন"), ("Sm", "শ্ম"), ("Sl", "শ্ল"),
            ("hN", "হ্ণ"), ("hn", "হ্ন"), ("hm", "হ্ম"), ("hl", "হ্ল")
        ])
    }

    fn build_phola_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("y", "য"), ("z", "য"), ("Z", "য"), ("r", "র"), ("w", "ব")
        ])
    }

    fn build_reph_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([("rr", "র্")])
    }

    fn build_diacritics_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("^`", "^"), (":`", ":"),
            (",,", HOSHONTO), (",.", "\u{09CD}\u{200C}"), (",..", "\u{09CD}\u{200D}"),
            ("a`", "া"), ("i`", "ি"), ("I`", "ী"), ("u`", "ু"), ("U`", "ূ"),
            ("e`", "ে"), ("o`", ""), ("O`", "ো"), ("OI`", "ৈ"), ("OU`", "ৌ"),
            ("rri`", "ৃ"), ("./", "়")
        ])
    }

    fn build_punctuations_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("`", ""), ("``", "`"), (".", "।"), ("..", "।।"), ("...", "..."),
            ("$$", "$"), (",", ","), ("$", "৳"), ("$%", "₹")
        ])
    }

    fn build_digits_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            (".1", ".১"), (".2", ".২"), (".3", ".৩"), (".4", ".৪"), (".5", ".৫"),
            (".6", ".৬"), (".7", ".৭"), (".8", ".৮"), (".9", ".৯"), (".0", ".০"),
            ("1", "১"), ("2", "২"), ("3", "৩"), ("4", "৪"), ("5", "৫"),
            ("6", "৬"), ("7", "৭"), ("8", "৮"), ("9", "৯"), ("0", "০")
        ])
    }

    pub fn is_bengali_consonant(c: char) -> bool {
        let code = c as u32;
        (0x0995..=0x09B9).contains(&code) || (0x09DC..=0x09DF).contains(&code) || code == 0x09CE
    }

    fn independent_vowel_to_keys(c: char) -> Option<Vec<char>> {
        match c as u32 {
            0x0985 => Some(vec!['o']),                // অ
            0x0986 => Some(vec!['a']),                // আ
            0x0987 | 0x0988 => Some(vec!['i']),       // ই, ঈ
            0x0989 | 0x098A => Some(vec!['u']),       // উ, ঊ
            0x098B => Some(vec!['r', 'i']),           // ঋ
            0x098F => Some(vec!['e']),                // এ
            0x0990 => Some(vec!['o', 'i']),           // ঐ
            0x0993 => Some(vec!['o']),                // ও
            0x0994 => Some(vec!['o', 'u']),           // ঔ
            _ => None,
        }
    }

    fn consonant_to_keys(c: char) -> Option<Vec<Vec<char>>> {
        match c {
            'ক' => Some(vec![vec!['k']]),
            'খ' => Some(vec![vec!['k', 'h']]),
            'গ' => Some(vec![vec!['g']]),
            'ঘ' => Some(vec![vec!['g', 'h']]),
            'ঙ' => Some(vec![vec!['n', 'g']]),
            'চ' => Some(vec![vec!['c']]),
            'ছ' => Some(vec![vec!['c', 'h']]),
            'জ' => Some(vec![vec!['j']]),
            'ঝ' => Some(vec![vec!['j', 'h']]),
            'ঞ' => Some(vec![vec!['n', 'g'], vec!['n']]),
            'ট' => Some(vec![vec!['t']]),
            'ঠ' => Some(vec![vec!['t', 'h']]),
            'ড' => Some(vec![vec!['d']]),
            'ঢ' => Some(vec![vec!['d', 'h']]),
            'ণ' => Some(vec![vec!['n']]),
            'ত' => Some(vec![vec!['t']]),
            'থ' => Some(vec![vec!['t', 'h']]),
            'দ' => Some(vec![vec!['d']]),
            'ধ' => Some(vec![vec!['d', 'h']]),
            'ন' => Some(vec![vec!['n']]),
            'প' => Some(vec![vec!['p']]),
            'ফ' => Some(vec![vec!['f'], vec!['p', 'h']]),
            'ব' => Some(vec![vec!['b']]),
            'ভ' => Some(vec![vec!['b', 'h'], vec!['v']]),
            'ম' => Some(vec![vec!['m']]),
            'য' => Some(vec![vec!['z'], vec!['j']]),
            'র' => Some(vec![vec!['r']]),
            'ল' => Some(vec![vec!['l']]),
            'শ' => Some(vec![vec!['s', 'h'], vec!['s']]),
            'ষ' => Some(vec![vec!['s', 'h'], vec!['s']]),
            'স' => Some(vec![vec!['s']]),
            'হ' => Some(vec![vec!['h']]),
            'ড়' => Some(vec![vec!['r']]),
            'ঢ়' => Some(vec![vec!['r', 'h']]),
            'য়' => Some(vec![vec!['y']]),
            'ৎ' => Some(vec![vec!['t']]),
            _ => None,
        }
    }

    /// Decomposes a target Bengali word into its canonical Latin phonetic key sequences on QWERTY.
    pub fn to_phonetic_keys(word: &str) -> Vec<Vec<char>> {
        let chars: Vec<char> = word.chars().collect();
        if chars.is_empty() {
            return Vec::new();
        }

        let mut paths: Vec<Vec<char>> = vec![Vec::new()];
        let len = chars.len();
        let mut i = 0;

        while i < len {
            let c = chars[i];
            let next = chars.get(i + 1).copied();

            if Self::is_kar_sign(c) {
                if c as u32 == 0x09BE && paths.len() <= 2 {
                    let mut alt_paths = Vec::with_capacity(paths.len() * 2);
                    for p in &paths {
                        let mut p1 = p.clone();
                        p1.push('a');
                        alt_paths.push(p1);

                        let mut p2 = p.clone();
                        p2.push('e');
                        alt_paths.push(p2);
                    }
                    paths = alt_paths;
                    i += 1;
                    continue;
                }

                let kar_keys = match c as u32 {
                    0x09BE => vec!['a'],
                    0x09BF | 0x09C0 => vec!['i'],
                    0x09C1 | 0x09C2 => vec!['u'],
                    0x09C3 => vec!['r', 'i'],
                    0x09C7 => vec!['e'],
                    0x09C8 => vec!['o', 'i'],
                    0x09CB => vec!['o'],
                    0x09CC | 0x09D7 => vec!['o', 'u'],
                    _ => vec!['a'],
                };
                for path in &mut paths {
                    path.extend(&kar_keys);
                }
                i += 1;
                continue;
            }

            if let Some(v_keys) = Self::independent_vowel_to_keys(c) {
                if c == 'অ' && i == 0 {
                    let mut alt_paths = Vec::with_capacity(paths.len() * 2);
                    for p in &paths {
                        let mut p1 = p.clone();
                        p1.push('o');
                        alt_paths.push(p1);
                        let mut p2 = p.clone();
                        p2.push('a');
                        alt_paths.push(p2);
                    }
                    paths = alt_paths;
                } else {
                    for path in &mut paths {
                        path.extend(&v_keys);
                    }
                }
                i += 1;
                continue;
            }

            if let Some(c_variants) = Self::consonant_to_keys(c) {
                if next == Some('\u{09CD}') {
                    let next2 = chars.get(i + 2).copied();
                    if next2 == Some('য') {
                        for path in &mut paths {
                            path.extend(&c_variants[0]);
                            path.push('y');
                        }
                        i += 3;
                        continue;
                    } else if next2 == Some('র') {
                        for path in &mut paths {
                            path.extend(&c_variants[0]);
                            path.push('r');
                        }
                        i += 3;
                        continue;
                    } else if next2 == Some('ব') {
                        for path in &mut paths {
                            path.extend(&c_variants[0]);
                            path.push('b');
                        }
                        i += 3;
                        continue;
                    } else if let Some(n2) = next2 {
                        if c == 'ক' && n2 == 'ষ' {
                            for path in &mut paths {
                                path.extend(&['k', 's', 'h']);
                            }
                            i += 3;
                            continue;
                        } else if c == 'জ' && n2 == 'ঞ' {
                            for path in &mut paths {
                                path.extend(&['g', 'g']);
                            }
                            i += 3;
                            continue;
                        } else {
                            for path in &mut paths {
                                path.extend(&c_variants[0]);
                            }
                            i += 2;
                            continue;
                        }
                    } else {
                        for path in &mut paths {
                            path.extend(&c_variants[0]);
                        }
                        i += 2;
                        continue;
                    }
                }

                if let Some(n) = next {
                    if Self::is_kar_sign(n) {
                        if c_variants.len() > 1 && paths.len() <= 2 {
                            let mut alt_paths = Vec::with_capacity(paths.len() * c_variants.len());
                            for p in &paths {
                                for v in &c_variants {
                                    let mut p_copy = p.clone();
                                    p_copy.extend(v);
                                    alt_paths.push(p_copy);
                                }
                            }
                            paths = alt_paths;
                        } else {
                            for path in &mut paths {
                                path.extend(&c_variants[0]);
                            }
                        }
                        i += 1;
                        continue;
                    }
                }

                let is_followed_by_consonant = next.map_or(false, |nc| Self::is_bengali_consonant(nc));
                if is_followed_by_consonant && paths.len() < 4 {
                    let mut alt_paths = Vec::with_capacity(paths.len() * 2);
                    for p in &paths {
                        for v in &c_variants {
                            let mut p1 = p.clone();
                            p1.extend(v);
                            p1.push('o');
                            alt_paths.push(p1);

                            let mut p2 = p.clone();
                            p2.extend(v);
                            alt_paths.push(p2);
                        }
                    }
                    paths = alt_paths;
                } else if c_variants.len() > 1 && paths.len() <= 2 {
                    let mut alt_paths = Vec::with_capacity(paths.len() * c_variants.len());
                    for p in &paths {
                        for v in &c_variants {
                            let mut p_copy = p.clone();
                            p_copy.extend(v);
                            alt_paths.push(p_copy);
                        }
                    }
                    paths = alt_paths;
                } else {
                    for path in &mut paths {
                        path.extend(&c_variants[0]);
                    }
                }
                i += 1;
                continue;
            }

            match c {
                'ং' => {
                    for path in &mut paths {
                        path.extend(&['n', 'g']);
                    }
                }
                'ঃ' => {
                    for path in &mut paths {
                        path.push('h');
                    }
                }
                'ৎ' => {
                    for path in &mut paths {
                        path.push('t');
                    }
                }
                _ => {}
            }
            i += 1;
        }

        // If word ends in a consonant (without hasanta), also generate alternate path with 'o'
        let last_char = *chars.last().unwrap();
        let mut final_paths = paths.clone();
        if Self::is_bengali_consonant(last_char) && chars.get(chars.len().saturating_sub(2)) != Some(&'\u{09CD}') {
            for p in &paths {
                if final_paths.len() >= 8 {
                    break;
                }
                let mut with_o = p.clone();
                with_o.push('o');
                final_paths.push(with_o);
            }
        } else if last_char == '\u{09CB}' { // ো-কার
            for p in &paths {
                if final_paths.len() >= 8 {
                    break;
                }
                if p.last() == Some(&'o') && p.len() > 2 {
                    let without_o = p[..p.len() - 1].to_vec();
                    final_paths.push(without_o);
                }
            }
        }

        let mut unique: Vec<Vec<char>> = Vec::new();
        for p in final_paths {
            if p.len() >= 2 && !unique.contains(&p) {
                unique.push(p);
            }
        }
        unique
    }

    /// Suggests phonetic transliteration alternatives by exploring common
    /// lowercase/uppercase and phonetic confusion points:
    /// ত|ট (t<->T), ে|া (e<->a), জ|য (j<->z), ন|ণ (n<->N), etc.
    pub fn suggest_transliterations(input: &str) -> Vec<String> {
        Self::suggest_transliterations_mode(input, false)
    }

    pub fn suggest_transliterations_avro(input: &str) -> Vec<String> {
        Self::suggest_transliterations_mode(input, true)
    }

    pub fn suggest_transliterations_mode(input: &str, is_avro: bool) -> Vec<String> {
        if input.is_empty() {
            return Vec::new();
        }

        let canonical = Self::convert_mode(input, is_avro);
        let mut results = Vec::new();
        if !canonical.is_empty() {
            results.push(canonical.clone());
        }

        let chars: Vec<char> = input.chars().collect();
        let len = chars.len();

        for i in 0..len {
            let c = chars[i];
            let flipped: &[char] = match c {
                't' => &['T'],
                'T' => &['t'],
                'e' => &['a'],
                'a' => &['e'],
                'j' => &['z'],
                'z' => &['j'],
                'n' => &['N'],
                'N' => &['n'],
                'o' => &['O'],
                'O' => &['o'],
                'd' => &['D'],
                'D' => &['d'],
                's' => &['S'],
                'S' => &['s'],
                'r' => &['R'],
                'R' => &['r'],
                'i' => &['I'],
                'I' => &['i'],
                'u' => &['U'],
                'U' => &['u'],
                _ => &[],
            };

            for &f in flipped {
                let mut alt = chars.clone();
                alt[i] = f;
                let alt_str: String = alt.into_iter().collect();
                let translit = Self::convert_mode(&alt_str, is_avro);
                if !translit.is_empty() && !results.contains(&translit) {
                    results.push(translit);
                }
            }
        }

        // If 'r' is followed by a consonant, also suggest Reph ('rr' -> র্)
        for i in 0..len {
            if chars[i] == 'r' && i + 1 < len {
                let next = chars[i + 1];
                let is_consonant = match next {
                    'k' | 'g' | 'c' | 'j' | 't' | 'T' | 'd' | 'D' | 'n' | 'N' |
                    'p' | 'f' | 'b' | 'v' | 'm' | 'z' | 's' | 'S' | 'l' | 'h' => true,
                    _ => false,
                };
                if is_consonant {
                    let mut alt = chars.clone();
                    alt.insert(i + 1, 'r');
                    let alt_str: String = alt.into_iter().collect();
                    let translit = Self::convert_mode(&alt_str, is_avro);
                    if !translit.is_empty() && !results.contains(&translit) {
                        results.push(translit);
                    }
                }
            }
        }

        // ri <-> rri substitution (case-sensitive) for র-ফলা and ঋ-কার
        // e.g. kri -> krri (কৃ), rishi -> rrishi (ঋষি), rin -> rrin (ঋণ), mrit -> mrrit (মৃত)
        if input.contains("ri") {
            let alt_rri = input.replace("ri", "rri");
            let translit = Self::convert_mode(&alt_rri, is_avro);
            if !translit.is_empty() && !results.contains(&translit) {
                results.push(translit);
            }
            // Combined with n <-> N (e.g. rin -> rriN -> ঋণ)
            if alt_rri.contains('n') {
                let alt_rri_n = alt_rri.replace('n', "N");
                let translit_n = Self::convert_mode(&alt_rri_n, is_avro);
                if !translit_n.is_empty() && !results.contains(&translit_n) {
                    results.push(translit_n);
                }
            }
            // Combined with sh <-> Sh (e.g. rishi -> rriShi -> ঋষি)
            if alt_rri.contains("sh") {
                let alt_rri_sh = alt_rri.replace("sh", "Sh");
                let translit_sh = Self::convert_mode(&alt_rri_sh, is_avro);
                if !translit_sh.is_empty() && !results.contains(&translit_sh) {
                    results.push(translit_sh);
                }
            }
            // Combined with s <-> Sh (e.g. sristi -> srriSTi -> সৃষ্টি)
            if alt_rri.contains("st") {
                let alt_rri_st = alt_rri.replace("st", "ST");
                let translit_st = Self::convert_mode(&alt_rri_st, is_avro);
                if !translit_st.is_empty() && !results.contains(&translit_st) {
                    results.push(translit_st);
                }
            }
        }
        if input.contains("rri") {
            let alt_ri = input.replace("rri", "ri");
            let translit = Self::convert_mode(&alt_ri, is_avro);
            if !translit.is_empty() && !results.contains(&translit) {
                results.push(translit);
            }
        }

        // o <-> O variations (both middle of word e.g. jabona -> যাবোনা, and ending e.g. jabo -> যাবো)
        // Also combine with j <-> z (e.g. jabona -> zabOna -> যাবোনা, jabo -> zabO -> যাবো)
        if input.contains('o') {
            let alt_o = input.replace('o', "O");
            let translit = Self::convert_mode(&alt_o, is_avro);
            if !translit.is_empty() && !results.contains(&translit) {
                results.push(translit);
            }
            if alt_o.contains('j') {
                let alt_zo = alt_o.replace('j', "z");
                let translit_zo = Self::convert_mode(&alt_zo, is_avro);
                if !translit_zo.is_empty() && !results.contains(&translit_zo) {
                    results.push(translit_zo);
                }
            }
        }
        if input.contains('O') {
            let alt_small_o = input.replace('O', "o");
            let translit = Self::convert_mode(&alt_small_o, is_avro);
            if !translit.is_empty() && !results.contains(&translit) {
                results.push(translit);
            }
            if alt_small_o.contains('j') {
                let alt_zo = alt_small_o.replace('j', "z");
                let translit_zo = Self::convert_mode(&alt_zo, is_avro);
                if !translit_zo.is_empty() && !results.contains(&translit_zo) {
                    results.push(translit_zo);
                }
            }
        }

        results
    }
}

pub struct BornoPhoneticTransliterator;

static BORNO_TRANSLITERATOR_INSTANCE: BornoPhoneticTransliterator = BornoPhoneticTransliterator;

pub fn get_borno_transliterator() -> &'static BornoPhoneticTransliterator {
    &BORNO_TRANSLITERATOR_INSTANCE
}

impl crate::translit::traits::PhoneticTransliterator for BornoPhoneticTransliterator {
    fn id(&self) -> &'static str {
        "bn_borno"
    }

    fn target_dict_lang(&self) -> &'static str {
        "bn"
    }

    fn transliterate(&self, input: &str) -> String {
        BornoPhonetic::convert(input)
    }

    fn to_phonetic_keys(&self, word: &str) -> Vec<Vec<char>> {
        BornoPhonetic::to_phonetic_keys(word)
    }

    fn proximity(&self) -> Option<&'static crate::dictionary::proximity::KeyProximity> {
        Some(crate::dictionary::proximity::KeyProximity::borno_phonetic_cached())
    }
}

pub struct AvroPhoneticTransliterator;

static AVRO_TRANSLITERATOR_INSTANCE: AvroPhoneticTransliterator = AvroPhoneticTransliterator;

pub fn get_avro_transliterator() -> &'static AvroPhoneticTransliterator {
    &AVRO_TRANSLITERATOR_INSTANCE
}

impl crate::translit::traits::PhoneticTransliterator for AvroPhoneticTransliterator {
    fn id(&self) -> &'static str {
        "bn_avro"
    }

    fn target_dict_lang(&self) -> &'static str {
        "bn"
    }

    fn transliterate(&self, input: &str) -> String {
        BornoPhonetic::convert_avro(input)
    }

    fn to_phonetic_keys(&self, word: &str) -> Vec<Vec<char>> {
        BornoPhonetic::to_phonetic_keys(word)
    }

    fn proximity(&self) -> Option<&'static crate::dictionary::proximity::KeyProximity> {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_borno_conversions() {
        assert_eq!(BornoPhonetic::convert("ami"), "আমি");
        assert_eq!(BornoPhonetic::convert("bhalO"), "ভালো");
        assert_eq!(BornoPhonetic::convert("bhalo"), "ভাল");
        assert_eq!(BornoPhonetic::convert("kemon"), "কেমন");
        assert_eq!(BornoPhonetic::convert("dhonyobad"), "ধন্যবাদ");
        assert_eq!(BornoPhonetic::convert("dhonnobad"), "ধন্নবাদ");
        assert_eq!(BornoPhonetic::convert("bangla"), "বাংলা");
        assert_eq!(BornoPhonetic::convert("sobai"), "সবাই");
        assert_eq!(BornoPhonetic::convert("shanti"), "শান্তি");

        // Borno vs Avro phonetic rules
        assert_eq!(BornoPhonetic::convert("mia"), "মিআ");
        assert_eq!(BornoPhonetic::convert("vaia"), "ভাইয়া");
        assert_eq!(BornoPhonetic::convert("mia vaia"), "মিআ ভাইয়া");
        assert_eq!(BornoPhonetic::convert("mua"), "মুআ");
        assert_eq!(BornoPhonetic::convert("aa"), "আআ");
        assert_eq!(BornoPhonetic::convert("ia"), "ইআ");
        assert_eq!(BornoPhonetic::convert("ka ba"), "কা বা");
    }

    #[test]
    fn test_conjunct_and_hasanta() {
        assert_eq!(BornoPhonetic::convert("shanto"), "শান্ত");
        assert_eq!(BornoPhonetic::convert("kkhoma"), "ক্ষমা");
        assert_eq!(BornoPhonetic::convert("jNGan"), "জ্ঞান");
        assert_eq!(BornoPhonetic::convert("sth"), "স্থ");
        assert_eq!(BornoPhonetic::convert("sthan"), "স্থান");
        assert_eq!(BornoPhonetic::convert("sthiti"), "স্থিতি");
        assert_eq!(BornoPhonetic::convert("sTeson"), "স্টেসন");
        assert_eq!(BornoPhonetic::convert("sTeshon"), "স্টেশন");
        assert_eq!(BornoPhonetic::convert("ShTh"), "ষ্ঠ");
        assert_eq!(BornoPhonetic::convert("hN"), "হ্ণ");
        assert_eq!(BornoPhonetic::convert("hn"), "হ্ন");
        assert_eq!(BornoPhonetic::convert("hm"), "হ্ম");
        assert_eq!(BornoPhonetic::convert("hl"), "হ্ল");
        assert_eq!(BornoPhonetic::convert("mth"), "ম্থ");
        assert_eq!(BornoPhonetic::convert("mbh"), "ম্ভ");
        assert_eq!(BornoPhonetic::convert("ddh"), "দ্ধ");
        assert_eq!(BornoPhonetic::convert("sph"), "স্ফ");
        assert_eq!(BornoPhonetic::convert("skh"), "স্খ");
    }

    #[test]
    fn test_to_phonetic_keys() {
        let ami_keys = BornoPhonetic::to_phonetic_keys("আমি");
        assert_eq!(ami_keys, vec![vec!['a', 'm', 'i']]);

        let amader_keys = BornoPhonetic::to_phonetic_keys("আমাদের");
        assert!(amader_keys.contains(&vec!['a', 'm', 'a', 'd', 'e', 'r']));

        let tumi_keys = BornoPhonetic::to_phonetic_keys("তুমি");
        assert_eq!(tumi_keys, vec![vec!['t', 'u', 'm', 'i']]);

        let bhalo_keys = BornoPhonetic::to_phonetic_keys("ভালো");
        assert!(bhalo_keys.contains(&vec!['b', 'h', 'a', 'l', 'o']));
        assert!(bhalo_keys.contains(&vec!['v', 'a', 'l', 'o']));

        let kemon_keys = BornoPhonetic::to_phonetic_keys("কেমন");
        assert!(kemon_keys.contains(&vec!['k', 'e', 'm', 'o', 'n']));

        let dhonyobad_keys = BornoPhonetic::to_phonetic_keys("ধন্যবাদ");
        assert!(dhonyobad_keys.contains(&vec!['d', 'h', 'o', 'n', 'y', 'b', 'a', 'd'])
            || dhonyobad_keys.contains(&vec!['d', 'h', 'o', 'n', 'y', 'o', 'b', 'a', 'd'])
            || dhonyobad_keys.contains(&vec!['d', 'h', 'n', 'y', 'b', 'a', 'd']));

        let bangla_keys = BornoPhonetic::to_phonetic_keys("বাংলা");
        assert!(bangla_keys.contains(&vec!['b', 'a', 'n', 'g', 'l', 'a']));
    }

    #[test]
    fn test_suggest_transliterations() {
        // Tumi (uppercase T) should suggest "তুমি" (lowercase t)
        let tumi_sugs = BornoPhonetic::suggest_transliterations("Tumi");
        assert!(tumi_sugs.contains(&"টুমি".to_string()));
        assert!(tumi_sugs.contains(&"তুমি".to_string()));

        // jabO should suggest "যাবো" (zabO)
        let jabo_sugs = BornoPhonetic::suggest_transliterations("jabO");
        assert!(jabo_sugs.contains(&"জাবো".to_string()));
        assert!(jabo_sugs.contains(&"যাবো".to_string()));

        // karon should suggest "কারণ" (karoN)
        let karon_sugs = BornoPhonetic::suggest_transliterations("karon");
        assert!(karon_sugs.contains(&"কারন".to_string()));
        assert!(karon_sugs.contains(&"কারণ".to_string()));

        // dekha should suggest "দাখা" (dakha)
        let dekha_sugs = BornoPhonetic::suggest_transliterations("dekha");
        assert!(dekha_sugs.contains(&"দেখা".to_string()));
        assert!(dekha_sugs.contains(&"দাখা".to_string()));

        // jabo (all lowercase) should suggest both "যাব" and "যাবো"
        let jabo_lower = BornoPhonetic::suggest_transliterations("jabo");
        assert!(jabo_lower.contains(&"যাব".to_string()));
        assert!(jabo_lower.contains(&"যাবো".to_string()));

        // kormo should suggest "কর্ম" via Reph substitution
        let kormo_sugs = BornoPhonetic::suggest_transliterations("kormo");
        assert!(kormo_sugs.contains(&"কর্ম".to_string()));

        // jabona should suggest "যাবোনা"
        let jabona_sugs = BornoPhonetic::suggest_transliterations("jabona");
        assert!(jabona_sugs.contains(&"যাবোনা".to_string()), "Expected 'যাবোনা' in suggestions for 'jabona': {:?}", jabona_sugs);

        // kri should suggest "কৃ" (ঋ-kar)
        let kri_sugs = BornoPhonetic::suggest_transliterations("kri");
        assert!(kri_sugs.contains(&"কৃ".to_string()), "Expected 'কৃ' in suggestions for 'kri': {:?}", kri_sugs);

        // rishi should suggest "ঋষি"
        let rishi_sugs = BornoPhonetic::suggest_transliterations("rishi");
        assert!(rishi_sugs.contains(&"ঋষি".to_string()), "Expected 'ঋষি' in suggestions for 'rishi': {:?}", rishi_sugs);

        // rin should suggest "ঋণ"
        let rin_sugs = BornoPhonetic::suggest_transliterations("rin");
        assert!(rin_sugs.contains(&"ঋণ".to_string()), "Expected 'ঋণ' in suggestions for 'rin': {:?}", rin_sugs);

        // mrit should suggest "মৃত"
        let mrit_sugs = BornoPhonetic::suggest_transliterations("mrit");
        assert!(mrit_sugs.contains(&"মৃত".to_string()), "Expected 'মৃত' in suggestions for 'mrit': {:?}", mrit_sugs);

        // krri should suggest "ক্রি" (র ফলা and ি-kar)
        let krri_sugs = BornoPhonetic::suggest_transliterations("krri");
        assert!(krri_sugs.contains(&"ক্রি".to_string()), "Expected 'ক্রি' in suggestions for 'krri': {:?}", krri_sugs);

        // Direct Bengali diacritics / characters within transliteration input: ৎ, ঁ, ঃ
        assert_eq!(BornoPhonetic::convert("caঁd"), "চাঁদ");
        assert_eq!(BornoPhonetic::convert("uৎsob"), "উৎসব");
        assert_eq!(BornoPhonetic::convert("duঃkho"), "দুঃখ");
        assert_eq!(BornoPhonetic::convert("hoThaৎ"), "হঠাৎ");
    }

    #[test]
    fn test_avro_conversions() {
        assert_eq!(BornoPhonetic::convert_avro("ami"), "আমি");
        assert_eq!(BornoPhonetic::convert_avro("bhalO"), "ভালো");

        // Avro vowel-after-vowel rules
        assert_eq!(BornoPhonetic::convert_avro("mia"), "মিয়া");
        assert_eq!(BornoPhonetic::convert_avro("vaia"), "ভাইয়া");
        assert_eq!(BornoPhonetic::convert_avro("mia vaia"), "মিয়া ভাইয়া");
        assert_eq!(BornoPhonetic::convert_avro("mua"), "মুয়া");
        assert_eq!(BornoPhonetic::convert_avro("aa"), "আআ");
        assert_eq!(BornoPhonetic::convert_avro("ia"), "ইয়া");
        assert_eq!(BornoPhonetic::convert_avro("ka ba"), "কা বা");

        // Avro specific: 'cb' and 'qq' for chandrabindu
        assert_eq!(BornoPhonetic::convert_avro("cacbd"), "চাঁদ");
        assert_eq!(BornoPhonetic::convert_avro("caqqd"), "চাঁদ");

        // Avro specific: 'HH' for bishorgo
        assert_eq!(BornoPhonetic::convert_avro("duHHkho"), "দুঃখ");

        // Avro specific: 'hs' for hasanta
        assert_eq!(BornoPhonetic::convert_avro("mhs"), "ম্");

        // Capital 'A' in Avro is vowel, NOT chandrabindu
        assert_ne!(BornoPhonetic::convert_avro("caAd"), "চাঁদ");
        // In Borno, capital 'A' after consonant is chandrabindu
        assert_eq!(BornoPhonetic::convert("caAd"), "চাঁদ");
    }
}
