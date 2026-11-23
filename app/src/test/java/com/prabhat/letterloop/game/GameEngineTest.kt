// Unit tests for GameEngine: every rule for submitting words and taking hints (test plan, section 14).
package com.prabhat.letterloop.game

import com.prabhat.letterloop.game.Direction.ACROSS
import com.prabhat.letterloop.game.Direction.DOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    // The sample level from the brief:
    //   S T O P
    //   . . . O
    //   . . . T
    //   T O P S
    private val stopLevel = Level(
        id = 4,
        letters = "STOP",
        words = listOf(
            PlacedWord("STOP", 0, 0, ACROSS),
            PlacedWord("POTS", 0, 3, DOWN),
            PlacedWord("TOPS", 3, 0, ACROSS),
        ),
        bonus = listOf("POST", "SPOT", "OPTS", "POT", "TOP", "OPT", "SOP"),
    )

    // A 3x3 word square: every across word is fully covered by the down words and vice versa.
    //   C A T
    //   O R E
    //   W E D
    private val squareLevel = Level(
        id = 99,
        letters = "CATOREWED",
        words = listOf(
            PlacedWord("CAT", 0, 0, ACROSS),
            PlacedWord("ORE", 1, 0, ACROSS),
            PlacedWord("WED", 2, 0, ACROSS),
            PlacedWord("COW", 0, 0, DOWN),
            PlacedWord("ARE", 0, 1, DOWN),
            PlacedWord("TED", 0, 2, DOWN),
        ),
    )

    private fun submitAll(state: GameState, vararg words: String): GameState =
        words.fold(state) { s, w -> GameEngine.submit(s, w).first }

    @Test
    fun gridWord_returnsGridWord_isFound_andRevealsExactlyItsCells() {
        val (state, result) = GameEngine.submit(GameState(stopLevel), "STOP")

        assertEquals(SubmitResult.GridWord, result)
        assertEquals(setOf("STOP"), state.foundWords)
        assertEquals(setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3)), state.revealedCells)
    }

    @Test
    fun bonusWord_returnsBonusWord_isFound_andRevealsNoCells() {
        val (state, result) = GameEngine.submit(GameState(stopLevel), "SPOT")

        assertEquals(SubmitResult.BonusWord, result)
        assertEquals(setOf("SPOT"), state.foundBonus)
        assertTrue(state.foundWords.isEmpty())
        assertTrue(state.revealedCells.isEmpty())
    }

    @Test
    fun sameGridWordTwice_returnsAlreadyFound_andChangesNothing() {
        val once = GameEngine.submit(GameState(stopLevel), "STOP").first
        val (twice, result) = GameEngine.submit(once, "STOP")

        assertEquals(SubmitResult.AlreadyFound, result)
        assertEquals(once, twice)
    }

    @Test
    fun sameBonusWordTwice_returnsAlreadyFound_andChangesNothing() {
        val once = GameEngine.submit(GameState(stopLevel), "POT").first
        val (twice, result) = GameEngine.submit(once, "POT")

        assertEquals(SubmitResult.AlreadyFound, result)
        assertEquals(once, twice)
    }

    @Test
    fun wordNotInLevel_returnsInvalid_andChangesNothing() {
        val start = GameState(stopLevel)
        val (state, result) = GameEngine.submit(start, "DOG")

        assertEquals(SubmitResult.Invalid, result)
        assertEquals(start, state)
    }

    @Test
    fun spellableButUnlistedWord_returnsInvalid() {
        // "OPS" uses only wheel letters but is neither a grid nor a bonus word.
        assertTrue(LevelValidator.canSpell("OPS", stopLevel.letters))
        val start = GameState(stopLevel)
        val (state, result) = GameEngine.submit(start, "OPS")

        assertEquals(SubmitResult.Invalid, result)
        assertEquals(start, state)
    }

    @Test
    fun emptyStringAndSingleLetter_returnInvalid_withoutCrashing() {
        val start = GameState(stopLevel)

        assertEquals(start to SubmitResult.Invalid, GameEngine.submit(start, ""))
        assertEquals(start to SubmitResult.Invalid, GameEngine.submit(start, "S"))
    }

    @Test
    fun twoCrossingWords_revealSharedCellOnce() {
        // STOP (4 cells) and POTS (4 cells) share the P at (0,3): 7 cells, not 8.
        val state = submitAll(GameState(stopLevel), "STOP", "POTS")

        assertEquals(7, state.revealedCells.size)
    }

    @Test
    fun wordRevealedByCrossingWords_countsAsFound_andThenReturnsAlreadyFound() {
        // The three down words cover every cell of the three across words.
        val state = submitAll(GameState(squareLevel), "COW", "ARE", "TED")

        assertTrue(state.foundWords.containsAll(listOf("CAT", "ORE", "WED")))
        assertEquals(SubmitResult.AlreadyFound, GameEngine.submit(state, "CAT").second)
    }

    @Test
    fun levelIncompleteWithOneWordMissing_completeWhenLastWordFound() {
        val almost = submitAll(GameState(stopLevel), "STOP", "POTS")
        assertFalse(almost.isComplete)

        val done = GameEngine.submit(almost, "TOPS").first
        assertTrue(done.isComplete)
    }

    @Test
    fun bonusWords_neverCompleteALevel() {
        val state = submitAll(GameState(stopLevel), *stopLevel.bonus.toTypedArray())

        assertEquals(stopLevel.bonus.toSet(), state.foundBonus)
        assertFalse(state.isComplete)
    }

    @Test
    fun hint_revealsFirstHiddenCellInReadingOrder_andRepeatedHintsContinueInOrder() {
        val one = GameEngine.hint(GameState(stopLevel))
        assertEquals(setOf(Cell(0, 0)), one.hintedCells)

        val two = GameEngine.hint(one)
        assertEquals(setOf(Cell(0, 0), Cell(0, 1)), two.hintedCells)

        // With STOP found, the first hidden cell in reading order is the O below its P.
        val afterStop = GameEngine.hint(submitAll(GameState(stopLevel), "STOP"))
        assertEquals(setOf(Cell(1, 3)), afterStop.hintedCells)
    }

    @Test
    fun wordFinishedByHints_countsAsFound() {
        // Four hints reveal row 0, which is all of STOP.
        val state = (1..4).fold(GameState(stopLevel)) { s, _ -> GameEngine.hint(s) }

        assertEquals(setOf("STOP"), state.foundWords)
        assertEquals(SubmitResult.AlreadyFound, GameEngine.submit(state, "STOP").second)
    }

    @Test
    fun hintsAlone_canCompleteALevel() {
        val state = stopLevel.cells.fold(GameState(stopLevel)) { s, _ -> GameEngine.hint(s) }

        assertTrue(state.isComplete)
        assertEquals(setOf("STOP", "POTS", "TOPS"), state.foundWords)
    }

    @Test
    fun hintOnCompleteLevel_changesNothing() {
        val done = submitAll(GameState(stopLevel), "STOP", "POTS", "TOPS")

        assertSame(done, GameEngine.hint(done))
    }

    @Test
    fun submitAndHint_returnNewState_andLeaveOriginalUntouched() {
        val original = GameState(stopLevel)
        val copy = original.copy()

        val afterSubmit = GameEngine.submit(original, "STOP").first
        val afterHint = GameEngine.hint(original)

        assertEquals(copy, original)
        assertTrue(original.foundWords.isEmpty() && original.hintedCells.isEmpty())
        assertEquals(setOf("STOP"), afterSubmit.foundWords)
        assertEquals(1, afterHint.hintedCells.size)
    }
}
