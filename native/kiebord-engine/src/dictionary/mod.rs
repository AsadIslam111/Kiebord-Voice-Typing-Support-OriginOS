pub mod data_bn;
pub mod data_en;

use data_bn::BN_WORDS;
use data_en::EN_WORDS;
use std::collections::HashMap;
use std::sync::OnceLock;

/// Node in the dictionary prefix trie.
#[derive(Default)]
pub struct TrieNode {
    pub is_end: bool,
    pub children: HashMap<char, TrieNode>,
    /// Word indices into the static word array, guaranteed ordered by frequency descending.
    pub top_indices: Vec<usize>,
}

pub struct Dictionary {
    words: &'static [(&'static str, u32)],
    root: TrieNode,
}

impl Dictionary {
    pub fn new(words: &'static [(&'static str, u32)]) -> Self {
        let mut root = TrieNode::default();

        // The words array is pre-sorted by frequency descending.
        // Therefore, appending indices preserves descending frequency order.
        for (idx, (word, _freq)) in words.iter().enumerate() {
            let mut curr = &mut root;
            if curr.top_indices.len() < 10 {
                curr.top_indices.push(idx);
            }

            for ch in word.chars() {
                curr = curr.children.entry(ch).or_default();
                if curr.top_indices.len() < 10 {
                    curr.top_indices.push(idx);
                }
            }
            curr.is_end = true;
        }

        Self { words, root }
    }

    /// Search words by prefix with a max limit.
    pub fn suggest(&self, prefix: &str, limit: usize) -> Vec<String> {
        let trimmed = prefix.trim();
        if trimmed.is_empty() {
            // Return top frequent words
            return self
                .root
                .top_indices
                .iter()
                .take(limit)
                .map(|&idx| self.words[idx].0.to_string())
                .collect();
        }

        // Determine casing style for English (lowercase, Titlecase, UPPERCASE)
        let is_all_upper = trimmed.chars().all(|c| !c.is_alphabetic() || c.is_uppercase())
            && trimmed.chars().any(|c| c.is_alphabetic());
        let is_title_case = !is_all_upper
            && trimmed
                .chars()
                .next()
                .map(|c| c.is_uppercase())
                .unwrap_or(false);

        let search_prefix = trimmed.to_lowercase();
        let mut curr = &self.root;

        for ch in search_prefix.chars() {
            match curr.children.get(&ch) {
                Some(next) => curr = next,
                None => return Vec::new(),
            }
        }

        let mut results = Vec::new();
        for &idx in curr.top_indices.iter().take(limit) {
            let word = self.words[idx].0;
            let formatted = if is_all_upper {
                word.to_uppercase()
            } else if is_title_case {
                let mut c = word.chars();
                match c.next() {
                    None => String::new(),
                    Some(first) => first.to_uppercase().collect::<String>() + c.as_str(),
                }
            } else {
                word.to_string()
            };
            results.push(formatted);
        }

        results
    }
}

static EN_DICT: OnceLock<Dictionary> = OnceLock::new();
static BN_DICT: OnceLock<Dictionary> = OnceLock::new();

pub fn get_en_dictionary() -> &'static Dictionary {
    EN_DICT.get_or_init(|| Dictionary::new(EN_WORDS))
}

pub fn get_bn_dictionary() -> &'static Dictionary {
    BN_DICT.get_or_init(|| Dictionary::new(BN_WORDS))
}

/// Main entry point for query suggestions.
/// Returns a pipe-delimited string of suggestions, e.g. "আমি|আমার|আমাদের"
pub fn get_suggestions(prefix: &str, lang: &str, max_results: usize) -> String {
    let dict = match lang {
        "bn" | "bn_borno" | "bn_probhat" => get_bn_dictionary(),
        _ => get_en_dictionary(),
    };

    let words = dict.suggest(prefix, max_results);
    words.join("|")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_en_dictionary_prefix() {
        let dict = get_en_dictionary();
        let suggestions = dict.suggest("th", 5);
        assert!(!suggestions.is_empty());
        assert!(suggestions.contains(&"the".to_string()));
        assert!(suggestions.contains(&"that".to_string()));
        assert!(suggestions.contains(&"this".to_string()));
    }

    #[test]
    fn test_en_casing() {
        let dict = get_en_dictionary();
        let title_sug = dict.suggest("Th", 3);
        assert_eq!(title_sug[0], "The");

        let upper_sug = dict.suggest("TH", 3);
        assert_eq!(upper_sug[0], "THE");
    }

    #[test]
    fn test_bn_dictionary_prefix() {
        let dict = get_bn_dictionary();
        let suggestions = dict.suggest("আম", 5);
        assert!(!suggestions.is_empty());
        assert!(suggestions.contains(&"আমি".to_string()));
        assert!(suggestions.contains(&"আমার".to_string()));
        assert!(suggestions.contains(&"আমাদের".to_string()));
    }

    #[test]
    fn test_empty_prefix_defaults() {
        let dict_en = get_en_dictionary();
        let top_en = dict_en.suggest("", 3);
        assert_eq!(top_en.len(), 3);
        assert_eq!(top_en[0], "you");
        assert_eq!(top_en[1], "i");
        assert_eq!(top_en[2], "the");

        let dict_bn = get_bn_dictionary();
        let top_bn = dict_bn.suggest("", 3);
        assert_eq!(top_bn.len(), 3);
        assert_eq!(top_bn[0], "আমি");
        assert_eq!(top_bn[1], "তুমি");
        assert_eq!(top_bn[2], "আপনি");
    }

    #[test]
    fn test_get_suggestions_pipe_delimited() {
        let en_pipe = get_suggestions("th", "en", 3);
        let items: Vec<&str> = en_pipe.split('|').collect();
        assert_eq!(items.len(), 3);
        assert_eq!(items[0], "the");

        let bn_pipe = get_suggestions("আম", "bn", 3);
        let bn_items: Vec<&str> = bn_pipe.split('|').collect();
        assert_eq!(bn_items.len(), 3);
        assert_eq!(bn_items[0], "আমি");
    }
}
