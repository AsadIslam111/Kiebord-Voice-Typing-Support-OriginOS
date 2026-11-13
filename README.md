<div align="center">

# Kiebord `OriginOS Edition`

<img width="112" height="112" alt="Kiebord Icon" src="https://github.com/user-attachments/assets/346d3099-7f42-462a-931c-959a216ebccc" style="border-radius: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.4);" />

### Seamless Voice Typing for Vivo OriginOS & Chinese ROMs

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Arch](https://img.shields.io/badge/Arch-arm64--v8a-blue?style=flat-square&logo=arm&logoColor=white)](https://github.com/AsadIslam111/Kiebord-Voice-Typing-Support-OriginOS/releases)
[![Size](https://img.shields.io/badge/APK%20Size-~7.86%20MB-blueviolet?style=flat-square)](https://github.com/AsadIslam111/Kiebord-Voice-Typing-Support-OriginOS/releases)
[![OriginOS](https://img.shields.io/badge/Tested%20on-OriginOS%204%20%7C%205-FF4F00?style=flat-square&logo=vivo&logoColor=white)](https://www.vivo.com/en/originos)
[![License](https://img.shields.io/badge/License-GPL%203.0-brightgreen?style=flat-square)](LICENSE)

<br/>

[🚀 **Quick Setup**](#-quick-setup) • [🎙️ **Voice Modes**](#-voice-typing-modes) • [📥 **Download APK**](#-download-apk) • [✨ **Upstream Project**](https://github.com/JayedAhsan/Kiebord)

</div>

---

### 💡 Why this Fork?

On **Vivo OriginOS** (and Chinese ROMs like Xiaomi HyperOS CN, Honor MagicOS CN), standard voice typing breaks because the system aggressively terminates background binder connections and replaces AOSP speech menus with Jovi / BlueLM.

This edition fixes that via:
- 🛡️ **Foreground Recognition Proxy (`VoiceRecognitionActivity`)**: Completely immune to OriginOS background process killing.
- 🎯 **Smart Commit Retry Queue**: Guarantees zero dropped text when the speech dialog closes and focus returns to your input field.
- ⚡ **Streamlined ARM64-only Build**: Optimized with ProGuard/R8 down to **~7.86 MB**.

> [!NOTE]
> For all general keyboard features, full layout documentation (Bangla Phonetic, Avro, Probhat, English, Arabic), themes, and source code, visit the **[Official Kiebord Repository](https://github.com/JayedAhsan/Kiebord)**.

---

### 🎙️ Voice Typing Modes

```
Choose your preferred voice typing engine in Kiebord Settings → Voice Typing
```

#### 🟢 Mode 1: Google Voice Dialog (Recommended & Zero-Setup)
> **No ADB or system permission tweaks required.** Works straight out of the box!

1. Open **Kiebord Settings → Voice Typing**.
2. Select **Google Voice Dialog (Popup)** (or keep **Auto**).
3. Tap the microphone icon on the keyboard and speak. Your text inserts directly into the active field.

---

#### 🟣 Mode 2: Inline Dictation (Toolbar Mode)
> For continuous speech recognition directly on the keyboard's top toolbar.

1. **Install Prerequisites (Google Play):**
   * [`Speech Recognition & Synthesis by Google`](https://play.google.com/store/apps/details?id=com.google.android.tts)
   * [`Google App`](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox)

2. **Grant OriginOS Permissions:**
   | Setting | Required Option | Why it's needed |
   | :--- | :--- | :--- |
   | **Autostart** | `ON` for Kiebord | Prevents OriginOS from killing the service |
   | **Associated Startup** | `ON` for Kiebord | Allows Google Speech Service to awaken Kiebord |
   | **Background Power** | `High power consumption` | Prevents sleep during live voice sessions |
   | **Default Assist App** | `Google` | Routes voice requests to Google's engine |

3. **Set Default Voice Engine:**
   * **Via [Activity Launcher](https://play.google.com/store/apps/details?id=de.szalkowski.activitylauncher):** Search for `VoiceInputSettingsActivity` or `Default apps` $\to$ select **Google**.
   * **Or via one-click ADB:**
     ```bash
     adb shell settings put secure voice_recognition_service com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService
     ```

---

### 📥 Download APK

| Build Variant | Architecture | Size | Download |
| :--- | :--- | :--- | :--- |
| **OriginOS Release** | `arm64-v8a` (Modern 64-bit phones) | **~7.86 MB** | [⬇️ **Download Latest APK**](https://github.com/AsadIslam111/Kiebord-Voice-Typing-Support-OriginOS/releases) |

---

### 🙏 Credits & Acknowledgements

Heartfelt thanks and full credit to:
* **[Jayed Ahsan Saad](https://jayed.me)** ([@JayedAhsan](https://github.com/JayedAhsan))
* **[Codepotro](https://codepotro.com)**

For architecting the original **[Kiebord](https://github.com/JayedAhsan/Kiebord)** project, its native Rust engine (`borno-engine`), and its sleek Jetpack Compose UI. This fork exists purely as a companion edition for the OriginOS community.

---

<div align="center">

<sub>Licensed under <a href="LICENSE">GNU General Public License v3.0 (GPL-3.0)</a></sub>

</div>
