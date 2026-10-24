use crate::dictionary::binary_dict::BinaryDict;
use crate::translit::traits::PhoneticTransliterator;
use parking_lot::RwLock;
use std::collections::{HashMap, HashSet};
use std::sync::{Arc, OnceLock};

#[derive(Debug, Clone, PartialEq)]
pub struct GesturePoint {
    pub x: f32,
    pub y: f32,
    pub timestamp_ms: i64,
}

#[derive(Debug, Clone, PartialEq)]
pub struct KeyCenter {
    pub char_code: char,
    pub x: f32,
    pub y: f32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct GestureCandidate {
    pub word: String,
    pub score: f32,
}

#[derive(Clone)]
pub struct PhoneticWordEntry {
    pub word_id: u32,
    pub freq: u8,
    pub keys: Vec<char>,
}

pub struct PhoneticIndex {
    pub buckets: HashMap<(char, char), Vec<PhoneticWordEntry>>,
}

impl PhoneticIndex {
    pub fn build(dict: &BinaryDict, transliterator: &dyn PhoneticTransliterator) -> Self {
        let mut buckets: HashMap<(char, char), Vec<PhoneticWordEntry>> = HashMap::with_capacity(700);
        let word_count = dict.word_count();

        for id in 0..word_count {
            if let Some(word) = dict.word_at(id) {
                let freq = dict.word_freq(word);
                let variants = transliterator.to_phonetic_keys(word);
                for v in variants {
                    if v.len() >= 2 {
                        let first = v[0];
                        let last = *v.last().unwrap();
                        buckets.entry((first, last)).or_default().push(PhoneticWordEntry {
                            word_id: id,
                            freq,
                            keys: v,
                        });
                    }
                }
            }
        }

        PhoneticIndex { buckets }
    }
}

static PHONETIC_CACHE: OnceLock<RwLock<HashMap<String, Arc<PhoneticIndex>>>> = OnceLock::new();

pub fn get_or_build_phonetic_index(
    dict: &BinaryDict,
    transliterator: &dyn PhoneticTransliterator,
) -> Arc<PhoneticIndex> {
    let cache_lock = PHONETIC_CACHE.get_or_init(|| RwLock::new(HashMap::new()));
    let id = transliterator.id();

    {
        let r = cache_lock.read();
        if let Some(idx) = r.get(id) {
            return Arc::clone(idx);
        }
    }

    let mut w = cache_lock.write();
    if let Some(idx) = w.get(id) {
        return Arc::clone(idx);
    }

    let index = Arc::new(PhoneticIndex::build(dict, transliterator));
    w.insert(id.to_string(), Arc::clone(&index));
    index
}

pub struct GestureDecoder;

impl GestureDecoder {
    /// Resamples an arbitrary length touch trajectory polyline into exactly `target_count` equidistant points
    pub fn resample_polyline(points: &[GesturePoint], target_count: usize) -> Vec<GesturePoint> {
        if points.is_empty() {
            return Vec::new();
        }
        if points.len() == 1 || target_count <= 1 {
            return vec![points[0].clone(); target_count.max(1)];
        }

        let mut total_length = 0.0f32;
        for i in 1..points.len() {
            let dx = points[i].x - points[i - 1].x;
            let dy = points[i].y - points[i - 1].y;
            total_length += (dx * dx + dy * dy).sqrt();
        }

        if total_length == 0.0 {
            return vec![points[0].clone(); target_count];
        }

        let interval = total_length / (target_count - 1) as f32;
        let mut resampled = Vec::with_capacity(target_count);
        resampled.push(points[0].clone());

        let mut accumulated = 0.0f32;
        let mut current_pt = points[0].clone();
        let mut i = 1;

        while i < points.len() && resampled.len() < target_count {
            let dx = points[i].x - current_pt.x;
            let dy = points[i].y - current_pt.y;
            let dist = (dx * dx + dy * dy).sqrt();

            if accumulated + dist >= interval {
                let t = (interval - accumulated) / dist;
                let next_pt = GesturePoint {
                    x: current_pt.x + t * dx,
                    y: current_pt.y + t * dy,
                    timestamp_ms: current_pt.timestamp_ms
                        + (t * (points[i].timestamp_ms - current_pt.timestamp_ms) as f32) as i64,
                };
                resampled.push(next_pt.clone());
                current_pt = next_pt;
                accumulated = 0.0;
            } else {
                accumulated += dist;
                current_pt = points[i].clone();
                i += 1;
            }
        }

        while resampled.len() < target_count {
            resampled.push(points.last().unwrap().clone());
        }

        resampled
    }

