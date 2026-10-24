package io.kiebord.feature.ime.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MoreKeysLayoutHelperTest {

    @Test
    fun testPeriodKeyLayoutMatchesBornoLayout() {
        val items = listOf(",", "?", "!", "#", ")", "(", "/", ";", "'", "@", ":", "-", "\"", "+", "%", "&")
        val result = MoreKeysLayoutHelper.computeLayout(
            itemCount = items.size,
            columnsHint = 8,
            anchorCenterX = 850f,
            keyboardWidthPx = 1080f,
            columnWidthPx = 108f
        )

        assertEquals(8, result.numColumns)
        assertEquals(2, result.numRows)
        assertEquals(6, result.leftKeys)
        assertEquals(2, result.rightKeys)
        assertEquals(0, result.defaultItemIndex)

        // Verify default item is comma at index 0, positioned at bottom row, col 6 (directly above period)
        assertEquals(Pair(1, 6), result.itemPositions[0])
        assertEquals(0, result.grid[1][6])
        assertEquals(",", items[result.grid[1][6]!!])

        // Verify Row 0 (top row): & % + " - : ' @
        val expectedRow0 = listOf("&", "%", "+", "\"", "-", ":", "'", "@")
        val actualRow0 = result.grid[0].map { items[it!!] }
        assertEquals(expectedRow0, actualRow0)

        // Verify Row 1 (bottom row): ; / ( ) # ! , ?
        val expectedRow1 = listOf(";", "/", "(", ")", "#", "!", ",", "?")
        val actualRow1 = result.grid[1].map { items[it!!] }
        assertEquals(expectedRow1, actualRow1)
    }

    @Test
    fun testEKeyLayoutPlacesDefaultInCenter() {
        val items = listOf("3", "é", "è", "ê", "ë", "ē")
        val result = MoreKeysLayoutHelper.computeLayout(
            itemCount = items.size,
            columnsHint = null,
            anchorCenterX = 250f,
            keyboardWidthPx = 1080f,
            columnWidthPx = 108f
        )

        assertEquals(3, result.numColumns)
        assertEquals(2, result.numRows)

        // Default item is "3" (index 0) in the middle of bottom row
        assertEquals(Pair(1, 1), result.itemPositions[0])
        assertEquals("3", items[result.grid[1][1]!!])

        // Bottom row: è, 3, é
        assertEquals("è", items[result.grid[1][0]!!])
        assertEquals("3", items[result.grid[1][1]!!])
        assertEquals("é", items[result.grid[1][2]!!])
    }

    @Test
    fun testAKeyLayoutPlacesDefaultOnLeftEdge() {
        val items = listOf("@", "à", "á", "â", "ä", "æ", "ã", "å", "ā")
        val result = MoreKeysLayoutHelper.computeLayout(
            itemCount = items.size,
            columnsHint = 5,
            anchorCenterX = 60f,
            keyboardWidthPx = 1080f,
            columnWidthPx = 108f
        )

        assertEquals(5, result.numColumns)
        assertEquals(2, result.numRows)

        // Default item is "@" (index 0) at (1, 0)
        assertEquals(Pair(1, 0), result.itemPositions[0])
        assertEquals("@", items[result.grid[1][0]!!])
    }
}
