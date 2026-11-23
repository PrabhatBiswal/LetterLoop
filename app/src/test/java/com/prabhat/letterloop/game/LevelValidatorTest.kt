// Tests the validator itself: one correct level passes, and one level broken on purpose for each
// of the eight rules is rejected with that rule named. A validator that accepts everything fails here.
package com.prabhat.letterloop.game

import com.prabhat.letterloop.game.Direction.ACROSS
import com.prabhat.letterloop.game.Direction.DOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelValidatorTest {

    //   S T O P
    //   . . . O
    //   . . . T
    //   T O P S
    private val valid = Level(
        id = 1,
        letters = "STOP",
        words = listOf(
            PlacedWord("STOP", 0, 0, ACROSS),
            PlacedWord("POTS", 0, 3, DOWN),
            PlacedWord("TOPS", 3, 0, ACROSS),
        ),
        bonus = listOf("SPOT", "POT"),
    )

    // Each broken level below breaks exactly one rule, so we can insist that every error names it.
    private fun assertRejectedOnlyBy(rule: Int, errors: List<String>) {
        assertTrue("expected a Rule $rule error, got none", errors.isNotEmpty())
        assertTrue("expected only Rule $rule errors, got $errors", errors.all { it.startsWith("Rule $rule:") })
    }

    @Test
    fun correctLevel_isAccepted() {
        assertEquals(emptyList<String>(), LevelValidator.validate(valid))
    }

    @Test
    fun rule1_sixWheelLetters_isRejected() {
        assertRejectedOnlyBy(1, LevelValidator.validate(valid.copy(letters = "STOPXY")))
    }

    @Test
    fun rule1_lowerCaseLetters_isRejected() {
        // Lower-case letters also make every word unspellable, so only check that Rule 1 is named.
        val errors = LevelValidator.validate(valid.copy(letters = "stop"))
        assertTrue(errors.toString(), errors.any { it.startsWith("Rule 1:") })
    }

    @Test
    fun rule2_wordThatCannotBeSpelled_isRejected() {
        assertRejectedOnlyBy(2, LevelValidator.validate(valid.copy(bonus = listOf("DOG"))))
    }

    @Test
    fun rule2_wordShorterThanThree_isRejected() {
        assertRejectedOnlyBy(2, LevelValidator.validate(valid.copy(bonus = listOf("TO"))))
    }

    @Test
    fun rule3_crossingWordsDisagreeOnALetter_isRejected() {
        // OPTS down from (0,3) puts O where STOP has P.
        val broken = valid.copy(
            words = listOf(
                PlacedWord("STOP", 0, 0, ACROSS),
                PlacedWord("OPTS", 0, 3, DOWN),
                PlacedWord("TOPS", 3, 0, ACROSS),
            ),
        )
        assertRejectedOnlyBy(3, LevelValidator.validate(broken))
    }

    @Test
    fun rule4_disconnectedWords_isRejected() {
        //   S T O P
        //   . . . .
        //   P O T S
        val broken = valid.copy(
            words = listOf(PlacedWord("STOP", 0, 0, ACROSS), PlacedWord("POTS", 2, 0, ACROSS)),
        )
        assertRejectedOnlyBy(4, LevelValidator.validate(broken))
    }

    @Test
    fun rule5_accidentalSideBySideWord_isRejected() {
        // OPT down from (0,2) sits next to POTS, making "PO" and "TT" across and "OPTP" down.
        //   S T O P
        //   . . P O
        //   . . T T
        //   T O P S
        val broken = valid.copy(words = valid.words + PlacedWord("OPT", 0, 2, DOWN))
        assertRejectedOnlyBy(5, LevelValidator.validate(broken))
    }

    @Test
    fun rule6_wordInBothGridAndBonus_isRejected() {
        assertRejectedOnlyBy(6, LevelValidator.validate(valid.copy(bonus = listOf("STOP"))))
    }

    @Test
    fun rule6_sameBonusWordTwice_isRejected() {
        assertRejectedOnlyBy(6, LevelValidator.validate(valid.copy(bonus = listOf("POT", "POT"))))
    }

    @Test
    fun rule7_gridWiderThanSeven_isRejected() {
        // A staircase of five connected words that ends 9 columns wide.
        val broken = valid.copy(
            bonus = emptyList(),
            words = listOf(
                PlacedWord("STOP", 0, 0, ACROSS),
                PlacedWord("POTS", 0, 3, DOWN),
                PlacedWord("SPOT", 3, 3, ACROSS),
                PlacedWord("TOPS", 3, 6, DOWN),
                PlacedWord("SOP", 6, 6, ACROSS),
            ),
        )
        assertEquals(9, broken.cols)
        assertRejectedOnlyBy(7, LevelValidator.validate(broken))
    }

    @Test
    fun rule7_negativeCoordinates_isRejected() {
        val broken = valid.copy(words = listOf(PlacedWord("STOP", 0, -1, ACROSS)), bonus = emptyList())
        assertRejectedOnlyBy(7, LevelValidator.validate(broken))
    }

    @Test
    fun rule8_idsWithAGap_isRejected() {
        val errors = LevelValidator.validateIds(listOf(valid.copy(id = 1), valid.copy(id = 3)))
        assertRejectedOnlyBy(8, errors)
    }

    @Test
    fun rule8_idsOutOfOrder_isRejected() {
        val errors = LevelValidator.validateIds(listOf(valid.copy(id = 2), valid.copy(id = 1)))
        assertRejectedOnlyBy(8, errors)
    }

    @Test
    fun rule8_idsOneToN_areAccepted() {
        assertEquals(emptyList<String>(), LevelValidator.validateIds(listOf(valid, valid.copy(id = 2))))
    }

    @Test
    fun wordNeedingALetterTwice_rejectedWithOneCopy_acceptedWithTwo() {
        //   L O O P
        //   . . . O
        //   . . . O
        //   . . . L
        val words = listOf(PlacedWord("LOOP", 0, 0, ACROSS), PlacedWord("POOL", 0, 3, DOWN))

        val oneO = Level(id = 1, letters = "LOPE", words = words)
        assertRejectedOnlyBy(2, LevelValidator.validate(oneO))

        val twoOs = Level(id = 1, letters = "LOOP", words = words)
        assertEquals(emptyList<String>(), LevelValidator.validate(twoOs))
    }
}
