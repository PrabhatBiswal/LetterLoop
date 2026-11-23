// Unit tests for the selection rule nextSelection() and for turning indices into a word.
package com.prabhat.letterloop.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class WheelSelectionTest {

    @Test
    fun hitOnUnselectedLetter_appendsIt() {
        assertEquals(listOf(0), nextSelection(emptyList(), 0))
        assertEquals(listOf(0, 3), nextSelection(listOf(0), 3))
    }

    @Test
    fun hitOnLastSelectedLetter_changesNothing() {
        // The finger is still resting on the letter it just selected.
        assertEquals(listOf(0, 1), nextSelection(listOf(0, 1), 1))
    }

    @Test
    fun hitOnSecondToLastLetter_removesTheLastOne() {
        assertEquals(listOf(0, 1), nextSelection(listOf(0, 1, 2), 1))
    }

    @Test
    fun hitOnAnyEarlierSelectedLetter_changesNothing() {
        val selected = listOf(0, 1, 2, 3)
        assertEquals(selected, nextSelection(selected, 0))
        assertEquals(selected, nextSelection(selected, 1))
    }

    @Test
    fun repeatedBacktracking_stopsAtOneLetter_neverEmpty() {
        var selected = listOf(0, 1, 2, 3)
        selected = nextSelection(selected, 2)
        assertEquals(listOf(0, 1, 2), selected)
        selected = nextSelection(selected, 1)
        assertEquals(listOf(0, 1), selected)
        selected = nextSelection(selected, 0)
        assertEquals(listOf(0), selected)
        // With one letter left there is no second-to-last letter to go back to.
        selected = nextSelection(selected, 0)
        assertEquals(listOf(0), selected)
    }

    @Test
    fun allLettersSelected_hitOnNonAdjacentSelectedLetter_changesNothing() {
        val all = listOf(0, 1, 2, 3, 4)
        for (letter in listOf(0, 1, 2)) {
            assertEquals("hit $letter", all, nextSelection(all, letter))
        }
    }

    @Test
    fun repeatedWheelLetter_bothCopiesSelectableIndependently() {
        // "LOOP": indices 1 and 2 are both O.
        var selected = nextSelection(emptyList(), 0)
        selected = nextSelection(selected, 1)
        selected = nextSelection(selected, 2)

        assertEquals(listOf(0, 1, 2), selected)
        assertEquals("LOO", wordFor(selected, "LOOP"))
    }

    @Test
    fun indicesConvertToTheRightWord_includingRepeatedLetters() {
        assertEquals("POOL", wordFor(listOf(3, 1, 2, 0), "LOOP"))
        assertEquals("POOL", wordFor(listOf(3, 2, 1, 0), "LOOP"))
        assertEquals("CA", wordFor(listOf(0, 1), "CAT"))
        assertEquals("", wordFor(emptyList(), "CAT"))
    }
}
