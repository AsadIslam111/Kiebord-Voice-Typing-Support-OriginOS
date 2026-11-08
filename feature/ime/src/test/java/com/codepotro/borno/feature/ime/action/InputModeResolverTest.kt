package com.codepotro.borno.feature.ime.action

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputModeResolverTest {

    @Test
    fun testNullEditorInfoDefaultsToDirectCommit() {
        assertTrue(InputModeResolver.isDirectCommit(null))
    }

    @Test
    fun testTypeNullResolvesToDirectCommit() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_NULL
        }
        assertTrue(InputModeResolver.isDirectCommit(info))
    }

    @Test
    fun testNonTextClassResolvesToDirectCommit() {
        val phoneInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        assertTrue(InputModeResolver.isDirectCommit(phoneInfo))

        val dateTimeInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_DATETIME
        }
        assertTrue(InputModeResolver.isDirectCommit(dateTimeInfo))
    }

    @Test
    fun testPasswordVariationsResolveToDirectCommit() {
        val passwordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(InputModeResolver.isDirectCommit(passwordInfo))

        val visiblePasswordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        assertTrue(InputModeResolver.isDirectCommit(visiblePasswordInfo))

        val webPasswordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        }
        assertTrue(InputModeResolver.isDirectCommit(webPasswordInfo))
    }

    @Test
    fun testTerminalAndConsoleAppsResolveToDirectCommit() {
        val termuxInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.termux"
        }
        assertTrue(InputModeResolver.isDirectCommit(termuxInfo))
        assertTrue(InputModeResolver.isTerminalOrConsoleApp(termuxInfo))

        val termuxFloatInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.termux.window"
        }
        assertTrue(InputModeResolver.isDirectCommit(termuxFloatInfo))

        val connectBotInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "org.connectbot"
        }
        assertTrue(InputModeResolver.isDirectCommit(connectBotInfo))

        val juiceSshInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.sonelli.juicessh"
        }
        assertTrue(InputModeResolver.isDirectCommit(juiceSshInfo))

        val androidTermInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "jackpal.androidterm"
        }
        assertTrue(InputModeResolver.isDirectCommit(androidTermInfo))
    }

    @Test
    fun testStandardEditTextDoesNotResolveToDirectCommit() {
        val standardText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            packageName = "org.telegram.messenger"
        }
        assertFalse(InputModeResolver.isDirectCommit(standardText))

        val multilineText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            packageName = "com.whatsapp"
        }
        assertFalse(InputModeResolver.isDirectCommit(multilineText))
    }

    @Test
    fun testIsSearchField() {
        assertFalse(InputModeResolver.isSearchField(null))

        val searchActionInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        assertTrue(InputModeResolver.isSearchField(searchActionInfo))

        val filterVariationInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER
        }
        assertTrue(InputModeResolver.isSearchField(filterVariationInfo))

        val autoCompleteInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        }
        assertTrue(InputModeResolver.isSearchField(autoCompleteInfo))

        val regularTextInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        assertFalse(InputModeResolver.isSearchField(regularTextInfo))
    }

    @Test
    fun testIsGeneralTextInputAndUrlOrCodeField() {
        val uriInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(uriInfo))
        assertFalse(InputModeResolver.isGeneralTextInput(uriInfo))

        val emailInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(emailInfo))
        assertFalse(InputModeResolver.isGeneralTextInput(emailInfo))
        assertTrue(InputModeResolver.isEmailField(emailInfo))

        val webEmailInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        }
        assertTrue(InputModeResolver.isEmailField(webEmailInfo))
        assertFalse(InputModeResolver.isGeneralTextInput(webEmailInfo))

        val passwordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(passwordInfo))
        assertFalse(InputModeResolver.isGeneralTextInput(passwordInfo))

        val termuxInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.termux"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(termuxInfo))
        assertFalse(InputModeResolver.isGeneralTextInput(termuxInfo))
        assertFalse(InputModeResolver.isAutoSpaceFriendly(termuxInfo))

        val acodeInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            packageName = "com.foxdebug.acode"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(acodeInfo))
        assertFalse(InputModeResolver.isGeneralTextInput(acodeInfo))
        assertFalse(InputModeResolver.isAutoSpaceFriendly(acodeInfo))

        val regularTextInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.example.chat"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(regularTextInfo))
        assertTrue(InputModeResolver.isGeneralTextInput(regularTextInfo))
        assertTrue(InputModeResolver.isAutoSpaceFriendly(regularTextInfo))

        // AI Chatbot app: ChatGPT with NO_SUGGESTIONS
        val chatGptInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            packageName = "com.openai.chatgpt"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(chatGptInfo))
        assertTrue(InputModeResolver.isGeneralTextInput(chatGptInfo))
        assertTrue(InputModeResolver.isAutoSpaceFriendly(chatGptInfo))

        // AI Chatbot app: Gemini in googlequicksearchbox
        val geminiPromptInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_SEND
            packageName = "com.google.android.googlequicksearchbox"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(geminiPromptInfo))
        assertTrue(InputModeResolver.isGeneralTextInput(geminiPromptInfo))

        // AI Chatbot app: Claude with WEB_EDIT_TEXT
        val claudeWebInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
            packageName = "com.anthropic.claude"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(claudeWebInfo))
        assertTrue(InputModeResolver.isGeneralTextInput(claudeWebInfo))

        // AI Search app: Perplexity with IME_ACTION_SEARCH
        val perplexityInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            packageName = "ai.perplexity.app.android"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(perplexityInfo))
        assertTrue(InputModeResolver.isGeneralTextInput(perplexityInfo))

        val keepInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            packageName = "com.google.android.keep"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(keepInfo))
        assertTrue(InputModeResolver.isGeneralTextInput(keepInfo))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(keepInfo))

        val braveOmnibox = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            packageName = "com.brave.browser"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(braveOmnibox))
        assertFalse(InputModeResolver.isGeneralTextInput(braveOmnibox))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(braveOmnibox))

        val standaloneUri = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            packageName = "com.example.app"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(standaloneUri))
        assertFalse(InputModeResolver.isGeneralTextInput(standaloneUri))
        assertTrue(InputModeResolver.shouldSuppressSuggestions(standaloneUri))

        // Google Search Widget (single-line search in googlequicksearchbox)
        val googleSearchWidget = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            packageName = "com.google.android.googlequicksearchbox"
        }
        assertTrue(InputModeResolver.isSearchOrWebAddressField(googleSearchWidget))
        assertTrue(InputModeResolver.isUrlOrCodeField(googleSearchWidget))
        assertFalse(InputModeResolver.isAutoSpaceFriendly(googleSearchWidget))

        // Chrome omnibox / URL search bar (single-line)
        val chromeOmnibox = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_GO
            packageName = "com.android.chrome"
        }
        assertTrue(InputModeResolver.isSearchOrWebAddressField(chromeOmnibox))
        assertTrue(InputModeResolver.isUrlOrCodeField(chromeOmnibox))
        assertFalse(InputModeResolver.isAutoSpaceFriendly(chromeOmnibox))

        // Firefox / Fenix search bar (single-line)
        val firefoxSearch = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            packageName = "org.mozilla.firefox"
        }
        assertTrue(InputModeResolver.isSearchOrWebAddressField(firefoxSearch))
        assertTrue(InputModeResolver.isUrlOrCodeField(firefoxSearch))
        assertFalse(InputModeResolver.isAutoSpaceFriendly(firefoxSearch))

        // Multiline web text area / comment inside Chrome (e.g. GitHub, forum, web ChatGPT)
        val chromeWebTextArea = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            packageName = "com.android.chrome"
        }
        assertFalse(InputModeResolver.isSearchOrWebAddressField(chromeWebTextArea))
        assertFalse(InputModeResolver.isUrlOrCodeField(chromeWebTextArea))
        assertTrue(InputModeResolver.isGeneralTextInput(chromeWebTextArea))
        assertTrue(InputModeResolver.isAutoSpaceFriendly(chromeWebTextArea))
    }
}
