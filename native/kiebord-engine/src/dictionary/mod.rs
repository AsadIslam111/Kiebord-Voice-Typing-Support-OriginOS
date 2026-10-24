pub mod binary_dict;
pub mod combined_parser;
pub mod proximity;

use binary_dict::BinaryDict;
use combined_parser::CombinedParser;
use proximity::KeyProximity;
use std::path::Path;
use std::sync::OnceLock;

static EN_PROXIMITY: OnceLock<KeyProximity> = OnceLock::new();
static BN_PROXIMITY: OnceLock<KeyProximity> = OnceLock::new();

pub fn get_en_proximity() -> &'static KeyProximity {
    EN_PROXIMITY.get_or_init(KeyProximity::qwerty)
}

pub fn get_bn_proximity() -> &'static KeyProximity {
    BN_PROXIMITY.get_or_init(KeyProximity::bengali)
}

static EN_BINARY_DICT: OnceLock<BinaryDict<'static>> = OnceLock::new();
static BN_BINARY_DICT: OnceLock<BinaryDict<'static>> = OnceLock::new();

/// Loads a compiled .dict file from disk via zero-copy memory mapping.
pub fn load_dictionary_path(lang: &str, path: &Path) -> bool {
    let file = match std::fs::File::open(path) {
        Ok(f) => f,
        Err(_) => return false,
    };
    let mmap = match unsafe { memmap2::Mmap::map(&file) } {
        Ok(m) => m,
        Err(_) => return false,
    };
    let mmap_ref: &'static memmap2::Mmap = Box::leak(Box::new(mmap));
    let static_slice: &'static [u8] = &mmap_ref[..];
    if let Ok(dict) = BinaryDict::from_bytes(static_slice) {
        match lang {
            "bn" | "bn_borno" | "bn_probhat" | "bn_national" => {
                let _ = BN_BINARY_DICT.set(dict);
                true
            }
            _ => {
                let _ = EN_BINARY_DICT.set(dict);
                true
            }
        }
    } else {
        false
    }
}

pub fn get_en_binary_dict() -> Option<&'static BinaryDict<'static>> {
    if let Some(d) = EN_BINARY_DICT.get() {
        return Some(d);
    }
    let candidates = [
        "../../feature/ime/src/main/assets/dictionaries/en.dict",
        "feature/ime/src/main/assets/dictionaries/en.dict",
        "assets/dictionaries/en.dict",
    ];
    for p in candidates {
        let path = Path::new(p);
        if path.exists() {
            if load_dictionary_path("en", path) {
                return EN_BINARY_DICT.get();
            }
        }
    }
    None
}

pub fn get_bn_binary_dict() -> Option<&'static BinaryDict<'static>> {
    if let Some(d) = BN_BINARY_DICT.get() {
        return Some(d);
    }
    let candidates = [
        "../../feature/ime/src/main/assets/dictionaries/bn.dict",
        "feature/ime/src/main/assets/dictionaries/bn.dict",
        "assets/dictionaries/bn.dict",
    ];
    for p in candidates {
        let path = Path::new(p);
        if path.exists() {
            if load_dictionary_path("bn", path) {
                return BN_BINARY_DICT.get();
            }
        }
    }
    None
}

/// Compiles a `.combined` file into a binary `.dict` file.
pub fn compile_combined_file(src_path: &Path, out_path: &Path) -> std::io::Result<()> {
    let content = std::fs::read_to_string(src_path)?;
    let parsed = CombinedParser::parse_str(&content);
    let bytes = BinaryDict::compile_combined(&parsed);

    if let Some(parent) = out_path.parent() {
        std::fs::create_dir_all(parent)?;
    }
    std::fs::write(out_path, bytes)?;
    Ok(())
}

/// Generates .dict binary files directly from .combined sources.
pub fn generate_dict_assets(combined_dir: &Path, out_dir: &Path) -> std::io::Result<()> {
    std::fs::create_dir_all(out_dir)?;

    let en_src = combined_dir.join("en.combined");
    let en_out = out_dir.join("en.dict");
    compile_combined_file(&en_src, &en_out)?;

    let bn_src = combined_dir.join("bn.combined");
    let bn_out = out_dir.join("bn.dict");
    compile_combined_file(&bn_src, &bn_out)?;

    Ok(())
}

/// Main entry point for query suggestions: exact completions + proximity typo corrections.
/// Returns a pipe-delimited string of suggestions, e.g. "the|that|this"
pub fn get_suggestions(input: &str, lang_or_layout: &str, max_results: usize) -> String {
    let is_bn = lang_or_layout.starts_with("bn");
    let dict_opt = if is_bn {
        get_bn_binary_dict()
    } else {
        get_en_binary_dict()
    };
    let prox = KeyProximity::for_layout(lang_or_layout);

    if let Some(dict) = dict_opt {
        let mut words = dict.suggest(input, max_results, prox);

        // If Borno Phonetic layout and room remains, also evaluate phonetic transliteration alternatives
        if words.len() < max_results && lang_or_layout == "bn_borno" {
            let alts = crate::translit::borno_phonetic::BornoPhonetic::suggest_transliterations(input);
            for alt in alts {
                if alt != input && !words.contains(&alt) && dict.contains_word(&alt) {
                    words.push(alt);
                    if words.len() >= max_results {
                        break;
                    }
                }
            }
        }

        // Strict language separation: purge cross-language words
        if is_bn {
            words.retain(|w| !w.chars().any(|c| c.is_ascii_alphabetic()));
        } else {
            words.retain(|w| !w.chars().any(|c| ('\u{0980}'..='\u{09FF}').contains(&c)));
        }

        words.join("|")
    } else {
        String::new()
    }
}

