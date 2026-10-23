package io.kiebord.common.model

enum class KeyType {
    CHARACTER,
    SHIFT,
    DELETE,
    ACTION,
    SPACE,
    SYMBOLS,
    SWITCH_LANGUAGE,
    MORE_KEYS
}

enum class ShiftState {
    UNSHIFTED,
    ONE_SHOT,
    CAPS_LOCK
}
