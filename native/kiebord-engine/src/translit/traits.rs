pub trait PhoneticTransliterator: Send + Sync {
    /// Unique identifier for this phonetic layout, e.g. "bn_borno", "bn_avro"
    fn id(&self) -> &'static str;

    /// Target language code corresponding to the underlying dictionary, e.g. "bn"
    fn target_dict_lang(&self) -> &'static str;

    /// Converts raw Latin phonetic input to target script
    fn transliterate(&self, input: &str) -> String;

    /// Converts a word in target script (e.g. "আমাদের")
    /// to its expected Latin phonetic key sequences on a QWERTY layout.
    fn to_phonetic_keys(&self, word: &str) -> Vec<Vec<char>>;
}

pub fn get_phonetic_transliterator(layout_id: &str) -> Option<&'static (dyn PhoneticTransliterator + 'static)> {
    match layout_id {
        "bn_borno" => Some(crate::translit::borno_phonetic::get_borno_transliterator()),
        _ => None,
    }
}

pub fn is_phonetic_layout(layout_id: &str) -> bool {
    get_phonetic_transliterator(layout_id).is_some()
}
