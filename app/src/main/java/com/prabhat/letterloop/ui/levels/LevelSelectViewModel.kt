// Holds Level Select's state: every level with its solved / current / locked status.
// It collects the saved-progress flow (rather than reading once), so returning from a game always
// shows the newest unlocks. Home uses it too, for its "Next up" line and progress bar.
package com.prabhat.letterloop.ui.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prabhat.letterloop.LetterLoopApp
import com.prabhat.letterloop.data.LevelRepository
import com.prabhat.letterloop.data.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

enum class LevelStatus { Completed, Current, Locked }

data class LevelTile(val id: Int, val letterCount: Int, val status: LevelStatus)

data class LevelSelectUiState(
    /** Empty while the levels are loading. */
    val levels: List<LevelTile> = emptyList(),
) {
    val solvedCount: Int get() = levels.count { it.status == LevelStatus.Completed }
    /** The level to play next, or null when every level is solved. */
    val nextLevel: Int? get() = levels.firstOrNull { it.status == LevelStatus.Current }?.id
}

class LevelSelectViewModel(
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    // Eagerly: the state is ready by the first frame, and it keeps following DataStore for as
    // long as the screen's back-stack entry exists, including while a game is on top of it.
    val uiState: StateFlow<LevelSelectUiState> =
        flow { emit(levelRepository.levels()) }
            .combine(progressRepository.highestUnlocked) { levels, highest ->
                LevelSelectUiState(
                    levels.map { level ->
                        // A level counts as completed when highest_unlocked > id (section 6).
                        val status = when {
                            level.id < highest -> LevelStatus.Completed
                            level.id == highest -> LevelStatus.Current
                            else -> LevelStatus.Locked
                        }
                        LevelTile(level.id, level.letters.length, status)
                    },
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LevelSelectUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as LetterLoopApp).container
                LevelSelectViewModel(container.levelRepository, container.progressRepository)
            }
        }
    }
}
