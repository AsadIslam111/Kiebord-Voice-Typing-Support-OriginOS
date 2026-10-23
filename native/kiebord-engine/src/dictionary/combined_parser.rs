use std::collections::HashMap;

/// Parsed unigram and bigram data from a `.combined` file.
#[derive(Default, Debug, Clone)]
pub struct CombinedDictData {
    pub unigrams: Vec<(String, u8)>,
    pub bigrams: HashMap<String, Vec<(String, u8)>>,
}

pub struct CombinedParser;

impl CombinedParser {
    /// Parses `.combined` format text (with `word=...,f=...`, `bigram=...,f=...`, `prev_word[0]=...`).
    pub fn parse_str(content: &str) -> CombinedDictData {
        let mut unigrams = Vec::new();
        let mut bigrams: HashMap<String, Vec<(String, u8)>> = HashMap::new();

        let mut curr_bigram: Option<String> = None;
        let mut curr_bigram_f: u8 = 0;

        for line in content.lines() {
            let trimmed = line.trim();
            if trimmed.is_empty() || trimmed.starts_with('#') {
                continue;
            }

            if trimmed.starts_with("word=") {
                let parts: Vec<&str> = trimmed.split(',').collect();
                if let Some(w_part) = parts.first() {
                    if let Some(w) = w_part.strip_prefix("word=") {
                        let mut freq = 0u8;
                        for p in &parts[1..] {
                            if let Some(f_str) = p.strip_prefix("f=") {
                                if let Ok(f_val) = f_str.parse::<u16>() {
                                    freq = f_val.min(255) as u8;
                                }
                            }
                        }
                        unigrams.push((w.to_string(), freq));
                        curr_bigram = None;
                    }
                }
            } else if trimmed.starts_with("bigram=") {
                let parts: Vec<&str> = trimmed.split(',').collect();
                if let Some(b_part) = parts.first() {
                    if let Some(bw) = b_part.strip_prefix("bigram=") {
                        let mut freq = 0u8;
                        for p in &parts[1..] {
                            if let Some(f_str) = p.strip_prefix("f=") {
                                if let Ok(f_val) = f_str.parse::<u16>() {
                                    freq = f_val.min(255) as u8;
                                }
                            }
                        }
                        curr_bigram = Some(bw.to_string());
                        curr_bigram_f = freq;
                    }
                }
            } else if trimmed.starts_with("prev_word[0]=") {
                if let Some(pw) = trimmed.strip_prefix("prev_word[0]=") {
                    if let Some(ref bg) = curr_bigram {
                        bigrams.entry(pw.to_string()).or_default().push((bg.clone(), curr_bigram_f));
                        curr_bigram = None;
                    }
                }
            }
        }

        // Sort bigrams by frequency descending
        for list in bigrams.values_mut() {
            list.sort_by(|a, b| b.1.cmp(&a.1));
        }

        CombinedDictData { unigrams, bigrams }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_combined_sample() {
        let sample = "\
word=আমাদের,f=102
 bigram=এই,f=4
  prev_word[0]=আমাদের
 bigram=আগ্রহের,f=3
  prev_word[0]=আমাদের
 bigram=দেশে,f=3
  prev_word[0]=আমাদের
word=আমি,f=100
 bigram=এই,f=3
  prev_word[0]=আমি
 bigram=এখন,f=3
  prev_word[0]=আমি";

        let data = CombinedParser::parse_str(sample);
        assert_eq!(data.unigrams.len(), 2);
        assert_eq!(data.unigrams[0].0, "আমাদের");
        assert_eq!(data.unigrams[0].1, 102);

        let amader_bigrams = data.bigrams.get("আমাদের").unwrap();
        assert_eq!(amader_bigrams.len(), 3);
        assert_eq!(amader_bigrams[0], ("এই".to_string(), 4));
        assert_eq!(amader_bigrams[1], ("আগ্রহের".to_string(), 3));
    }
}
