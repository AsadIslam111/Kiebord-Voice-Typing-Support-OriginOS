use crate::dictionary::combined_parser::CombinedDictData;
use crate::dictionary::proximity::KeyProximity;
use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap};

pub const MAGIC: u32 = 0x4B42_4443; // "KBDC" (Kiebord Dictionary Core)
pub const VERSION: u16 = 3;
pub const FLAG_LABELS_U8: u16 = 1;
pub const FLAG_DEGREE_U8: u16 = 2;
pub const MAX_SYMBOLS: usize = 256;
pub const CHECKPOINT_STRIDE: usize = 32;
pub const CHECKPOINT_SHIFT: u32 = 5;
const UNUSED_SYMBOL: u16 = 0xFFFF;

fn pad4(len: usize) -> usize {
    (len + 3) & !3
}

/// Zero-copy, in-place binary dictionary with compact trie and word-ID bigrams.
pub struct BinaryDict<'a> {
    bytes: &'a [u8],
    word_count: u32,
    node_count: usize,
    _edge_count: usize,
    _symbol_count: usize,
    _bigram_count: usize,
    labels_are_u8: bool,
    symbols_table: [u16; 256],
    child_count_off: usize,
    checkpoints_off: usize,
    edge_label_off: usize,
    freq_off: usize,
    max_subtree_off: usize,
    is_word_off: usize,
    vocab_offsets_off: usize,
    vocab_bytes_off: usize,
    bigram_run_start_off: usize,
    bigram_followers_off: usize,
    _bigram_freqs_off: usize,
}

