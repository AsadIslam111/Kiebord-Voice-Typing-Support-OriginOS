package io.kiebord.feature.ime.action

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
     * Checks if the field is a URL, email, password, code, or terminal field where
     * auto-capitalization, double-space period, and predictive word suggestions should be suppressed.
     */
    fun isUrlOrCodeField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val flags = inputType and InputType.TYPE_MASK_FLAGS

        // Non-text fields or TYPE_NULL
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            return true
        }

        // URI / URL fields
        if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
            return true
        }

        // Email address fields
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        ) {
            return true
        }

        // Password fields
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) {
            return true
        }

        // Code / Terminal fields: typically have TYPE_TEXT_FLAG_NO_SUGGESTIONS
        if ((flags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
            return true
        }

        // Known terminal / console apps
        if (isTerminalOrConsoleApp(info)) {
            return true
        }

        return false
    }
}

