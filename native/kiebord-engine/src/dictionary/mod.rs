pub mod binary_dict;
pub mod data_bn;
pub mod data_en;
pub mod proximity;

use binary_dict::BinaryDict;
use data_bn::BN_WORDS;
use data_en::EN_WORDS;
use proximity::KeyProximity;
use std::sync::OnceLock;

static EN_PROXIMITY: OnceLock<KeyProximity> = OnceLock::new();
static BN_PROXIMITY: OnceLock<KeyProximity> = OnceLock::new();

pub fn get_en_proximity() -> &'static KeyProximity {
    EN_PROXIMITY.get_or_init(KeyProximity::qwerty)
}

pub fn get_bn_proximity() -> &'static KeyProximity {
    BN_PROXIMITY.get_or_init(KeyProximity::bengali)
}

static EN_DICT_BYTES: OnceLock<Vec<u8>> = OnceLock::new();
static BN_DICT_BYTES: OnceLock<Vec<u8>> = OnceLock::new();

static EN_BINARY_DICT: OnceLock<BinaryDict<'static>> = OnceLock::new();
static BN_BINARY_DICT: OnceLock<BinaryDict<'static>> = OnceLock::new();

pub fn get_en_binary_dict() -> &'static BinaryDict<'static> {
    EN_BINARY_DICT.get_or_init(|| {
        let bytes_ref = EN_DICT_BYTES.get_or_init(|| BinaryDict::compile(EN_WORDS));
        let static_bytes: &'static [u8] = unsafe { &*(bytes_ref.as_slice() as *const [u8]) };
        BinaryDict::from_bytes(static_bytes).expect("Valid compiled EN binary dict")
    })
}

pub fn get_bn_binary_dict() -> &'static BinaryDict<'static> {
    BN_BINARY_DICT.get_or_init(|| {
        let bytes_ref = BN_DICT_BYTES.get_or_init(|| BinaryDict::compile(BN_WORDS));
        let static_bytes: &'static [u8] = unsafe { &*(bytes_ref.as_slice() as *const [u8]) };
        BinaryDict::from_bytes(static_bytes).expect("Valid compiled BN binary dict")
    })
}

/// Generates .dict binary files for disk storage / Android APK assets.
pub fn generate_dict_assets(out_dir: &std::path::Path) -> std::io::Result<()> {
    use std::fs;
    fs::create_dir_all(out_dir)?;

    let en_bytes = BinaryDict::compile(EN_WORDS);
    fs::write(out_dir.join("en.dict"), en_bytes)?;

    let bn_bytes = BinaryDict::compile(BN_WORDS);
    fs::write(out_dir.join("bn.dict"), bn_bytes)?;

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

        // Typo: neighbor key slip on end ('widt' -> 'with')
        let widt_sug = dict.suggest("widt", 3, prox);
        assert!(widt_sug.contains(&"with".to_string()));
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
        assert!(bn_sug.contains(&"আমাদের".to_string()));

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

    #[test]
    fn test_dict_file_generation() {
        let temp_dir = std::env::temp_dir().join("borno_dict_test");
        generate_dict_assets(&temp_dir).expect("Generate dict assets");
        assert!(temp_dir.join("en.dict").exists());
        assert!(temp_dir.join("bn.dict").exists());
        let _ = std::fs::remove_dir_all(temp_dir);
    }
}
