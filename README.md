# Kiebord – OriginOS Voice Typing Edition

<p align="center">
  <img width="128" height="128" alt="Kiebord" src="https://github.com/user-attachments/assets/346d3099-7f42-462a-931c-959a216ebccc" />
</p>

<p align="center">
  <b>A customized version of Kiebord with working voice typing support for Vivo OriginOS and other Chinese ROMs.</b>
</p>

---

## About

Kiebord – OriginOS Edition is a specialized fork of [Kiebord](https://github.com/JayedAhsan/Kiebord), designed to bring reliable voice typing to Vivo OriginOS and other Chinese ROMs.

Since these systems often restrict standard Android voice input services, third-party keyboards may not support voice typing properly.

This version addresses that limitation with a **Foreground Recognition Proxy** and a **Smart Text-Commit Loop**, enabling speech-to-text directly from the keyboard.

For keyboard layouts, themes, and other features, visit the [Official Kiebord Repository](https://github.com/JayedAhsan/Kiebord).

## Voice Typing Setup

Two voice typing modes are available.

### 1. Google Voice Dialog (Recommended)

The easiest option. No additional configuration or ADB commands required.

1. Open **Kiebord Settings → Voice Typing**.
2. Select **Google Voice Dialog (Popup)** or leave it on **Auto**.
3. Tap the microphone icon and start speaking.

Your recognized text will be inserted into the active input field.

### 2. Inline Dictation

For continuous voice recognition directly from the keyboard toolbar.

**Step 1 – Install Required Apps**

Make sure these Google apps are installed:

- [Speech Recognition & Synthesis by Google](https://play.google.com/store/apps/details?id=com.google.android.tts)
- [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox)

**Step 2 – Configure System Settings**

Allow the following permissions to prevent OriginOS from restricting background recognition.

| Setting | Configuration |
|---|---|
| Autostart | Enable for Kiebord |
| Associated Startup | Enable for Kiebord |
| Background Power | Set Kiebord and Google to High background power consumption |
| Assist App | Select Google, if available |

**Step 3 – Set Default Voice Recognition**

If OriginOS hides the voice input settings:

1. Install [Activity Launcher](https://play.google.com/store/apps/details?id=de.szalkowski.activitylauncher).
2. Search for `VoiceInputSettingsActivity` or `Default apps`.
3. Open the relevant settings and select Google as the voice recognition provider.

Alternatively, run this ADB command:

```bash
adb shell settings put secure voice_recognition_service com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService
```

---

## Download

**Latest APK:** [Download from Releases](https://github.com/AsadIslam111/Kiebord-Voice-Typing-Support-OriginOS/releases)

- Architecture: `arm64-v8a` (64-bit ARM devices)
- APK Size: Approximately 7.86 MB

---

## Credits

Special thanks to [Jayed Ahsan Saad](https://jayed.me) ([@JayedAhsan](https://github.com/JayedAhsan)) and [Codepotro](https://codepotro.com) for creating and maintaining the original [Kiebord](https://github.com/JayedAhsan/Kiebord) project and its Rust-based engine.

This project is built upon their open-source work.

## License

Licensed under the **GNU General Public License v3.0 (GPL-3.0)**, following the original Kiebord project.