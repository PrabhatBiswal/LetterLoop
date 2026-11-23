// Hand-written fakes for the two repository interfaces, used by the ViewModel tests instead of a
// mocking library. They keep everything in memory and record what the ViewModel asked for.
package com.prabhat.letterloop.fakes

import com.prabhat.letterloop.data.LevelProgress
import com.prabhat.letterloop.data.LevelRepository
import com.prabhat.letterloop.data.ProgressRepository
import com.prabhat.letterloop.game.Level
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLevelRepository(private val levels: List<Level>) : LevelRepository {
    override suspend fun levels(): List<Level> = levels
}

class FakeProgressRepository(highest: Int = 1) : ProgressRepository {
    private val saved = MutableStateFlow<Map<Int, LevelProgress>>(emptyMap())
    private val highestFlow = MutableStateFlow(highest)

    /** How many times saveProgress was called, across all levels. */
    var saveCount = 0
        private set
    val clearedLevels = mutableListOf<Int>()

    var highest: Int
        get() = highestFlow.value
        set(value) { highestFlow.value = value }

    fun savedProgress(levelId: Int): LevelProgress? = saved.value[levelId]

    /** Seeds saved data without counting it as a save by the ViewModel. */
    fun seed(levelId: Int, progress: LevelProgress) = saved.update { it + (levelId to progress) }

    override val highestUnlocked: Flow<Int> = highestFlow

    override fun progress(levelId: Int): Flow<LevelProgress> = saved.map { it[levelId] ?: LevelProgress() }

    override suspend fun saveProgress(levelId: Int, progress: LevelProgress) {
        saveCount++
        saved.update { it + (levelId to progress) }
    }

    override suspend fun clearProgress(levelId: Int) {
        clearedLevels += levelId
        saved.update { it - levelId }
    }

    // Same contract as the real repository: never goes down.
    override suspend fun unlockUpTo(levelId: Int) = highestFlow.update { maxOf(it, levelId) }
}