/// Main entry point for next-word prediction based on previous committed word.
/// Returns a pipe-delimited string of predicted words, e.g. "are|is|do"
pub fn predict_next_words(prev_word: &str, lang_or_layout: &str, max_results: usize) -> String {
    let is_bn = lang_or_layout.starts_with("bn");
    let dict_opt = if is_bn {
        get_bn_binary_dict()
    } else {
        get_en_binary_dict()
    };

    if let Some(dict) = dict_opt {
        let mut words = dict.predict_next(prev_word, max_results);
        if is_bn {
            words.retain(|w| !w.chars().any(|c| c.is_ascii_alphabetic()));
        } else {
            words.retain(|w| !w.chars().any(|c| ('\u{0980}'..='\u{09FF}').contains(&c)));
        }
        words.join("|")
    } else {
        String::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_en_exact_and_proximity() {
        let dict = get_en_binary_dict().expect("dict loaded");
        let prox = get_en_proximity();

        // Exact prefix
        let th_sug = dict.suggest("th", 5, prox);
        assert!(!th_sug.is_empty());
        assert_eq!(th_sug[0], "the");

        // Typo: neighbor key slip ('g' instead of 'h' in 'gelo' -> 'hello')
        let gelo_sug = dict.suggest("gelo", 10, prox);
        assert!(!gelo_sug.is_empty());
        assert!(gelo_sug.contains(&"hello".to_string()));

        // Typo: omission ('helo' -> 'hello')
        let helo_sug = dict.suggest("helo", 5, prox);
        assert!(!helo_sug.is_empty());
        assert!(helo_sug.contains(&"hello".to_string()));

        // Typo: transposition ('hte' -> 'the')
        let hte_sug = dict.suggest("hte", 3, prox);
        assert!(!hte_sug.is_empty());
        assert_eq!(hte_sug[0], "the");
    }

    #[test]
    fn test_suggestions_ranked_by_frequency() {
        let dict = get_en_binary_dict().expect("en dict loaded");
        let prox = get_en_proximity();

        // If nothing is typed (empty input), suggest returns empty
        let empty_sug = dict.suggest("", 5, prox);
        assert!(empty_sug.is_empty());

        // Exact prefix suggestions for "th" must be sorted with highest frequency first
        let th_sug = dict.suggest("th", 5, prox);
        assert_eq!(th_sug[0], "the"); // "the" has max frequency 255
    }

    #[test]
    fn test_next_word_prediction() {
        // English bigrams
        let after_thank = predict_next_words("thank", "en", 3);
        assert!(!after_thank.is_empty());
        let thank_items: Vec<&str> = after_thank.split('|').collect();
        assert_eq!(thank_items[0], "you");

        let after_how = predict_next_words("how", "en", 3);
        assert!(!after_how.is_empty());

        // Bengali bigrams from corpus format
        let after_amader = predict_next_words("আমাদের", "bn", 3);
        assert_eq!(after_amader, "এই|আগ্রহের|দেশে");
    }

    #[test]
    fn test_en_casing() {
        let dict = get_en_binary_dict().expect("dict loaded");
        let prox = get_en_proximity();

        let title_sug = dict.suggest("Th", 3, prox);
        assert_eq!(title_sug[0], "The");

        let upper_sug = dict.suggest("TH", 3, prox);
        assert_eq!(upper_sug[0], "THE");
    }

    #[test]
    fn test_bn_proximity_and_typos() {
        let dict = get_bn_binary_dict().expect("dict loaded");
        let prox = get_bn_proximity();

        // Exact prefix
        let bn_sug = dict.suggest("আম", 10, prox);
        assert!(!bn_sug.is_empty());
        assert!(bn_sug.contains(&"আমি".to_string()));

        // Typo: missing kar 'আমদের' -> 'আমাদের'
        let typo_sug = dict.suggest("আমদের", 3, prox);
        assert!(!typo_sug.is_empty());
        assert_eq!(typo_sug[0], "আমাদের");
    }

    #[test]
    fn test_pipe_delimited_output() {
        let en_pipe = get_suggestions("hte", "en", 3);
        let items: Vec<&str> = en_pipe.split('|').collect();
        assert_eq!(items[0], "the");

        let bn_pipe = get_suggestions("আম", "bn", 5);
        assert!(!bn_pipe.is_empty());
        let bn_items: Vec<&str> = bn_pipe.split('|').collect();
        assert!(bn_items.contains(&"আমি") || bn_items.contains(&"আম"));
    }

    #[test]
    fn test_borno_phonetic_layout_suggestions() {
        // 'টুমি' (typed with uppercase T) should suggest 'তুমি' (with lowercase t) in Borno Phonetic layout
        let tumi_sugs = get_suggestions("টুমি", "bn_borno", 5);
        let items: Vec<&str> = tumi_sugs.split('|').filter(|s| !s.is_empty()).collect();
        assert!(items.contains(&"তুমি"), "Expected 'তুমি' in suggestions for 'টুমি': {:?}", items);

        // 'কারন' (typed with lowercase n) should suggest 'কারণ' (with uppercase N) in Borno Phonetic layout
        let karon_sugs = get_suggestions("কারন", "bn_borno", 5);
        let k_items: Vec<&str> = karon_sugs.split('|').filter(|s| !s.is_empty()).collect();
        assert!(k_items.contains(&"কারণ"), "Expected 'কারণ' in suggestions for 'কারন': {:?}", k_items);
    }
}
