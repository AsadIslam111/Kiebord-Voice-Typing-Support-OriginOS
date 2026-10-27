# Kiebord Keyboard Layout Development Guide

This guide describes how to create, test, and package custom keyboard layouts for Kiebord using the JSON layout format.

---

## 1. Overview

Kiebord supports dynamic and bundled keyboard layouts defined in JSON. Layouts can be:
- Bundled with the application under `assets/layouts/<layout_id>.json`.
- Installed dynamically by users via the in-app file importer (`.json`) into internal storage.

Layouts are parsed with compile-time serializers and cached in memory to ensure zero latency during typing and language switching.

---

## 2. File Placement and Naming

- File extension: `.json`
- File naming: `<language_code>_<layout_name>.json` (example: `ar_arabic.json`, `fr_azerty.json`, `de_qwertz.json`)
- Character encoding: UTF-8 (without BOM)

---

## 3. Schema Reference

### Top-Level Properties

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | String | Yes | Unique identifier for the layout (e.g., `ar_arabic`, `en_dvorak`). |
| `name` | String | Yes | Display name shown in Settings and language switchers (e.g., `Arabic (العربية)`). |
| `language` | String | Yes | ISO 639-1 two-letter language code (e.g., `en`, `bn`, `ar`, `es`). |
| `direction` | String | No | Text and keyboard flow direction: `"LTR"` (default) or `"RTL"`. |
| `extraRows` | Array of `LayoutExtraRow` | No | Optional feature rows that appear above the main alphabet keys. |
| `numberRow` | `KeyRow` | No | Optional layout-specific number row override. |
| `rows` | Array of `KeyRow` | Yes | List of main keyboard rows from top to bottom. |

---

### `LayoutExtraRow` Properties

Extra rows allow layouts to provide specialized key rows (such as Arabic Harakat diacritics, accented vowel bars, or mathematical symbol strips). Kiebord automatically creates a toggle switch under the layout in Settings for each declared extra row.

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | String | Yes | Unique identifier for the feature row (e.g., `harakat`). |
| `title` | String | Yes | Human-readable label displayed in Settings (e.g., `Harakat row (حركات)`). |
| `defaultEnabled` | Boolean | No | Whether the row is enabled by default (`true` or `false`). Default is `true`. |
| `keys` | Array of `KeyDef` | Yes | List of key definitions for this row. |

---

### `KeyRow` Properties

| Field | Type | Required | Description |
|---|---|---|---|
| `keys` | Array of `KeyDef` | Yes | Ordered list of keys in this row. |
| `startPaddingWeight` | Float | No | Horizontal spacing weight before the first key (default: `0.0`). |
| `endPaddingWeight` | Float | No | Horizontal spacing weight after the last key (default: `0.0`). |

---

### `KeyDef` Properties

| Field | Type | Required | Description |
|---|---|---|---|
| `label` | String | No | Primary character or symbol displayed on the key cap. |
| `shiftedLabel` | String | No | Label displayed when Shift or Caps Lock is active. |
| `commitText` | String | No | Text sent to the input field if different from `label`. Useful for combining diacritics. |
| `type` | String | No | Key functional type (see Key Types table below). Default: `"CHARACTER"`. |
| `icon` | String | No | Vector icon identifier (`"backspace"`, `"enter"`, `"globe"`, `"space"`, `"shift"`). |
| `widthWeight` | Float | No | Relative width multiplier. Standard key is `1.0`. Space is typically `4.0`. Modifiers are often `1.5`. |
| `flickUp` | String | No | Character displayed as a hint in the top-right corner and committed on flick-up gesture. |
| `shiftedFlickUp` | String | No | Flick-up character when Shift or Caps Lock is active. |
| `moreKeys` | Array of String | No | List of characters or actions shown in the long-press popup menu. |
| `moreKeysColumns` | Integer | No | Maximum number of columns in the long-press popup menu. |
| `slidingInput` | Boolean | No | When `true`, sliding from this key seamlessly transitions to symbols mode (used on `?123`). |

---

## 4. Key Functional Types

| Key Type | Description |
|---|---|
| `CHARACTER` | Standard typing key that outputs text (default). |
| `SHIFT` | Toggles Shift (Unshifted, One-Shot, Caps Lock). |
| `DELETE` | Backspace key with tap, long-press delete, and swipe-to-delete support. |
| `ACTION` | Enter / Search / Send / Done action key. |
| `SPACE` | Space bar with gesture trackpad cursor control. |
| `SYMBOLS` | Toggles between alphanumeric and symbol layout modes. |
| `SWITCH_LANGUAGE` | Globe key that switches between enabled keyboard layouts. |

---

## 5. Built-In Actions in `moreKeys`

You can include special action codes in the `moreKeys` array to provide direct access to editor utilities:

| Action Code | Function |
|---|---|
| `ACTION_SELECT_ALL` | Selects all text in the active input field. |
| `ACTION_CUT` | Cuts selected text to the clipboard. |
| `ACTION_COPY` | Copies selected text to the clipboard. |
| `ACTION_PASTE` | Pastes current clipboard contents. |
| `ACTION_SETTINGS` | Opens Kiebord settings. |
| `ACTION_ONE_HANDED` | Toggles one-handed mode. |
| `ACTION_EMOJI` | Opens the emoji picker. |

---

## 6. Minimal Layout Template

Save the following content as `sample_layout.json` to create a starter layout:

