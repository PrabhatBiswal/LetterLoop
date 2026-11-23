// Unit tests for GameViewModel with fake repositories and virtual time. Dispatchers.Main is a test
// dispatcher, so viewModelScope runs on the test scheduler and the 1.5 s celebration costs no time.
package com.prabhat.letterloop.ui.game

import androidx.lifecycle.SavedStateHandle
import com.prabhat.letterloop.data.LevelProgress
import com.prabhat.letterloop.data.parseLevels
import com.prabhat.letterloop.fakes.FakeLevelRepository
import com.prabhat.letterloop.fakes.FakeProgressRepository
import com.prabhat.letterloop.game.Cell
import com.prabhat.letterloop.game.Level
import com.prabhat.letterloop.game.SubmitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val levels: List<Level> = parseLevels(File("src/main/assets/levels.json").readText())
    private val progress = FakeProgressRepository()

    @Before
    fun setUp() {
        // Unconfined: viewModelScope work runs straight away, like Dispatchers.Main.immediate.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A ViewModel as Navigation creates it: only the route argument is in the handle. */
    private fun viewModel(
        routeLevelId: Int = 1,
        handle: SavedStateHandle = SavedStateHandle(mapOf(GameViewModel.KEY_ROUTE_LEVEL_ID to routeLevelId)),
    ) = GameViewModel(FakeLevelRepository(levels), progress, handle, Random(42))

    private fun level(id: Int) = levels.first { it.id == id }
    private fun GameViewModel.state() = uiState.value
    private fun GameViewModel.game() = uiState.value.game!!
    private fun GameViewModel.solve(levelId: Int) = level(levelId).words.forEach { onWordSubmitted(it.word) }

    /** Runs the celebration delay in virtual time. */
    private fun TestScope.finishCelebration() {
        advanceTimeBy(GameViewModel.CELEBRATION_MILLIS)
        runCurrent()
    }

    @Test
    fun startsOnRouteLevel_withItsWheelLetters_andAnEmptyGrid() = runTest {
        val vm = viewModel(routeLevelId = 3)

        assertEquals(3, vm.state().levelId)
        assertEquals(level(3).letters, vm.state().letters)
        assertTrue(vm.game().revealedCells.isEmpty())
        assertEquals(GamePhase.Playing, vm.state().phase)
    }

    @Test
    fun savedProgress_isRestored_foundWordsRevealedInFirstState() = runTest {
        progress.seed(4, LevelProgress(foundWords = listOf("STOP"), foundBonus = listOf("POT")))

        val vm = viewModel(routeLevelId = 4)

        assertEquals(setOf("STOP"), vm.game().foundWords)
        assertEquals(setOf("POT"), vm.game().foundBonus)
        assertTrue(vm.game().revealedCells.containsAll(listOf(Cell(0, 0), Cell(0, 3))))
    }

    @Test
    fun savedProgressNamingWordsNotInTheLevel_startsTheLevelEmpty() = runTest {
        progress.seed(4, LevelProgress(foundWords = listOf("STOP", "ZEBRA")))

        val vm = viewModel(routeLevelId = 4)

        assertTrue(vm.game().foundWords.isEmpty())
        assertTrue(vm.game().revealedCells.isEmpty())
    }

    @Test
    fun gridWord_revealsCells_feedbackGrid_savedExactlyOnce() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("STOP")

        assertTrue(vm.game().revealedCells.containsAll(listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3))))
        assertEquals(SubmitResult.GridWord, vm.state().feedback?.result)
        assertEquals(1, progress.saveCount)
        assertEquals(listOf("STOP"), progress.savedProgress(4)?.foundWords)
    }

    @Test
    fun bonusWord_counterUp_listed_saved_feedbackBonus() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("SPOT")

        assertEquals(setOf("SPOT"), vm.game().foundBonus)
        assertEquals(SubmitResult.BonusWord, vm.state().feedback?.result)
        assertEquals(1, progress.saveCount)
        assertEquals(listOf("SPOT"), progress.savedProgress(4)?.foundBonus)
    }

    @Test
    fun invalidWord_nothingSaved_feedbackInvalid() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("DOG")

        assertEquals(SubmitResult.Invalid, vm.state().feedback?.result)
        assertEquals(0, progress.saveCount)
    }

    @Test
    fun alreadyFoundWord_nothingSaved_feedbackAlreadyFound() = runTest {
        progress.seed(4, LevelProgress(foundWords = listOf("STOP")))
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("STOP")

        assertEquals(SubmitResult.AlreadyFound, vm.state().feedback?.result)
        assertEquals(0, progress.saveCount)
    }

    @Test
    fun feedbackIdsIncrease_shownClearsIt_andAnOldIdDoesNotClearANewerOne() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("DOG")
        val first = vm.state().feedback!!
        vm.onWordSubmitted("STOP")
        val second = vm.state().feedback!!
        assertTrue(second.id > first.id)

        vm.onFeedbackShown(first.id)
        assertEquals(second, vm.state().feedback)

        vm.onFeedbackShown(second.id)
        assertNull(vm.state().feedback)
    }

    @Test
    fun sameWordTwiceInQuickSuccession_secondIsAlreadyFound_savedOnce() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("STOP")
        vm.onWordSubmitted("STOP")

        assertEquals(SubmitResult.AlreadyFound, vm.state().feedback?.result)
        assertEquals(1, progress.saveCount)
    }

    @Test
    fun lastGridWord_raisesHighestUnlocked_andStartsCelebrating() = runTest {
        val vm = viewModel(routeLevelId = 1)

        vm.solve(1)

        assertEquals(GamePhase.Celebrating, vm.state().phase)
        assertEquals(2, progress.highest)
    }

    @Test
    fun afterCelebrationDelay_nextLevelLoaded_savedStateUpdated_finishedProgressCleared() = runTest {
        val handle = SavedStateHandle(mapOf(GameViewModel.KEY_ROUTE_LEVEL_ID to 1))
        val vm = viewModel(handle = handle)
        vm.solve(1)

        advanceTimeBy(GameViewModel.CELEBRATION_MILLIS - 1)
        runCurrent()
        assertEquals("still celebrating just before the delay ends", 1, vm.state().levelId)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, vm.state().levelId)
        assertEquals(GamePhase.Playing, vm.state().phase)
        assertEquals(level(2).letters, vm.state().letters)
        assertEquals(2, handle.get<Int>(GameViewModel.KEY_CURRENT_LEVEL))
        assertNull(progress.savedProgress(1))
        assertEquals(listOf(1), progress.clearedLevels)
    }

    @Test
    fun wordsHintAndShuffle_areIgnoredWhileCelebrating() = runTest {
        val vm = viewModel(routeLevelId = 1)
        vm.solve(1)
        val before = vm.state()
        val savesBefore = progress.saveCount

        vm.onWordSubmitted("CAT")
        vm.onHint()
        vm.onShuffle()

        assertEquals(before, vm.state())
        assertEquals(savesBefore, progress.saveCount)
    }

    @Test
    fun finishingLevel15_phaseAllLevelsDone_highestUnlockedIs16() = runTest {
        progress.highest = 15
        val vm = viewModel(routeLevelId = 15)

        vm.solve(15)
        advanceUntilIdle()

        assertEquals(GamePhase.AllLevelsDone, vm.state().phase)
        assertEquals(15, vm.state().levelId)
        assertEquals(16, progress.highest)
    }

    @Test
    fun rebuiltForALevelWhoseSavedProgressIsComplete_celebratesAndAdvances() = runTest {
        // Process death during the celebration: level 5 was saved complete, and the handle
        // already holds currentLevelId because this ViewModel existed before the kill.
        progress.seed(5, LevelProgress(foundWords = level(5).words.map { it.word }))
        val handle = SavedStateHandle(
            mapOf(GameViewModel.KEY_ROUTE_LEVEL_ID to 5, GameViewModel.KEY_CURRENT_LEVEL to 5),
        )
        val vm = viewModel(handle = handle)

        assertEquals(GamePhase.Celebrating, vm.state().phase)
        assertTrue(vm.game().isComplete)

        finishCelebration()
        assertEquals(6, vm.state().levelId)
        assertEquals(GamePhase.Playing, vm.state().phase)
    }

    @Test
    fun rebuiltWithASavedLevelIdDifferentFromTheRoute_usesTheSavedOne() = runTest {
        // Process death after advancing from level 3 to 4 inside the same Game destination.
        val handle = SavedStateHandle(
            mapOf(GameViewModel.KEY_ROUTE_LEVEL_ID to 3, GameViewModel.KEY_CURRENT_LEVEL to 4),
        )
        val vm = viewModel(handle = handle)

        assertEquals(4, vm.state().levelId)
        assertEquals(level(4).letters.toList().sorted(), vm.state().letters.toList().sorted())
    }

    @Test
    fun openingAFinishedLevelFromLevelSelect_startsItFresh() = runTest {
        // Level 15 keeps its complete progress after the all-done card; a later visit replays it.
        progress.highest = 16
        progress.seed(15, LevelProgress(foundWords = level(15).words.map { it.word }))

        val vm = viewModel(routeLevelId = 15)

        assertEquals(GamePhase.Playing, vm.state().phase)
        assertTrue(vm.game().foundWords.isEmpty())
        assertEquals(listOf(15), progress.clearedLevels)
    }

    @Test
    fun replayingAnOlderFinishedLevel_neverLowersHighestUnlocked() = runTest {
        progress.highest = 10
        val vm = viewModel(routeLevelId = 2)

        vm.solve(2)
        finishCelebration()

        assertEquals(10, progress.highest)
    }

    @Test
    fun levelIdThatDoesNotExist_fallsBackToLevel1_withoutCrashing() = runTest {
        val vm = viewModel(routeLevelId = 99)

        assertEquals(1, vm.state().levelId)
        assertEquals(level(1), vm.game().level)
    }

    @Test
    fun celebrationDoesNotAdvanceWhileScreenIsStopped_andAdvancesOnceStartedAgain() = runTest {
        val vm = viewModel(routeLevelId = 1)
        vm.solve(1)

        vm.onScreenStopped()
        advanceTimeBy(10 * GameViewModel.CELEBRATION_MILLIS)
        runCurrent()
        assertEquals(1, vm.state().levelId)
        assertEquals(GamePhase.Celebrating, vm.state().phase)
        assertTrue("finished level's progress is still saved", progress.savedProgress(1) != null)

        vm.onScreenStarted()
        finishCelebration()
        assertEquals(2, vm.state().levelId)
    }

    @Test
    fun shuffle_sameLettersNewOrder_gridUntouched_savedToAndRestoredFromHandle() = runTest {
        val handle = SavedStateHandle(mapOf(GameViewModel.KEY_ROUTE_LEVEL_ID to 9))
        val vm = viewModel(handle = handle)
        vm.onWordSubmitted("HEART")
        val gameBefore = vm.game()
        val lettersBefore = vm.state().letters

        vm.onShuffle()
        val shuffled = vm.state().letters

        assertNotEquals(lettersBefore, shuffled)
        assertEquals(lettersBefore.toList().sorted(), shuffled.toList().sorted())
        assertEquals(gameBefore, vm.game())
        assertEquals(shuffled, handle.get<String>(GameViewModel.KEY_WHEEL_LETTERS))

        // A ViewModel rebuilt from the same handle (process death) shows the same order.
        val rebuilt = viewModel(handle = handle)
        assertEquals(shuffled, rebuilt.state().letters)
    }

    @Test
    fun hint_revealsOneCell_andSavesIt() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onHint()

        assertEquals(setOf(Cell(0, 0)), vm.game().hintedCells)
        assertEquals(1, progress.saveCount)
        assertEquals(listOf(Cell(0, 0)), progress.savedProgress(4)?.hintedCells)
    }

    @Test
    fun hintThatFillsTheLastCell_startsTheCompletionFlow() = runTest {
        val vm = viewModel(routeLevelId = 1)
        vm.onWordSubmitted("CAT") // leaves the two lower cells of ACT hidden

        vm.onHint()
        assertFalse(vm.game().isComplete)
        vm.onHint()

        assertTrue(vm.game().isComplete)
        assertEquals(GamePhase.Celebrating, vm.state().phase)
        assertEquals(2, progress.highest)
    }

    @Test
    fun bonusList_holdsFoundBonusWordsInTheOrderFound() = runTest {
        val vm = viewModel(routeLevelId = 4)

        vm.onWordSubmitted("SPOT")
        vm.onWordSubmitted("POT")
        vm.onWordSubmitted("SPOT")

        assertEquals(listOf("SPOT", "POT"), vm.game().foundBonus.toList())
        assertEquals(listOf("SPOT", "POT"), progress.savedProgress(4)?.foundBonus)
    }
}
