package com.codepotro.borno.feature.ime.action

import android.text.InputType
import android.view.inputmethod.EditorInfo

object InputModeResolver {

    /**
     * Checks if the field is a password, PIN, or passcode input.
     * Evaluates text and numeric password variations, as well as common security heuristics.
     */
    fun isPasswordField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType

        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        // Standard text password variations
        if (inputClass == InputType.TYPE_CLASS_TEXT) {
            if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            ) {
                return true
            }
        }

        // Numeric password variations (PIN pads)
        if (inputClass == InputType.TYPE_CLASS_NUMBER) {
            if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                return true
            }
        }

        // Direct variation matches for text passwords if class is omitted / 0
        if (inputClass == 0) {
            if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            ) {
                return true
            }
        }

        // Heuristics for banking apps / custom PIN fields declaring normal or null inputs
        val fieldName = (info.fieldName ?: "").lowercase()
        val hint = (info.hintText?.toString() ?: "").lowercase()
        if (fieldName.contains("password") || fieldName.contains("passcode") ||
            fieldName.contains("pin_") || fieldName.endsWith("_pin") ||
            fieldName == "pin" || fieldName == "pass" ||
            fieldName.contains("mpin") || fieldName.contains("tpin") ||
            fieldName.contains("cvv") || fieldName.contains("otp")
        ) {
            return true
        }
        if (hint.contains("password") || hint.contains("passcode") ||
            hint.contains("enter pin") || hint.contains("pin code") ||
            hint.contains("mpin") || hint.contains("tpin") ||
            hint.contains("security code")
        ) {
            return true
        }

        return false
    }

    /**
     * Checks if the field is in Incognito or Private browsing mode.
     * Supports standard Android IME_FLAG_NO_PERSONALIZED_LEARNING and major browser extras.
     */
    fun isIncognitoMode(info: EditorInfo?): Boolean {
        if (info == null) return false

        // Standard platform flag (API 26+)
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
            return true
        }

        // Browser-specific extras (Chrome, Chromium, Firefox, etc.)
        val extras = info.extras
        if (extras != null) {
            if (extras.getBoolean("org.chromium.chrome.browser.incognito", false) ||
                extras.getBoolean("incognito", false) ||
                extras.getBoolean("is_incognito", false) ||
                extras.getBoolean("private_browsing_mode", false)
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Determines whether the given editor field requires direct character commitment
     * (bypassing composing buffers/spans and delivering text immediately via `commitText`).
     */
    fun isDirectCommit(info: EditorInfo?): Boolean {
        if (info == null) return true

        // Password & PIN fields must always commit directly to prevent composing spans / keylogger detections
        if (isPasswordField(info)) {
            return true
        }

        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS

        // 1. TYPE_NULL: Android contract specifies no complex edits / no composing spans supported.
        if (inputType == InputType.TYPE_NULL || inputClass == InputType.TYPE_NULL) {
            return true
        }

        // 2. Non-text fields (Number, Phone, DateTime or custom non-text classes)
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            return true
        }

        val variation = inputType and InputType.TYPE_MASK_VARIATION

        // 3. Password variations
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) {
            return true
        }

        // 4. Terminal / console emulator applications (as a fail-safe even if they report an unusual inputType)
        if (isTerminalOrConsoleApp(info)) {
            return true
        }

        return false
    }

    /**
     * Checks if the target application is a terminal emulator, SSH client, or console shell.
     */
    fun isTerminalOrConsoleApp(info: EditorInfo?): Boolean {
        if (info == null) return false
        val pkg = (info.packageName ?: "").lowercase()
        return pkg.startsWith("com.termux") ||
                pkg.contains("termux") ||
                pkg.contains("terminal") ||
                pkg.contains("connectbot") ||
                pkg.contains("juicessh") ||
                pkg.contains("termius") ||
                pkg.contains("androidterm") ||
                pkg.contains("console") ||
                pkg.contains("shell")
    }

    /**
     * Checks if the target application is a code editor or IDE (e.g. Acode, Replit, QuickEdit).
     */
    fun isCodeEditorApp(info: EditorInfo?): Boolean {
        if (info == null) return false
        val pkg = (info.packageName ?: "").lowercase()
        return pkg.contains("acode") ||
                pkg.contains("replit") ||
                pkg.contains("droidedit") ||
                pkg.contains("quickedit") ||
                pkg.contains("aode")
    }

    fun isEmailField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) return false
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    fun isWebBrowserApp(info: EditorInfo?): Boolean {
        if (info == null) return false
        val pkg = (info.packageName ?: "").lowercase()
        return pkg.contains("browser") ||
                pkg.contains("chrome") ||
                pkg.contains("firefox") ||
                pkg.contains("fenix") ||
                pkg.contains("brave") ||
                pkg.contains("duckduckgo") ||
                pkg.contains("opera") ||
                pkg.contains("vivaldi") ||
                pkg.contains("edge") ||
                pkg.contains("emmx")
    }

    /**
     * Checks if the field is a search field (e.g. Android SearchView, in-app search bar, or filter).
     */
    fun isSearchField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        val flags = info.inputType and InputType.TYPE_MASK_FLAGS
        val pkg = (info.packageName ?: "").lowercase()
        return action == EditorInfo.IME_ACTION_SEARCH ||
                action == EditorInfo.IME_ACTION_GO ||
                variation == InputType.TYPE_TEXT_VARIATION_FILTER ||
                (flags and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0 ||
                pkg.contains("search") ||
                isWebBrowserApp(info)
    }

    /**
     * Determines whether the field is a general natural text input, eligible for
     * auto-capitalization, punctuation handling, and text suggestions.
     * Follows Borno Keyboard's mIsGeneralTextInput protocol-based classification.
     */
    fun isGeneralTextInput(info: EditorInfo?): Boolean {
        if (info == null) return false
        if (isPasswordField(info)) return false

        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) return false

        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS &&
                variation != InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS &&
                variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
                variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
                variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD &&
                variation != InputType.TYPE_TEXT_VARIATION_URI &&
                variation != InputType.TYPE_TEXT_VARIATION_FILTER &&
                variation != InputType.TYPE_TEXT_VARIATION_PHONETIC &&
                !isTerminalOrConsoleApp(info) &&
                !isCodeEditorApp(info)
    }

    fun isAutoSpaceFriendly(info: EditorInfo?): Boolean {
        return isGeneralTextInput(info) && !isSearchOrWebAddressField(info)
    }

    fun isSearchOrWebAddressField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) return false

        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_FILTER
        ) {
            return true
        }

        val flags = inputType and InputType.TYPE_MASK_FLAGS
        val isMultiline = (flags and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION

        // Single-line fields with search/go action, autocomplete flag, or inside search widgets/browser bars
        if (!isMultiline) {
            if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_GO) {
                return true
            }
            if ((flags and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                return true
            }
            if (isWebBrowserApp(info)) {
                return true
            }
            val pkg = (info.packageName ?: "").lowercase()
            if (pkg.contains("googlequicksearchbox") || pkg.contains("search")) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if the field is a URL, search, email, password, code, or terminal field where
     * auto-capitalization, punctuation auto-spacing, and double-space period should be suppressed.
     */
    fun isUrlOrCodeField(info: EditorInfo?): Boolean {
        return !isGeneralTextInput(info) || isSearchOrWebAddressField(info)
    }

    /**
     * Determines if suggestions should be strictly suppressed.
     * Always suppresses suggestions for password/PIN fields, fields with NO_SUGGESTIONS flag,
     * terminal/code editors, and direct commit inputs.
     */
    fun shouldSuppressSuggestions(info: EditorInfo?): Boolean {
        if (info == null) return true

        if (isPasswordField(info)) {
            return true
        }

        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            return true
        }

        if (isTerminalOrConsoleApp(info) || isCodeEditorApp(info)) {
            return true
        }

        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) {
            return true
        }

        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        ) {
            return true
        }

        if (isSearchField(info)) {
            return false
        }

        if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
            return !isWebBrowserApp(info)
        }

        if (isWebBrowserApp(info) || variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT) {
            return false
        }

        return false
    }
}
