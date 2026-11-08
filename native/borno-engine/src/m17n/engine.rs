use std::collections::HashMap;
use std::sync::Arc;

use super::ast::*;

#[derive(Debug, Clone)]
struct Snapshot {
    state: String,
    vars: HashMap<String, i32>,
    preedit: Vec<char>,
    cursor: usize,
    committed: String,
}

#[derive(Debug, Clone)]
pub struct M17nEngine {
    im: Arc<InputMethod>,
    state: String,
    vars: HashMap<String, i32>,
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
            self.preedit = snap.preedit;
            self.cursor = snap.cursor;
            self.committed = snap.committed;
            self.key_buffer.clear();
            return true;
        }

        if self.cursor > 0 && !self.preedit.is_empty() {
            self.preedit.remove(self.cursor - 1);
            self.cursor -= 1;
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
                // If not a prefix, process first char as fallback
                let first = self.key_buffer.remove(0);
                self.fallback_char(first);
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

    fn fallback_char(&mut self, c: char) {
        if self.state != self.im.initial_state {
            self.commit();
            self.state = self.im.initial_state.clone();
            self.run_transient_rules();
            let s = c.to_string();
            if self.step_longest_match(&s).is_none() {
                self.committed.push(c);
            }
        } else {
            self.committed.push(c);
        }
    }

    pub fn transliterate(&mut self, input: &str) -> String {
        self.reset();
        let input_chars: Vec<char> = input.chars().collect();
        let mut pos = 0;

        while pos < input_chars.len() {
            let rem: String = input_chars[pos..].iter().collect();
            if let Some(matched_len) = self.step_longest_match(&rem) {
                pos += matched_len;
            } else {
                let c = input_chars[pos];
                self.fallback_char(c);
                pos += 1;
            }
        }

        self.current_output()
    }

    fn step_longest_match(&mut self, rem: &str) -> Option<usize> {
        let state_def = self.im.states.get(&self.state)?.clone();

        struct MatchCandidate {
            rule_len: usize,
            rule_actions: Vec<Action>,
            rule_insert: Option<String>,
            branch_actions: Vec<Action>,
        }

        let mut best: Option<MatchCandidate> = None;

        for branch in &state_def.branches {
            match &branch.target {
                BranchTarget::Transient => {}
                BranchTarget::Map(map_name) => {
                    if let Some(map_def) = self.im.maps.get(map_name) {
                        for rule in &map_def.rules {
                            match &rule.key_seq {
                                KeySeq::Chars(seq) => {
                                    if rem.starts_with(seq) {
                                        let seq_len = seq.chars().count();
                                        let is_better = match &best {
                                            None => true,
                                            Some(cur) => seq_len > cur.rule_len,
                                        };
                                        if is_better {
                                            best = Some(MatchCandidate {
                                                rule_len: seq_len,
                                                rule_actions: rule.actions.clone(),
                                                rule_insert: rule.insert_text.clone(),
                                                branch_actions: branch.actions.clone(),
                                            });
                                        }
                                    }
                                }
                                KeySeq::Keysym(_) => {}
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
            for ch in text.chars() {
                self.preedit.insert(self.cursor, ch);
                self.cursor += 1;
            }
        }

        // 3. Execute branch actions
        for act in &m.branch_actions {
            self.execute_action(act);
        }

        Some(matched_len)
    }

    fn execute_action(&mut self, action: &Action) {
        match action {
            Action::Insert(s) => {
                for c in s.chars() {
                    self.preedit.insert(self.cursor, c);
                    self.cursor += 1;
                }
            }
            Action::Delete(pos) => {
                match pos {
                    Pos::Relative(offset) => {
                        if *offset < 0 {
                            let count = (-offset) as usize;
                            let start = self.cursor.saturating_sub(count);
                            self.preedit.drain(start..self.cursor);
                            self.cursor = start;
                        } else if *offset > 0 {
                            let count = *offset as usize;
                            let end = (self.cursor + count).min(self.preedit.len());
                            self.preedit.drain(self.cursor..end);
                        }
                    }
                    Pos::Begin => {
                        self.preedit.drain(0..self.cursor);
                        self.cursor = 0;
                    }
                    Pos::End => {
                        self.preedit.drain(self.cursor..);
                    }
                    Pos::Absolute(idx) => {
                        if *idx < self.preedit.len() {
                            self.preedit.remove(*idx);
                            if self.cursor > *idx {
                                self.cursor -= 1;
                            }
                        }
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
                }
            }
            Action::Set(var, val) => {
                let v = match val {
                    VarVal::Const(c) => *c,
                    VarVal::Var(other) => self.vars.get(other).copied().unwrap_or(0),
                };
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
        }
    }

    fn eval_cond(&self, expr: &CondExpr) -> bool {
        match expr {
            CondExpr::AlwaysTrue => true,
            CondExpr::Eq(var, val) => {
                let actual = self.vars.get(var).copied().unwrap_or(0);
                let expected = match val {
                    VarVal::Const(c) => *c,
                    VarVal::Var(v) => self.vars.get(v).copied().unwrap_or(0),
                };
                actual == expected
            }
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
