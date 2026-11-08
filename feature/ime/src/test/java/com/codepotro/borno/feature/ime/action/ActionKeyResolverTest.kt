package com.codepotro.borno.feature.ime.action

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.codepotro.borno.common.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionKeyResolverTest {

    @Test
    fun testNullEditorInfoDefaultsToEnter() {
        val visual = ActionKeyResolver.resolve(null, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.ENTER, visual.type)
        assertFalse(visual.isAction)
        assertFalse(visual.isAccentStyle)
    }

    @Test
    fun testSearchAction() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val visual = ActionKeyResolver.resolve(info, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.SEARCH, visual.type)
        assertTrue(visual.isAction)
        assertTrue(visual.isAccentStyle)
        assertEquals(EditorInfo.IME_ACTION_SEARCH, visual.actionId)
    }

    @Test
    fun testSendAction() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val visual = ActionKeyResolver.resolve(info, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.SEND, visual.type)
        assertTrue(visual.isAction)
        assertTrue(visual.isAccentStyle)
        assertEquals(EditorInfo.IME_ACTION_SEND, visual.actionId)
    }

    @Test
    fun testFacebookCommentBoxMultilineWithNoEnterAction() {
        // Facebook comment box or multiline edit texts typically set IME_FLAG_NO_ENTER_ACTION
        // and TYPE_TEXT_FLAG_MULTI_LINE so Enter creates a newline
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val visual = ActionKeyResolver.resolve(info, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.ENTER, visual.type)
        assertFalse(visual.isAction)
        assertFalse(visual.isAccentStyle)
    }

    @Test
    fun testMultilineFieldUnspecifiedActionDefaultsToEnter() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val visual = ActionKeyResolver.resolve(info, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.ENTER, visual.type)
        assertFalse(visual.isAction)
        assertFalse(visual.isAccentStyle)
    }

    @Test
    fun testShiftEnterOverridesActionToNewline() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val visual = ActionKeyResolver.resolve(info, ShiftState.ONE_SHOT)
        assertEquals(ActionKeyType.ENTER, visual.type)
        assertFalse(visual.isAction)
        assertFalse(visual.isAccentStyle)
    }

    @Test
    fun testNextAndDoneActions() {
        val nextInfo = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val nextVisual = ActionKeyResolver.resolve(nextInfo, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.NEXT, nextVisual.type)
        assertTrue(nextVisual.isAction)
        assertTrue(nextVisual.isAccentStyle)

        val doneInfo = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val doneVisual = ActionKeyResolver.resolve(doneInfo, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.DONE, doneVisual.type)
        assertTrue(doneVisual.isAction)
        assertTrue(doneVisual.isAccentStyle)
    }

    @Test
    fun testCustomActionLabel() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            actionLabel = "Post"
            actionId = 99
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val visual = ActionKeyResolver.resolve(info, ShiftState.UNSHIFTED)
        assertEquals(ActionKeyType.CUSTOM_LABEL, visual.type)
        assertEquals("Post", visual.customLabel)
        assertEquals(99, visual.actionId)
        assertTrue(visual.isAction)
        assertTrue(visual.isAccentStyle)
    }
}

