use std::collections::HashMap;
use std::sync::Arc;

use super::ast::*;

#[derive(Debug, Clone)]
struct Snapshot {
    state: String,
    vars: HashMap<String, i32>,
    markers: HashMap<String, usize>,
    preedit: Vec<char>,
    cursor: usize,
    committed: String,
}

#[derive(Debug, Clone)]
pub struct M17nEngine {
    im: Arc<InputMethod>,
    state: String,
    vars: HashMap<String, i32>,
    markers: HashMap<String, usize>,
    preedit: Vec<char>,
    cursor: usize,
    committed: String,
    undo_stack: Vec<Snapshot>,
    key_buffer: Vec<char>,
}

impl M17nEngine {
    pub fn new(im: Arc<InputMethod>) -> Self {
        let initial_state = im.initial_state.clone();
        let mut engine = Self {
            im,
            state: initial_state,
            vars: HashMap::new(),
            markers: HashMap::new(),
            preedit: Vec::new(),
            cursor: 0,
            committed: String::new(),
            undo_stack: Vec::new(),
            key_buffer: Vec::new(),
        };
        engine.run_transient_rules();
        engine
    }

    pub fn reset(&mut self) {
        self.state = self.im.initial_state.clone();
        self.vars.clear();
        self.markers.clear();
        self.preedit.clear();
        self.cursor = 0;
        self.committed.clear();
        self.undo_stack.clear();
        self.key_buffer.clear();
        self.run_transient_rules();
    }

    fn save_snapshot(&mut self) {
        self.undo_stack.push(Snapshot {
            state: self.state.clone(),
            vars: self.vars.clone(),
            markers: self.markers.clone(),
            preedit: self.preedit.clone(),
            cursor: self.cursor,
            committed: self.committed.clone(),
        });
        if self.undo_stack.len() > 100 {
            self.undo_stack.remove(0);
        }
    }

    pub fn feed_backspace(&mut self) -> bool {
        if let Some(snap) = self.undo_stack.pop() {
            self.state = snap.state;
            self.vars = snap.vars;
            self.markers = snap.markers;
            self.preedit = snap.preedit;
            self.cursor = snap.cursor;
            self.committed = snap.committed;
            self.key_buffer.clear();
            return true;
        }

        if self.cursor > 0 && !self.preedit.is_empty() {
            let removed_idx = self.cursor - 1;
            self.delete_range(removed_idx, removed_idx + 1);
            return true;
        }
        false
    }

    pub fn feed_char(&mut self, c: char) {
        self.save_snapshot();
        self.key_buffer.push(c);
        let rem: String = self.key_buffer.iter().collect();

        if let Some(matched_len) = self.step_longest_match(&rem) {
            self.key_buffer.drain(0..matched_len);
        } else {
            // Check if key_buffer can be a prefix of any rule in current state
            let can_be_prefix = self.can_prefix_any_rule(&rem);
            if !can_be_prefix {
                if self.state != self.im.initial_state {
                    // According to m17n specification:
                    // If the typed key sequence does not match any branch in the current state,
                    // the input method transits to the initial state without consuming the key event.
                    self.commit();
                    self.state = self.im.initial_state.clone();
                    self.run_transient_rules();

                    let rem2: String = self.key_buffer.iter().collect();
                    if let Some(matched_len) = self.step_longest_match(&rem2) {
                        self.key_buffer.drain(0..matched_len);
                    } else if !self.can_prefix_any_rule(&rem2) {
                        let first = self.key_buffer.remove(0);
                        self.committed.push(first);
                    }
                } else {
                    // If already in initial state and not a prefix, output first char
                    let first = self.key_buffer.remove(0);
                    self.committed.push(first);
                }
            }
        }
    }

