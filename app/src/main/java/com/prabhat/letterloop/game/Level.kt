// The level data model: exactly what levels.json describes, plus the grid cells computed from it.
// @Serializable lets the data layer parse the JSON straight into these classes; kotlinx.serialization
// is plain Kotlin, so this package still has no Android or Compose imports.
package com.prabhat.letterloop.game

import kotlinx.serialization.Serializable

@Serializable
enum class Direction { ACROSS, DOWN }

/** One square of the crossword. Zero-based from the top-left. */
@Serializable
data class Cell(val row: Int, val col: Int)

/** A grid word: its text, the cell of its first letter, and which way it runs. */
@Serializable
data class PlacedWord(val word: String, val row: Int, val col: Int, val dir: Direction) {
    /** The cells this word covers, first letter first, so `cells[i]` holds `word[i]`. */
    val cells: List<Cell>
        get() = word.indices.map { i ->
            if (dir == Direction.ACROSS) Cell(row, col + i) else Cell(row + i, col)
        }
}

@Serializable
data class Level(
    val id: Int,
    val letters: String,
    val words: List<PlacedWord>,
    val bonus: List<String> = emptyList(),
) {
    // `by lazy` properties have no backing field, so kotlinx.serialization ignores them.

    /** The letter that belongs in every grid cell. The validator guarantees crossing words agree. */
    val letterAt: Map<Cell, Char> by lazy {
        words.flatMap { w -> w.cells.mapIndexed { i, cell -> cell to w.word[i] } }.toMap()
    }

    /** All grid cells in reading order: top row first, left to right. Hints follow this order. */
    val cells: List<Cell> by lazy {
        letterAt.keys.sortedWith(compareBy({ it.row }, { it.col }))
    }

    // Grid size is not stored in the JSON; it is the smallest box that holds every word.
    val rows: Int by lazy { (letterAt.keys.maxOfOrNull { it.row } ?: -1) + 1 }
    val cols: Int by lazy { (letterAt.keys.maxOfOrNull { it.col } ?: -1) + 1 }
}
