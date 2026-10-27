package io.kiebord.common.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class ShiftState {
    UNSHIFTED,
    ONE_SHOT,
    CAPS_LOCK
}
