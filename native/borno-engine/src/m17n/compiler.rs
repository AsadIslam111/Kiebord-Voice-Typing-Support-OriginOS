use std::collections::HashMap;

use super::ast::*;
use super::sexpr::SExpr;

pub fn compile(exprs: &[SExpr]) -> Result<InputMethod, String> {
    let mut lang = String::new();
    let mut name = String::new();
    let mut title = None;
    let mut maps = HashMap::new();
    let mut states = HashMap::new();
    let mut initial_state = String::from("init");

    for expr in exprs {
        let list = match expr {
            SExpr::List(l) => l,
            _ => continue,
        };

        if list.is_empty() {
            continue;
        }

        let tag = match list[0].as_symbol() {
            Some(t) => t,
            None => continue,
        };

        match tag {
            "input-method" => {
                if list.len() >= 3 {
                    lang = list[1].as_symbol().unwrap_or("").to_string();
                    name = list[2].as_symbol().unwrap_or("").to_string();
                }
            }
            "title" => {
                if list.len() >= 2 {
                    if let Some(s) = list[1].as_string() {
                        title = Some(s.to_string());
                    }
                }
            }
            "map" => {
                // (map (map1 (rule1...) (rule2...)) (map2 ...))
                for item in &list[1..] {
                    let map_def = parse_map_def(item)?;
                    maps.insert(map_def.name.clone(), map_def);
                }
            }
            "state" => {
                // (state (init (branch1...) ...) (state2 ...))
                let mut first_state = true;
                for item in &list[1..] {
                    let state_def = parse_state_def(item)?;
                    if first_state {
                        initial_state = state_def.name.clone();
                        first_state = false;
                    }
                    states.insert(state_def.name.clone(), state_def);
                }
            }
            _ => {
                // Other sections (e.g. description, macro) are ignored
            }
        }
    }

    Ok(InputMethod {
        lang,
        name,
        title,
        maps,
        states,
        initial_state,
    })
}

fn parse_map_def(expr: &SExpr) -> Result<MapDef, String> {
    let list = expr.as_list().ok_or_else(|| "Map definition must be a list".to_string())?;
    if list.is_empty() {
        return Err("Empty map definition".to_string());
    }

    let map_name = list[0]
        .as_symbol()
        .ok_or_else(|| "Map name must be a symbol".to_string())?
        .to_string();

    let mut rules = Vec::new();
    for rule_expr in &list[1..] {
        let rule_list = rule_expr
            .as_list()
            .ok_or_else(|| format!("Rule in map '{}' must be a list", map_name))?;

        if rule_list.is_empty() {
            continue;
        }

        // Rule key sequence
        let key_seq = parse_key_seq(&rule_list[0])?;
        let mut actions = Vec::new();
        let mut insert_text = None;

        for action_expr in &rule_list[1..] {
            match action_expr {
                SExpr::String(s) => {
                    insert_text = Some(s.clone());
                }
                SExpr::Char(c) => {
                    insert_text = Some(c.to_string());
                }
                _ => {
                    actions.push(parse_action(action_expr)?);
                }
            }
        }

        rules.push(MapRule {
            key_seq,
            actions,
            insert_text,
        });
    }

    Ok(MapDef {
        name: map_name,
        rules,
    })
}

fn parse_key_seq(expr: &SExpr) -> Result<KeySeq, String> {
    match expr {
        SExpr::String(s) => Ok(KeySeq::Chars(s.clone())),
        SExpr::Char(c) => Ok(KeySeq::Chars(c.to_string())),
        SExpr::List(l) => {
            if l.len() == 1 {
                if let Some(sym) = l[0].as_symbol() {
                    return Ok(KeySeq::Keysym(sym.to_string()));
                }
            }
            Err(format!("Unsupported key sequence format: {:?}", expr))
        }
        _ => Err(format!("Invalid key sequence in map rule: {:?}", expr)),
    }
}

