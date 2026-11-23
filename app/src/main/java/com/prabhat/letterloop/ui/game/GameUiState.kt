// Everything the game screen draws, as one immutable value (section 4). GameViewModel produces
// it; the screen renders it and sends user actions back through named functions.
package com.prabhat.letterloop.ui.game

import com.prabhat.letterloop.game.GameState
import com.prabhat.letterloop.game.SubmitResult

enum class GamePhase { Playing, Celebrating, AllLevelsDone }

/**
 * One-shot feedback for the last submitted word. The id goes up with every submission, so the
 * screen can tell a new event from one it already played, and the screen clears it once shown,
 * so a rotation does not replay it (section 9, animation rules).
 */
data class Feedback(val id: Long, val result: SubmitResult, val word: String)

data class GameUiState(
    val levelId: Int,
    /** Null until the level has loaded. */
    val game: GameState? = null,
    /** The wheel's letter order. Starts as the level's letters and changes with shuffle. */
    val letters: String = "",
    val phase: GamePhase = GamePhase.Playing,
    val feedback: Feedback? = null,
)
