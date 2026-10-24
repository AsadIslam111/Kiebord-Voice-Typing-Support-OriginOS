package io.kiebord.feature.ime.ui

data class MoreKeysLayoutResult(
    val numColumns: Int,
    val numRows: Int,
    val leftKeys: Int,
    val rightKeys: Int,
    val grid: List<List<Int?>>, // visualRow -> visualCol -> originalItemIndex or null
    val itemPositions: Map<Int, Pair<Int, Int>>, // originalItemIndex -> (visualRow, visualCol)
    val defaultItemIndex: Int = 0
)

/**
 * Computes standard Android AOSP / Borno layout for popup morekeys.
 * - Primary / default key is item 0 (e.g. comma for period, default accent, etc.).
 * - Default key is placed on the bottom row (closest to finger), centered above anchor key.
 * - Surrounding keys radiate outwards left and right from the default key.
 * - Multi-row keys place secondary rows above the bottom row.
 */
object MoreKeysLayoutHelper {
    fun computeLayout(
        itemCount: Int,
        columnsHint: Int?,
        anchorCenterX: Float,
        keyboardWidthPx: Float,
        columnWidthPx: Float
    ): MoreKeysLayoutResult {
        if (itemCount <= 0) {
            return MoreKeysLayoutResult(
                numColumns = 1,
                numRows = 1,
                leftKeys = 0,
                rightKeys = 1,
                grid = listOf(listOf(null)),
                itemPositions = emptyMap()
            )
        }

        val isFixedColumn = columnsHint != null && columnsHint > 0
        val maxColumns = if (isFixedColumn) columnsHint!! else 5
        val numRows = (itemCount + maxColumns - 1) / maxColumns

        val numColumns = if (isFixedColumn) {
            minOf(itemCount, maxColumns)
        } else {
            var cols = minOf(itemCount, maxColumns)
            while (cols > 1) {
                val rem = itemCount % cols
                val emptySlots = if (rem == 0) 0 else cols - rem
                if (emptySlots >= numRows) {
                    cols--
                } else {
                    break
                }
            }
            cols
        }

        val topKeys = if (itemCount % numColumns == 0) numColumns else itemCount % numColumns
        val numLeftKeys = (numColumns - 1) / 2
        val numRightKeys = numColumns - numLeftKeys

        val colWidth = if (columnWidthPx > 0f) columnWidthPx else 108f
        val maxLeftKeys = (anchorCenterX / colWidth).toInt()
        val maxRightKeys = (((keyboardWidthPx - anchorCenterX).coerceAtLeast(0f)) / colWidth).toInt()

        var leftKeys: Int
        var rightKeys: Int

        if (numLeftKeys > maxLeftKeys) {
            leftKeys = maxLeftKeys
            rightKeys = numColumns - leftKeys
        } else if (numRightKeys > maxRightKeys + 1) {
            rightKeys = maxRightKeys + 1
            leftKeys = numColumns - rightKeys
        } else {
            leftKeys = numLeftKeys
            rightKeys = numRightKeys
        }

        if (maxLeftKeys == leftKeys && leftKeys > 0) {
            leftKeys--
            rightKeys++
        }
        if (maxRightKeys == rightKeys - 1 && rightKeys > 1) {
            leftKeys++
            rightKeys--
        }

        val topRowAdjustment = if (numRows == 1 || topKeys == 1 || numColumns % 2 == topKeys % 2 || leftKeys == 0 || rightKeys == 1) {
            0
        } else {
            -1
        }

        fun isTopRow(row: Int): Boolean = numRows > 1 && row == numRows - 1

        fun getAutoColPos(n: Int): Int {
            val col = n % numColumns
            val row = n / numColumns
            var lKeys = leftKeys
            if (isTopRow(row)) {
                lKeys += topRowAdjustment
            }
            if (col == 0) return 0

            var pos = 0
            var right = 1
            var left = 0
            var i = 0
            while (true) {
                if (right < rightKeys) {
                    pos = right
                    right++
                    i++
                }
                if (i >= col) break

                if (left < lKeys) {
                    left++
                    pos = -left
                    i++
                }
                if (i >= col) break
            }
            return pos
        }

        val grid = MutableList(numRows) { MutableList<Int?>(numColumns) { null } }
        val itemPositions = mutableMapOf<Int, Pair<Int, Int>>()

        for (n in 0 until itemCount) {
            val row = n / numColumns // 0 is bottom row, numRows - 1 is top row
            val visualRow = numRows - 1 - row // visualRow 0 is top row, numRows - 1 is bottom row
            val colPos = getAutoColPos(n)
            val visualCol = leftKeys + colPos

            if (visualRow in 0 until numRows && visualCol in 0 until numColumns) {
                grid[visualRow][visualCol] = n
                itemPositions[n] = Pair(visualRow, visualCol)
            }
        }

        return MoreKeysLayoutResult(
            numColumns = numColumns,
            numRows = numRows,
            leftKeys = leftKeys,
            rightKeys = rightKeys,
            grid = grid,
            itemPositions = itemPositions,
            defaultItemIndex = 0
        )
    }
}
