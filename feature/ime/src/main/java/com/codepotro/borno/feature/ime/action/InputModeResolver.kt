package com.codepotro.borno.feature.ime.action

import android.text.InputType
import android.view.inputmethod.EditorInfo

object InputModeResolver {

    /**
     * Determines whether the given editor field requires direct character commitment
     * (bypassing composing buffers/spans and delivering text immediately via `commitText`).
     */
    fun isDirectCommit(info: EditorInfo?): Boolean {
        if (info == null) return true

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

        // 3. Password variations: Always commit directly to prevent uncommitted composing buffers
        // and ensure instant character delivery/masking.
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

    fun shouldSuppressSuggestions(info: EditorInfo?): Boolean {
        if (info == null) return true

        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            return true
        }

        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) {
            return true
        }

        if (isTerminalOrConsoleApp(info) || isCodeEditorApp(info)) {
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
