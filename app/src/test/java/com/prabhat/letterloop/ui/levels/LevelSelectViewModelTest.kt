// Unit tests for LevelSelectViewModel: tile status from highest_unlocked, and live updates.
package com.prabhat.letterloop.ui.levels

import com.prabhat.letterloop.data.parseLevels
import com.prabhat.letterloop.fakes.FakeLevelRepository
import com.prabhat.letterloop.fakes.FakeProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LevelSelectViewModelTest {

    private val levels = parseLevels(File("src/main/assets/levels.json").readText())

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun statuses(vm: LevelSelectViewModel) = vm.uiState.value.levels.associate { it.id to it.status }

    @Test
    fun freshInstall_level1Unlocked_levels2To15Locked() = runTest {
        val vm = LevelSelectViewModel(FakeLevelRepository(levels), FakeProgressRepository(highest = 1))

        val s = statuses(vm)
        assertEquals(15, s.size)
        assertEquals(LevelStatus.Current, s[1])
        (2..15).forEach { assertEquals("level $it", LevelStatus.Locked, s[it]) }
    }

    @Test
    fun highestUnlocked5_levels1To4Completed_5Current_6To15Locked() = runTest {
        val vm = LevelSelectViewModel(FakeLevelRepository(levels), FakeProgressRepository(highest = 5))

        val s = statuses(vm)
        (1..4).forEach { assertEquals("level $it", LevelStatus.Completed, s[it]) }
        assertEquals(LevelStatus.Current, s[5])
        (6..15).forEach { assertEquals("level $it", LevelStatus.Locked, s[it]) }
        assertEquals(4, vm.uiState.value.solvedCount)
        assertEquals(5, vm.uiState.value.nextLevel)
    }

    @Test
    fun highestUnlocked16_allFifteenCompleted() = runTest {
        val vm = LevelSelectViewModel(FakeLevelRepository(levels), FakeProgressRepository(highest = 16))

        assertEquals(List(15) { LevelStatus.Completed }, vm.uiState.value.levels.map { it.status })
        assertEquals(15, vm.uiState.value.solvedCount)
        assertNull(vm.uiState.value.nextLevel)
    }

    @Test
    fun whenTheProgressFlowEmits_stateUpdatesWithoutANewViewModel() = runTest {
        val progress = FakeProgressRepository(highest = 1)
        val vm = LevelSelectViewModel(FakeLevelRepository(levels), progress)
        assertEquals(LevelStatus.Locked, statuses(vm)[3])

        progress.highest = 3

        assertEquals(LevelStatus.Completed, statuses(vm)[2])
        assertEquals(LevelStatus.Current, statuses(vm)[3])
    }

    @Test
    fun levelsAreGroupedByTheirLetterCountFromTheData() = runTest {
        val vm = LevelSelectViewModel(FakeLevelRepository(levels), FakeProgressRepository())

        val groups = vm.uiState.value.levels.groupBy { it.letterCount }.mapValues { (_, t) -> t.map { it.id } }
        assertEquals(mapOf(3 to (1..3).toList(), 4 to (4..8).toList(), 5 to (9..15).toList()), groups)
    }
}
