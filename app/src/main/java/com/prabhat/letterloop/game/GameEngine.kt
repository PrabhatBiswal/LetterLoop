// The game rules: what a submitted word does, and what a hint reveals.
// Pure functions from one GameState to the next, with no Android code, so every rule is unit tested.
package com.prabhat.letterloop.game

object GameEngine {

    fun submit(state: GameState, word: String): Pair<GameState, SubmitResult> {
        val w = word.uppercase()
        return when {
            // Checked first so a repeat never changes state, whichever list the word is in.
            w in state.foundWords || w in state.foundBonus -> state to SubmitResult.AlreadyFound
            state.level.words.any { it.word == w } ->
                withCompletedWords(state.copy(foundWords = state.foundWords + w)) to SubmitResult.GridWord
            w in state.level.bonus -> state.copy(foundBonus = state.foundBonus + w) to SubmitResult.BonusWord
            // Empty strings, single letters and unlisted words all land here.
            else -> state to SubmitResult.Invalid
        }
    }

    /** Reveals the first hidden cell in reading order. Deterministic, so it is testable. */
    fun hint(state: GameState): GameState {
        val cell = state.level.cells.firstOrNull { it !in state.revealedCells } ?: return state
        return withCompletedWords(state.copy(hintedCells = state.hintedCells + cell))
    }

    // A grid word whose cells are all revealed (by crossing words or hints) counts as found.
    // Adding it reveals no new cells, so one pass is enough; there is nothing to cascade.
    private fun withCompletedWords(state: GameState): GameState {
        val revealed = state.revealedCells
        val completed = state.level.words
            .filter { it.word !in state.foundWords && revealed.containsAll(it.cells) }
            .map { it.word }
        return if (completed.isEmpty()) state else state.copy(foundWords = state.foundWords + completed)
    }
}