impl<'a> BinaryDict<'a> {
    /// Compiles parsed `.combined` unigrams and bigrams into a compact `KBDC` binary dictionary.
    pub fn compile_combined(data: &CombinedDictData) -> Vec<u8> {
        // 1. Collect all distinct vocabulary words (unigrams + bigram words)
        let mut vocab_set = std::collections::BTreeSet::new();
        for (w, _) in &data.unigrams {
            let clean = w.trim();
            if !clean.is_empty() {
                vocab_set.insert(clean.to_string());
            }
        }
        for (prev, cands) in &data.bigrams {
            let clean_prev = prev.trim();
            if !clean_prev.is_empty() {
                vocab_set.insert(clean_prev.to_string());
            }
            for (nw, _) in cands {
                let clean_nw = nw.trim();
                if !clean_nw.is_empty() {
                    vocab_set.insert(clean_nw.to_string());
                }
            }
        }

        // Vocabulary sorted in lexicographical UTF-8 byte order
        let vocab: Vec<String> = vocab_set.into_iter().collect();
        let word_count = vocab.len();
        let mut word_to_id: HashMap<&str, u32> = HashMap::with_capacity(word_count);
        for (id, word) in vocab.iter().enumerate() {
            word_to_id.insert(word.as_str(), id as u32);
        }

        // Build unigram frequency map
        let mut freq_map: HashMap<&str, u8> = HashMap::with_capacity(data.unigrams.len());
        for (w, f) in &data.unigrams {
            let clean = w.trim();
            let entry = freq_map.entry(clean).or_insert(*f);
            if *f > *entry {
                *entry = *f;
            }
        }

        // 2. Build intermediate prefix tree in UTF-16 units
        struct TempNode {
            freq: u8,
            is_word: bool,
            children: Vec<(u16, TempNode)>,
        }

        let mut root = TempNode {
            freq: 0,
            is_word: false,
            children: Vec::new(),
        };

        for word in &vocab {
            let f = *freq_map.get(word.as_str()).unwrap_or(&1);
            let units: Vec<u16> = word.encode_utf16().collect();
            let mut curr = &mut root;
            for unit in units {
                let pos = curr.children.iter().position(|(u, _)| *u == unit);
                let idx = match pos {
                    Some(i) => i,
                    None => {
                        curr.children.push((
                            unit,
                            TempNode {
                                freq: 0,
                                is_word: false,
                                children: Vec::new(),
                            },
                        ));
                        curr.children.sort_by_key(|(u, _)| *u);
                        curr.children.iter().position(|(u, _)| *u == unit).unwrap()
                    }
                };
                curr = &mut curr.children[idx].1;
            }
            curr.is_word = true;
            curr.freq = f;
        }

        // 3. Flatten tree breadth-first (BFS)
        // Edge e always points to node e + 1!
        let mut degrees: Vec<u8> = Vec::new();
        let mut edge_labels_raw: Vec<u16> = Vec::new();
        let mut freqs: Vec<u8> = Vec::new();
        let mut is_word: Vec<bool> = Vec::new();

        let mut queue = std::collections::VecDeque::new();
        degrees.push(root.children.len() as u8);
        freqs.push(root.freq);
        is_word.push(root.is_word);
        queue.push_back(&root);

        while let Some(t_node) = queue.pop_front() {
            for &(unit, ref child) in &t_node.children {
                edge_labels_raw.push(unit);
                degrees.push(child.children.len() as u8);
                freqs.push(child.freq);
                is_word.push(child.is_word);
                queue.push_back(child);
            }
        }
        let node_count = freqs.len();
        let edge_count = edge_labels_raw.len();

        let mut child_starts: Vec<u32> = Vec::with_capacity(node_count);
        let mut running_sum = 0u32;
        for &deg in &degrees {
            child_starts.push(running_sum);
            running_sum += deg as u32;
        }

        // 4. Compute max_subtree_freq bottom-up
        let mut max_subtree_freq = freqs.clone();
        for id in (0..node_count).rev() {
            let start = child_starts[id] as usize;
            let count = degrees[id] as usize;
            let mut best = if is_word[id] { freqs[id] } else { 0 };
            for e in start..start + count {
                let child_id = e + 1;
                if child_id < node_count {
                    best = best.max(max_subtree_freq[child_id]);
                }
            }
            max_subtree_freq[id] = best;
        }

        // 5. Symbol table (compact 1-byte edge labels)
        let mut symbols = edge_labels_raw.clone();
        symbols.sort_unstable();
        symbols.dedup();
        let labels_are_u8 = symbols.len() <= MAX_SYMBOLS;
        let symbol_count = if labels_are_u8 { symbols.len() } else { 0 };

        // 6. Checkpoints for 32-node stride
        let checkpoint_count = (node_count >> CHECKPOINT_SHIFT) + 1;
        let mut checkpoints: Vec<u32> = Vec::with_capacity(checkpoint_count);
        for k in 0..checkpoint_count {
            let node_idx = (k << CHECKPOINT_SHIFT).min(node_count - 1);
            checkpoints.push(child_starts[node_idx]);
        }

        // 7. Vocabulary section (concatenated UTF-8 bytes + offsets)
        let mut vocab_offsets: Vec<u32> = Vec::with_capacity(word_count + 1);
        let mut vocab_bytes: Vec<u8> = Vec::new();
        for word in &vocab {
            vocab_offsets.push(vocab_bytes.len() as u32);
            vocab_bytes.extend_from_slice(word.as_bytes());
        }
        vocab_offsets.push(vocab_bytes.len() as u32);

        // 8. Word-ID bigram section
        let mut bigram_run_start: Vec<u32> = Vec::with_capacity(word_count + 1);
        let mut bigram_followers: Vec<u32> = Vec::new();
        let mut bigram_freqs: Vec<u8> = Vec::new();

        for word in &vocab {
            bigram_run_start.push(bigram_followers.len() as u32);
            if let Some(cands) = data.bigrams.get(word.as_str()) {
                let mut sorted_cands = cands.clone();
                sorted_cands.sort_by(|a, b| b.1.cmp(&a.1));
                // Keep top 24 follower candidates per head word
                for (nw, f) in sorted_cands.into_iter().take(24) {
                    if let Some(&nw_id) = word_to_id.get(nw.as_str()) {
                        bigram_followers.push(nw_id);
                        bigram_freqs.push(f);
                    }
                }
            }
        }
        bigram_run_start.push(bigram_followers.len() as u32);
        let total_bigrams = bigram_followers.len();

        // 9. Layout sections and calculate offsets
        let header_bytes = 4 + 2 + 2 + 4 + 4 + 4 + 4 + 4 + 12 * 4;
        let mut cursor = header_bytes;
        let mut offsets = [0u32; 12];

        let mut alloc_section = |size: usize| -> u32 {
            let at = cursor;
            cursor += pad4(size);
            at as u32
        };

        offsets[0] = alloc_section(symbol_count * 2);
        offsets[1] = alloc_section(node_count); // degrees
        offsets[2] = alloc_section(checkpoint_count * 4); // checkpoints
        offsets[3] = alloc_section(if labels_are_u8 { edge_count } else { edge_count * 2 });
        offsets[4] = alloc_section(node_count); // freqs
        offsets[5] = alloc_section(node_count); // max_subtree_freq
        offsets[6] = alloc_section(node_count.div_ceil(8)); // is_word bitset
        offsets[7] = alloc_section((word_count + 1) * 4); // vocab_offsets
        offsets[8] = alloc_section(vocab_bytes.len()); // vocab_bytes
        offsets[9] = alloc_section((word_count + 1) * 4); // bigram_run_start
        offsets[10] = alloc_section(total_bigrams * 4); // bigram_followers
        offsets[11] = alloc_section(total_bigrams); // bigram_freqs

        // 10. Write binary output
        let mut out = Vec::with_capacity(cursor);
        out.extend_from_slice(&MAGIC.to_be_bytes());
        out.extend_from_slice(&VERSION.to_be_bytes());
        let flags = (if labels_are_u8 { FLAG_LABELS_U8 } else { 0 }) | FLAG_DEGREE_U8;
        out.extend_from_slice(&flags.to_be_bytes());
        out.extend_from_slice(&(word_count as u32).to_be_bytes());
        out.extend_from_slice(&(node_count as u32).to_be_bytes());
        out.extend_from_slice(&(edge_count as u32).to_be_bytes());
        out.extend_from_slice(&(symbol_count as u32).to_be_bytes());
        out.extend_from_slice(&(total_bigrams as u32).to_be_bytes());

        for off in offsets {
            out.extend_from_slice(&off.to_be_bytes());
        }

        // Section 0: symbols
        for &sym in &symbols[..symbol_count] {
            out.extend_from_slice(&sym.to_be_bytes());
        }
        while out.len() < offsets[1] as usize { out.push(0); }

        // Section 1: degrees
        out.extend_from_slice(&degrees);
        while out.len() < offsets[2] as usize { out.push(0); }

        // Section 2: checkpoints
        for &cp in &checkpoints {
            out.extend_from_slice(&cp.to_be_bytes());
        }
        while out.len() < offsets[3] as usize { out.push(0); }

        // Section 3: edge_labels
        if labels_are_u8 {
            for &lbl in &edge_labels_raw {
                let idx = symbols.binary_search(&lbl).expect("symbol in table") as u8;
                out.push(idx);
            }
        } else {
            for &lbl in &edge_labels_raw {
                out.extend_from_slice(&lbl.to_be_bytes());
            }
        }
        while out.len() < offsets[4] as usize { out.push(0); }

        // Section 4: freqs
        out.extend_from_slice(&freqs);
        while out.len() < offsets[5] as usize { out.push(0); }

        // Section 5: max_subtree_freq
        out.extend_from_slice(&max_subtree_freq);
        while out.len() < offsets[6] as usize { out.push(0); }

        // Section 6: is_word bitset
        let mut byte = 0u8;
        for (id, &w) in is_word.iter().enumerate() {
            if w {
                byte |= 1 << (id & 7);
            }
            if id & 7 == 7 {
                out.push(byte);
                byte = 0;
            }
        }
        if node_count & 7 != 0 {
            out.push(byte);
        }
        while out.len() < offsets[7] as usize { out.push(0); }

        // Section 7: vocab_offsets
        for &vo in &vocab_offsets {
            out.extend_from_slice(&vo.to_be_bytes());
        }
        while out.len() < offsets[8] as usize { out.push(0); }

        // Section 8: vocab_bytes
        out.extend_from_slice(&vocab_bytes);
        while out.len() < offsets[9] as usize { out.push(0); }

        // Section 9: bigram_run_start
        for &bs in &bigram_run_start {
            out.extend_from_slice(&bs.to_be_bytes());
        }
        while out.len() < offsets[10] as usize { out.push(0); }

        // Section 10: bigram_followers
        for &bf in &bigram_followers {
            out.extend_from_slice(&bf.to_be_bytes());
        }
        while out.len() < offsets[11] as usize { out.push(0); }

        // Section 11: bigram_freqs
        out.extend_from_slice(&bigram_freqs);
        while out.len() < cursor { out.push(0); }

        out
    }

