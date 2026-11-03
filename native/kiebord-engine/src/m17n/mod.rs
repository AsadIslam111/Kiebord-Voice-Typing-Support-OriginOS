pub mod ast;
pub mod compiler;
pub mod engine;
pub mod sexpr;
#[cfg(test)]
pub mod tests;

use parking_lot::RwLock;
use std::collections::HashMap;
use std::sync::Arc;

pub use ast::InputMethod;
pub use engine::M17nEngine;

static M17N_REGISTRY: RwLock<Option<HashMap<String, Arc<InputMethod>>>> = RwLock::new(None);

pub fn parse_input_method(content: &str) -> Result<InputMethod, String> {
    let mut parser = sexpr::SExprParser::new(content);
    let exprs = parser.parse_all()?;
    compiler::compile(&exprs)
}

pub fn create_engine(content: &str) -> Result<M17nEngine, String> {
    let im = parse_input_method(content)?;
    Ok(M17nEngine::new(Arc::new(im)))
}

pub fn register_m17n_layout(layout_id: &str, content: &str) -> Result<(), String> {
    let im = parse_input_method(content)?;
    let arc_im = Arc::new(im);
    let mut lock = M17N_REGISTRY.write();
    let map = lock.get_or_insert_with(HashMap::new);
    map.insert(layout_id.to_string(), arc_im.clone());
    if layout_id == "khipro" {
        map.insert("bn_khipro".to_string(), arc_im.clone());
    } else if layout_id == "bn_khipro" {
        map.insert("khipro".to_string(), arc_im);
    }
    Ok(())
}

pub fn is_m17n_layout(layout_id: &str) -> bool {
    let lock = M17N_REGISTRY.read();
    if let Some(map) = lock.as_ref() {
        map.contains_key(layout_id)
            || map.contains_key(layout_id.trim_start_matches("bn_"))
            || map.contains_key(&format!("bn_{}", layout_id))
    } else {
        false
    }
}

pub fn transliterate_m17n(layout_id: &str, input: &str) -> Option<String> {
    let lock = M17N_REGISTRY.read();
    let im = lock.as_ref()?.get(layout_id)
        .or_else(|| lock.as_ref()?.get(layout_id.trim_start_matches("bn_")))
        .or_else(|| lock.as_ref()?.get(&format!("bn_{}", layout_id)))?
        .clone();
    drop(lock);
    let mut engine = M17nEngine::new(im);
    Some(engine.transliterate(input))
}