```json
{
  "id": "sample_layout",
  "name": "Sample QWERTY",
  "language": "en",
  "direction": "LTR",
  "rows": [
    {
      "keys": [
        { "label": "q", "shiftedLabel": "Q", "flickUp": "1", "moreKeys": ["1"] },
        { "label": "w", "shiftedLabel": "W", "flickUp": "2", "moreKeys": ["2"] },
        { "label": "e", "shiftedLabel": "E", "flickUp": "3", "moreKeys": ["3", "é", "è"] },
        { "label": "r", "shiftedLabel": "R", "flickUp": "4", "moreKeys": ["4"] },
        { "label": "t", "shiftedLabel": "T", "flickUp": "5", "moreKeys": ["5"] },
        { "label": "y", "shiftedLabel": "Y", "flickUp": "6", "moreKeys": ["6"] },
        { "label": "u", "shiftedLabel": "U", "flickUp": "7", "moreKeys": ["7"] },
        { "label": "i", "shiftedLabel": "I", "flickUp": "8", "moreKeys": ["8"] },
        { "label": "o", "shiftedLabel": "O", "flickUp": "9", "moreKeys": ["9"] },
        { "label": "p", "shiftedLabel": "P", "flickUp": "0", "moreKeys": ["0"] }
      ]
    },
    {
      "startPaddingWeight": 0.5,
      "endPaddingWeight": 0.5,
      "keys": [
        { "label": "a", "shiftedLabel": "A", "flickUp": "@", "moreKeys": ["@"] },
        { "label": "s", "shiftedLabel": "S", "flickUp": "#", "moreKeys": ["#"] },
        { "label": "d", "shiftedLabel": "D", "flickUp": "$", "moreKeys": ["$"] },
        { "label": "f", "shiftedLabel": "F", "flickUp": "%", "moreKeys": ["%"] },
        { "label": "g", "shiftedLabel": "G", "flickUp": "&", "moreKeys": ["&"] },
        { "label": "h", "shiftedLabel": "H", "flickUp": "-", "moreKeys": ["-"] },
        { "label": "j", "shiftedLabel": "J", "flickUp": "+", "moreKeys": ["+"] },
        { "label": "k", "shiftedLabel": "K", "flickUp": "(", "moreKeys": ["("] },
        { "label": "l", "shiftedLabel": "L", "flickUp": ")", "moreKeys": [")"] }
      ]
    },
    {
      "keys": [
        { "type": "SHIFT", "icon": "shift", "widthWeight": 1.5 },
        { "label": "z", "shiftedLabel": "Z", "moreKeys": ["ACTION_SELECT_ALL"] },
        { "label": "x", "shiftedLabel": "X", "moreKeys": ["ACTION_CUT"] },
        { "label": "c", "shiftedLabel": "C", "moreKeys": ["ACTION_COPY"] },
        { "label": "v", "shiftedLabel": "V", "moreKeys": ["ACTION_PASTE"] },
        { "label": "b", "shiftedLabel": "B" },
        { "label": "n", "shiftedLabel": "N" },
        { "label": "m", "shiftedLabel": "M" },
        { "type": "DELETE", "icon": "backspace", "widthWeight": 1.5 }
      ]
    },
    {
      "keys": [
        { "label": "?123", "type": "SYMBOLS", "widthWeight": 1.5, "slidingInput": true },
        { "label": ",", "widthWeight": 1.0, "moreKeys": ["ACTION_SETTINGS", "ACTION_ONE_HANDED"] },
        { "type": "SWITCH_LANGUAGE", "icon": "globe", "widthWeight": 1.0 },
        { "label": " ", "type": "SPACE", "icon": "space", "widthWeight": 4.0 },
        { "label": ".", "widthWeight": 1.0, "moreKeys": ["?", "!", ":", ";", "-"] },
        { "type": "ACTION", "icon": "enter", "widthWeight": 1.5, "moreKeys": ["ACTION_EMOJI"] }
      ]
    }
  ]
}
```

---

## 7. Adding Layout-Specific Extra Rows

To provide an optional extra row (such as the Harakat row in Arabic or an accent strip in Spanish):

```json
{
  "id": "ar_arabic",
  "name": "Arabic (العربية)",
  "language": "ar",
  "direction": "RTL",
  "extraRows": [
    {
      "id": "harakat",
      "title": "Harakat row (حركات)",
      "defaultEnabled": true,
      "keys": [
        { "label": "ـَ", "commitText": "\u064E" },
        { "label": "ـً", "commitText": "\u064B" },
        { "label": "ـُ", "commitText": "\u064F" },
        { "label": "ـٌ", "commitText": "\u064C" },
        { "label": "ـِ", "commitText": "\u0650" },
        { "label": "ـٍ", "commitText": "\u064D" },
        { "label": "ـْ", "commitText": "\u0652" },
        { "label": "ـّ", "commitText": "\u0651" },
        { "label": "ـٰ", "commitText": "\u0670" },
        { "label": "ـٖ", "commitText": "\u0656" },
        { "label": "ـٗ", "commitText": "\u0657" }
      ]
    }
  ],
  "rows": [
    ...
  ]
}
```

When Kiebord loads this layout:
1. It automatically adds a **Harakat row (حركات)** switch under the layout card in **Settings > Languages & Layouts**.
2. Users can toggle the row on or off with immediate live preview updating.
3. The choice is preserved across reboots in app preferences.

---

## 8. Installing Custom Layouts on a Device

1. Save your custom JSON file onto your phone (via USB, Downloads, or cloud storage).
2. Open the **Kiebord** app.
3. Navigate to **Settings > Languages & Layouts**.
4. Scroll to the bottom and tap **Import Custom Layout (.json)**.
5. Select your `.json` file from the system file picker.
6. Kiebord validates the structure and enables the layout immediately.
7. Tap the layout preview to customize any optional extra rows.
