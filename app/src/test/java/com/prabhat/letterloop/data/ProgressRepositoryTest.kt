// Tests the real DataStoreProgressRepository against a DataStore file in a temporary folder, on
// the JVM. Raw values are written straight into the DataStore to simulate corrupt saved data.
package com.prabhat.letterloop.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.prabhat.letterloop.game.Cell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>

    // DataStore runs in the test's backgroundScope, which is cancelled when each test ends.
    private fun TestScope.repository(): DataStoreProgressRepository {
        dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "progress.preferences_pb")
        }
        return DataStoreProgressRepository(dataStore, levelCount = 15)
    }

    @Test
    fun nothingSaved_emptyProgress_andHighestUnlockedIs1() = runTest {
        val repo = repository()

        assertEquals(LevelProgress(), repo.progress(1).first())
        assertEquals(1, repo.highestUnlocked.first())
    }

    @Test
    fun saveThenRead_returnsTheSameWordsBonusAndHintedCells() = runTest {
        val repo = repository()
        val saved = LevelProgress(listOf("STOP", "POTS"), listOf("SPOT"), listOf(Cell(3, 0), Cell(3, 1)))

        repo.saveProgress(4, saved)

        assertEquals(saved, repo.progress(4).first())
    }

    @Test
    fun progressForOneLevel_doesNotAppearInAnother() = runTest {
        val repo = repository()

        repo.saveProgress(1, LevelProgress(foundWords = listOf("CAT")))

        assertEquals(LevelProgress(), repo.progress(2).first())
    }

    @Test
    fun clearingOneLevel_leavesTheOthersIntact() = runTest {
        val repo = repository()
        val two = LevelProgress(foundWords = listOf("NOW"))
        repo.saveProgress(1, LevelProgress(foundWords = listOf("CAT")))
        repo.saveProgress(2, two)

        repo.clearProgress(1)

        assertEquals(LevelProgress(), repo.progress(1).first())
        assertEquals(two, repo.progress(2).first())
    }

    @Test
    fun unparseableStoredValue_readsAsEmptyProgress_withoutException() = runTest {
        val repo = repository()
        dataStore.edit { it[stringPreferencesKey("progress_3")] = "{ this is not json" }
        dataStore.edit { it[stringPreferencesKey("progress_4")] = """{"foundWords": 42}""" }

        assertEquals(LevelProgress(), repo.progress(3).first())
        assertEquals(LevelProgress(), repo.progress(4).first())
    }

    @Test
    fun highestUnlocked_neverDecreases_andOutOfRangeValuesAreClamped() = runTest {
        val repo = repository()

        repo.unlockUpTo(5)
        assertEquals(5, repo.highestUnlocked.first())
        repo.unlockUpTo(3)
        assertEquals(5, repo.highestUnlocked.first())
        repo.unlockUpTo(99)
        assertEquals(16, repo.highestUnlocked.first())

        val key = intPreferencesKey("highest_unlocked")
        dataStore.edit { it[key] = 99 }
        assertEquals(16, repo.highestUnlocked.first())
        dataStore.edit { it[key] = 0 }
        assertEquals(1, repo.highestUnlocked.first())
        dataStore.edit { it[key] = -4 }
        assertEquals(1, repo.highestUnlocked.first())
    }

    @Test
    fun theFlowsEmit_whenAValueChanges() = runTest {
        val repo = repository()
        val highest = mutableListOf<Int>()
        val level2 = mutableListOf<LevelProgress>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.highestUnlocked.toList(highest) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.progress(2).toList(level2) }
        runCurrent()

        repo.unlockUpTo(3)
        repo.saveProgress(2, LevelProgress(foundWords = listOf("NOW")))
        runCurrent()

        assertEquals(listOf(1, 3), highest)
        assertEquals(listOf(LevelProgress(), LevelProgress(foundWords = listOf("NOW"))), level2)
    }
}
