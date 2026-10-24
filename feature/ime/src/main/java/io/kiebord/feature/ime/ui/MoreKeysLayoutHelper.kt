package io.kiebord.feature.ime.ui

data class MoreKeysDimensions(
    val itemWidthPx: Float,
    val itemHeightPx: Float,
    val colGapPx: Float,
    val rowGapPx: Float,
    val horizontalPaddingPx: Float,
    val verticalPaddingPx: Float,
    val fontSizeSp: Float = 19f,
    val iconSizeDp: Float = 22f
) {
    fun totalWidthPx(numColumns: Int): Float =
        numColumns * itemWidthPx + (numColumns - 1).coerceAtLeast(0) * colGapPx + horizontalPaddingPx * 2

    fun totalHeightPx(numRows: Int): Float =
        numRows * itemHeightPx + (numRows - 1).coerceAtLeast(0) * rowGapPx + verticalPaddingPx * 2
}

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
    fun calculateDimensions(
        density: Float,
        scaleFactor: Float,
        keyboardWidthPx: Float
    ): MoreKeysDimensions {
        val baseItemSizePx = 42f * scaleFactor * density
        val colGapPx = 3f * scaleFactor * density
        val rowGapPx = 4f * scaleFactor * density
        val horizPaddingPx = 6f * scaleFactor * density
        val vertPaddingPx = 5f * scaleFactor * density

        val maxAllowedWidthPx = (keyboardWidthPx - 16f * density).coerceAtLeast(100f)

        // For default keyboard width, itemWidthPx is the full 42dp.
        // Only scale down if the available keyboard width is extremely small (e.g. narrow one-handed mode or extreme margin).
        val minCols = 4
        val neededForMinCols = minCols * baseItemSizePx + (minCols - 1) * colGapPx + horizPaddingPx * 2
        val itemWidthPx = if (maxAllowedWidthPx < neededForMinCols) {
            ((maxAllowedWidthPx - horizPaddingPx * 2 - (minCols - 1) * colGapPx) / minCols).coerceAtLeast(26f * density)
        } else {
            baseItemSizePx
        }
        val itemHeightPx = itemWidthPx

        val sizeRatio = (itemWidthPx / baseItemSizePx).coerceIn(0.6f, 1.0f)
        val fontSizeSp = 19f * scaleFactor * sizeRatio
        val iconSizeDp = (22f * scaleFactor * sizeRatio).coerceAtLeast(14f)

        return MoreKeysDimensions(
            itemWidthPx = itemWidthPx,
            itemHeightPx = itemHeightPx,
            colGapPx = colGapPx,
            rowGapPx = rowGapPx,
            horizontalPaddingPx = horizPaddingPx,
            verticalPaddingPx = vertPaddingPx,
            fontSizeSp = fontSizeSp,
            iconSizeDp = iconSizeDp
        )
    }

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

        val colWidth = if (columnWidthPx > 0f) columnWidthPx else 100f
        // Limit maximum columns to what physically fits in keyboard width
        val maxFitCols = ((keyboardWidthPx - 20f) / colWidth).toInt().coerceAtLeast(3)
        val desiredCols = if (columnsHint != null && columnsHint > 0) {
            minOf(columnsHint, maxFitCols)
        } else {
            minOf(5, maxFitCols)
        }
        val maxColumns = desiredCols.coerceAtLeast(3)
        val numRows = (itemCount + maxColumns - 1) / maxColumns

        val numColumns = if (columnsHint != null && columnsHint > 0) {
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

        val maxLeftKeys = (anchorCenterX / colWidth).toInt().coerceAtLeast(0)
        val maxRightKeys = (((keyboardWidthPx - anchorCenterX).coerceAtLeast(0f)) / colWidth).toInt().coerceAtLeast(0)

        var leftKeys = numLeftKeys.coerceIn(0, numColumns - 1)
        var rightKeys = numColumns - leftKeys

        if (leftKeys > maxLeftKeys) {
            leftKeys = maxLeftKeys.coerceIn(0, numColumns - 1)
            rightKeys = numColumns - leftKeys
        } else if (rightKeys > maxRightKeys + 1) {
            rightKeys = (maxRightKeys + 1).coerceIn(1, numColumns)
            leftKeys = numColumns - rightKeys
        }

        if (maxLeftKeys == leftKeys && leftKeys > 0) {
            leftKeys--
            rightKeys++
        }
        if (maxRightKeys == rightKeys - 1 && rightKeys > 1) {
            leftKeys++
            rightKeys--
        }

        leftKeys = leftKeys.coerceIn(0, numColumns - 1)
        rightKeys = (numColumns - leftKeys).coerceIn(1, numColumns)

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
