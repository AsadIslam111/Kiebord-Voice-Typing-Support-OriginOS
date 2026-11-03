package io.kiebord.common.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class KeyType {
    @JsonNames("character", "CHARACTER")
    CHARACTER,
    @JsonNames("shift", "SHIFT")
    SHIFT,
    @JsonNames("delete", "DELETE")
    DELETE,
    @JsonNames("action", "ACTION")
    ACTION,
    @JsonNames("space", "SPACE")
    SPACE,
    @JsonNames("symbols", "SYMBOLS")
    SYMBOLS,
    @JsonNames("switch_language", "SWITCH_LANGUAGE")
    SWITCH_LANGUAGE,
    @JsonNames("more_keys", "MORE_KEYS")
    MORE_KEYS
}

@Serializable
enum class ShiftState {
    UNSHIFTED,
    ONE_SHOT,
    CAPS_LOCK
}
