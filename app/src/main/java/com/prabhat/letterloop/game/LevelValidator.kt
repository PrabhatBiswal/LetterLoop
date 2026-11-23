// The eight rules every level must pass (development.md section 5).
// The app does not call this at runtime; LevelsValidationTest runs it over the real levels.json,
// so a hand-written level with a mistake fails the build instead of shipping unplayable.
package com.prabhat.letterloop.game

object LevelValidator {

    const val MAX_GRID_SIZE = 7

    /** Checks one level against rules 1–7. Each error message starts with "Rule N:". */
    fun validate(level: Level): List<String> {
        val errors = mutableListOf<String>()
        val gridWords = level.words.map { it.word }

        // Rule 1: 3, 4 or 5 upper-case A–Z wheel letters.
        if (level.letters.length !in 3..5 || level.letters.any { it !in 'A'..'Z' }) {
            errors += "Rule 1: letters \"${level.letters}\" must be 3 to 5 upper-case A-Z characters"
        }

        // Rule 2: every word is at least 3 letters and can be spelled from the wheel.
        for (word in gridWords + level.bonus) {
            if (word.length < 3) {
                errors += "Rule 2: \"$word\" is shorter than 3 letters"
            } else if (!canSpell(word, level.letters)) {
                errors += "Rule 2: \"$word\" cannot be spelled from the letters ${level.letters}"
            }
        }

        // Rule 3: where two words share a cell, they have the same letter there.
        // Built by hand rather than with level.letterAt, which would silently keep one of the letters.
        val grid = mutableMapOf<Cell, Char>()
        for (placed in level.words) {
            placed.cells.forEachIndexed { i, cell ->
                val letter = placed.word[i]
                val previous = grid.put(cell, letter)
                if (previous != null && previous != letter) {
                    errors += "Rule 3: \"${placed.word}\" puts $letter at (${cell.row},${cell.col}) " +
                        "where another word has $previous"
                }
            }
        }

        // Rule 4: all grid words form one connected crossword.
        if (level.words.isEmpty()) {
            errors += "Rule 4: the level has no grid words"
        } else {
            val reached = connectedWords(level.words)
            val unreached = level.words.indices.filter { it !in reached }.map { level.words[it].word }
            if (unreached.isNotEmpty()) {
                errors += "Rule 4: not connected to the rest of the crossword: $unreached"
            }
        }

        // Rule 5: every run of 2+ filled cells, across or down, is exactly one declared word.
        // A run is described by its start cell, direction and length; the two sets must match.
        val declared = level.words.map { Run(Cell(it.row, it.col), it.dir, it.word.length) }.toSet()
        val runs = findRuns(grid.keys)
        for (run in runs - declared) {
            errors += "Rule 5: \"${spell(run, grid)}\" runs ${run.dir} from " +
                "(${run.start.row},${run.start.col}) but is not a declared word"
        }
        for (run in declared - runs) {
            errors += "Rule 5: \"${spell(run, grid)}\" is not a complete run; " +
                "it touches other letters in line with it"
        }

        // Rule 6: no word twice; no word both in the grid and in the bonus list.
        val all = gridWords + level.bonus
        val duplicates = all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            errors += "Rule 6: these words appear more than once: $duplicates"
        }

        // Rule 7: the grid fits in 7x7. Coordinates start at 0, because the grid size is
        // computed as max + 1; a negative or unused leading row would break that.
        if (grid.isNotEmpty()) {
            if (grid.keys.any { it.row < 0 || it.col < 0 }) {
                errors += "Rule 7: a word starts at a negative row or column"
            } else if (grid.keys.minOf { it.row } != 0 || grid.keys.minOf { it.col } != 0) {
                errors += "Rule 7: the grid must start at row 0 and column 0"
            }
            if (level.rows > MAX_GRID_SIZE || level.cols > MAX_GRID_SIZE) {
                errors += "Rule 7: grid is ${level.cols} columns x ${level.rows} rows; " +
                    "the limit is $MAX_GRID_SIZE x $MAX_GRID_SIZE"
            }
        }

        return errors
    }

    /** Rule 8: ids are 1, 2, 3, ... in file order with no gaps. */
    fun validateIds(levels: List<Level>): List<String> {
        val ids = levels.map { it.id }
        val expected = (1..levels.size).toList()
        return if (ids == expected) emptyList() else listOf("Rule 8: ids are $ids, expected $expected")
    }

    /** True if [word] uses each wheel letter at most as many times as it appears in [letters]. */
    fun canSpell(word: String, letters: String): Boolean =
        word.groupingBy { it }.eachCount().all { (letter, needed) -> letters.count { it == letter } >= needed }

    /** The level drawn as text, '.' for an empty cell, for readable test failure messages. */
    fun asciiGrid(level: Level): String {
        val grid = level.letterAt
        val rows = (0 until level.rows).map { row ->
            (0 until level.cols).joinToString(" ") { col -> (grid[Cell(row, col)] ?: '.').toString() }
        }
        return rows.joinToString("\n")
    }

    private data class Run(val start: Cell, val dir: Direction, val length: Int)

    // Breadth-first search over words: two words are neighbours when they share a cell.
    private fun connectedWords(words: List<PlacedWord>): Set<Int> {
        val cellSets = words.map { it.cells.toSet() }
        val reached = mutableSetOf(0)
        val queue = ArrayDeque(listOf(0))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (other in words.indices) {
                if (other !in reached && cellSets[current].any { it in cellSets[other] }) {
                    reached += other
                    queue += other
                }
            }
        }
        return reached
    }

    // A run starts at a filled cell whose previous neighbour (left, or above) is empty.
    private fun findRuns(filled: Set<Cell>): Set<Run> {
        val runs = mutableSetOf<Run>()
        for (cell in filled) {
            for (dir in Direction.entries) {
                val step = { c: Cell -> if (dir == Direction.ACROSS) Cell(c.row, c.col + 1) else Cell(c.row + 1, c.col) }
                val before = if (dir == Direction.ACROSS) Cell(cell.row, cell.col - 1) else Cell(cell.row - 1, cell.col)
                if (before in filled) continue
                var length = 1
                var next = step(cell)
                while (next in filled) {
                    length++
                    next = step(next)
                }
                if (length >= 2) runs += Run(cell, dir, length)
            }
        }
        return runs
    }

    private fun spell(run: Run, grid: Map<Cell, Char>): String =
        (0 until run.length).map { i ->
            val cell = if (run.dir == Direction.ACROSS) Cell(run.start.row, run.start.col + i)
            else Cell(run.start.row + i, run.start.col)
            grid[cell] ?: '?'
        }.joinToString("")
}