    /// Decodes a touch trajectory against keyboard key centroids and binary dictionary.
    pub fn decode(
        points: &[GesturePoint],
        keys: &[KeyCenter],
        key_width: f32,
        dict: &BinaryDict,
        limit: usize,
    ) -> Vec<GestureCandidate> {
        if points.len() < 2 || keys.is_empty() || key_width <= 0.0 {
            return Vec::new();
        }

        let kw = key_width.max(10.0);
        let sample_count = 30;
        let user_path = Self::resample_polyline(points, sample_count);
        let start_pt = &user_path[0];
        let end_pt = &user_path[sample_count - 1];

        let mut key_map: HashMap<char, (f32, f32)> = HashMap::with_capacity(keys.len());
        for k in keys {
            key_map.insert(k.char_code.to_ascii_lowercase(), (k.x, k.y));
        }

        let start_radius = 2.4 * kw;
        let end_radius = 2.4 * kw;

        let start_keys: Vec<char> = keys
            .iter()
            .filter(|k| {
                let dx = k.x - start_pt.x;
                let dy = k.y - start_pt.y;
                dx * dx + dy * dy <= start_radius * start_radius
            })
            .map(|k| k.char_code.to_ascii_lowercase())
            .collect();

        let end_keys: HashSet<char> = keys
            .iter()
            .filter(|k| {
                let dx = k.x - end_pt.x;
                let dy = k.y - end_pt.y;
                dx * dx + dy * dy <= end_radius * end_radius
            })
            .map(|k| k.char_code.to_ascii_lowercase())
            .collect();

        if start_keys.is_empty() || end_keys.is_empty() {
            return Vec::new();
        }

        let mut candidates: Vec<GestureCandidate> = Vec::new();

        for &start_ch in &start_keys {
            if let Some(start_node) = dict.child(0, start_ch as u16) {
                let mut word_buffer = vec![start_ch as u16];
                Self::collect_words(
                    dict,
                    start_node,
                    &mut word_buffer,
                    &end_keys,
                    &key_map,
                    &user_path,
                    kw,
                    &mut candidates,
                );
            }
        }

        let mut word_scores: HashMap<String, f32> = HashMap::with_capacity(candidates.len());
        for c in candidates {
            let slot = word_scores.entry(c.word).or_insert(f32::MIN);
            if c.score > *slot {
                *slot = c.score;
            }
        }

        let mut deduped: Vec<GestureCandidate> = word_scores
            .into_iter()
            .map(|(word, score)| GestureCandidate { word, score })
            .collect();

        deduped.sort_by(|a, b| b.score.partial_cmp(&a.score).unwrap_or(std::cmp::Ordering::Equal));
        deduped.truncate(limit);
        deduped
    }

