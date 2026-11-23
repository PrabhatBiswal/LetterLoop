// What is saved for one level: the words, bonus words and hinted cells found so far.
// Stored as JSON under the DataStore key progress_<levelId>.
package com.prabhat.letterloop.data

import com.prabhat.letterloop.game.Cell
import com.prabhat.letterloop.game.GameState
import kotlinx.serialization.Serializable

@Serializable
data class LevelProgress(
    val foundWords: List<String> = emptyList(),
    val foundBonus: List<String> = emptyList(),
    val hintedCells: List<Cell> = emptyList(),
) {
    companion object {
        // Lists keep the order things were found, so bonus chips show in that order.
        fun from(game: GameState) = LevelProgress(
            foundWords = game.foundWords.toList(),
            foundBonus = game.foundBonus.toList(),
            hintedCells = game.hintedCells.toList(),
        )
    }
}