fn parse_state_def(expr: &SExpr) -> Result<StateDef, String> {
    let list = expr.as_list().ok_or_else(|| "State definition must be a list".to_string())?;
    if list.is_empty() {
        return Err("Empty state definition".to_string());
    }

    let state_name = list[0]
        .as_symbol()
        .ok_or_else(|| "State name must be a symbol".to_string())?
        .to_string();

    let mut branches = Vec::new();
    for branch_expr in &list[1..] {
        let branch_list = branch_expr
            .as_list()
            .ok_or_else(|| format!("Branch in state '{}' must be a list", state_name))?;

        if branch_list.is_empty() {
            continue;
        }

        let target = match &branch_list[0] {
            SExpr::Symbol(s) => {
                if s == "t" {
                    BranchTarget::Transient
                } else if s == "nil" {
                    BranchTarget::Fallback
                } else {
                    BranchTarget::Map(s.clone())
                }
            }
            SExpr::Int(1) => BranchTarget::Fallback,
            SExpr::List(l) => {
                if l.len() == 1 {
                    if let Some(s) = l[0].as_symbol() {
                        BranchTarget::Map(s.to_string())
                    } else {
                        return Err(format!("Invalid branch target in state '{}': {:?}", state_name, l));
                    }
                } else {
                    return Err(format!("Invalid branch target list in state '{}': {:?}", state_name, l));
                }
            }
            _ => return Err(format!("Unexpected branch target in state '{}': {:?}", state_name, branch_list[0])),
        };

        let mut actions = Vec::new();
        for action_expr in &branch_list[1..] {
            actions.push(parse_action(action_expr)?);
        }

        branches.push(StateBranch { target, actions });
    }

    Ok(StateDef {
        name: state_name,
        branches,
    })
}

fn parse_action(expr: &SExpr) -> Result<Action, String> {
    match expr {
        SExpr::String(s) => Ok(Action::Insert(s.clone())),
        SExpr::Char(c) => Ok(Action::Insert(c.to_string())),
        SExpr::List(list) => {
            if list.is_empty() {
                return Err("Empty action list".to_string());
            }

            let tag = list[0]
                .as_symbol()
                .ok_or_else(|| format!("Action tag must be a symbol, got {:?}", list[0]))?;

            match tag {
                "set" => {
                    if list.len() < 3 {
                        return Err(format!("Invalid 'set' action: {:?}", list));
                    }
                    let var = list[1]
                        .as_symbol()
                        .ok_or_else(|| format!("Expected variable name in 'set', got {:?}", list[1]))?
                        .to_string();
                    let val = parse_var_val(&list[2])?;
                    Ok(Action::Set(var, val))
                }
                "shift" => {
                    if list.len() < 2 {
                        return Err(format!("Invalid 'shift' action: {:?}", list));
                    }
                    let target = list[1]
                        .as_symbol()
                        .ok_or_else(|| format!("Expected state name in 'shift', got {:?}", list[1]))?
                        .to_string();
                    Ok(Action::Shift(target))
                }
                "delete" => {
                    let pos = if list.len() >= 2 {
                        parse_pos(&list[1])?
                    } else {
                        Pos::Relative(-1)
                    };
                    Ok(Action::Delete(pos))
                }
                "move" => {
                    if list.len() < 2 {
                        return Err(format!("Invalid 'move' action: {:?}", list));
                    }
                    let pos = parse_pos(&list[1])?;
                    Ok(Action::Move(pos))
                }
                "insert" => {
                    if list.len() < 2 {
                        return Err(format!("Invalid 'insert' action: {:?}", list));
                    }
                    let text = match &list[1] {
                        SExpr::String(s) => s.clone(),
                        SExpr::Char(c) => c.to_string(),
                        SExpr::Symbol(s) => s.clone(),
                        _ => return Err(format!("Invalid argument to 'insert': {:?}", list[1])),
                    };
                    Ok(Action::Insert(text))
                }
                "commit" => Ok(Action::Commit),
                "undo" => Ok(Action::Undo),
                "cond" => {
                    // (cond ((test1) a1 a2...) ((test2) b1...) ...)
                    let mut clauses = Vec::new();
                    for clause_expr in &list[1..] {
                        let clause_list = clause_expr
                            .as_list()
                            .ok_or_else(|| format!("Clause in 'cond' must be a list: {:?}", clause_expr))?;
                        if clause_list.is_empty() {
                            continue;
                        }
                        let cond_expr = parse_cond_expr(&clause_list[0])?;
                        let mut actions = Vec::new();
                        for a in &clause_list[1..] {
                            actions.push(parse_action(a)?);
                        }
                        clauses.push((cond_expr, actions));
                    }
                    Ok(Action::Cond(clauses))
                }
                _ => Err(format!("Unknown action '{}' in {:?}", tag, list)),
            }
        }
        _ => Err(format!("Invalid action expression: {:?}", expr)),
    }
}

