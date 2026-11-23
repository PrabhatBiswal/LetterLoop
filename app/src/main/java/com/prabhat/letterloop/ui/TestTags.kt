// Every test tag in the app, in one place, so device tests and screens cannot drift apart.
package com.prabhat.letterloop.ui

object TestTags {
    const val WHEEL = "wheel"
    const val WORD_PREVIEW = "word_preview"
    const val FOUND_COUNTER = "found_counter"
    const val LEVEL_TITLE = "level_title"

    fun wheelLetter(index: Int) = "wheel_letter_$index"
    fun gridCell(row: Int, col: Int) = "cell_${row}_$col"
}
