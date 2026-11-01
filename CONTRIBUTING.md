# Contributing to Kiebord

Thank you for your interest in making **Kiebord** better! We welcome contributions ranging from bug fixes, code optimizations, and documentation improvements to new layout definitions and themes.

Please take a few moments to read through this guide before you begin.

---

## 1. Project Overview & Licensing Model

Kiebord is maintained by **Codepotro**.

- **Open Source Track (Kiebord):** The source code of Kiebord is publicly available and licensed under the **GNU General Public License v3.0 (GPLv3)**.
- **Proprietary Ecosystem (Borno):** Codepotro also develops and maintains **Borno**, a closed-source, proprietary keyboard application and ecosystem.

To allow seamless collaboration between the open-source community and Codepotro's software ecosystem, **all contributors are required to agree to the [Kiebord Contributor License Agreement (CLA)](CLA.md)**.

### What the CLA Means For You:
1. **You keep your copyright:** You retain ownership of your submitted code and are free to use your own code elsewhere.
2. **Dual-Licensing Permission:** You grant Codepotro permission to use, adapt, and distribute your contribution both within the open-source Kiebord project (GPLv3) and within the closed-source Borno application (proprietary).
3. **No GPL Obligations for Borno:** Codepotro is not required to open-source Borno when incorporating contributions.

---

## 2. Code Purity & Intellectual Property Guidelines

To ensure the safety, integrity, and legality of both Kiebord and Borno:

- **100% Original Work:** All contributions must be your original creation.
- **No Decompiled Code:** Do **not** submit code, resources, or algorithms reverse-engineered, extracted, or decompiled from third-party APKs, binaries, or proprietary software.
- **No Copied Code:** Do not copy code snippets from proprietary sources without legally documented authorization.
- **Third-Party Open Source Code:** If your contribution relies on code from permissively licensed open-source projects (e.g., MIT, Apache 2.0, BSD), you must clearly disclose this in your Pull Request description, retain the original author's copyright notices, and include their license text.

---

## 3. Development & Local Setup

### Prerequisites
- **Android Studio** (Ladybug / Koala or newer recommended)
- **JDK 17**
- **Android SDK & NDK** (NDK configured for native engine builds)
- **Rust Toolchain** (with `cargo-ndk` if compiling native dictionary/gesture engines under `native/kiebord-engine`)

### Building the Project
Clone the repository:
```bash
git clone https://github.com/JayedAhsan/Kiebord.git
cd Kiebord
```

Build the debug APK:
```bash
./gradlew assembleDebug
```

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 4. Pull Request Workflow

1. **Fork & Branch:** Create a dedicated feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. **Write Clean Code:** Follow existing architectural patterns (Kotlin, Jetpack Compose, modular Gradle structure). Preserve existing comments and docstrings.
3. **Run Tests:** Ensure all unit tests pass locally before submitting:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Open a Pull Request:**
   - Use a descriptive PR title (e.g. `feat(ime): ...` or `fix(translit): ...`).
   - Fill out the Pull Request template.
   - Check the mandatory **Contributor License Agreement (CLA)** confirmation box.
   - If prompted by the automated CLA Assistant bot, comment on the PR to confirm your agreement.

Thank you for contributing to Kiebord and supporting the Codepotro ecosystem!

