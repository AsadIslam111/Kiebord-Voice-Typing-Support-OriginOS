use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jstring};
use jni::JNIEnv;

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
pub extern "system" fn Java_io_kiebord_feature_ime_NativeEngine_ping(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1
}