    /// Windowed Dynamic Time Warping distance between two sampled polyline sequences.
    pub fn dtw_distance(seq_a: &[GesturePoint], seq_b: &[GesturePoint], window: usize) -> f32 {
        let n = seq_a.len();
        let m = seq_b.len();
        if n == 0 || m == 0 {
            return f32::MAX;
        }

        const MAX_LEN: usize = 64;
        let mut prev = [f32::INFINITY; MAX_LEN];
        let mut curr = [f32::INFINITY; MAX_LEN];

        prev[0] = 0.0;

        for i in 1..=n.min(MAX_LEN - 1) {
            let pt_a = &seq_a[i - 1];
            curr[0] = f32::INFINITY;

            let j_start = if i > window { i - window } else { 1 };
            let j_end = (i + window).min(m.min(MAX_LEN - 1));

            for j in 1..j_start {
                curr[j] = f32::INFINITY;
            }

            for j in j_start..=j_end {
                let pt_b = &seq_b[j - 1];
                let dx = pt_a.x - pt_b.x;
                let dy = pt_a.y - pt_b.y;
                let dist = (dx * dx + dy * dy).sqrt();

                let min_prev = prev[j].min(curr[j - 1]).min(prev[j - 1]);
                curr[j] = dist + min_prev;
            }

            for j in (j_end + 1)..=m.min(MAX_LEN - 1) {
                curr[j] = f32::INFINITY;
            }

            prev = curr;
        }

        let target_m = m.min(MAX_LEN - 1);
        let total = prev[target_m];
        if total.is_finite() {
            total / (n.max(m) as f32)
        } else {
            let mut sum = 0.0;
            let count = n.min(m);
            for k in 0..count {
                let dx = seq_a[k].x - seq_b[k].x;
                let dy = seq_a[k].y - seq_b[k].y;
                sum += (dx * dx + dy * dy).sqrt();
            }
            sum / count.max(1) as f32
        }
    }

    /// Evaluates shape match and scoring of a candidate word path against the user's touch trajectory.
    /// Uses windowed Dynamic Time Warping (DTW), key-visit proximity verification, and log-scaled frequency.
    pub fn evaluate_candidate(
        ideal_chars: &[char],
        key_map: &HashMap<char, (f32, f32)>,
        user_path: &[GesturePoint],
        kw: f32,
        freq: u8,
    ) -> Option<f32> {
        if ideal_chars.len() < 2 || user_path.len() < 2 {
            return None;
        }

        let mut ideal_pts = Vec::with_capacity(ideal_chars.len());
        for &ch in ideal_chars {
            if let Some(&pos) = key_map.get(&ch.to_ascii_lowercase()) {
                ideal_pts.push(GesturePoint {
                    x: pos.0,
                    y: pos.1,
                    timestamp_ms: 0,
                });
            } else {
                return None;
            }
        }

        // Start and end anchor distances (normalized by key width)
        let d_start = {
            let dx = user_path[0].x - ideal_pts[0].x;
            let dy = user_path[0].y - ideal_pts[0].y;
            (dx * dx + dy * dy).sqrt() / kw
        };
        let d_end = {
            let dx = user_path.last().unwrap().x - ideal_pts.last().unwrap().x;
            let dy = user_path.last().unwrap().y - ideal_pts.last().unwrap().y;
            (dx * dx + dy * dy).sqrt() / kw
        };

        // If start or end key is too far from gesture endpoints, reject
        if d_start > 2.2 || d_end > 2.2 {
            return None;
        }

        // Key-visit verification: every key in the candidate must be approached by the trajectory
        let mut max_key_miss = 0.0f32;
        let mut total_key_miss = 0.0f32;
        for k_pt in &ideal_pts {
            let mut min_d = f32::MAX;
            for u_pt in user_path {
                let dx = u_pt.x - k_pt.x;
                let dy = u_pt.y - k_pt.y;
                let d = (dx * dx + dy * dy).sqrt();
                if d < min_d {
                    min_d = d;
                }
            }
            let norm_d = min_d / kw;
            if norm_d > max_key_miss {
                max_key_miss = norm_d;
            }
            total_key_miss += norm_d;
        }

        // Candidates containing keys never visited (e.g. > 1.6 key widths away) are eliminated
        if max_key_miss > 1.6 {
            return None;
        }

        let avg_key_miss = total_key_miss / ideal_pts.len() as f32;

        // Dynamic Time Warping (DTW) with Sakoe-Chiba band window
        let resampled_ideal = Self::resample_polyline(&ideal_pts, user_path.len());
        let dtw_dist = Self::dtw_distance(user_path, &resampled_ideal, 6) / kw;

        // Key miss penalty for keys that were relatively far (> 1.15 key width)
        let miss_penalty = if max_key_miss > 1.15 {
            (max_key_miss - 1.15) * 2.5
        } else {
            0.0
        };

        let total_cost = dtw_dist + 0.5 * d_start + 0.5 * d_end + 0.4 * avg_key_miss + miss_penalty;

        if total_cost > 3.0 {
            return None;
        }

        // Shape score dominates (0 to 100), log-scaled frequency acts as tie-breaker (0 to 15)
        let shape_score = (-total_cost * 1.5).exp();
        let freq_score = (1.0 + freq as f32).ln() / (256.0f32).ln();
        let score = 100.0 * shape_score + 15.0 * freq_score;

        Some(score)
    }

