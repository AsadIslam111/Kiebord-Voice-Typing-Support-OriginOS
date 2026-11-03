use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq)]
pub enum Pos {
    Relative(i32),
    Begin,
    End,
    Absolute(usize),
}

#[derive(Debug, Clone, PartialEq)]
pub enum KeySeq {
    Chars(String),
    Keysym(String),
}

#[derive(Debug, Clone, PartialEq)]
pub enum VarVal {
    Const(i32),
    Var(String),
}

#[derive(Debug, Clone, PartialEq)]
pub enum CondExpr {
    Eq(String, VarVal),
    And(Vec<CondExpr>),
    Or(Vec<CondExpr>),
    Not(Box<CondExpr>),
    AlwaysTrue,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Action {
    Insert(String),
    Delete(Pos),
    Move(Pos),
    Set(String, VarVal),
    Shift(String),
    Commit,
    Undo,
    Cond(Vec<(CondExpr, Vec<Action>)>),
}

#[derive(Debug, Clone)]
pub struct MapRule {
    pub key_seq: KeySeq,
    pub actions: Vec<Action>,
    pub insert_text: Option<String>,
}

#[derive(Debug, Clone, Default)]
pub struct MapDef {
    pub name: String,
    pub rules: Vec<MapRule>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum BranchTarget {
    Transient,        // (t ...)
    Map(String),      // (map_name ...)
    Fallback,         // (nil ...) or (1 ...)
}

#[derive(Debug, Clone)]
pub struct StateBranch {
    pub target: BranchTarget,
    pub actions: Vec<Action>,
}

#[derive(Debug, Clone, Default)]
pub struct StateDef {
    pub name: String,
    pub branches: Vec<StateBranch>,
}

#[derive(Debug, Clone)]
pub struct InputMethod {
    pub lang: String,
    pub name: String,
    pub title: Option<String>,
    pub maps: HashMap<String, MapDef>,
    pub states: HashMap<String, StateDef>,
    pub initial_state: String,
}
