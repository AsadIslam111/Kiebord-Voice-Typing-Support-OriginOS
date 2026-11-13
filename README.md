# Kiebord (Vivo & OriginOS Edition with Voice Typing)

<p align="center">
  <img width="128" height="128" alt="Kiebord" src="https://github.com/user-attachments/assets/346d3099-7f42-462a-931c-959a216ebccc" />
</p>

<p align="center">
  <b>A customized build of Kiebord tailored specifically for Vivo OriginOS, Xiaomi HyperOS, and Chinese ROM devices with 100% working Voice Typing & Speech-to-Text.</b>
</p>

<p align="center">
  <a href="#-quick-setup-for-vivo--originos-users">Quick Setup</a> •
  <a href="#-why-this-fork-exists">Why This Project Exists</a> •
  <a href="#-features">Features</a> •
  <a href="#-download-apk">Download APK</a> •
  <a href="docs/VIVO_ORIGINOS_VOICE_GUIDE.md">Full Setup Guide</a>
</p>

---

## 🌟 Why This Project Exists

On **Vivo OriginOS** (and other Chinese ROMs like Xiaomi HyperOS CN, Honor MagicOS CN, Oppo ColorOS):
1. **Background Voice Services are Blocked**: OriginOS aggressively prevents third-party keyboards from connecting to Google's background speech recognizer, resulting in immediate failures (`Error 5: Client Error` or silent drops).
2. **Missing AOSP Voice Settings**: Vivo replaces standard Android voice settings with proprietary Chinese assistants (Jovi / BlueLM), leaving Google voice typing broken out of the box.
3. **IME Window Loss on Popup**: Standard speech dialogs cause the soft keyboard to lose focus, dropping recognized text before it can reach the text field.

### How this build solves it:
* 🚀 **Foreground Proxy Engine (`VoiceRecognitionActivity`)**: Bypasses 100% of OriginOS background restrictions by running recognition in a transparent foreground activity with direct ActivityManager priority.
* 🔄 **Smart Asynchronous Commit Queue**: Guarantees that speech text is buffered and committed directly into the text field even across asynchronous window focus changes.
* 🎙️ **Dual Mode Flexibility**: Choose between **Google Voice Dialog (Popup)** (zero setup needed) or **Continuous Inline Typing** with automatic fallback.
* ⚡ **Pre-configured Google Speech Component Discovery**: Directly binds to `com.google.android.tts` (`GoogleTTSRecognitionService`) and `com.google.android.googlequicksearchbox`.
* 📦 **Lightweight ARM64 Build**: Stripped of legacy 32-bit binaries, optimized with R8/ProGuard down to just **~7.8 MB**.

---

## 📱 Quick Setup for Vivo & OriginOS Users

### Recommended Method: Google Voice Dialog (Zero Setup)
Inside Kiebord settings:
1. Open **Kiebord Settings** $\to$ **Voice Typing**.
2. Select **Google Voice Dialog (Popup)** or leave it on **Auto**.
3. Done! Tap the microphone icon on the keyboard anytime and speak.

---

### For Continuous Inline Dictation (Toolbar Mode)
If you prefer voice typing directly on the keyboard toolbar without any popup:

1. **Install Google Speech Services**:
   - Install **Speech Recognition and Synthesis by Google** (`com.google.android.tts`) and **Google** from Google Play Store.
2. **Set Default Assist App**:
   - Go to phone **Settings** $\to$ **Apps** $\to$ **Default apps** $\to$ **Assist app & voice input**.
   - Set **Default assist app** to **Google**.
3. **Grant Background Permissions**:
   - In phone **Settings** $\to$ **Battery** $\to$ **Background power consumption management**, set both **Kiebord** and **Google** to **High background power consumption**.
   - In **Permission management** $\to$ **Autostart**, turn **ON** both **Autostart** and **Associated startup** for Kiebord.

> 📖 **Full step-by-step guide with screenshots & ADB commands:** See [docs/VIVO_ORIGINOS_VOICE_GUIDE.md](docs/VIVO_ORIGINOS_VOICE_GUIDE.md).

---

## 📥 Download APK

| Build | Architecture | Size | Link |
| :--- | :--- | :--- | :--- |
| **Release APK** | `arm64-v8a` | **~7.86 MB** | [app-release.apk](app/build/outputs/apk/release/app-release.apk) |

---

## ✨ General Kiebord Features

* **Supported Layouts**:
  * **Bangla**: Borno Phonetic, Avro Phonetic, Borno Easy, National (BSTI), Probhat, Khipro
  * **English**: QWERTY, Borno Math
  * **Arabic**: Arabic with dedicated Harakat/diacritics row
  * **Custom extensions**: `.bornolang` pack support
* **Native Rust Engine**: High-performance spatial touch modeling and dictionary lookup (`borno-engine`).
* **Gesture Typing**: Glide typing and word predictions.
* **Smart Gestures**: Slide spacebar to navigate cursor, slide backspace to delete words.
* **Clipboard Manager & Text Editing Panel**: Built-in clipboard history and precision cursor arrows.
* **Privacy-First**: No internet access required for regular typing, 100% on-device keyboard logic.

---

## 🛠️ Building from Source

```bash
# Clone this repository
git clone https://github.com/AsadIslam111/Kiebord.git
cd Kiebord

# Build the Release APK (arm64-v8a)
./gradlew assembleRelease
```
Output APK location: `app/build/outputs/apk/release/app-release.apk`.

---

## 📜 Credits & License

* Forked from the upstream project [Kiebord](https://github.com/JayedAhsan/Kiebord) by [Jayed Ahsan Saad](https://jayed.me) / [Codepotro](https://codepotro.com).
* Licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
