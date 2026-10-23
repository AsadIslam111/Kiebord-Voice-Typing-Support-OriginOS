//! Complete, clean-room port of Jayed Ahsan's BornoPhonetic transliterator from BornoTSF.
//! Reference: https://github.com/JayedAhsan/BornoTSF/blob/main/layoutParsers/Transliterators/BornoPhonetic.hpp

use std::collections::{HashMap, HashSet};

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
        Self::parse(query)
    }

    pub fn parse(input: &str) -> String {
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
                    1 => format!("{}\\", Self::parse_segment(left_text)),
                    2 => left_text.to_string(),
                    3 => format!("{}\\", left_text),
                    4 => format!("{}\\\\", Self::parse_segment(left_text)),
                    _ if k % 2 == 1 => format!("{}{}", left_text, "\\".repeat((k - 1) / 2)),
                    _ => format!("{}{}", Self::parse_segment(left_text), "\\".repeat(k / 2)),
                }
            } else {
                match k {
                    1 => format!("{}\\{}", Self::parse_segment(left_text), Self::parse(right_text)),
                    2 => format!("{}{}", left_text, Self::parse(right_text)),
                    3 => format!("{}\\{}", left_text, Self::parse(right_text)),
                    4 => format!("{}\\\\{}", Self::parse_segment(left_text), Self::parse(right_text)),
                    _ if k % 2 == 1 => format!("{}{}{}", left_text, "\\".repeat((k - 1) / 2), Self::parse(right_text)),
                    _ => format!("{}{}{}", Self::parse_segment(left_text), "\\".repeat(k / 2), Self::parse(right_text)),
                }
            }
        } else {
            Self::parse_segment(input)
        }
    }

    fn parse_segment(input: &str) -> String {
        if input.is_empty() {
            return String::new();
        }

        let corrected = Self::case_handle(input);
        let tokens = Self::tokenize(&corrected);
        let mut output = String::with_capacity(corrected.len() * 3);
        let mut last_type = PrevTokenType::StartOfWord;

        let phola_map = Self::build_phola_map();
        let consonants_map = Self::build_consonants_map();
        let conjuncts_map = Self::build_conjuncts_map();
        let symbol_map = Self::build_symbol_map();
        let diacritics_map = Self::build_diacritics_map();
        let punctuations_map = Self::build_punctuations_map();
        let digits_map = Self::build_digits_map();
        let initial_map = Self::build_initial_map();
        let vowel_signs_map = Self::build_vowel_signs_map();
        let after_vowel_map = Self::build_after_vowel_map();
        let reph_map = Self::build_reph_map();

        let mut i = 0;
        let token_count = tokens.len();

        while i < token_count {
            let token = &tokens[i];

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

            if let Some(val) = symbol_map.get(token.as_str()) {
                output.push_str(val);
                last_type = PrevTokenType::Symbol;
                i += 1;
                continue;
            }

            if let Some(val) = digits_map.get(token.as_str()) {
                output.push_str(val);
                last_type = PrevTokenType::Other;
                i += 1;
                continue;
            }

            if let Some(val) = punctuations_map.get(token.as_str()) {
                output.push_str(val);
                last_type = PrevTokenType::Other;
                i += 1;
                continue;
            }

            if let Some(val) = reph_map.get(token.as_str()) {
                if last_type == PrevTokenType::Consonant {
                    output.push_str(HOSHONTO);
                    output.push_str("রর");
                    last_type = PrevTokenType::Consonant;
                } else {
                    let mut cond_reph = false;
                    if i + 1 < token_count {
                        let next = &tokens[i + 1];
                        let next_is_vowel = initial_map.contains_key(next.as_str());
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

            if let Some(val) = diacritics_map.get(token.as_str()) {
                output.push_str(val);
                if token == ",," || token == ",." || token == ",.." {
                    last_type = PrevTokenType::Consonant;
                } else {
                    last_type = PrevTokenType::Vowel;
                }
                i += 1;
                continue;
            }

            if let Some(val) = conjuncts_map.get(token.as_str()) {
                output.push_str(val);
                last_type = PrevTokenType::Consonant;
                i += 1;
                continue;
            }

            if let Some(val) = phola_map.get(token.as_str()) {
                if last_type == PrevTokenType::Consonant {
                    if token == "y" || token == "z" || token == "Z" {
                        let prev_is_r = i > 0 && tokens[i - 1] == "r";
                        if prev_is_r {
                            let mut prev2_is_special = false;
                            if i >= 2 {
                                let prev2 = &tokens[i - 2];
                                if (consonants_map.contains_key(prev2.as_str()) || conjuncts_map.contains_key(prev2.as_str()))
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
                        let ra_phola = format!("{}{}", HOSHONTO, val);
                        if output.ends_with(&ra_phola) {
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

            if let Some(val) = consonants_map.get(token.as_str()) {
                output.push_str(val);
                last_type = PrevTokenType::Consonant;
                i += 1;
                continue;
            }

            if let Some(val) = initial_map.get(token.as_str()) {
                if last_type == PrevTokenType::Consonant {
                    if token == "o" {
                        // Inherent 'o' elision: empty string
                    } else if let Some(sign) = vowel_signs_map.get(token.as_str()) {
                        output.push_str(sign);
                    }
                } else if last_type == PrevTokenType::Vowel {
                    if token == "o" {
                        output.push_str("ও");
                    } else if let Some(after) = after_vowel_map.get(token.as_str()) {
                        output.push_str(after);
                    }
                } else {
                    output.push_str(val);
                }
                last_type = PrevTokenType::Vowel;
                i += 1;
                continue;
            }

            output.push_str(token);
            last_type = PrevTokenType::Other;
            i += 1;
        }

        Self::remove_consecutive_kar_signs(&output)
    }

    fn case_handle(input: &str) -> String {
        let mut result = String::with_capacity(input.len());
        for c in input.chars() {
            match c {
                'a' | 'A' | 'o' | 'O' | 'i' | 'I' | 'u' | 'U' | 'd' | 'D' | 'g' | 'G' | 'j' | 'J'
                | 'n' | 'N' | 'r' | 'R' | 's' | 'S' | 't' | 'T' | 'y' | 'Y' | 'z' | 'Z' | 'h'
                | 'H' | 'x' | 'X' => result.push(c),
                other => result.push(other.to_ascii_lowercase()),
            }
        }
        result
    }

    fn tokenize(text: &str) -> Vec<String> {
        let mut tokens = Vec::new();
        if text.is_empty() {
            return tokens;
        }

        let keys_set = Self::build_global_keys_set();
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
                                            tokens.push(shorter.to_string());
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
                                            tokens.push(shorter.to_string());
                                            i += l - 1;
                                            matched = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        tokens.push(segment.to_string());
                        i += l;
                        matched = true;
                        break;
                    }
                }
            }

            if !matched {
                if let Some(single) = text.get(i..i + 1) {
                    tokens.push(single.to_string());
                }
                i += 1;
            }
        }

        tokens
    }

    fn remove_consecutive_kar_signs(s: &str) -> String {
        let mut chars: Vec<char> = s.chars().collect();
        if chars.len() < 2 {
            return s.to_string();
        }

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

    fn build_symbol_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("ng", "ং"), ("TH", "ৎ"), ("X", "ৎ"), ("t``", "ৎ"),
            ("xx", "ঁ"), ("::", "ঃ")
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
            ("ns", "ন্স"), ("nc", "ঞ্চ"), ("cch", "চ্ছ"), ("cc", "চ্চ"),
            ("jjh", "জ্ঝ"), ("jNG", "জ্ঞ"), ("jj", "জ্জ"), ("tth", "ত্থ"),
            ("tn", "ত্ন"), ("tm", "ত্ম"), ("tt", "ত্ত"), ("TT", "ট্ট"),
            ("ddh", "দ্ধ"), ("dd", "দ্দ"), ("pp", "প্প"), ("pl", "প্ল"),
            ("bdh", "ব্ধ"), ("bb", "ব্ব"), ("bl", "ব্ল"), ("mm", "ম্ম"),
            ("ll", "ল্ল"), ("st", "স্ত"), ("sp", "স্প"), ("sk", "স্ক")
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
            (",,", HOSHONTO),
            (",.", "\u{09CD}\u{200C}"),
            (",..", "\u{09CD}\u{200D}"),
            ("./", "়")
        ])
    }

    fn build_punctuations_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            (".", "।"), ("..", "।।"), ("$", "৳"), ("$%", "₹")
        ])
    }

    fn build_digits_map() -> HashMap<&'static str, &'static str> {
        HashMap::from([
            ("0", "০"), ("1", "১"), ("2", "২"), ("3", "৩"), ("4", "৪"),
            ("5", "৫"), ("6", "৬"), ("7", "৭"), ("8", "৮"), ("9", "৯")
        ])
    }

    fn build_global_keys_set() -> HashSet<&'static str> {
        HashSet::from([
            "kkhN", "kShN", "kkhm", "kShm", "kxN", "kxm", "kkh", "kSh", "ksh", "kx",
            "kk", "kT", "kt", "km", "kl", "ks", "ghn", "Ghn", "gdh", "Gdh", "gN", "GN",
            "gn", "Gn", "gm", "Gm", "gl", "Gl", "gg", "GG", "Gg", "gG", "nch", "njh",
            "ndh", "nTh", "nth", "nj", "nn", "nm", "nd", "nT", "nD", "nt", "ns", "nc",
            "cch", "cc", "jjh", "jNG", "jj", "tth", "tn", "tm", "tt", "TT", "ddh", "dd",
            "pp", "pl", "bdh", "bb", "bl", "mm", "ll", "st", "sp", "sk", "kh", "gh",
            "ch", "th", "Th", "dh", "Dh", "ph", "bh", "sh", "Sh", "SH", "Rh", "ng", "TH",
            "X", "xx", "::", "rri", "OI", "OU", "rr", ",,", ",.", ",..", "./", "..",
            "$%", "a", "A", "i", "I", "u", "U", "e", "E", "o", "O", "k", "g", "G", "c",
            "j", "J", "t", "T", "d", "D", "n", "N", "p", "P", "f", "b", "B", "v", "m",
            "z", "Z", "y", "Y", "r", "R", "l", "s", "S", "h", "H", "q", "x", "w", ".",
            "$", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
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
                        let mut p1 = p.clone();
                        p1.extend(&c_variants[0]);
                        p1.push('o');
                        alt_paths.push(p1);

                        let mut p2 = p.clone();
                        p2.extend(&c_variants[0]);
                        alt_paths.push(p2);
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

        let mut unique: Vec<Vec<char>> = Vec::new();
        for p in paths {
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
        if input.is_empty() {
            return Vec::new();
        }

        let canonical = Self::convert(input);
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
                let translit = Self::convert(&alt_str);
                if !translit.is_empty() && !results.contains(&translit) {
                    results.push(translit);
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
    }

    #[test]
    fn test_conjunct_and_hasanta() {
        assert_eq!(BornoPhonetic::convert("shanto"), "শান্ত");
        assert_eq!(BornoPhonetic::convert("kkhoma"), "ক্ষমা");
        assert_eq!(BornoPhonetic::convert("jNGan"), "জ্ঞান");
    }

    #[test]
    fn test_to_phonetic_keys() {
        let ami_keys = BornoPhonetic::to_phonetic_keys("আমি");
        assert_eq!(ami_keys, vec![vec!['a', 'm', 'i']]);

        let amader_keys = BornoPhonetic::to_phonetic_keys("আমাদের");
        assert_eq!(amader_keys, vec![vec!['a', 'm', 'a', 'd', 'e', 'r']]);

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
        assert_eq!(bangla_keys, vec![vec!['b', 'a', 'n', 'g', 'l', 'a']]);
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
    }
}
