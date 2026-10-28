package com.mohitarora.dualclone

import com.mohitarora.dualclone.core.designsystem.components.paletteColumns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteColumnsTest {

    @Test
    fun smallSetsShareOneRow() {
        assertEquals(1, paletteColumns(1))
        assertEquals(2, paletteColumns(2))
        assertEquals(3, paletteColumns(3))
        assertEquals(4, paletteColumns(4))
        assertEquals(5, paletteColumns(5))
    }

    @Test
    fun largerSetsSplitEvenly() {
        assertEquals(3, paletteColumns(6))
        assertEquals(4, paletteColumns(7))
        assertEquals(4, paletteColumns(8))
        assertEquals(5, paletteColumns(9))
        assertEquals(5, paletteColumns(10))
    }

    /** No grid up to 20 cells ends on a lone cell. The palette holds at most about ten, so larger sets are out of scope. */
    @Test
    fun lastRowIsNeverAnOrphanForSetsOfFiveOrMore() {
        for (count in 5..20) {
            val columns = paletteColumns(count)
            val lastRow = count % columns
            assertTrue("$count cells in $columns columns leave $lastRow in the last row", lastRow == 0 || lastRow >= 2)
        }
    }

    @Test
    fun neverMoreThanTheMaximumColumns() {
        for (count in 1..60) assertTrue(paletteColumns(count) <= 5)
    }
}