    /// Loads a binary dictionary into an in-place zero-copy reader.
    pub fn from_bytes(bytes: &'a [u8]) -> Result<Self, &'static str> {
        let header_min = 4 + 2 + 2 + 4 + 4 + 4 + 4 + 4 + 12 * 4;
        if bytes.len() < header_min {
            return Err("Binary dict too small");
        }

        let magic = u32::from_be_bytes(bytes[0..4].try_into().unwrap());
        if magic != MAGIC {
            return Err("Invalid binary dict magic (expected KBDC)");
        }

        let version = u16::from_be_bytes(bytes[4..6].try_into().unwrap());
        if version != VERSION {
            return Err("Unsupported binary dict version");
        }

        let flags = u16::from_be_bytes(bytes[6..8].try_into().unwrap());
        let word_count = u32::from_be_bytes(bytes[8..12].try_into().unwrap());
        let node_count = u32::from_be_bytes(bytes[12..16].try_into().unwrap()) as usize;
        let edge_count = u32::from_be_bytes(bytes[16..20].try_into().unwrap()) as usize;
        let symbol_count = u32::from_be_bytes(bytes[20..24].try_into().unwrap()) as usize;
        let bigram_count = u32::from_be_bytes(bytes[24..28].try_into().unwrap()) as usize;

        let labels_are_u8 = flags & FLAG_LABELS_U8 != 0;

        let mut offsets = [0usize; 12];
        for i in 0..12 {
            offsets[i] = u32::from_be_bytes(bytes[28 + i * 4..32 + i * 4].try_into().unwrap()) as usize;
        }

        let mut symbols_table = [UNUSED_SYMBOL; 256];
        if labels_are_u8 {
            for i in 0..symbol_count.min(256) {
                symbols_table[i] = u16::from_be_bytes(bytes[offsets[0] + i * 2..offsets[0] + i * 2 + 2].try_into().unwrap());
            }
        }

        Ok(BinaryDict {
            bytes,
            word_count,
            node_count,
            _edge_count: edge_count,
            _symbol_count: symbol_count,
            _bigram_count: bigram_count,
            labels_are_u8,
            symbols_table,
            child_count_off: offsets[1],
            checkpoints_off: offsets[2],
            edge_label_off: offsets[3],
            freq_off: offsets[4],
            max_subtree_off: offsets[5],
            is_word_off: offsets[6],
            vocab_offsets_off: offsets[7],
            vocab_bytes_off: offsets[8],
            bigram_run_start_off: offsets[9],
            bigram_followers_off: offsets[10],
            _bigram_freqs_off: offsets[11],
        })
    }

    #[inline]
    pub fn word_count(&self) -> u32 {
        self.word_count
    }

    #[inline]
    pub fn node_count(&self) -> usize {
        self.node_count
    }

    #[inline]
    fn child_start(&self, node: usize) -> usize {
        let cp_idx = node >> CHECKPOINT_SHIFT;
        let cp_val = u32::from_be_bytes(self.bytes[self.checkpoints_off + cp_idx * 4..self.checkpoints_off + cp_idx * 4 + 4].try_into().unwrap()) as usize;
        let start_node = node & !(CHECKPOINT_STRIDE - 1);
        let sum: usize = self.bytes[self.child_count_off + start_node..self.child_count_off + node]
            .iter()
            .map(|&d| d as usize)
            .sum();
        cp_val + sum
    }

    #[inline]
    fn child_count(&self, node: usize) -> usize {
        self.bytes[self.child_count_off + node] as usize
    }

    #[inline]
    fn edge_label(&self, edge: usize) -> u16 {
        if self.labels_are_u8 {
            let sym_idx = self.bytes[self.edge_label_off + edge] as usize;
            self.symbols_table[sym_idx]
        } else {
            u16::from_be_bytes(self.bytes[self.edge_label_off + edge * 2..self.edge_label_off + edge * 2 + 2].try_into().unwrap())
        }
    }

    #[inline]
    fn edge_child(edge: usize) -> u32 {
        (edge + 1) as u32
    }

    #[inline]
    fn child(&self, node: u32, unit: u16) -> Option<u32> {
        let node_idx = node as usize;
        let start = self.child_start(node_idx);
        let count = self.child_count(node_idx);
        if count == 0 {
            return None;
        }

        let mut lo = start as isize;
        let mut hi = (start + count) as isize - 1;
        while lo <= hi {
            let mid = ((lo + hi) >> 1) as usize;
            match self.edge_label(mid).cmp(&unit) {
                Ordering::Less => lo = mid as isize + 1,
                Ordering::Greater => hi = mid as isize - 1,
                Ordering::Equal => return Some(Self::edge_child(mid)),
            }
        }
        None
    }

    #[inline]
    fn is_word(&self, node: u32) -> bool {
        let n = node as usize;
        (self.bytes[self.is_word_off + (n >> 3)] >> (n & 7)) & 1 != 0
    }

    #[inline]
    fn freq(&self, node: u32) -> u8 {
        self.bytes[self.freq_off + node as usize]
    }

    #[inline]
    fn max_subtree_freq(&self, node: u32) -> u8 {
        self.bytes[self.max_subtree_off + node as usize]
    }

    pub fn word_at(&self, id: u32) -> Option<&'a str> {
        if id >= self.word_count {
            return None;
        }
        let off_idx = self.vocab_offsets_off + id as usize * 4;
        let start = u32::from_be_bytes(self.bytes[off_idx..off_idx + 4].try_into().unwrap()) as usize;
        let end = u32::from_be_bytes(self.bytes[off_idx + 4..off_idx + 8].try_into().unwrap()) as usize;
        std::str::from_utf8(&self.bytes[self.vocab_bytes_off + start..self.vocab_bytes_off + end]).ok()
    }

