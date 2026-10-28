use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DictMetadata {
    pub name: String,
    pub author: String,
    pub version: u32,
    pub date: u64,
    pub license: String,
}

impl Default for DictMetadata {
    fn default() -> Self {
        Self {
            name: "Kiebord Word Dictionary".to_string(),
            author: "Jayed Ahsan Saad".to_string(),
            version: 1,
            date: 0,
            license: "Only for Kiebord".to_string(),
        }
    }
}

impl DictMetadata {
    pub fn apply_header_str(&mut self, header: &str) {
        let delimiter = if header.contains(';') { ';' } else { ',' };
        for part in header.split(delimiter) {
            let trimmed = part.trim();
            if trimmed.is_empty() {
                continue;
            }
            if let Some((k, v)) = trimmed.split_once('=') {
                let key = k.trim();
                let val = v.trim();
                match key {
                    "name" | "description" => {
                        if !val.is_empty() {
                            self.name = val.to_string();
                        }
                    }
                    "author" => {
                        if !val.is_empty() {
                            self.author = val.to_string();
                        }
                    }
                    "version" => {
                        if let Ok(ver) = val.parse::<u32>() {
                            self.version = ver;
                        }
                    }
                    "date" => {
                        if let Ok(ts) = val.parse::<u64>() {
                            self.date = ts;
                        }
                    }
                    "license" => {
                        if !val.is_empty() {
                            self.license = val.to_string();
                        }
                    }
                    _ => {}
                }
            }
        }
    }

    pub fn from_header_str(header: &str) -> Self {
        let mut meta = Self::default();
        meta.apply_header_str(header);
        meta
    }

    pub fn to_header_string(&self) -> String {
        format!(
            "name={};author={};version={};date={};license={};",
            self.name, self.author, self.version, self.date, self.license
        )
    }
}

/// Parsed metadata, unigram, and bigram data from a `.combined` file.
#[derive(Default, Debug, Clone)]
pub struct CombinedDictData {
    pub metadata: DictMetadata,
    pub unigrams: Vec<(String, u8)>,
    pub bigrams: HashMap<String, Vec<(String, u8)>>,
}

pub struct CombinedParser;

impl CombinedParser {
    /// Parses `.combined` format text (with optional header metadata, `word=...,f=...`, `bigram=...,f=...`, `prev_word[0]=...`).
    pub fn parse_str(content: &str) -> CombinedDictData {
        let mut metadata = DictMetadata::default();
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
            } else if unigrams.is_empty() && (trimmed.contains("name=") || trimmed.contains("author=") || trimmed.contains("version=") || trimmed.contains("license=") || trimmed.contains("dictionary=")) {
                metadata.apply_header_str(trimmed);
            }
        }

        // Sort bigrams by frequency descending
        for list in bigrams.values_mut() {
            list.sort_by(|a, b| b.1.cmp(&a.1));
        }

        CombinedDictData {
            metadata,
            unigrams,
            bigrams,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_combined_sample() {
        let sample = "\
name=Kiebord Bangla Word Dictionary;author=Jayed Ahsan Saad;version=2;date=1761681600;license=Only for Kiebord;
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
        assert_eq!(data.metadata.name, "Kiebord Bangla Word Dictionary");
        assert_eq!(data.metadata.author, "Jayed Ahsan Saad");
        assert_eq!(data.metadata.version, 2);
        assert_eq!(data.metadata.date, 1761681600);
        assert_eq!(data.metadata.license, "Only for Kiebord");

        assert_eq!(data.unigrams.len(), 2);
        assert_eq!(data.unigrams[0].0, "আমাদের");
        assert_eq!(data.unigrams[0].1, 102);

        let amader_bigrams = data.bigrams.get("আমাদের").unwrap();
        assert_eq!(amader_bigrams.len(), 3);
        assert_eq!(amader_bigrams[0], ("এই".to_string(), 4));
        assert_eq!(amader_bigrams[1], ("আগ্রহের".to_string(), 3));
    }
}