fn parse_pos(expr: &SExpr) -> Result<Pos, String> {
    match expr {
        SExpr::Symbol(s) => match s.as_str() {
            "@-" => Ok(Pos::Relative(-1)),
            "@-1" => Ok(Pos::Relative(-1)),
            "@-2" => Ok(Pos::Relative(-2)),
            "@-3" => Ok(Pos::Relative(-3)),
            "@-4" => Ok(Pos::Relative(-4)),
            "@-5" => Ok(Pos::Relative(-5)),
            "@>" => Ok(Pos::End),
            "@<" => Ok(Pos::Begin),
            "@+" => Ok(Pos::Relative(1)),
            "@+1" => Ok(Pos::Relative(1)),
            _ => {
                if s.starts_with("@-") {
                    if let Ok(n) = s[2..].parse::<i32>() {
                        return Ok(Pos::Relative(-n));
                    }
                } else if s.starts_with("@+") {
                    if let Ok(n) = s[2..].parse::<i32>() {
                        return Ok(Pos::Relative(n));
                    }
                } else if s.starts_with('@') {
                    if let Ok(n) = s[1..].parse::<usize>() {
                        return Ok(Pos::Absolute(n));
                    }
                }
                Err(format!("Unrecognized position symbol: {}", s))
            }
        },
        _ => Err(format!("Expected position symbol, got {:?}", expr)),
    }
}

fn parse_var_val(expr: &SExpr) -> Result<VarVal, String> {
    match expr {
        SExpr::Int(i) => Ok(VarVal::Const(*i as i32)),
        SExpr::Symbol(s) => {
            if let Ok(i) = s.parse::<i32>() {
                Ok(VarVal::Const(i))
            } else {
                Ok(VarVal::Var(s.clone()))
            }
        }
        _ => Err(format!("Invalid variable value: {:?}", expr)),
    }
}

fn parse_cond_expr(expr: &SExpr) -> Result<CondExpr, String> {
    match expr {
        SExpr::Int(1) => Ok(CondExpr::AlwaysTrue),
        SExpr::Symbol(s) if s == "1" || s == "t" => Ok(CondExpr::AlwaysTrue),
        SExpr::List(list) => {
            if list.is_empty() {
                return Ok(CondExpr::AlwaysTrue);
            }
            let op = list[0]
                .as_symbol()
                .ok_or_else(|| format!("Expected operator symbol in condition, got {:?}", list[0]))?;

            match op {
                "=" => {
                    if list.len() < 3 {
                        return Err(format!("Invalid '=' condition: {:?}", list));
                    }
                    let var = list[1]
                        .as_symbol()
                        .ok_or_else(|| format!("Expected variable name in '=' condition: {:?}", list[1]))?
                        .to_string();
                    let val = parse_var_val(&list[2])?;
                    Ok(CondExpr::Eq(var, val))
                }
                "&" => {
                    let mut subs = Vec::new();
                    for item in &list[1..] {
                        subs.push(parse_cond_expr(item)?);
                    }
                    Ok(CondExpr::And(subs))
                }
                "|" => {
                    let mut subs = Vec::new();
                    for item in &list[1..] {
                        subs.push(parse_cond_expr(item)?);
                    }
                    Ok(CondExpr::Or(subs))
                }
                "!" => {
                    if list.len() < 2 {
                        return Err(format!("Invalid '!' condition: {:?}", list));
                    }
                    let sub = parse_cond_expr(&list[1])?;
                    Ok(CondExpr::Not(Box::new(sub)))
                }
                _ => Err(format!("Unknown condition operator '{}' in {:?}", op, list)),
            }
        }
        _ => Err(format!("Invalid condition expression: {:?}", expr)),
    }
}

