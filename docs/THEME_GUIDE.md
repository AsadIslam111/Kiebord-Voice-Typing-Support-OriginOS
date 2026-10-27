# Kiebord Keyboard Theme Development & Import Guide

This guide describes how to create, customize, and import keyboard themes for Kiebord, including full compatibility with Borno Keyboard (`.kbtheme` and `.json`) color theme files.

---

## 1. Overview

Kiebord supports:
- Built-in original themes (Default Dark, AMOLED Black, Tokyo Night, Nord Frost, Default Light).
- Bundled Borno color themes (Classics, AMOLED, Aesthetic, Pastels, and Accent colors).
- Dynamic custom themes loaded from storage via `.kbtheme` or `.json` files.

Themes are parsed using compile-time serializers and managed via `ThemeRepository` with in-memory caching for instantaneous switching without frame drops.

---

## 2. Supported Formats

Kiebord accepts three theme file formats:

### Format A: Standalone Borno Color Theme (`.kbtheme` or `.json`)
```json
{
  "id": 1012,
  "name": "Cosmic Nebula",
  "backgroundColor": "#101028",
  "keyColor": "#202040",
  "labelKeyColor": "#E0B0FF",
  "functionalKeyColor": "#202040",
  "labelFunctionalKeyColor": "#E0B0FF",
  "enterColor": "#800080",
  "enterColorIcon": "#FFFFFF",
  "selectedKeyColor": "#9400D3",
  "keyStrokeColor": "#4b0082",
  "keyStrokeSize": 2,
  "isFlat": true
}
```

### Format B: Borno Multi-Theme Bundle Export (`.kbtheme` or `.json`)
```json
{
  "themes": [
    {
      "colors": [
        {
          "id": 1001,
          "name": "Material Light",
          "backgroundColor": "#ffebeff2",
          "keyColor": "#ffffffff",
          "labelKeyColor": "#ff000000",
          "functionalKeyColor": "#ffdfe1e3",
          "labelFunctionalKeyColor": "#B3000000",
          "enterColor": "#ff67c3b8"
        },
        {
          "id": 1011,
          "name": "AMOLED Purity",
          "backgroundColor": "#000000",
          "keyColor": "#282828",
          "labelKeyColor": "#FFFFFF",
          "functionalKeyColor": "#181818",
          "labelFunctionalKeyColor": "#EEEEEE",
          "enterColor": "#FDFE03"
        }
      ]
    }
  ]
}
```

### Format C: Native Kiebord Flat Theme Definition (`.json`)
```json
{
  "id": "custom_neon_cyber",
  "name": "Neon Cyber",
  "isDark": true,
  "containerBackground": "#0F0F1A",
  "keyBackground": "#1A1A2E",
  "keyForeground": "#E0E0FF",
  "keyBorder": "#2A2A4E",
  "keyShadow": "#00000000",
  "actionKeyBackground": "#16213E",
  "actionKeyForeground": "#00FFF5",
  "shiftActiveColor": "#00FFF5",
  "accentColor": "#00FFF5",
  "moreKeysBackground": "#1A1A2E",
  "guidelineColor": "#00FFF5",
  "suggestionBackground": "#0F0F1A",
  "suggestionForeground": "#E0E0FF",
  "keyPressedBackground": "#16213E",
  "keyPressedForeground": "#00FFF5"
}
```

---

## 3. Color Format Specifications

Colors may be specified in any of the following standard hex representations:
- `#RGB` (3 hex characters, e.g., `#FFF`, `#F00`)
- `#ARGB` (4 hex characters with alpha, e.g., `#8FFF`)
- `#RRGGBB` (6 hex characters, e.g., `#282828`, `#FDFE03`)
- `#AARRGGBB` (8 hex characters with alpha, e.g., `#4D000000`, `#FFEBEFF2`)

Kiebord's internal parser safely normalizes these formats without relying on platform-dependent Android APIs, ensuring compatibility across all devices and test environments.

---

## 4. Key Attributes Mapping

| Borno Field | Kiebord Attribute | Description |
|---|---|---|
| `backgroundColor` | `containerBackground` | Root keyboard surface background color |
| `keyColor` | `keyBackground` | Standard alphabet and number keycaps |
| `labelKeyColor` | `keyForeground` | Character text and secondary glyph color |
| `functionalKeyColor` | `actionKeyBackground` | Modifiers: Shift, Delete, Symbols (`?123`), Comma |
| `labelFunctionalKeyColor` | `actionKeyForeground` | Modifier key icons and symbol labels |
| `enterColor` | `accentColor` | Action/Enter keycap background and accent elements |
| `selectedKeyColor` | `shiftActiveColor` | Active Shift indicator and selected key highlight |
| `popupColor` | `moreKeysBackground` | Long-press popup menu surface color |
| `pressedKeyColor` | `keyPressedBackground` | Keycap background when pressed |
| `keyStrokeColor` | `keyBorder` | Separation stroke color around keycaps |
| `keyShadowColor` | `keyShadow` | Keycap drop shadow color |
| `stripColor` | `suggestionBackground` | Suggestion bar background |
| `stripIcoColor` | `suggestionForeground` | Suggestion bar icons and candidate text color |

---

## 5. Importing Themes into Kiebord

1. Open Kiebord Settings.
2. Navigate to **Theme & Appearance**.
3. Scroll to the bottom and select **Import Custom Theme (.kbtheme / .json)**.
4. Select your file from the system document picker.
5. If the file contains a bundle, all themes within it are imported simultaneously.
6. The new theme will appear under **Custom Imported Themes** with an instant preview and delete option.
