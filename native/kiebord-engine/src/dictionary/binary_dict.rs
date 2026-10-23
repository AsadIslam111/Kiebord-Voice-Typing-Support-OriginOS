use crate::dictionary::combined_parser::CombinedDictData;
use crate::dictionary::proximity::KeyProximity;
use std::collections::HashMap;

pub const MAGIC: u32 = 0x424B_4443; // "BKDC" (Borno Keyboard Dict)
pub const VERSION: u16 = 2; // Version 2 supports bigram next-word prediction section

/// In-memory node representation in the packed trie.
#[derive(Clone, Debug, Default)]
pub struct DictNode {
    pub edge_char: char,
    pub freq: u8,
    pub max_subtree_freq: u8,
    pub child_start: u32,
    pub child_count: u16,
    pub is_word: bool,
}

/// Binary dictionary with zero-copy decoding, proximity typo search, and next-word prediction.
pub struct BinaryDict<'a> {
    pub word_count: u32,
    pub nodes: Vec<DictNode>,
    pub bigrams: HashMap<String, Vec<(String, u8)>>,
    _raw_bytes: Option<&'a [u8]>,
}

impl<'a> BinaryDict<'a> {
    /// Compiles parsed `.combined` data into binary `.dict` bytes.
    pub fn compile_combined(data: &CombinedDictData) -> Vec<u8> {
        let mut sorted = data.unigrams.clone();
        sorted.sort_by(|a, b| a.0.cmp(&b.0));

        struct TempNode {
            freq: u8,
            is_word: bool,
            children: HashMap<char, TempNode>,
        }

        let mut root = TempNode {
            freq: 0,
            is_word: false,
            children: HashMap::new(),
        };

        for (word, freq) in &sorted {
            if word.is_empty() {
                continue;
            }
            let mut curr = &mut root;
            for ch in word.chars() {
                curr = curr.children.entry(ch).or_insert_with(|| TempNode {
                    freq: 0,
                    is_word: false,
                    children: HashMap::new(),
                });
            }
            curr.is_word = true;
            curr.freq = *freq;
        }

        // Flatten breadth-first into flat nodes array
        let mut flat_nodes: Vec<DictNode> = Vec::new();
        let mut queue: std::collections::VecDeque<(&TempNode, usize)> = std::collections::VecDeque::new();

        flat_nodes.push(DictNode {
            edge_char: '\0',
            freq: 0,
            max_subtree_freq: 0,
            child_start: 0,
            child_count: 0,
            is_word: false,
        });
        queue.push_back((&root, 0));

        while let Some((t_node, flat_idx)) = queue.pop_front() {
            if t_node.children.is_empty() {
                continue;
            }

            let start = flat_nodes.len() as u32;
            let count = t_node.children.len() as u16;

            flat_nodes[flat_idx].child_start = start;
            flat_nodes[flat_idx].child_count = count;

            let mut sorted_children: Vec<(&char, &TempNode)> = t_node.children.iter().collect();
            sorted_children.sort_by_key(|(&c, _)| c);

            for (&ch, child_t) in sorted_children {
                let child_idx = flat_nodes.len();
                flat_nodes.push(DictNode {
                    edge_char: ch,
                    freq: child_t.freq,
                    max_subtree_freq: child_t.freq,
                    child_start: 0,
                    child_count: 0,
                    is_word: child_t.is_word,
                });
                queue.push_back((child_t, child_idx));
            }
        }

        // Compute max_subtree_freq bottom-up
        for i in (0..flat_nodes.len()).rev() {
            let start = flat_nodes[i].child_start as usize;
            let end = start + flat_nodes[i].child_count as usize;
            let mut max_f = flat_nodes[i].freq;
            for c_idx in start..end {
                if c_idx < flat_nodes.len() {
                    max_f = max_f.max(flat_nodes[c_idx].max_subtree_freq);
                }
            }
            flat_nodes[i].max_subtree_freq = max_f;
        }

        // Build bigram section bytes
        let mut bigram_bytes = Vec::new();
        for (prev_word, candidates) in &data.bigrams {
            let pw_bytes = prev_word.as_bytes();
            bigram_bytes.extend_from_slice(&(pw_bytes.len() as u16).to_be_bytes());
            bigram_bytes.extend_from_slice(pw_bytes);

            bigram_bytes.extend_from_slice(&(candidates.len() as u16).to_be_bytes());
            for (next_word, freq) in candidates {
                let nw_bytes = next_word.as_bytes();
                bigram_bytes.extend_from_slice(&(nw_bytes.len() as u16).to_be_bytes());
                bigram_bytes.extend_from_slice(nw_bytes);
                bigram_bytes.push(*freq);
            }
        }

        // Header:
        // [u32 magic][u16 version][u16 flags][u32 word_count][u32 node_count][u32 bigram_count][u32 bigram_offset]
        let node_size = 16;
        let header_size = 24;
        let bigram_offset = (header_size + flat_nodes.len() * node_size) as u32;

        let mut bytes = Vec::new();
        bytes.extend_from_slice(&MAGIC.to_be_bytes());
        bytes.extend_from_slice(&VERSION.to_be_bytes());
        bytes.extend_from_slice(&0u16.to_be_bytes()); // flags
        bytes.extend_from_slice(&(sorted.len() as u32).to_be_bytes());
        bytes.extend_from_slice(&(flat_nodes.len() as u32).to_be_bytes());
        bytes.extend_from_slice(&(data.bigrams.len() as u32).to_be_bytes());
        bytes.extend_from_slice(&bigram_offset.to_be_bytes());

        for node in &flat_nodes {
            bytes.extend_from_slice(&(node.edge_char as u32).to_be_bytes());
            bytes.push(node.freq);
            bytes.push(node.max_subtree_freq);
            bytes.push(if node.is_word { 1 } else { 0 });
            bytes.push(0); // pad
            bytes.extend_from_slice(&node.child_start.to_be_bytes());
            bytes.extend_from_slice(&node.child_count.to_be_bytes());
            bytes.extend_from_slice(&0u16.to_be_bytes()); // pad2
        }

        bytes.extend_from_slice(&bigram_bytes);
        bytes
    }

