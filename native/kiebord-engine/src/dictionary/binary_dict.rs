use crate::dictionary::proximity::KeyProximity;
use std::collections::HashMap;

pub const MAGIC: u32 = 0x424B_4443; // "BKDC" (Borno Keyboard Dict)
pub const VERSION: u16 = 1;

/// In-memory node representation in the packed trie.
#[derive(Clone, Debug, Default)]
pub struct DictNode {
    pub edge_char: char,
    pub freq: u32,
    pub max_subtree_freq: u32,
    pub child_start: u32,
    pub child_count: u16,
    pub is_word: bool,
}

/// Binary dictionary with zero-copy decoding and proximity typo search.
pub struct BinaryDict<'a> {
    pub word_count: u32,
    pub nodes: Vec<DictNode>,
    pub words: Vec<String>,
    _raw_bytes: Option<&'a [u8]>,
}

impl<'a> BinaryDict<'a> {
    /// Compiles a wordlist into binary `.dict` file bytes.
    pub fn compile(entries: &[(&str, u32)]) -> Vec<u8> {
        let mut sorted = entries.to_vec();
        sorted.sort_by(|a, b| a.0.cmp(b.0));

        struct TempNode {
            freq: u32,
            is_word: bool,
            children: HashMap<char, TempNode>,
        }

        let mut root = TempNode {
            freq: 0,
            is_word: false,
            children: HashMap::new(),
        };

        for &(word, freq) in &sorted {
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
            curr.freq = freq;
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

        // Serialize to bytes:
        // Header: [u32 magic][u16 version][u16 flags][u32 word_count][u32 node_count]
        let mut bytes = Vec::new();
        bytes.extend_from_slice(&MAGIC.to_be_bytes());
        bytes.extend_from_slice(&VERSION.to_be_bytes());
        bytes.extend_from_slice(&0u16.to_be_bytes()); // flags
        bytes.extend_from_slice(&(sorted.len() as u32).to_be_bytes());
        bytes.extend_from_slice(&(flat_nodes.len() as u32).to_be_bytes());

        for node in &flat_nodes {
            bytes.extend_from_slice(&(node.edge_char as u32).to_be_bytes());
            bytes.extend_from_slice(&node.freq.to_be_bytes());
            bytes.extend_from_slice(&node.max_subtree_freq.to_be_bytes());
            bytes.extend_from_slice(&node.child_start.to_be_bytes());
            bytes.extend_from_slice(&node.child_count.to_be_bytes());
            bytes.push(if node.is_word { 1 } else { 0 });
            bytes.push(0); // padding byte
        }

        bytes
    }

    /// Loads binary `.dict` bytes into a `BinaryDict`.
    pub fn from_bytes(bytes: &'a [u8]) -> Result<Self, &'static str> {
        if bytes.len() < 16 {
            return Err("Binary dict too small");
        }

        let magic = u32::from_be_bytes(bytes[0..4].try_into().unwrap());
        if magic != MAGIC {
            return Err("Invalid binary dict magic");
        }

        let version = u16::from_be_bytes(bytes[4..6].try_into().unwrap());
        if version != VERSION {
            return Err("Unsupported binary dict version");
        }

        let word_count = u32::from_be_bytes(bytes[8..12].try_into().unwrap());
        let node_count = u32::from_be_bytes(bytes[12..16].try_into().unwrap()) as usize;

        let node_size = 20;
        let expected_size = 16 + node_count * node_size;
        if bytes.len() < expected_size {
            return Err("Truncated binary dict nodes");
        }

        let mut nodes = Vec::with_capacity(node_count);
        let mut offset = 16;
        for _ in 0..node_count {
            let edge_u32 = u32::from_be_bytes(bytes[offset..offset + 4].try_into().unwrap());
            let edge_char = char::from_u32(edge_u32).unwrap_or('\0');
            let freq = u32::from_be_bytes(bytes[offset + 4..offset + 8].try_into().unwrap());
            let max_subtree_freq = u32::from_be_bytes(bytes[offset + 8..offset + 12].try_into().unwrap());
            let child_start = u32::from_be_bytes(bytes[offset + 12..offset + 16].try_into().unwrap());
            let child_count = u16::from_be_bytes(bytes[offset + 16..offset + 18].try_into().unwrap());
            let is_word = bytes[offset + 18] != 0;

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

        Ok(Self {
            word_count,
            nodes,
            words: Vec::new(),
            _raw_bytes: Some(bytes),
        })
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
                            cost + 0.02, // very small penalty for each completed letter
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

            // Check missed repeated letter (e.g. "helo" -> "hello", user typed 'l' once instead of twice)
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

    fn collect_all_words(&self, node_idx: usize, path: &mut String, out: &mut Vec<(String, u32)>) {
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

    #[test]
    fn test_compile_and_load_binary_dict() {
        let words = &[
            ("hello", 8000),
            ("help", 7000),
            ("hell", 6000),
            ("the", 9000),
            ("that", 8500),
            ("this", 8200),
            ("with", 7800),
            ("wide", 6500),
        ];

        let bytes = BinaryDict::compile(words);
        assert!(bytes.len() > 16);

        let dict = BinaryDict::from_bytes(&bytes).expect("Failed to load binary dict");
        assert_eq!(dict.word_count, 8);

        let prox = KeyProximity::qwerty();

        // 1. Exact prefix matching
        let th_sug = dict.suggest("th", 3, &prox);
        assert_eq!(th_sug[0], "the");

        // 2. Neighbor key typo correction ("gelo" -> "hello")
        let gelo_sug = dict.suggest("gelo", 3, &prox);
        assert!(!gelo_sug.is_empty());
        assert_eq!(gelo_sug[0], "hello");

        // 3. Omission typo ("helo" -> "hello")
        let helo_sug = dict.suggest("helo", 3, &prox);
        assert!(!helo_sug.is_empty());
        assert_eq!(helo_sug[0], "hello");

        // 4. Transposition typo ("hte" -> "the")
        let hte_sug = dict.suggest("hte", 3, &prox);
        assert!(!hte_sug.is_empty());
        assert_eq!(hte_sug[0], "the");

        // 5. Neighbor slip on last char ("widt" -> 't' is neighbor of 'h' -> "with")
        let widt_sug = dict.suggest("widt", 3, &prox);
        assert!(!widt_sug.is_empty());
        assert!(widt_sug.contains(&"with".to_string()));
    }
}