    #[allow(clippy::too_many_arguments)]
    fn collect_words(
        dict: &BinaryDict,
        node: u32,
        buffer: &mut Vec<u16>,
        end_keys: &HashSet<char>,
        key_map: &HashMap<char, (f32, f32)>,
        user_path: &[GesturePoint],
        kw: f32,
        results: &mut Vec<GestureCandidate>,
    ) {
        if buffer.len() > 24 {
            return;
        }

        if dict.is_word(node) && buffer.len() >= 2 {
            let last_u16 = *buffer.last().unwrap();
            if let Some(last_ch) = char::from_u32(last_u16 as u32) {
                if end_keys.contains(&last_ch.to_ascii_lowercase()) {
                    let freq = dict.freq(node);
                    if freq >= 1 {
                        let chars: Vec<char> = buffer.iter().filter_map(|&u| char::from_u32(u as u32)).collect();
                        if chars.len() == buffer.len() {
                            if let Some(score) = Self::evaluate_candidate(
                                &chars,
                                key_map,
                                user_path,
                                kw,
                                freq,
                            ) {
                                let word_str = String::from_utf16_lossy(buffer);
                                results.push(GestureCandidate {
                                    word: word_str,
                                    score,
                                });
                            }
                        }
                    }
                }
            }
        }

        let start = dict.child_start(node as usize);
        let count = dict.child_count(node as usize);
        for e in start..start + count {
            let child_node = (e + 1) as u32;
            let label = dict.edge_label(e);
            buffer.push(label);
            Self::collect_words(
                dict,
                child_node,
                buffer,
                end_keys,
                key_map,
                user_path,
                kw,
                results,
            );
            buffer.pop();
        }
    }

