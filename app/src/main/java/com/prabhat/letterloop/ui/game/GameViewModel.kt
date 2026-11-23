// Holds the game screen's state: runs words and hints through the GameEngine, saves progress after
// every change, and drives the level-complete flow. It never holds a Context, Activity,
// NavController or composable lambda, so it cannot leak them.
package com.prabhat.letterloop.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prabhat.letterloop.LetterLoopApp
import com.prabhat.letterloop.data.LevelProgress
import com.prabhat.letterloop.data.LevelRepository
import com.prabhat.letterloop.data.ProgressRepository
import com.prabhat.letterloop.game.GameEngine
import com.prabhat.letterloop.game.GameState
import com.prabhat.letterloop.game.Level
import com.prabhat.letterloop.game.SubmitResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GameViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val savedStateHandle: SavedStateHandle,
    private val random: Random = Random.Default,
) : ViewModel() {

    // KEY_CURRENT_LEVEL is only written by this class. If it is already there when the ViewModel
    // is created, this is a rebuild after process death, not a fresh visit from Level Select.
    private val restoring = savedStateHandle.contains(KEY_CURRENT_LEVEL)

    private val _uiState = MutableStateFlow(
        GameUiState(levelId = savedStateHandle[KEY_CURRENT_LEVEL] ?: savedStateHandle[KEY_ROUTE_LEVEL_ID] ?: 1),
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var levels: List<Level> = emptyList()
    private var lastFeedbackId = 0L

    // The celebration countdown only runs while the game screen is started (see onScreenStopped).
    private var screenStarted = true
    private var celebrationTimer: Job? = null

    init {
        viewModelScope.launch {
            levels = levelRepository.levels()
            // An id that is not in the file falls back to level 1 instead of crashing (Decision log).
            val level = levels.firstOrNull { it.id == _uiState.value.levelId } ?: levels.first()
            openLevel(level, restoring)
        }
    }

    fun onWordSubmitted(word: String) {
        val current = _uiState.value
        val game = current.game ?: return
        if (current.phase != GamePhase.Playing) return
        val (next, result) = GameEngine.submit(game, word)
        // State first, then saving and animation follow; nothing here waits for either.
        _uiState.value = current.copy(game = next, feedback = Feedback(++lastFeedbackId, result, word))
        if (result == SubmitResult.GridWord || result == SubmitResult.BonusWord) save(next)
        if (next.isComplete) onLevelComplete(next.level)
    }

    fun onHint() {
        val current = _uiState.value
        val game = current.game ?: return
        if (current.phase != GamePhase.Playing) return
        val next = GameEngine.hint(game)
        if (next === game) return // nothing left to reveal
        _uiState.value = current.copy(game = next)
        save(next)
        if (next.isComplete) onLevelComplete(next.level)
    }

    fun onShuffle() {
        val current = _uiState.value
        if (current.game == null || current.phase != GamePhase.Playing) return
        val letters = current.letters
        // Draw again if the shuffle gives back the same order, or the button would look broken.
        // Every level has at least two different letters, so the loop ends.
        var shuffled: String
        do {
            shuffled = letters.toList().shuffled(random).joinToString("")
        } while (shuffled == letters && letters.toSet().size > 1)
        // The wheel order lives in SavedStateHandle, so it survives rotation and process death.
        savedStateHandle[KEY_WHEEL_LETTERS] = shuffled
        _uiState.value = current.copy(letters = shuffled)
    }

    /** Clears the feedback the screen has finished playing; an older id never clears a newer one. */
    fun onFeedbackShown(id: Long) {
        _uiState.update { if (it.feedback?.id == id) it.copy(feedback = null) else it }
    }

    fun onScreenStarted() {
        screenStarted = true
        if (_uiState.value.phase == GamePhase.Celebrating && celebrationTimer == null) startCelebrationTimer()
    }

    /**
     * The countdown stops while the screen is not visible. Android saves the screen's state when
     * the app goes to the background; if we advanced to the next level behind the player's back,
     * that saved state (level N, complete) would be out of date, and a process kill would bring
     * back level N with its progress already cleared. Paused, the saved state stays true, and on
     * return the countdown simply runs again (Decision log).
     */
    fun onScreenStopped() {
        screenStarted = false
        celebrationTimer?.cancel()
        celebrationTimer = null
    }

    private suspend fun openLevel(level: Level, restoring: Boolean) {
        var game = restore(level, progressRepository.progress(level.id).first())
        if (game.isComplete && !restoring) {
            // Complete progress on a fresh visit is left over from finishing this level earlier
            // (the last level keeps it so the all-done card survives process death). Replaying
            // a finished level starts fresh.
            progressRepository.clearProgress(level.id)
            game = GameState(level)
        }
        // A shuffled order is kept only on a rebuild, and only if it is still this level's letters.
        val savedLetters: String? = savedStateHandle[KEY_WHEEL_LETTERS]
        val letters = savedLetters
            ?.takeIf { restoring && it.toList().sorted() == level.letters.toList().sorted() }
            ?: level.letters
        savedStateHandle[KEY_CURRENT_LEVEL] = level.id
        savedStateHandle[KEY_WHEEL_LETTERS] = letters
        _uiState.value = GameUiState(levelId = level.id, game = game, letters = letters)
        // Rebuilt after process death during the celebration: pick the completion flow back up.
        if (game.isComplete) onLevelComplete(level)
    }

    private fun onLevelComplete(level: Level) {
        val isLast = levels.indexOf(level) == levels.lastIndex
        _uiState.update { it.copy(phase = if (isLast) GamePhase.AllLevelsDone else GamePhase.Celebrating) }
        // Unlock the next level straight away, so a kill during the celebration cannot lose it.
        viewModelScope.launch { withContext(NonCancellable) { progressRepository.unlockUpTo(level.id + 1) } }
        if (!isLast && screenStarted) startCelebrationTimer()
    }

    private fun startCelebrationTimer() {
        celebrationTimer = viewModelScope.launch {
            delay(CELEBRATION_MILLIS)
            // Past this point the move to the next level must finish, even if the screen stops.
            celebrationTimer = null
            val finished = _uiState.value.game?.level ?: return@launch
            val next = levels[levels.indexOf(finished) + 1]
            progressRepository.clearProgress(finished.id)
            // Next level swaps in place: no navigation, so the back stack never grows.
            openLevel(next, restoring = false)
        }
    }

    // Saved data is never trusted blindly: anything that does not belong to this level means the
    // save is stale or corrupt, so the level starts fresh instead of showing nonsense or crashing.
    private fun restore(level: Level, saved: LevelProgress): GameState {
        val gridWords = level.words.map { it.word }
        val valid = gridWords.containsAll(saved.foundWords) &&
            level.bonus.containsAll(saved.foundBonus) &&
            level.letterAt.keys.containsAll(saved.hintedCells)
        if (!valid) return GameState(level)
        return GameState(level, saved.foundWords.toSet(), saved.foundBonus.toSet(), saved.hintedCells.toSet())
    }

    // Written straight away after every accepted word or hint, not in onStop, so backgrounding or a
    // kill loses nothing. NonCancellable lets a started write finish even if the screen closes.
    private fun save(game: GameState) {
        viewModelScope.launch {
            withContext(NonCancellable) { progressRepository.saveProgress(game.level.id, LevelProgress.from(game)) }
        }
    }

    companion object {
        const val KEY_CURRENT_LEVEL = "currentLevelId"
        const val KEY_WHEEL_LETTERS = "wheelLetters"
        /** The Game route's property name; Navigation stores route arguments under it. */
        const val KEY_ROUTE_LEVEL_ID = "levelId"
        const val CELEBRATION_MILLIS = 1_500L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as LetterLoopApp).container
                GameViewModel(container.levelRepository, container.progressRepository, createSavedStateHandle())
            }
        }
    }
}
