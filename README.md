# Kiebord (Vivo OriginOS Voice Typing Edition)

<p align="center">
  <img width="128" height="128" alt="Kiebord" src="https://github.com/user-attachments/assets/346d3099-7f42-462a-931c-959a216ebccc" />
</p>

<p align="center">
  <b>A specialized fork of Kiebord customized specifically for Vivo OriginOS (and Chinese ROMs) with 100% working Voice Typing & Speech-to-Text.</b>
</p>

---

## 📌 About This Project

This repository is a customized side-project of **Kiebord** tailored for **Vivo OriginOS** users. 

On OriginOS, third-party keyboards fail to use voice typing because the system restricts background services, lacks standard AOSP voice input menus, and delegates voice commands to Jovi/BlueLM. 

This build solves those issues completely by integrating a **Foreground Recognition Proxy (`VoiceRecognitionActivity`)** and a **Smart Text-Commit Loop**, allowing seamless speech-to-text on OriginOS without missing or dropped text.

> ℹ️ *For the full keyboard features, layouts (Bangla Phonetic, Avro, Probhat, English, Arabic), themes, and general documentation, please visit the [Official Main Repository](https://github.com/JayedAhsan/Kiebord).*

---

## 🚀 How to Set Up & Enable Voice Typing on Vivo OriginOS

There are two ways to use voice typing in this build:

### Method 1: Google Voice Dialog / Popup (Zero Setup Required) ⭐ *Easiest*
1. Open **Kiebord Settings** $\to$ **Voice Typing**.
2. Under **Voice Typing Engine**, choose **Google Voice Dialog (Popup)** (or leave it on **Auto**).
3. Tap the microphone icon on your keyboard, speak, and your text will be inserted directly into the active field. No permission tweaks or ADB needed!

---

### Method 2: Inline Dictation (Directly on the Keyboard Bar)
If you prefer continuous voice typing directly on the keyboard toolbar without a popup dialog, configure these settings:

#### 1. Required Apps
Make sure these two Google apps are installed on your device (from Play Store):
* **Speech Recognition and Synthesis by Google** (`com.google.android.tts`)
* **Google** (`com.google.android.googlequicksearchbox`)

#### 2. System Permissions & Battery Settings
Because OriginOS aggressively freezes background apps, grant these permissions:

| Setting | Location in OriginOS Settings | Configuration |
| :--- | :--- | :--- |
| **Kiebord Autostart** | Settings $\to$ Apps $\to$ Permission management $\to$ Autostart | Turn **ON** |
| **Associated Startup** | Inside Autostart $\to$ Associated startup | Turn **ON** for Kiebord |
| **Background Power** | Settings $\to$ Battery $\to$ Background power consumption | Set both **Kiebord** and **Google** to **High background power consumption** |
| **Default Assist App** | Settings $\to$ Apps $\to$ Default apps $\to$ Assist app & voice input | Set to **Google** |

#### 3. Activity Launcher Setup (If Assist Settings Are Hidden)
If your OriginOS build hides the default assist app settings:
1. Install **Activity Launcher** from Play Store.
2. Search for `VoiceInputSettingsActivity` or `Default apps` under `Settings`.
3. Open it and ensure **Speech Services by Google** is selected as the active voice recognizer.

*(Alternatively, via one-time ADB command: `adb shell settings put secure voice_recognition_service com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService`)*

---

## 📥 Download APK

Pre-built and optimized exclusively for 64-bit devices (`arm64-v8a`), stripped of bloated architectures:

* **Direct Release APK**: [app-release.apk](app/build/outputs/apk/release/app-release.apk) *(~7.86 MB)*
* Or check the **[Releases](https://github.com/AsadIslam111/Kiebord-Voice-Typing-Support-OriginOS/releases)** section.

---

## 🙏 Credits & Special Thanks

A huge thanks and heartfelt credit to **[Jayed Ahsan Saad](https://jayed.me)** ([@JayedAhsan](https://github.com/JayedAhsan)) and **[Codepotro](https://codepotro.com)** for creating and maintaining the amazing **[Kiebord](https://github.com/JayedAhsan/Kiebord)** open-source project and its high-performance Rust engine. 

Without their foundational work on Borno / Kiebord, this specialized OriginOS edition would not have been possible.

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** in accordance with the original Kiebord repository.