    /// Decodes a touch trajectory against keyboard key centroids for a phonetic / transliteration layout.
    /// The keys on screen are Latin QWERTY, but the target vocabulary comes from the target language dictionary.
    pub fn decode_phonetic(
        points: &[GesturePoint],
        keys: &[KeyCenter],
        key_width: f32,
        dict: &BinaryDict,
        transliterator: &dyn PhoneticTransliterator,
        limit: usize,
    ) -> Vec<GestureCandidate> {
        if points.len() < 2 || keys.is_empty() || key_width <= 0.0 {
            return Vec::new();
        }

        let kw = key_width.max(10.0);
        let sample_count = 30;
        let user_path = Self::resample_polyline(points, sample_count);
        let start_pt = &user_path[0];
        let end_pt = &user_path[sample_count - 1];

        let mut key_map: HashMap<char, (f32, f32)> = HashMap::with_capacity(keys.len());
        for k in keys {
            key_map.insert(k.char_code.to_ascii_lowercase(), (k.x, k.y));
        }

        let start_radius = 2.4 * kw;
        let end_radius = 2.4 * kw;

        let start_keys: Vec<char> = keys
            .iter()
            .filter(|k| {
                let dx = k.x - start_pt.x;
                let dy = k.y - start_pt.y;
                dx * dx + dy * dy <= start_radius * start_radius
            })
            .map(|k| k.char_code.to_ascii_lowercase())
            .collect();

        let end_keys: HashSet<char> = keys
            .iter()
            .filter(|k| {
                let dx = k.x - end_pt.x;
                let dy = k.y - end_pt.y;
                dx * dx + dy * dy <= end_radius * end_radius
            })
            .map(|k| k.char_code.to_ascii_lowercase())
            .collect();

        if start_keys.is_empty() || end_keys.is_empty() {
            return Vec::new();
        }

        let index = get_or_build_phonetic_index(dict, transliterator);

        let mut word_scores: HashMap<String, f32> = HashMap::new();

        for &s in &start_keys {
            for &e in &end_keys {
                if let Some(entries) = index.buckets.get(&(s, e)) {
                    for entry in entries {
                        if let Some(score) = Self::evaluate_candidate(
                            &entry.keys,
                            &key_map,
                            &user_path,
                            kw,
                            entry.freq,
                        ) {
                            if let Some(word_str) = dict.word_at(entry.word_id) {
                                let norm_word = word_str.replace("\u{0985}\u{09BE}", "আ");
                                let slot = word_scores.entry(norm_word).or_insert(f32::MIN);
                                if score > *slot {
                                    *slot = score;
                                }
                            }
                        }
                    }
                }
            }
        }

        let mut candidates: Vec<GestureCandidate> = word_scores
            .into_iter()
            .map(|(word, score)| GestureCandidate { word, score })
            .collect();

        candidates.sort_by(|a, b| b.score.partial_cmp(&a.score).unwrap_or(std::cmp::Ordering::Equal));
        candidates.truncate(limit);
        candidates
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dictionary::{get_bn_binary_dict, get_en_binary_dict};
    use crate::translit::borno_phonetic::BornoPhoneticTransliterator;

    #[test]
    fn test_resample_polyline() {
        let pts = vec![
            GesturePoint { x: 0.0, y: 0.0, timestamp_ms: 0 },
            GesturePoint { x: 100.0, y: 0.0, timestamp_ms: 100 },
        ];
        let resampled = GestureDecoder::resample_polyline(&pts, 5);
        assert_eq!(resampled.len(), 5);
        assert!((resampled[0].x - 0.0).abs() < 1e-3);
        assert!((resampled[1].x - 25.0).abs() < 1e-3);
        assert!((resampled[2].x - 50.0).abs() < 1e-3);
        assert!((resampled[3].x - 75.0).abs() < 1e-3);
        assert!((resampled[4].x - 100.0).abs() < 1e-3);
    }

    #[test]
    fn test_decode_the_gesture() {
        let dict = get_en_binary_dict().expect("en dict");

        // Mock QWERTY key centers: 't' -> 'h' -> 'e'
        let keys = vec![
            KeyCenter { char_code: 'q', x: 20.0, y: 50.0 },
            KeyCenter { char_code: 'w', x: 60.0, y: 50.0 },
            KeyCenter { char_code: 'e', x: 100.0, y: 50.0 },
            KeyCenter { char_code: 'r', x: 140.0, y: 50.0 },
            KeyCenter { char_code: 't', x: 180.0, y: 50.0 },
            KeyCenter { char_code: 'y', x: 220.0, y: 50.0 },
            KeyCenter { char_code: 'u', x: 260.0, y: 50.0 },
            KeyCenter { char_code: 'i', x: 300.0, y: 50.0 },
            KeyCenter { char_code: 'o', x: 340.0, y: 50.0 },
            KeyCenter { char_code: 'p', x: 380.0, y: 50.0 },
            KeyCenter { char_code: 'a', x: 40.0, y: 120.0 },
            KeyCenter { char_code: 's', x: 80.0, y: 120.0 },
            KeyCenter { char_code: 'd', x: 120.0, y: 120.0 },
            KeyCenter { char_code: 'f', x: 160.0, y: 120.0 },
            KeyCenter { char_code: 'g', x: 200.0, y: 120.0 },
            KeyCenter { char_code: 'h', x: 240.0, y: 120.0 },
            KeyCenter { char_code: 'j', x: 280.0, y: 120.0 },
            KeyCenter { char_code: 'k', x: 320.0, y: 120.0 },
            KeyCenter { char_code: 'l', x: 360.0, y: 120.0 },
        ];

        // Swipe points for "the": (180, 50) -> (240, 120) -> (100, 50)
        let stroke = vec![
            GesturePoint { x: 180.0, y: 50.0, timestamp_ms: 0 },
            GesturePoint { x: 210.0, y: 85.0, timestamp_ms: 50 },
            GesturePoint { x: 240.0, y: 120.0, timestamp_ms: 100 },
            GesturePoint { x: 170.0, y: 85.0, timestamp_ms: 150 },
            GesturePoint { x: 100.0, y: 50.0, timestamp_ms: 200 },
        ];

        let results = GestureDecoder::decode(&stroke, &keys, 40.0, dict, 5);
        assert!(!results.is_empty());
        assert_eq!(results[0].word, "the");
    }

    #[test]
    fn test_decode_borno_phonetic_gesture() {
        let dict = match get_bn_binary_dict() {
            Some(d) => d,
            None => return, // In test environments where bn.dict is not deployed
        };
        let translit = BornoPhoneticTransliterator;

        let keys = vec![
            KeyCenter { char_code: 'q', x: 20.0, y: 50.0 },
            KeyCenter { char_code: 'w', x: 60.0, y: 50.0 },
            KeyCenter { char_code: 'e', x: 100.0, y: 50.0 },
            KeyCenter { char_code: 'r', x: 140.0, y: 50.0 },
            KeyCenter { char_code: 't', x: 180.0, y: 50.0 },
            KeyCenter { char_code: 'y', x: 220.0, y: 50.0 },
            KeyCenter { char_code: 'u', x: 260.0, y: 50.0 },
            KeyCenter { char_code: 'i', x: 300.0, y: 50.0 },
            KeyCenter { char_code: 'o', x: 340.0, y: 50.0 },
            KeyCenter { char_code: 'p', x: 380.0, y: 50.0 },
            KeyCenter { char_code: 'a', x: 40.0, y: 120.0 },
            KeyCenter { char_code: 's', x: 80.0, y: 120.0 },
            KeyCenter { char_code: 'd', x: 120.0, y: 120.0 },
            KeyCenter { char_code: 'f', x: 160.0, y: 120.0 },
            KeyCenter { char_code: 'g', x: 200.0, y: 120.0 },
            KeyCenter { char_code: 'h', x: 240.0, y: 120.0 },
            KeyCenter { char_code: 'j', x: 280.0, y: 120.0 },
            KeyCenter { char_code: 'k', x: 320.0, y: 120.0 },
            KeyCenter { char_code: 'l', x: 360.0, y: 120.0 },
            KeyCenter { char_code: 'z', x: 60.0, y: 190.0 },
            KeyCenter { char_code: 'x', x: 100.0, y: 190.0 },
            KeyCenter { char_code: 'c', x: 140.0, y: 190.0 },
            KeyCenter { char_code: 'v', x: 180.0, y: 190.0 },
            KeyCenter { char_code: 'b', x: 220.0, y: 190.0 },
            KeyCenter { char_code: 'n', x: 260.0, y: 190.0 },
            KeyCenter { char_code: 'm', x: 300.0, y: 190.0 },
        ];

        // Gesture: 'a' (40, 120) -> 'm' (300, 190) -> 'i' (300, 50) for "আমি"
        let stroke_ami = vec![
            GesturePoint { x: 40.0, y: 120.0, timestamp_ms: 0 },
            GesturePoint { x: 170.0, y: 155.0, timestamp_ms: 50 },
            GesturePoint { x: 300.0, y: 190.0, timestamp_ms: 100 },
            GesturePoint { x: 300.0, y: 120.0, timestamp_ms: 150 },
            GesturePoint { x: 300.0, y: 50.0, timestamp_ms: 200 },
        ];

        let results_ami = GestureDecoder::decode_phonetic(&stroke_ami, &keys, 40.0, dict, &translit, 5);
        assert!(!results_ami.is_empty(), "Expected results for 'ami'");
        assert_eq!(results_ami[0].word, "আমি");

        // Gesture: 'a' -> 'm' -> 'a' -> 'd' -> 'e' -> 'r' for "আমাদের"
        let stroke_amader = vec![
            GesturePoint { x: 40.0, y: 120.0, timestamp_ms: 0 },    // a
            GesturePoint { x: 300.0, y: 190.0, timestamp_ms: 50 },  // m
            GesturePoint { x: 40.0, y: 120.0, timestamp_ms: 100 },  // a
            GesturePoint { x: 120.0, y: 120.0, timestamp_ms: 150 }, // d
            GesturePoint { x: 100.0, y: 50.0, timestamp_ms: 200 },  // e
            GesturePoint { x: 140.0, y: 50.0, timestamp_ms: 250 },  // r
        ];

        let results_amader = GestureDecoder::decode_phonetic(&stroke_amader, &keys, 40.0, dict, &translit, 5);
        assert!(!results_amader.is_empty(), "Expected results for 'amader'");
        assert_eq!(results_amader[0].word, "আমাদের");
    }

    #[test]
    fn test_decode_milking_gesture() {
        let dict = get_en_binary_dict().expect("en dict");
        let keys = vec![
            KeyCenter { char_code: 'q', x: 20.0, y: 50.0 },
            KeyCenter { char_code: 'w', x: 60.0, y: 50.0 },
            KeyCenter { char_code: 'e', x: 100.0, y: 50.0 },
            KeyCenter { char_code: 'r', x: 140.0, y: 50.0 },
            KeyCenter { char_code: 't', x: 180.0, y: 50.0 },
            KeyCenter { char_code: 'y', x: 220.0, y: 50.0 },
            KeyCenter { char_code: 'u', x: 260.0, y: 50.0 },
            KeyCenter { char_code: 'i', x: 300.0, y: 50.0 },
            KeyCenter { char_code: 'o', x: 340.0, y: 50.0 },
            KeyCenter { char_code: 'p', x: 380.0, y: 50.0 },
            KeyCenter { char_code: 'a', x: 40.0, y: 120.0 },
            KeyCenter { char_code: 's', x: 80.0, y: 120.0 },
            KeyCenter { char_code: 'd', x: 120.0, y: 120.0 },
            KeyCenter { char_code: 'f', x: 160.0, y: 120.0 },
            KeyCenter { char_code: 'g', x: 200.0, y: 120.0 },
            KeyCenter { char_code: 'h', x: 240.0, y: 120.0 },
            KeyCenter { char_code: 'j', x: 280.0, y: 120.0 },
            KeyCenter { char_code: 'k', x: 320.0, y: 120.0 },
            KeyCenter { char_code: 'l', x: 360.0, y: 120.0 },
            KeyCenter { char_code: 'z', x: 60.0, y: 190.0 },
            KeyCenter { char_code: 'x', x: 100.0, y: 190.0 },
            KeyCenter { char_code: 'c', x: 140.0, y: 190.0 },
            KeyCenter { char_code: 'v', x: 180.0, y: 190.0 },
            KeyCenter { char_code: 'b', x: 220.0, y: 190.0 },
            KeyCenter { char_code: 'n', x: 260.0, y: 190.0 },
            KeyCenter { char_code: 'm', x: 300.0, y: 190.0 },
        ];

        // Swipe points for "milking": m -> i -> l -> k -> i -> n -> g
        let stroke_milking = vec![
            GesturePoint { x: 300.0, y: 190.0, timestamp_ms: 0 },   // m
            GesturePoint { x: 300.0, y: 50.0, timestamp_ms: 50 },    // i
            GesturePoint { x: 360.0, y: 120.0, timestamp_ms: 100 },  // l
            GesturePoint { x: 320.0, y: 120.0, timestamp_ms: 150 },  // k
            GesturePoint { x: 300.0, y: 50.0, timestamp_ms: 200 },   // i
            GesturePoint { x: 260.0, y: 190.0, timestamp_ms: 250 },  // n
            GesturePoint { x: 200.0, y: 120.0, timestamp_ms: 300 },  // g
        ];

        let results = GestureDecoder::decode(&stroke_milking, &keys, 40.0, dict, 10);
        assert!(!results.is_empty(), "Results should not be empty");
        assert_eq!(results[0].word, "milking", "Expected 'milking' to be the top candidate: {:?}", results);
    }

    #[test]
    fn test_decode_intarnet_gesture() {
        let dict = get_bn_binary_dict().expect("bn dict");
        let translit = BornoPhoneticTransliterator;

        let keys = vec![
            KeyCenter { char_code: 'q', x: 20.0, y: 50.0 },
            KeyCenter { char_code: 'w', x: 60.0, y: 50.0 },
            KeyCenter { char_code: 'e', x: 100.0, y: 50.0 },
            KeyCenter { char_code: 'r', x: 140.0, y: 50.0 },
            KeyCenter { char_code: 't', x: 180.0, y: 50.0 },
            KeyCenter { char_code: 'y', x: 220.0, y: 50.0 },
            KeyCenter { char_code: 'u', x: 260.0, y: 50.0 },
            KeyCenter { char_code: 'i', x: 300.0, y: 50.0 },
            KeyCenter { char_code: 'o', x: 340.0, y: 50.0 },
            KeyCenter { char_code: 'p', x: 380.0, y: 50.0 },
            KeyCenter { char_code: 'a', x: 40.0, y: 120.0 },
            KeyCenter { char_code: 's', x: 80.0, y: 120.0 },
            KeyCenter { char_code: 'd', x: 120.0, y: 120.0 },
            KeyCenter { char_code: 'f', x: 160.0, y: 120.0 },
            KeyCenter { char_code: 'g', x: 200.0, y: 120.0 },
            KeyCenter { char_code: 'h', x: 240.0, y: 120.0 },
            KeyCenter { char_code: 'j', x: 280.0, y: 120.0 },
            KeyCenter { char_code: 'k', x: 320.0, y: 120.0 },
            KeyCenter { char_code: 'l', x: 360.0, y: 120.0 },
            KeyCenter { char_code: 'z', x: 60.0, y: 190.0 },
            KeyCenter { char_code: 'x', x: 100.0, y: 190.0 },
            KeyCenter { char_code: 'c', x: 140.0, y: 190.0 },
            KeyCenter { char_code: 'v', x: 180.0, y: 190.0 },
            KeyCenter { char_code: 'b', x: 220.0, y: 190.0 },
            KeyCenter { char_code: 'n', x: 260.0, y: 190.0 },
            KeyCenter { char_code: 'm', x: 300.0, y: 190.0 },
        ];

        // Swipe: i -> n -> t -> a -> r -> n -> e -> t
        let stroke_intarnet = vec![
            GesturePoint { x: 300.0, y: 50.0, timestamp_ms: 0 },    // i
            GesturePoint { x: 260.0, y: 190.0, timestamp_ms: 50 },  // n
            GesturePoint { x: 180.0, y: 50.0, timestamp_ms: 100 },  // t
            GesturePoint { x: 40.0, y: 120.0, timestamp_ms: 150 },  // a
            GesturePoint { x: 140.0, y: 50.0, timestamp_ms: 200 },  // r
            GesturePoint { x: 260.0, y: 190.0, timestamp_ms: 250 },  // n
            GesturePoint { x: 100.0, y: 50.0, timestamp_ms: 300 },  // e
            GesturePoint { x: 180.0, y: 50.0, timestamp_ms: 350 },  // t
        ];

        let results = GestureDecoder::decode_phonetic(&stroke_intarnet, &keys, 40.0, dict, &translit, 10);
        assert!(!results.is_empty(), "Results for intarnet should not be empty");
        assert_eq!(results[0].word, "ইন্টারনেট", "Expected 'ইন্টারনেট' to be top candidate for intarnet stroke: {:?}", results);
    }
}