    fn can_prefix_any_rule(&self, prefix: &str) -> bool {
        let state_def = match self.im.states.get(&self.state) {
            Some(s) => s,
            None => return false,
        };

        for branch in &state_def.branches {
            if let BranchTarget::Map(map_name) = &branch.target {
                if let Some(map_def) = self.im.maps.get(map_name) {
                    for rule in &map_def.rules {
                        if let KeySeq::Chars(seq) = &rule.key_seq {
                            if seq.starts_with(prefix) && seq.len() > prefix.len() {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        false
    }

    pub fn transliterate(&mut self, input: &str) -> String {
        self.reset();
        let input_chars: Vec<char> = input.chars().collect();
        let mut pos = 0;

        while pos < input_chars.len() {
            let rem: String = input_chars[pos..].iter().collect();
            if let Some(matched_len) = self.step_longest_match(&rem) {
                pos += matched_len;
            } else if self.state != self.im.initial_state {
                // According to m17n specification:
                // If the typed key sequence does not match any branch in the current state
                // (and there's no nil branch), the input method transits to the initial state
                // without consuming the key event.
                self.commit();
                self.state = self.im.initial_state.clone();
                self.run_transient_rules();
            } else {
                let c = input_chars[pos];
                self.committed.push(c);
                pos += 1;
            }
        }

        self.current_output()
    }

    fn step_longest_match(&mut self, rem: &str) -> Option<usize> {
        let state_def = match self.im.states.get(&self.state) {
            Some(s) => s.clone(),
            None => return None,
        };

        struct MatchCandidate {
            rule_len: usize,
            rule_actions: Vec<Action>,
            rule_insert: Option<String>,
            branch_actions: Vec<Action>,
        }

        let mut best: Option<MatchCandidate> = None;

        for branch in &state_def.branches {
            match &branch.target {
                BranchTarget::Transient => continue,
                BranchTarget::Map(map_name) => {
                    if let Some(map_def) = self.im.maps.get(map_name) {
                        for rule in &map_def.rules {
                            if let KeySeq::Chars(seq) = &rule.key_seq {
                                if rem.starts_with(seq) {
                                    let seq_len = seq.chars().count();
                                    if best.as_ref().map_or(true, |b| seq_len > b.rule_len) {
                                        best = Some(MatchCandidate {
                                            rule_len: seq_len,
                                            rule_actions: rule.actions.clone(),
                                            rule_insert: rule.insert_text.clone(),
                                            branch_actions: branch.actions.clone(),
                                        });
                                    }
                                }
                            }
                        }
                    }
                }
                BranchTarget::Fallback => {
                    if best.is_none() && !rem.is_empty() {
                        let ch = rem.chars().next().unwrap();
                        best = Some(MatchCandidate {
                            rule_len: 1,
                            rule_actions: Vec::new(),
                            rule_insert: Some(ch.to_string()),
                            branch_actions: branch.actions.clone(),
                        });
                    }
                }
            }
        }

        let m = best?;
        let matched_len = m.rule_len;

        // 1. Execute map actions
        for act in &m.rule_actions {
            self.execute_action(act);
        }

        // 2. Insert map text
        if let Some(text) = &m.rule_insert {
            self.insert_chars(self.cursor, text);
        }

        // 3. Execute branch actions
        for act in &m.branch_actions {
            self.execute_action(act);
        }

        Some(matched_len)
    }

    fn insert_chars(&mut self, at: usize, s: &str) {
        let count = s.chars().count();
        if count == 0 {
            return;
        }
        let pos = at.min(self.preedit.len());
        for (offset, c) in s.chars().enumerate() {
            self.preedit.insert(pos + offset, c);
        }
        if self.cursor >= pos {
            self.cursor += count;
        }
        for m_pos in self.markers.values_mut() {
            if *m_pos > pos {
                *m_pos += count;
            }
        }
    }

    fn delete_range(&mut self, start: usize, end: usize) {
        let start = start.min(self.preedit.len());
        let end = end.min(self.preedit.len());
        if start >= end {
            return;
        }
        let count = end - start;
        self.preedit.drain(start..end);

        if self.cursor >= end {
            self.cursor -= count;
        } else if self.cursor > start {
            self.cursor = start;
        }

        for m_pos in self.markers.values_mut() {
            if *m_pos >= end {
                *m_pos -= count;
            } else if *m_pos > start {
                *m_pos = start;
            }
        }
    }

    fn execute_action(&mut self, action: &Action) {
        match action {
            Action::Insert(s) => {
                self.insert_chars(self.cursor, s);
            }
            Action::Delete(pos) => {
                match pos {
                    Pos::Relative(offset) => {
                        if *offset < 0 {
                            let count = (-offset) as usize;
                            let start = self.cursor.saturating_sub(count);
                            self.delete_range(start, self.cursor);
                        } else if *offset > 0 {
                            let count = *offset as usize;
                            let end = (self.cursor + count).min(self.preedit.len());
                            self.delete_range(self.cursor, end);
                        }
                    }
                    Pos::Begin => {
                        self.delete_range(0, self.cursor);
                    }
                    Pos::End => {
                        let len = self.preedit.len();
                        self.delete_range(self.cursor, len);
                    }
                    Pos::Absolute(idx) => {
                        self.delete_range(*idx, *idx + 1);
                    }
                    Pos::Mark(name) => {
                        let mark_pos = self.markers.get(name).copied().unwrap_or(self.cursor).min(self.preedit.len());
                        let (start, end) = if self.cursor <= mark_pos {
                            (self.cursor, mark_pos)
                        } else {
                            (mark_pos, self.cursor)
                        };
                        self.delete_range(start, end);
                    }
                }
            }
            Action::Move(pos) => {
                match pos {
                    Pos::Relative(offset) => {
                        if *offset < 0 {
                            self.cursor = self.cursor.saturating_sub((-offset) as usize);
                        } else {
                            self.cursor = (self.cursor + *offset as usize).min(self.preedit.len());
                        }
                    }
                    Pos::Begin => {
                        self.cursor = 0;
                    }
                    Pos::End => {
                        self.cursor = self.preedit.len();
                    }
                    Pos::Absolute(idx) => {
                        self.cursor = (*idx).min(self.preedit.len());
                    }
                    Pos::Mark(name) => {
                        if let Some(&m_pos) = self.markers.get(name) {
                            self.cursor = m_pos.min(self.preedit.len());
                        }
                    }
                }
            }
            Action::Mark(name) => {
                self.markers.insert(name.clone(), self.cursor);
            }
            Action::Set(var, val) => {
                let v = self.eval_var_val(val);
                self.vars.insert(var.clone(), v);
            }
            Action::Add(var, val) => {
                let current = self.get_var(var);
                let v = current + self.eval_var_val(val);
                self.vars.insert(var.clone(), v);
            }
            Action::Sub(var, val) => {
                let current = self.get_var(var);
                let v = current - self.eval_var_val(val);
                self.vars.insert(var.clone(), v);
            }
            Action::Shift(target) => {
                self.state = target.clone();
                self.run_transient_rules();
            }
            Action::Commit => {
                self.commit();
            }
            Action::Undo => {
                self.feed_backspace();
            }
            Action::Cond(clauses) => {
                for (cond, acts) in clauses {
                    if self.eval_cond(cond) {
                        for a in acts {
                            self.execute_action(a);
                        }
                        break;
                    }
                }
            }
            Action::MacroCall(name) => {
                if let Some(acts) = self.im.macros.get(name).cloned() {
                    for a in &acts {
                        self.execute_action(a);
                    }
                }
            }
        }
    }

    fn get_var(&self, var: &str) -> i32 {
        if var == "@-" || var == "@-1" {
            if self.cursor > 0 && self.cursor <= self.preedit.len() {
                self.preedit[self.cursor - 1] as i32
            } else {
                0
            }
        } else if var.starts_with("@-") {
            if let Ok(n) = var[2..].parse::<usize>() {
                if self.cursor >= n && self.cursor <= self.preedit.len() {
                    self.preedit[self.cursor - n] as i32
                } else {
                    0
                }
            } else {
                0
            }
        } else if var == "@+" || var == "@+1" {
            if self.cursor < self.preedit.len() {
                self.preedit[self.cursor] as i32
            } else {
                0
            }
        } else {
            self.vars.get(var).copied().unwrap_or(0)
        }
    }

    fn eval_var_val(&self, val: &VarVal) -> i32 {
        match val {
            VarVal::Const(c) => *c,
            VarVal::Var(v) => self.get_var(v),
        }
    }

    fn eval_cond(&self, expr: &CondExpr) -> bool {
        match expr {
            CondExpr::AlwaysTrue => true,
            CondExpr::Eq(var, val) => self.get_var(var) == self.eval_var_val(val),
            CondExpr::Ne(var, val) => self.get_var(var) != self.eval_var_val(val),
            CondExpr::Lt(var, val) => self.get_var(var) < self.eval_var_val(val),
            CondExpr::Le(var, val) => self.get_var(var) <= self.eval_var_val(val),
            CondExpr::Gt(var, val) => self.get_var(var) > self.eval_var_val(val),
            CondExpr::Ge(var, val) => self.get_var(var) >= self.eval_var_val(val),
            CondExpr::And(subs) => subs.iter().all(|s| self.eval_cond(s)),
            CondExpr::Or(subs) => subs.iter().any(|s| self.eval_cond(s)),
            CondExpr::Not(sub) => !self.eval_cond(sub),
        }
    }

    fn run_transient_rules(&mut self) {
        let mut visited = Vec::new();
        while let Some(state_def) = self.im.states.get(&self.state).cloned() {
            if visited.contains(&self.state) {
                break;
            }
            visited.push(self.state.clone());

            let mut transitioned = false;
            for branch in &state_def.branches {
                if branch.target == BranchTarget::Transient {
                    for act in &branch.actions {
                        let old_state = self.state.clone();
                        self.execute_action(act);
                        if self.state != old_state {
                            transitioned = true;
                            break;
                        }
                    }
                }
            }
            if !transitioned {
                break;
            }
        }
    }

    pub fn commit(&mut self) {
        self.committed.extend(self.preedit.drain(..));
        self.cursor = 0;
        self.markers.clear();
    }

    pub fn current_preedit(&self) -> String {
        self.preedit.iter().collect()
    }

    pub fn current_output(&self) -> String {
        let mut out = self.committed.clone();
        out.extend(self.preedit.iter());
        out
    }
}
