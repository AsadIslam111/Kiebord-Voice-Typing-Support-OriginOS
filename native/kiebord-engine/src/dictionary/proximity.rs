use std::collections::{HashMap, HashSet};

/// Physical key adjacency and phonetic confusion matrix for typo weighting.
#[derive(Clone, Debug, Default)]
pub struct KeyProximity {
    neighbors: HashMap<char, HashSet<char>>,
}

impl KeyProximity {
    /// Builds proximity from keyboard row strings.
    pub fn of_rows(rows: &[&str]) -> Self {
        let mut neighbors: HashMap<char, HashSet<char>> = HashMap::new();
        let grid: Vec<Vec<char>> = rows.iter().map(|r| r.chars().collect()).collect();

        for (r, row) in grid.iter().enumerate() {
            for (c, &ch) in row.iter().enumerate() {
                let set = neighbors.entry(ch).or_default();

                // Same row left and right
                if c > 0 {
                    set.insert(row[c - 1]);
                }
                if c + 1 < row.len() {
                    set.insert(row[c + 1]);
                }

                // Row above: c-1, c, c+1
                if r > 0 {
                    let above = &grid[r - 1];
                    for col in c.saturating_sub(1)..=c + 1 {
                        if let Some(&adj) = above.get(col) {
                            set.insert(adj);
                        }
                    }
                }

                // Row below: c-1, c, c+1
                if r + 1 < grid.len() {
                    let below = &grid[r + 1];
                    for col in c.saturating_sub(1)..=c + 1 {
                        if let Some(&adj) = below.get(col) {
                            set.insert(adj);
                        }
                    }
                }
            }
        }

        Self { neighbors }
    }

    /// English QWERTY key proximity.
    pub fn qwerty() -> Self {
        Self::of_rows(&["qwertyuiop", "asdfghjkl", "zxcvbnm"])
    }

    /// Bengali Probhat key proximity and phonetically confusable pairs.
    pub fn bengali() -> Self {
        let mut prox = Self::of_rows(&[
            "দধপফবভতমযয",
            "আঅেসহকখগঘ",
            "য়ড়ঢ়ণতথদধ",
        ]);

        // Add common Bengali phonetic/orthographic confusion clusters
        let confusion_groups: &[&[char]] = &[
            &['শ', 'ষ', 'স'],
            &['র', 'ড়', 'ঢ়'],
            &['ন', 'ণ'],
            &['য', 'জ'],
            &['ব', 'ভ'],
            &['প', 'ফ'],
            &['ক', 'খ'],
            &['গ', 'ঘ'],
            &['ত', 'ৎ', 'থ'],
            &['দ', 'ধ'],
            &['ি', 'ী'],
            &['ু', 'ূ'],
            &['এ', 'ঐ'],
            &['ও', 'ঔ'],
        ];

        for &group in confusion_groups {
            for &c1 in group {
                for &c2 in group {
                    if c1 != c2 {
                        prox.neighbors.entry(c1).or_default().insert(c2);
                    }
                }
            }
        }

        prox
    }

    /// Checks if two characters are physical keyboard neighbors or confusable.
    pub fn are_neighbors(&self, a: char, b: char) -> bool {
        self.neighbors.get(&a).map_or(false, |s| s.contains(&b))
    }

    /// Returns iterator over neighboring or confusable characters for a given key.
    pub fn get_neighbors(&self, c: char) -> impl Iterator<Item = &char> {
        self.neighbors.get(&c).into_iter().flat_map(|s| s.iter())
    }

    /// Calculates edit penalty:
    /// - 0.0 for identical characters
    /// - 0.4 for neighboring keys (finger slip)
    /// - 1.2 for distant substitution
    pub fn penalty(&self, typed: char, candidate: char) -> f32 {
        if typed == candidate {
            0.0
        } else if self.are_neighbors(typed, candidate) {
            0.4
        } else {
            1.2
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_qwerty_neighbors() {
        let q = KeyProximity::qwerty();
        // 'g' and 'h' are side by side
        assert!(q.are_neighbors('g', 'h'));
        assert!(q.are_neighbors('h', 'g'));

        // 'h' and 'y' (above row)
        assert!(q.are_neighbors('h', 'y'));

        // 'h' and 'b' (below row)
        assert!(q.are_neighbors('h', 'b'));

        // 'q' and 'p' are on opposite ends
        assert!(!q.are_neighbors('q', 'p'));

        // Penalty tests
        assert_eq!(q.penalty('h', 'h'), 0.0);
        assert_eq!(q.penalty('g', 'h'), 0.4);
        assert_eq!(q.penalty('q', 'h'), 1.2);
    }

    #[test]
    fn test_bengali_confusion() {
        let b = KeyProximity::bengali();
        assert!(b.are_neighbors('শ', 'ষ'));
        assert!(b.are_neighbors('র', 'ড়'));
        assert!(b.are_neighbors('ন', 'ণ'));
        assert_eq!(b.penalty('শ', 'ষ'), 0.4);
    }
}
