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

// Embedded compiled .dict assets for zero-overhead, zero-file-dependency runtime
static EN_DICT_BYTES: &[u8] = include_bytes!("../../../../feature/ime/src/main/assets/dictionaries/en.dict");
static BN_DICT_BYTES: &[u8] = include_bytes!("../../../../feature/ime/src/main/assets/dictionaries/bn.dict");

static EN_BINARY_DICT: OnceLock<BinaryDict<'static>> = OnceLock::new();
static BN_BINARY_DICT: OnceLock<BinaryDict<'static>> = OnceLock::new();

pub fn get_en_binary_dict() -> &'static BinaryDict<'static> {
    EN_BINARY_DICT.get_or_init(|| {
        BinaryDict::from_bytes(EN_DICT_BYTES).expect("Valid compiled EN binary dict")
    })
}

pub fn get_bn_binary_dict() -> &'static BinaryDict<'static> {
    BN_BINARY_DICT.get_or_init(|| {
        BinaryDict::from_bytes(BN_DICT_BYTES).expect("Valid compiled BN binary dict")
    })
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
pub fn get_suggestions(input: &str, lang: &str, max_results: usize) -> String {
    let (dict, prox) = match lang {
        "bn" | "bn_borno" | "bn_probhat" => (get_bn_binary_dict(), get_bn_proximity()),
        _ => (get_en_binary_dict(), get_en_proximity()),
    };

    let words = dict.suggest(input, max_results, prox);
    words.join("|")
}

/// Main entry point for next-word prediction based on previous committed word.
/// Returns a pipe-delimited string of predicted words, e.g. "are|is|do"
pub fn predict_next_words(prev_word: &str, lang: &str, max_results: usize) -> String {
    let dict = match lang {
        "bn" | "bn_borno" | "bn_probhat" => get_bn_binary_dict(),
        _ => get_en_binary_dict(),
    };

    let words = dict.predict_next(prev_word, max_results);
    words.join("|")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_en_exact_and_proximity() {
        let dict = get_en_binary_dict();
        let prox = get_en_proximity();

        // Exact prefix
        let th_sug = dict.suggest("th", 5, prox);
        assert!(!th_sug.is_empty());
        assert!(th_sug.contains(&"the".to_string()));

        // Typo: neighbor key slip ('g' instead of 'h' in 'gelo' -> 'hello')
        let gelo_sug = dict.suggest("gelo", 5, prox);
        assert!(!gelo_sug.is_empty());
        assert!(gelo_sug.contains(&"hello".to_string()));

        // Typo: omission ('helo' -> 'hello')
        let helo_sug = dict.suggest("helo", 3, prox);
        assert!(!helo_sug.is_empty());
        assert_eq!(helo_sug[0], "hello");

        // Typo: transposition ('hte' -> 'the')
        let hte_sug = dict.suggest("hte", 3, prox);
        assert!(!hte_sug.is_empty());
        assert_eq!(hte_sug[0], "the");
    }

    #[test]
    fn test_next_word_prediction() {
        // English bigrams
        let after_thank = predict_next_words("thank", "en", 3);
        assert_eq!(after_thank, "you|so|very");

        let after_how = predict_next_words("how", "en", 3);
        assert_eq!(after_how, "are|is|do");

        // Bengali bigrams from user's corpus format
        let after_amader = predict_next_words("আমাদের", "bn", 3);
        assert_eq!(after_amader, "এই|আগ্রহের|দেশে");

        let after_ami = predict_next_words("আমি", "bn", 3);
        assert_eq!(after_ami, "ভালো|আছি|করব");
    }

    #[test]
    fn test_en_casing() {
        let dict = get_en_binary_dict();
        let prox = get_en_proximity();

        let title_sug = dict.suggest("Th", 3, prox);
        assert_eq!(title_sug[0], "The");

        let upper_sug = dict.suggest("TH", 3, prox);
        assert_eq!(upper_sug[0], "THE");
    }

    #[test]
    fn test_bn_proximity_and_typos() {
        let dict = get_bn_binary_dict();
        let prox = get_bn_proximity();

        // Exact prefix
        let bn_sug = dict.suggest("আম", 5, prox);
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

        let bn_pipe = get_suggestions("আম", "bn", 3);
        assert!(bn_pipe.contains("আমি"));
    }
}