    /// Loads binary `.dict` bytes into a `BinaryDict`.
    pub fn from_bytes(bytes: &'a [u8]) -> Result<Self, &'static str> {
        if bytes.len() < 24 {
            return Err("Binary dict too small");
        }

        let magic = u32::from_be_bytes(bytes[0..4].try_into().unwrap());
        if magic != MAGIC {
            return Err("Invalid binary dict magic");
        }

        let version = u16::from_be_bytes(bytes[4..6].try_into().unwrap());
        if version != 1 && version != 2 {
            return Err("Unsupported binary dict version");
        }

        let word_count = u32::from_be_bytes(bytes[8..12].try_into().unwrap());
        let node_count = u32::from_be_bytes(bytes[12..16].try_into().unwrap()) as usize;

        let mut bigrams = HashMap::new();

        let node_size = if version == 1 { 20 } else { 16 };
        let header_size = if version == 1 { 16 } else { 24 };

        if bytes.len() < header_size + node_count * node_size {
            return Err("Truncated binary dict nodes");
        }

        let mut nodes = Vec::with_capacity(node_count);
        let mut offset = header_size;
        for _ in 0..node_count {
            let edge_u32 = u32::from_be_bytes(bytes[offset..offset + 4].try_into().unwrap());
            let edge_char = char::from_u32(edge_u32).unwrap_or('\0');

            let (freq, max_subtree_freq, is_word, child_start, child_count) = if version == 1 {
                let f = u32::from_be_bytes(bytes[offset + 4..offset + 8].try_into().unwrap()).min(255) as u8;
                let mf = u32::from_be_bytes(bytes[offset + 8..offset + 12].try_into().unwrap()).min(255) as u8;
                let cs = u32::from_be_bytes(bytes[offset + 12..offset + 16].try_into().unwrap());
                let cc = u16::from_be_bytes(bytes[offset + 16..offset + 18].try_into().unwrap());
                let iw = bytes[offset + 18] != 0;
                (f, mf, iw, cs, cc)
            } else {
                let f = bytes[offset + 4];
                let mf = bytes[offset + 5];
                let iw = bytes[offset + 6] != 0;
                let cs = u32::from_be_bytes(bytes[offset + 8..offset + 12].try_into().unwrap());
                let cc = u16::from_be_bytes(bytes[offset + 12..offset + 14].try_into().unwrap());
                (f, mf, iw, cs, cc)
            };

            nodes.push(DictNode {
                edge_char,
                freq,
                max_subtree_freq,
                child_start,
                child_count,
                is_word,
            });
            offset += node_size;
        }

        // If version 2, decode bigram section
        if version >= 2 {
            let bigram_count = u32::from_be_bytes(bytes[16..20].try_into().unwrap()) as usize;
            let bigram_offset = u32::from_be_bytes(bytes[20..24].try_into().unwrap()) as usize;

            let mut bg_ptr = bigram_offset;
            for _ in 0..bigram_count {
                if bg_ptr + 4 > bytes.len() {
                    break;
                }
                let pw_len = u16::from_be_bytes(bytes[bg_ptr..bg_ptr + 2].try_into().unwrap()) as usize;
                bg_ptr += 2;
                if bg_ptr + pw_len + 2 > bytes.len() {
                    break;
                }
                let prev_word = String::from_utf8_lossy(&bytes[bg_ptr..bg_ptr + pw_len]).to_string();
                bg_ptr += pw_len;

                let cand_count = u16::from_be_bytes(bytes[bg_ptr..bg_ptr + 2].try_into().unwrap()) as usize;
                bg_ptr += 2;

                let mut cand_list = Vec::with_capacity(cand_count);
                for _ in 0..cand_count {
                    if bg_ptr + 2 > bytes.len() {
                        break;
                    }
                    let nw_len = u16::from_be_bytes(bytes[bg_ptr..bg_ptr + 2].try_into().unwrap()) as usize;
                    bg_ptr += 2;
                    if bg_ptr + nw_len + 1 > bytes.len() {
                        break;
                    }
                    let next_word = String::from_utf8_lossy(&bytes[bg_ptr..bg_ptr + nw_len]).to_string();
                    bg_ptr += nw_len;
                    let freq = bytes[bg_ptr];
                    bg_ptr += 1;
                    cand_list.push((next_word, freq));
                }
                bigrams.insert(prev_word, cand_list);
            }
        }

        Ok(Self {
            word_count,
            nodes,
            bigrams,
            _raw_bytes: Some(bytes),
        })
    }

    /// Predicts next words based on preceding committed word.
    pub fn predict_next(&self, prev_word: &str, limit: usize) -> Vec<String> {
        let clean = prev_word.trim().to_lowercase();
        if let Some(list) = self.bigrams.get(clean.as_str()) {
            list.iter().take(limit).map(|(w, _)| w.clone()).collect()
        } else {
            Vec::new()
        }
    }

    /// Suggests completions and typo-corrected candidates using key proximity.
    pub fn suggest(&self, input: &str, limit: usize, prox: &KeyProximity) -> Vec<String> {
        let trimmed = input.trim();
        if trimmed.is_empty() {
            return self.top_idle_words(limit);
        }

        let is_all_upper = trimmed.chars().all(|c| !c.is_alphabetic() || c.is_uppercase())
            && trimmed.chars().any(|c| c.is_alphabetic());
        let is_title_case = !is_all_upper
            && trimmed.chars().next().map_or(false, |c| c.is_uppercase());

        let search_input = trimmed.to_lowercase();
        let typed_chars: Vec<char> = search_input.chars().collect();

        let mut scored_results: HashMap<String, f32> = HashMap::new();
        let mut word_buffer = String::new();

        self.fuzzy_walk(
            0,
            &typed_chars,
            0,
            &mut word_buffer,
            0.0,
            1.8, // Max typo edit cost
            prox,
            &mut scored_results,
        );

        // Sort by final score descending
        let mut ranked: Vec<(String, f32)> = scored_results.into_iter().collect();
        ranked.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        let mut final_suggestions = Vec::new();
        for (word, _score) in ranked.into_iter().take(limit) {
            let formatted = if is_all_upper {
                word.to_uppercase()
            } else if is_title_case {
                let mut c = word.chars();
                match c.next() {
                    None => String::new(),
                    Some(first) => first.to_uppercase().collect::<String>() + c.as_str(),
                }
            } else {
                word
            };
            final_suggestions.push(formatted);
        }

        final_suggestions
    }

    /// Trie-guided recursive walk for prefix completions + proximity typo corrections.
    fn fuzzy_walk(
        &self,
        node_idx: usize,
        typed: &[char],
        typed_pos: usize,
        accum: &mut String,
        cost: f32,
        max_cost: f32,
        prox: &KeyProximity,
        results: &mut HashMap<String, f32>,
    ) {
        if cost > max_cost || node_idx >= self.nodes.len() {
            return;
        }

        let node = &self.nodes[node_idx];

        // If we matched all typed characters:
        if typed_pos == typed.len() {
            if node.is_word {
                let score = (node.freq as f32 + 1.0).ln() - (2.5 * cost);
                let entry = results.entry(accum.clone()).or_insert(f32::NEG_INFINITY);
                if score > *entry {
                    *entry = score;
                }
            }

            // Descend deeper for completions (up to 8 extra characters)
            let max_extra = if cost == 0.0 { 8 } else { 4 };
            if accum.chars().count() < typed.len() + max_extra {
                let start = node.child_start as usize;
                let end = start + node.child_count as usize;
                for c_idx in start..end {
                    if c_idx < self.nodes.len() {
                        let ch = self.nodes[c_idx].edge_char;
                        accum.push(ch);
                        self.fuzzy_walk(
                            c_idx,
                            typed,
                            typed_pos,
                            accum,
                            cost + 0.02,
                            max_cost,
                            prox,
                            results,
                        );
                        accum.pop();
                    }
                }
            }
            return;
        }

        let target_char = typed[typed_pos];
        let start = node.child_start as usize;
        let end = start + node.child_count as usize;

        // 1. Check transposition (swapped adjacent letters, e.g. "hte" -> "the")
        if typed_pos + 1 < typed.len() {
            let next_target = typed[typed_pos + 1];
            for c_idx in start..end {
                if c_idx < self.nodes.len() && self.nodes[c_idx].edge_char == next_target {
                    let g_start = self.nodes[c_idx].child_start as usize;
                    let g_end = g_start + self.nodes[c_idx].child_count as usize;
                    for g_idx in g_start..g_end {
                        if g_idx < self.nodes.len() && self.nodes[g_idx].edge_char == target_char {
                            accum.push(next_target);
                            accum.push(target_char);
                            self.fuzzy_walk(
                                g_idx,
                                typed,
                                typed_pos + 2,
                                accum,
                                cost + 0.4,
                                max_cost,
                                prox,
                                results,
                            );
                            accum.pop();
                            accum.pop();
                        }
                    }
                }
            }
        }

        // 2. Explore children edges (exact match, neighbor slip, or substitution)
        for c_idx in start..end {
            if c_idx >= self.nodes.len() {
                break;
            }
            let edge_char = self.nodes[c_idx].edge_char;

            let is_missed_double = accum.chars().last() == Some(edge_char);
            let omission_penalty = if is_missed_double { 0.3 } else { 0.75 };

            let edit_penalty = prox.penalty(target_char, edge_char);

            // Exact match (penalty = 0.0) or neighbor/sub
            if cost + edit_penalty <= max_cost {
                accum.push(edge_char);
                self.fuzzy_walk(
                    c_idx,
                    typed,
                    typed_pos + 1,
                    accum,
                    cost + edit_penalty,
                    max_cost,
                    prox,
                    results,
                );
                accum.pop();
            }

            // 3. Deletion in input (user omitted a letter)
            if cost + omission_penalty <= max_cost {
                accum.push(edge_char);
                self.fuzzy_walk(
                    c_idx,
                    typed,
                    typed_pos,
                    accum,
                    cost + omission_penalty,
                    max_cost,
                    prox,
                    results,
                );
                accum.pop();
            }
        }

        // 4. Insertion in input (user typed an extra accidental letter, e.g. "heello" -> "hello")
        if cost + 0.75 <= max_cost {
            self.fuzzy_walk(
                node_idx,
                typed,
                typed_pos + 1,
                accum,
                cost + 0.75,
                max_cost,
                prox,
                results,
            );
        }
    }

    /// Returns top idle words sorted by frequency.
    fn top_idle_words(&self, limit: usize) -> Vec<String> {
        let mut terminal_words = Vec::new();
        let mut path = String::new();
        self.collect_all_words(0, &mut path, &mut terminal_words);
        terminal_words.sort_by(|a, b| b.1.cmp(&a.1));
        terminal_words.into_iter().take(limit).map(|(w, _)| w).collect()
    }

    fn collect_all_words(&self, node_idx: usize, path: &mut String, out: &mut Vec<(String, u8)>) {
        if node_idx >= self.nodes.len() {
            return;
        }
        let node = &self.nodes[node_idx];
        if node.is_word {
            out.push((path.clone(), node.freq));
        }
        let start = node.child_start as usize;
        let end = start + node.child_count as usize;
        for c_idx in start..end {
            if c_idx < self.nodes.len() {
                path.push(self.nodes[c_idx].edge_char);
                self.collect_all_words(c_idx, path, out);
                path.pop();
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dictionary::combined_parser::CombinedParser;

    #[test]
    fn test_compile_combined_and_predict_next() {
        let sample = "\
word=hello,f=200
 bigram=world,f=100
  prev_word[0]=hello
 bigram=there,f=90
  prev_word[0]=hello
word=help,f=180
word=the,f=250
word=that,f=240
word=this,f=230";

        let parsed = CombinedParser::parse_str(sample);
        let bytes = BinaryDict::compile_combined(&parsed);
        assert!(bytes.len() > 24);

        let dict = BinaryDict::from_bytes(&bytes).expect("Load compiled dict");
        assert_eq!(dict.word_count, 5);

        // Test next word prediction
        let nexts = dict.predict_next("hello", 3);
        assert_eq!(nexts, vec!["world", "there"]);

        // Test typo suggestion
        let prox = KeyProximity::qwerty();
        let helo_sug = dict.suggest("helo", 3, &prox);
        assert_eq!(helo_sug[0], "hello");
    }
}
