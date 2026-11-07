package io.kiebord.feature.ime.action

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
    fun testIsUrlOrCodeField() {
        val uriInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(uriInfo))

        val emailInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(emailInfo))
        assertTrue(InputModeResolver.isEmailField(emailInfo))

        val webEmailInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        }
        assertTrue(InputModeResolver.isEmailField(webEmailInfo))
        assertFalse(InputModeResolver.isEmailField(uriInfo))

        val passwordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(passwordInfo))

        val noSuggestionsInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(noSuggestionsInfo))

        val searchWithNoSuggestions = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(searchWithNoSuggestions))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(searchWithNoSuggestions))

        val searchViewInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(searchViewInfo))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(searchViewInfo))

        val googleSearchWidget = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            packageName = "com.google.android.googlequicksearchbox"
        }
        assertTrue(InputModeResolver.isSearchField(googleSearchWidget))
        assertTrue(InputModeResolver.isUrlOrCodeField(googleSearchWidget))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(googleSearchWidget))

        val termuxInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.termux"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(termuxInfo))

        val regularTextInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.example.chat"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(regularTextInfo))

        val keepInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            packageName = "com.google.android.keep"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(keepInfo))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(keepInfo))

        val braveOmnibox = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            packageName = "com.brave.browser"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(braveOmnibox))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(braveOmnibox))

        val braveWebSearch = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            packageName = "com.brave.browser"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(braveWebSearch))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(braveWebSearch))

        val standaloneUri = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            packageName = "com.example.app"
        }
        assertTrue(InputModeResolver.isUrlOrCodeField(standaloneUri))
        assertTrue(InputModeResolver.shouldSuppressSuggestions(standaloneUri))

        val multiLineWithCaps = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            packageName = "com.some.notes"
        }
        assertFalse(InputModeResolver.isUrlOrCodeField(multiLineWithCaps))
        assertFalse(InputModeResolver.shouldSuppressSuggestions(multiLineWithCaps))
    }
}