    pub fn find_word_id(&self, word: &str) -> Option<u32> {
        let mut lo = 0isize;
        let mut hi = self.word_count as isize - 1;
        while lo <= hi {
            let mid = ((lo + hi) >> 1) as u32;
            let mid_word = self.word_at(mid)?;
            match mid_word.cmp(word) {
                Ordering::Less => lo = mid as isize + 1,
                Ordering::Greater => hi = mid as isize - 1,
                Ordering::Equal => return Some(mid),
            }
        }
        None
    }

    /// Predicts next words based on preceding committed word in O(1) time without heap allocations.
    pub fn predict_next(&self, prev_word: &str, limit: usize) -> Vec<String> {
        let clean = prev_word.trim().to_lowercase();
        let word_id = match self.find_word_id(&clean) {
            Some(id) => id,
            None => return Vec::new(),
        };

        let run_idx = self.bigram_run_start_off + word_id as usize * 4;
        let start = u32::from_be_bytes(self.bytes[run_idx..run_idx + 4].try_into().unwrap()) as usize;
        let end = u32::from_be_bytes(self.bytes[run_idx + 4..run_idx + 8].try_into().unwrap()) as usize;

        let count = (end - start).min(limit);
        let mut results = Vec::with_capacity(count);
        for i in 0..count {
            let f_off = self.bigram_followers_off + (start + i) * 4;
            let follower_id = u32::from_be_bytes(self.bytes[f_off..f_off + 4].try_into().unwrap());
            if let Some(w) = self.word_at(follower_id) {
                results.push(w.to_string());
            }
        }
        results
    }

