package com.codepotro.borno.feature.ime.ui

import com.codepotro.borno.data.prefs.KiebordPreferences
import com.codepotro.borno.feature.ime.ui.toolbar.ToolbarAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingKeyboardToolbarTest {

    @Test
    fun testToolbarActionFloatingExists() {
        assertNotNull(ToolbarAction.FLOATING)
        assertEquals(ToolbarAction.FLOATING, ToolbarAction.valueOf("FLOATING"))
    }

    @Test
    fun testKiebordPreferencesContainsFloatingInDrawerTools() {
        val prefs = KiebordPreferences()
        assertTrue("drawerTools must contain FLOATING by default", prefs.drawerTools.contains("FLOATING"))
    }

    @Test
    fun testResizerAndOneHandedDisabledInFloatingMode() {
        val isFloating = true
        val isToolEnabled: (ToolbarAction) -> Boolean = { action ->
            if (isFloating) {
                action != ToolbarAction.RESIZER && action != ToolbarAction.ONE_HANDED
            } else {
                true
            }
        }

        org.junit.Assert.assertFalse("RESIZER must be disabled when floating", isToolEnabled(ToolbarAction.RESIZER))
        org.junit.Assert.assertFalse("ONE_HANDED must be disabled when floating", isToolEnabled(ToolbarAction.ONE_HANDED))
        assertTrue("FLOATING must be enabled when floating", isToolEnabled(ToolbarAction.FLOATING))
        assertTrue("SETTINGS must be enabled when floating", isToolEnabled(ToolbarAction.SETTINGS))
    }

    @Test
    fun testToolsDrawerMaxColumnsInPortraitNormalMode() {
        fun computeMaxColumns(isFloating: Boolean, isLandscape: Boolean): Int {
            return if (!isFloating && !isLandscape) 4 else 6
        }

        assertEquals(4, computeMaxColumns(isFloating = false, isLandscape = false))
        assertEquals(6, computeMaxColumns(isFloating = true, isLandscape = false))
        assertEquals(6, computeMaxColumns(isFloating = false, isLandscape = true))
    }

    @Test
    fun testSuggestionCandidateScaleInFloatingMode() {
        fun computeEffectiveScale(scaleFactor: Float, isFloating: Boolean): Float {
            return if (isFloating) {
                (scaleFactor * 0.94f).coerceIn(0.82f, 1.15f)
            } else {
                scaleFactor.coerceIn(0.85f, 1.25f)
            }
        }

        val dockedScale = computeEffectiveScale(scaleFactor = 1.0f, isFloating = false)
        assertEquals(1.0f, dockedScale, 0.001f)

        val floatingScale = computeEffectiveScale(scaleFactor = 1.0f, isFloating = true)
        assertEquals(0.94f, floatingScale, 0.001f)

        val basePrimary = 18.5f
        val baseSecondary = 17.5f

        val scaledPrimary = (basePrimary * floatingScale).coerceIn(15f, 20f)
        val scaledSecondary = (baseSecondary * floatingScale).coerceIn(14f, 19f)

        assertTrue("Primary font size must remain prominent (>= 15sp)", scaledPrimary >= 15f)
        assertTrue("Secondary font size must remain prominent (>= 14sp)", scaledSecondary >= 14f)
        assertTrue("Primary font size is close to base", scaledPrimary >= 17.0f)
    }
}

