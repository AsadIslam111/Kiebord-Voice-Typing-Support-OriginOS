# Kiebord Keyboard Theme Development & Import Guide

This guide describes how to create, customize, and import keyboard themes for Kiebord using the unified `.bornotheme` format.

---

## 1. Overview

Kiebord supports:
- Built-in original themes (Default Dark, AMOLED Black, Tokyo Night, Nord Frost, Default Light).
- Bundled Borno color presets (Classics, AMOLED, Aesthetic, Pastels, and Accent colors).
- Dynamic custom themes loaded from storage via `.bornotheme` files.

The **`.bornotheme`** format is versioned by `schemaVersion`:
- **`.bornotheme` Version 1 (`schemaVersion: 1`)**: Borno's legacy format for backward compatibility.
- **`.bornotheme` Version 2 (`schemaVersion: 2`)**: Kiebord's transformed modern native format retaining all advanced engine capabilities.

---

## 2. Supported Formats

### Format A: Borno Theme Version 1 (`.bornotheme` with `schemaVersion = 1`)
The legacy Borno Keyboard theme format supporting solid colors, 2-stop linear gradients, and portable Base64-encoded wallpaper backgrounds. Files can be raw JSON or Base64-encoded UTF-8 strings.
```json
{
  "schemaVersion": 1,
  "builder": "100",
  "name": "Borno Sunset Orange",
  "backgroundType": 4,
  "backgroundColor": "#1A1A1A",
  "backgroundOpacity": 255,
  "keyColor": "#2D2D2D",
  "labelKeyColor": "#FFFFFF",
  "functionalKeyColor": "#222222",
  "labelFunctionalKeyColor": "#E0E0E0",
  "enterColor": "#FF5722",
  "enterIconColor": "#FFFFFF",
  "selectedKeyColor": "#FF5722",
  "stripColor": "#00000000",
  "keyStrokeColor": "#3A3A3A",
  "keyStrokeSize": 1,
  "keyRadius": 12.0,
  "isFlat": false,
  "isModern": true,
  "backgroundImageBase64": "<optional_base64_jpeg_data>"
}
```

> **Note on Image Backgrounds**: When `backgroundImageBase64` is included, Kiebord automatically decodes the image, downscales it to optimal dimensions (max 1280px), stores it in internal app storage, and manages its lifecycle.

### Format B: Borno Theme Version 2 (`.bornotheme` Single-File JSON)
The transformed native Kiebord theme format retaining all modern design and rendering capabilities:
- Multi-stop Linear, Radial, Sweep (angular), and 2D Mesh gradients.
- Image backgrounds with blur radius, dimming overlay, and scale types (`crop`, `fit`, `fill`, `tile`).
- Granular per-key overrides (`perKeyOverrides`) by coordinate, label, or key type.
- Fractional stroke widths (`widthDp`), 2D elevation shadows (`offsetX`, `offsetY`, `radiusDp`), pill shapes, and custom corner radii.
- Custom vector icons for modifier/action keys (`icons`).
- Custom typography font file link (`fontPath`).
- Independent component-level theming for Suggestion strip, Tools drawer, and MoreKeys popups.
- Embedded `backgroundImageBase64` for 100% portable single-file distribution.

```json
{
  "schemaVersion": 2,
  "id": "custom_cyber_neon",
  "name": "Cyber Neon v2",
  "isDark": true,
  "background": {
    "gradient": {
      "type": "linear",
      "colors": ["#0F0C29", "#302B63", "#24243E"],
      "stops": [0.0, 0.5, 1.0],
      "angle": 135.0
    }
  },
  "characterKey": {
    "background": "#1A1B2F",
    "foreground": "#E0E7FF",
    "stroke": {
      "color": "#3B82F6",
      "widthDp": 0.5,
      "alpha": 0.35
    },
    "shadow": {
      "color": "#4D000000",
      "elevationDp": 1.5
    },
    "cornerRadiusDp": 8.0,
    "pressedBackground": "#28294E"
  },
  "functionalKey": {
    "background": "#141527",
    "foreground": "#93C5FD",
    "shadow": {
      "color": "#4D000000",
      "elevationDp": 1.5
    }
  },
  "actionKey": {
    "background": "#3B82F6",
    "foreground": "#FFFFFF",
    "isPill": true,
    "pressedBackground": "#2563EB"
  },
  "accentColor": "#3B82F6",
  "shiftActiveColor": "#60A5FA",
  "flatKeyLabelColor": "#E0E7FF",
  "activeOptionHighlightColor": "#00E5FF",
  "fontPath": "font.ttf",
  "icons": {
    "shift": "icons/shift.png",
    "backspace": "icons/backspace.png",
    "action": "icons/action.png"
  },
  "suggestionBackground": {
    "solidColor": "#0F0C29"
  },
  "suggestionForeground": "#E0E7FF",
  "moreKeysBackground": "#1A1B2F",
  "guidelineColor": "#3B82F6",
  "perKeyOverrides": {
    "space": {
      "background": "#1A1B2F",
      "cornerRadiusDp": 16.0
    }
  }
}
```

### Format C: Full Asset Bundle (`.bornotheme` ZIP Package)
For themes that include heavy binary assets (wallpapers, custom fonts, and custom key icons), `.bornotheme` files can be standard ZIP packages containing raw companion files alongside `theme.json`:

```
my_theme.bornotheme (ZIP Archive)
├── theme.json                     # Theme specification (KeyboardThemeSpec v2)
├── background.jpg                 # Background wallpaper image (auto-discovered)
├── font.ttf                       # Custom typography font (auto-discovered)
└── icons/                         # Custom functional key icons (auto-discovered)
    ├── shift.png
    ├── shift_active.png
    ├── caps_lock.png
    ├── backspace.png
    ├── action.png
    ├── language.png
    ├── emoji.png
    └── settings.png
```

**Auto-Discovery & Path Normalization**:
- Kiebord automatically checks for ZIP magic bytes (`PK\u0003\u0004`) upon opening.
- Unpacks safely into `custom_themes/<theme_id>/` with strict ZipSlip directory traversal security.
- Automatically connects `background.*` to the keyboard wallpaper.
- Automatically connects `font.ttf` / `font.otf` to the keyboard font family.
- Automatically connects `icons/*` to functional keys (Shift, Backspace, Action, Language, etc.).
- Normalizes any relative paths in `theme.json` to local absolute paths on the device.

### Format D: Bundled Presets (`borno_themes.json` asset)
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
        }
      ]
    }
  ]
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
| `flatKeyLabelColor` | `flatKeyForeground` | Key label color when key borders are disabled |
| `activeOptionHighlightColor` | `activeOptionHighlightColor` | Highlight glow/border/icon tint for active toggleable toolbar options |
| `stripColor` | `suggestionBackground` | Suggestion bar background |
| `stripIcoColor` | `suggestionForeground` | Suggestion bar icons and candidate text color |
| `fontPath` | `fontPath` | Path to custom TTF/OTF typography font file |
| `icons` | `icons` | Custom functional keycap icons (Shift, Delete, Action, Language, etc.) |

---

## 5. Importing Themes into Kiebord

1. Open Kiebord Settings.
2. Navigate to **Theme & Appearance**.
3. Tap the **+** card under Custom Themes and select **Import (.bornotheme)**.
4. Select your `.bornotheme` file from the system document picker (or tap it in any file manager / messaging app).
5. An interactive **Pre-Import Preview** bottom sheet will open with a live preview of the keyboard.
6. Tap **Import** to confirm and apply, or **Cancel** to discard.