    /// High-performance admissible best-first beam search with branch-and-bound pruning.
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
        let typed: Vec<u16> = search_input.encode_utf16().collect();

        #[derive(Clone)]
        struct State {
            node: u32,
            typed_pos: usize,
            accum: Vec<u16>,
            cost: f32,
            bound: f32,
        }

        impl PartialEq for State {
            fn eq(&self, other: &Self) -> bool {
                self.bound == other.bound
            }
        }
        impl Eq for State {}
        impl PartialOrd for State {
            fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
                self.bound.partial_cmp(&other.bound)
            }
        }
        impl Ord for State {
            fn cmp(&self, other: &Self) -> Ordering {
                self.partial_cmp(other).unwrap_or(Ordering::Equal)
            }
        }

        let max_cost = 1.8f32;
        let mut heap: BinaryHeap<State> = BinaryHeap::new();
        let mut candidates: HashMap<String, (f32, u8)> = HashMap::new();

        let root_bound = (self.max_subtree_freq(0) as f32 + 1.0).ln();
        heap.push(State {
            node: 0,
            typed_pos: 0,
            accum: Vec::new(),
            cost: 0.0,
            bound: root_bound,
        });

        let mut expansions = 0;
        let max_expansions = 450;

        while let Some(state) = heap.pop() {
            expansions += 1;
            if expansions > max_expansions {
                break;
            }

            if state.cost > max_cost {
                continue;
            }

            // Pruning: if we have enough candidates and state cannot beat the worst
            if candidates.len() >= limit {
                let worst_score = candidates.values().map(|(s, _)| *s).fold(f32::INFINITY, f32::min);
                if state.bound < worst_score - 0.2 {
                    continue;
                }
            }

            // Reached end of typed string
            if state.typed_pos == typed.len() {
                if self.is_word(state.node) {
                    let freq = self.freq(state.node);
                    let word_str = String::from_utf16_lossy(&state.accum);
                    let score = (freq as f32 + 1.0).ln() - (2.5 * state.cost);
                    let entry = candidates.entry(word_str).or_insert((f32::NEG_INFINITY, 0));
                    if score > entry.0 {
                        *entry = (score, freq);
                    }
                }

                // Prefix completions (descend deeper)
                let max_extra = if state.cost == 0.0 { 6 } else { 3 };
                if state.accum.len() < typed.len() + max_extra {
                    let start = self.child_start(state.node as usize);
                    let count = self.child_count(state.node as usize);
                    for e in start..start + count {
                        let child_node = Self::edge_child(e);
                        let label = self.edge_label(e);
                        let mut next_accum = state.accum.clone();
                        next_accum.push(label);
                        let child_mf = self.max_subtree_freq(child_node) as f32;
                        let child_cost = state.cost + 0.04;
                        let child_bound = (child_mf + 1.0).ln() - child_cost;
                        heap.push(State {
                            node: child_node,
                            typed_pos: state.typed_pos,
                            accum: next_accum,
                            cost: child_cost,
                            bound: child_bound,
                        });
                    }
                }
                continue;
            }

            let target = typed[state.typed_pos];

            // 1. Exact match
            if let Some(child_node) = self.child(state.node, target) {
                let mut next_accum = state.accum.clone();
                next_accum.push(target);
                let child_mf = self.max_subtree_freq(child_node) as f32;
                heap.push(State {
                    node: child_node,
                    typed_pos: state.typed_pos + 1,
                    accum: next_accum,
                    cost: state.cost,
                    bound: (child_mf + 1.0).ln() - state.cost,
                });
            }

            // 2. Proximity substitution
            if state.cost + 0.35 <= max_cost {
                if let Some(target_ch) = char::from_u32(target as u32) {
                    for &neighbor in prox.get_neighbors(target_ch) {
                        let n_u16 = neighbor as u16;
                        if n_u16 != target {
                            if let Some(child_node) = self.child(state.node, n_u16) {
                                let mut next_accum = state.accum.clone();
                                next_accum.push(n_u16);
                                let child_mf = self.max_subtree_freq(child_node) as f32;
                                let child_cost = state.cost + 0.35;
                                heap.push(State {
                                    node: child_node,
                                    typed_pos: state.typed_pos + 1,
                                    accum: next_accum,
                                    cost: child_cost,
                                    bound: (child_mf + 1.0).ln() - child_cost,
                                });
                            }
                        }
                    }
                }
            }

            // 3. Transposition (adjacent swap: 'teh' -> 'the')
            if state.typed_pos + 1 < typed.len() && state.cost + 0.40 <= max_cost {
                let next_target = typed[state.typed_pos + 1];
                if let Some(c1) = self.child(state.node, next_target) {
                    if let Some(c2) = self.child(c1, target) {
                        let mut next_accum = state.accum.clone();
                        next_accum.push(next_target);
                        next_accum.push(target);
                        let child_mf = self.max_subtree_freq(c2) as f32;
                        let child_cost = state.cost + 0.40;
                        heap.push(State {
                            node: c2,
                            typed_pos: state.typed_pos + 2,
                            accum: next_accum,
                            cost: child_cost,
                            bound: (child_mf + 1.0).ln() - child_cost,
                        });
                    }
                }
            }

            // 4. Repeated letter in dictionary word omitted by user ('helo' -> 'hello', 'gelo' -> 'hello')
            if let Some(&last_unit) = state.accum.last() {
                if state.cost + 0.30 <= max_cost {
                    if let Some(repeat_node) = self.child(state.node, last_unit) {
                        let mut next_accum = state.accum.clone();
                        next_accum.push(last_unit);
                        let child_mf = self.max_subtree_freq(repeat_node) as f32;
                        let child_cost = state.cost + 0.30;
                        heap.push(State {
                            node: repeat_node,
                            typed_pos: state.typed_pos,
                            accum: next_accum,
                            cost: child_cost,
                            bound: (child_mf + 1.0).ln() - child_cost,
                        });
                    }
                }
            }

            // 5. Letter omission / deletion ('আমদের' -> 'আমাদের')
            if state.cost + 0.60 <= max_cost {
                let start = self.child_start(state.node as usize);
                let count = self.child_count(state.node as usize);
                for e in start..start + count {
                    let child_node = Self::edge_child(e);
                    let label = self.edge_label(e);
                    if label != target {
                        // Check if grandchild matches current target
                        if let Some(grandchild) = self.child(child_node, target) {
                            let mut next_accum = state.accum.clone();
                            next_accum.push(label);
                            next_accum.push(target);
                            let child_mf = self.max_subtree_freq(grandchild) as f32;
                            let child_cost = state.cost + 0.60;
                            heap.push(State {
                                node: grandchild,
                                typed_pos: state.typed_pos + 1,
                                accum: next_accum,
                                cost: child_cost,
                                bound: (child_mf + 1.0).ln() - child_cost,
                            });
                        }
                    }
                }
            }

            // 6. Insertion (user typed an extra letter)
            if state.cost + 0.65 <= max_cost {
                heap.push(State {
                    node: state.node,
                    typed_pos: state.typed_pos + 1,
                    accum: state.accum.clone(),
                    cost: state.cost + 0.65,
                    bound: state.bound - 0.65,
                });
            }
        }

        // Rank candidates by score descending
        let mut ranked: Vec<(String, f32, u8)> = candidates.into_iter().map(|(w, (s, f))| (w, s, f)).collect();
        ranked.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(Ordering::Equal).then_with(|| b.2.cmp(&a.2)));

        let mut final_suggestions = Vec::new();
        for (word, _, _) in ranked.into_iter().take(limit) {
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

    fn top_idle_words(&self, limit: usize) -> Vec<String> {
        let mut words = Vec::new();
        for i in 0..self.word_count.min(limit as u32) {
            if let Some(w) = self.word_at(i) {
                words.push(w.to_string());
            }
        }
        words
    }
}
