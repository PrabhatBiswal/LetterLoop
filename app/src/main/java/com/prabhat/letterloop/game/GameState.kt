// The state of one level in play, and the possible outcomes of submitting a word.
// GameState is immutable: the engine always returns a new one, which keeps the rules easy to test.
package com.prabhat.letterloop.game

data class GameState(
    val level: Level,
    val foundWords: Set<String> = emptySet(),
    val foundBonus: Set<String> = emptySet(),
    val hintedCells: Set<Cell> = emptySet(),
) {
    /** Cells shown because a grid word containing them has been found. */
    val wordCells: Set<Cell> by lazy {
        level.words.filter { it.word in foundWords }.flatMap { it.cells }.toSet()
    }

    /** A cell is revealed if it belongs to a found word or has been hinted. */
    val revealedCells: Set<Cell> by lazy { wordCells + hintedCells }

    /** The level is complete when every grid cell is revealed. */
    val isComplete: Boolean by lazy { revealedCells.containsAll(level.cells) }
}

enum class SubmitResult { GridWord, BonusWord, AlreadyFound, Invalid }
