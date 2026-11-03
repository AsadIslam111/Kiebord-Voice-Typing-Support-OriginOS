# Kiebord Keyboard Layout Development Guide

This guide describes how to create, test, and package custom keyboard layouts for Kiebord using the JSON layout format.

---

## 1. Overview

Kiebord supports dynamic and bundled keyboard layouts defined in JSON. Layouts can be:
- Bundled with the application under `assets/layouts/<layout_id>.json`.

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
| `name` | String | Yes | Name shown in Settings (e.g., `Arabic`, `English`). |
| `displayName` | String | No | Concise display name for space bar and quick switchers (e.g., `Arabic`). Defaults to `name`. |
| `language` | String | Yes | ISO 639-1 two-letter language code (e.g., `en`, `bn`, `ar`, `es`). |
| `direction` | String | No | Text and keyboard flow direction: `"LTR"` (default) or `"RTL"`. |
| `extraRows` | Array of `LayoutExtraRow` | No | Optional feature rows that appear above the main alphabet keys. |
| `numberRow` | `KeyRow` | No | Optional layout-specific number row override. |
| `shiftedNumberRow` | `KeyRow` | No | Optional layout-specific number row displayed when Shift or Caps Lock is active. |
| `symbolsLayout` | `KeyboardLayoutDef` | No | Optional layout-specific symbols page (`?123`). Used to match row count and supply language-specific symbols. Falls back to default symbols if omitted. |
| `moreSymbolsLayout` | `KeyboardLayoutDef` | No | Optional secondary symbols page (`=\<`). Falls back to default more symbols if omitted. |
| `rows` | Array of `KeyRow` | Yes | List of main keyboard rows from top to bottom. |

---

### Custom Symbol Pages (`?123` and `=\<`)

Layouts can optionally provide their own `symbolsLayout` and `moreSymbolsLayout`.
- **Row Height and Count Synchronization**: Whether switching between the alphabet page and `?123` (or `=\<`), Kiebord preserves identical row heights and row counts:
  - If the top number row is enabled (`showNumberRow`), both the alphabet and symbol pages render the top number row at row index 0 with matching height `(50 * scaleFactor).dp`.
  - If an extra row (such as Arabic Harakat diacritics or custom feature rows) is enabled, both the alphabet and symbol pages render the extra row with matching standard height `keyRowHeight`.
  - If either row is disabled by the user or layout configuration, neither page renders it.
- **Dynamic Extra Row Carry-Over and Linking**: If an alphabet layout defines an enabled extra row (such as `harakat`), the symbols page can either provide its own corresponding extra row under the same `id` (e.g. Quranic stop marks for Arabic symbols), or Kiebord automatically carries over the alphabet extra row to the symbols page if row counts need matching. Toggling the feature in Settings updates both pages in lockstep.
- **Duplicate Number Avoidance**: When the top number row is enabled on a symbol layout whose first core row contains digits, Kiebord automatically replaces that core row with the alternate symbols row (`~ < > _ ` º \ = { }` or shifted symbols) so numbers are not repeated across consecutive rows.
- **Localized Symbols**: Language-specific punctuation (such as Arabic question mark `؟`, comma `،`, semicolon `؛`, Eastern Arabic numbers `١, ٢, ٣...`, or Quranic stop marks) can be positioned directly on the symbol page.
- **Automatic Fallback**: If a layout does not specify `symbolsLayout` (such as English QWERTY or Bengali layouts), Kiebord automatically falls back to the standard built-in symbols page with synchronized top number row behavior.

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
| `shiftedCommitText` | String | No | Text committed when Shift or Caps Lock is active if different from `shiftedLabel`. |
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
  "name": "Arabic",
  "displayName": "Arabic",
  "language": "ar",
  "direction": "RTL",
  "extraRows": [
    {
      "id": "harakat",
      "title": "Harakat row (حركات)",
      "defaultEnabled": false,
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
4. On symbol pages (`?123` and `=\<`):
   - For each enabled extra row in the alphabet layout, if the symbol page defines a corresponding extra row (matching `id`), that row is used. If not found, the alphabet extra row is moved to the symbol page so row counts and heights match.
   - If the top number row is enabled, the symbol page uses its own defined `numberRow` or automatically places the alphabet layout's number row at the top.
