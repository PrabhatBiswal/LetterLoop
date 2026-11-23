// Runs the validator and the engine over the real assets/levels.json, so a mistake in a
// hand-written level fails the build. A failing level is printed as an ASCII grid.
package com.prabhat.letterloop.game

import com.prabhat.letterloop.data.parseLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LevelsValidationTest {

    // Gradle runs JVM unit tests with the module directory (app/) as the working directory.
    private val levels = parseLevels(File("src/main/assets/levels.json").readText())

    private fun failureMessage(level: Level, errors: List<String>): String =
        "Level ${level.id} (${level.letters}) is broken:\n" +
            errors.joinToString("\n") + "\n" +
            LevelValidator.asciiGrid(level)

    @Test
    fun fileHasExactly15Levels_andEveryOnePassesTheValidator() {
        assertEquals(15, levels.size)
        assertEquals(emptyList<String>(), LevelValidator.validateIds(levels))
        for (level in levels) {
            val errors = LevelValidator.validate(level)
            assertTrue(failureMessage(level, errors), errors.isEmpty())
        }
    }

    @Test
    fun everyLevel_isCompletedBySubmittingAllItsGridWords() {
        for (level in levels) {
            val end = level.words.fold(GameState(level)) { state, placed ->
                val (next, result) = GameEngine.submit(state, placed.word)
                // AlreadyFound is fine: crossing words may have revealed this word already.
                assertTrue(
                    "Level ${level.id}: ${placed.word} gave $result\n${LevelValidator.asciiGrid(level)}",
                    result == SubmitResult.GridWord || result == SubmitResult.AlreadyFound,
                )
                next
            }
            assertTrue(failureMessage(level, listOf("not complete after all grid words")), end.isComplete)
        }
    }

    @Test
    fun everyBonusWord_returnsBonusWord() {
        for (level in levels) {
            for (word in level.bonus) {
                assertEquals("Level ${level.id}: $word", SubmitResult.BonusWord, GameEngine.submit(GameState(level), word).second)
            }
        }
    }

    @Test
    fun letterCounts_followTheRamp() {
        for (level in levels) {
            val expected = when (level.id) {
                in 1..3 -> 3
                in 4..8 -> 4
                else -> 5
            }
            assertEquals("Level ${level.id} letters ${level.letters}", expected, level.letters.length)
        }
    }

    @Test
    fun failingLevel_printsItsGridAsAscii() {
        // Level 1 with an invalid wheel, run through the same message the tests above use.
        val broken = levels.first().copy(letters = "CA")
        val message = failureMessage(broken, LevelValidator.validate(broken))

        assertTrue(message, message.contains("C A T\n. C .\n. T ."))
        assertTrue(message, message.contains("Rule 1:"))
    }
}
