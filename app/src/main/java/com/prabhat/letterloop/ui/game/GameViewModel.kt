// Holds the game screen's state and runs submitted words through the GameEngine.
// It never holds a Context, Activity, NavController or composable lambda, so it cannot leak them.
package com.prabhat.letterloop.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prabhat.letterloop.LetterLoopApp
import com.prabhat.letterloop.data.LevelRepository
import com.prabhat.letterloop.game.GameEngine
import com.prabhat.letterloop.game.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val levelRepository: LevelRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val levelId: Int = savedStateHandle[KEY_LEVEL_ID] ?: 1

    private val _uiState = MutableStateFlow(GameUiState(levelId = levelId))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var lastFeedbackId = 0L

    init {
        viewModelScope.launch {
            val level = levelRepository.levels().first { it.id == levelId }
            _uiState.update { it.copy(game = GameState(level), letters = level.letters) }
        }
    }

    fun onWordSubmitted(word: String) {
        val current = _uiState.value
        val game = current.game ?: return
        if (current.phase != GamePhase.Playing) return
        val (next, result) = GameEngine.submit(game, word)
        _uiState.value = current.copy(
            game = next,
            feedback = Feedback(++lastFeedbackId, result, word),
            phase = if (next.isComplete) GamePhase.Celebrating else GamePhase.Playing,
        )
    }

    /** Clears the feedback the screen has finished playing; an older id never clears a newer one. */
    fun onFeedbackShown(id: Long) {
        _uiState.update { if (it.feedback?.id == id) it.copy(feedback = null) else it }
    }

    companion object {
        const val KEY_LEVEL_ID = "currentLevelId"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LetterLoopApp
                GameViewModel(app.container.levelRepository, createSavedStateHandle())
            }
        }
    }
}
