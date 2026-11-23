// Every test tag in the app, in one place, so device tests and screens cannot drift apart.
package com.prabhat.letterloop.ui

object TestTags {
    const val HOME_SCREEN = "home_screen"
    const val PLAY_BUTTON = "play_button"

    const val LEVEL_SELECT_SCREEN = "level_select_screen"
    const val LEVELS_BACK_BUTTON = "levels_back_button"
    fun levelTile(id: Int) = "level_tile_$id"

    const val GAME_SCREEN = "game_screen"
    const val LEVEL_TITLE = "level_title"
    const val FOUND_COUNTER = "found_counter"
    const val PAUSE_BUTTON = "pause_button"
    const val HINT_BUTTON = "hint_button"
    const val SHUFFLE_BUTTON = "shuffle_button"
    const val BONUS_BUTTON = "bonus_button"
    const val BONUS_COUNT = "bonus_count"
    const val WHEEL = "wheel"
    const val WORD_PREVIEW = "word_preview"
    fun wheelLetter(index: Int) = "wheel_letter_$index"
    fun gridCell(row: Int, col: Int) = "cell_${row}_$col"

    const val LEVEL_COMPLETE_CARD = "level_complete_card"
    const val BACK_TO_LEVELS_BUTTON = "back_to_levels_button"

    const val PAUSE_DIALOG = "pause_dialog"
    const val RESUME_BUTTON = "resume_button"
    const val PAUSE_LEVELS_BUTTON = "pause_levels_button"
    const val PAUSE_HOME_BUTTON = "pause_home_button"
}
