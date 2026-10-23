use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring};
use jni::JNIEnv;

pub mod dictionary;
pub mod gesture;
pub mod spatial;
pub mod translit;

use translit::borno_phonetic::BornoPhonetic;

#[no_mangle]
pub extern "system" fn Java_io_kiebord_feature_ime_NativeEngine_transliterateBorno(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let input_str: String = match env.get_string(&input) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let result = BornoPhonetic::convert(&input_str);
    env.new_string(result).unwrap().into_raw()
}

#[no_mangle]
pub extern "system" fn Java_io_kiebord_feature_ime_NativeEngine_getSuggestions(
    mut env: JNIEnv,
    _class: JClass,
    prefix: JString,
    lang: JString,
    max_results: jint,
) -> jstring {
    let prefix_str: String = match env.get_string(&prefix) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };
    let lang_str: String = match env.get_string(&lang) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let limit = if max_results <= 0 { 5 } else { max_results as usize };
    let result = dictionary::get_suggestions(&prefix_str, &lang_str, limit);
    env.new_string(result).unwrap().into_raw()
}

#[no_mangle]
pub extern "system" fn Java_io_kiebord_feature_ime_NativeEngine_predictNextWords(
    mut env: JNIEnv,
    _class: JClass,
    prev_word: JString,
    lang: JString,
    max_results: jint,
) -> jstring {
    let prev_str: String = match env.get_string(&prev_word) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };
    let lang_str: String = match env.get_string(&lang) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let limit = if max_results <= 0 { 3 } else { max_results as usize };
    let result = dictionary::predict_next_words(&prev_str, &lang_str, limit);
    env.new_string(result).unwrap().into_raw()
}

#[no_mangle]
pub extern "system" fn Java_io_kiebord_feature_ime_NativeEngine_loadDictionary(
    mut env: JNIEnv,
    _class: JClass,
    lang: JString,
    path: JString,
) -> jboolean {
    let lang_str: String = match env.get_string(&lang) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    if dictionary::load_dictionary_path(&lang_str, std::path::Path::new(&path_str)) {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_io_kiebord_feature_ime_NativeEngine_ping(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1
}
