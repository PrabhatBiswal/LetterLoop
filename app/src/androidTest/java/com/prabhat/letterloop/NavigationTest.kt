// Device tests for navigation and the back stack (R6), driving the real MainActivity.
// Saved data is reset before the activity launches, so every test starts on a fresh install.
package com.prabhat.letterloop

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prabhat.letterloop.data.parseLevels
import com.prabhat.letterloop.game.Level
import com.prabhat.letterloop.ui.TestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        resetSavedProgress()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() = scenario.close()

    private fun assertShown(tag: String) = rule.onNodeWithTag(tag).assertIsDisplayed()
    private fun assertGone(tag: String) = rule.onNodeWithTag(tag).assertDoesNotExist()

    private fun openLevel1() {
        rule.onNodeWithTag(TestTags.PLAY_BUTTON).performClick()
        rule.waitForTag(TestTags.levelTile(1))
        rule.onNodeWithTag(TestTags.levelTile(1)).performClick()
        rule.waitForTag(TestTags.wheelLetter(0))
    }

    @Test
    fun homePlay_showsLevelSelect_thenLevel1_showsTheGame() {
        rule.onNodeWithTag(TestTags.PLAY_BUTTON).performClick()
        assertShown(TestTags.LEVEL_SELECT_SCREEN)

        rule.waitForTag(TestTags.levelTile(1))
        rule.onNodeWithTag(TestTags.levelTile(1)).performClick()
        assertShown(TestTags.GAME_SCREEN)
        rule.onNodeWithTag(TestTags.LEVEL_TITLE).assertTextEquals("Level 1")
    }

    @Test
    fun backInGame_showsPause_backAgain_showsGameWithPauseGone() {
        openLevel1()

        pressBack()
        assertShown(TestTags.PAUSE_DIALOG)

        pressBack()
        assertGone(TestTags.PAUSE_DIALOG)
        assertShown(TestTags.GAME_SCREEN)

        // And a third back opens Pause again: back never leaves the level.
        pressBack()
        assertShown(TestTags.PAUSE_DIALOG)
    }

    @Test
    fun pauseLevels_showsLevelSelect_thenBack_showsHome() {
        openLevel1()
        pressBack()

        rule.onNodeWithTag(TestTags.PAUSE_LEVELS_BUTTON).performClick()
        assertShown(TestTags.LEVEL_SELECT_SCREEN)
        assertGone(TestTags.GAME_SCREEN)

        pressBack()
        assertShown(TestTags.HOME_SCREEN)
        assertGone(TestTags.LEVEL_SELECT_SCREEN)
    }

    @Test
    fun pauseHome_showsHome_thenBack_leavesTheApp() {
        openLevel1()
        pressBack()

        rule.onNodeWithTag(TestTags.PAUSE_HOME_BUTTON).performClick()
        assertShown(TestTags.HOME_SCREEN)

        pressBackUnconditionally()
        // Since Android 12, back on a launcher activity moves the task to the background instead
        // of finishing it, so either state means the app was left (Decision log).
        rule.waitUntil(5_000) {
            scenario.state == Lifecycle.State.CREATED || scenario.state == Lifecycle.State.DESTROYED
        }
    }

    @Test
    fun doubleClickingPlay_opensOneLevelSelect() {
        rule.onNodeWithTag(TestTags.PLAY_BUTTON).performTouchInput { doubleClick() }
        assertShown(TestTags.LEVEL_SELECT_SCREEN)

        // One back press returns Home; a duplicate Level Select would still be showing.
        pressBack()
        assertShown(TestTags.HOME_SCREEN)
        assertGone(TestTags.LEVEL_SELECT_SCREEN)
    }

    @Test
    fun doubleClickingALevel_opensOneGame() {
        rule.onNodeWithTag(TestTags.PLAY_BUTTON).performClick()
        rule.waitForTag(TestTags.levelTile(1))

        rule.onNodeWithTag(TestTags.levelTile(1)).performTouchInput { doubleClick() }
        assertShown(TestTags.GAME_SCREEN)

        // In the game, back's "previous screen" is Pause; Pause then pops straight to Level Select,
        // and one more back reaches Home, so no extra Game or Level Select entry exists.
        pressBack()
        assertShown(TestTags.PAUSE_DIALOG)
        rule.onNodeWithTag(TestTags.PAUSE_LEVELS_BUTTON).performClick()
        assertShown(TestTags.LEVEL_SELECT_SCREEN)
        pressBack()
        assertShown(TestTags.HOME_SCREEN)
    }

    @Test
    fun doubleClickingPause_opensOnePause() {
        openLevel1()

        rule.onNodeWithTag(TestTags.PAUSE_BUTTON).performTouchInput { doubleClick() }
        assertShown(TestTags.PAUSE_DIALOG)

        // One back press closes it; a second Pause stacked on top would still be showing.
        pressBack()
        assertGone(TestTags.PAUSE_DIALOG)
        assertShown(TestTags.GAME_SCREEN)
    }

    @Test
    fun tappingALockedLevel_staysOnLevelSelect() {
        rule.onNodeWithTag(TestTags.PLAY_BUTTON).performClick()
        rule.waitForTag(TestTags.levelTile(2))

        rule.onNodeWithTag(TestTags.levelTile(2)).performClick()

        assertShown(TestTags.LEVEL_SELECT_SCREEN)
        assertGone(TestTags.GAME_SCREEN)
    }

    // While the next level slides in, both levels are briefly on screen, so read every title.
    private fun levelTitles(): List<String> =
        rule.onAllNodesWithTag(TestTags.LEVEL_TITLE).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.Text].first().text }

    /** Swipes the level's grid words until the complete card shows, then waits for the next level. */
    private fun solveLevel(level: Level) {
        for (placed in level.words) {
            if (rule.onAllNodesWithTag(TestTags.LEVEL_COMPLETE_CARD).fetchSemanticsNodes().isNotEmpty()) break
            rule.swipeWord(placed.word)
        }
        assertShown(TestTags.LEVEL_COMPLETE_CARD)
        // The ViewModel's 1.5 s celebration runs in real time on the device; poll until it is done.
        rule.waitUntil(5_000) { "Level ${level.id + 1}" in levelTitles() }
        rule.waitForIdle()
    }

    @Test
    fun completingLevels1To3_advancesInPlace_andPauseLevelsBack_stillLandsOnHome() {
        val levels = InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("levels.json").bufferedReader().use { parseLevels(it.readText()) }
        openLevel1()

        solveLevel(levels[0])
        assertEquals(listOf("Level 2"), levelTitles())
        solveLevel(levels[1])
        solveLevel(levels[2])
        assertEquals(listOf("Level 4"), levelTitles())

        // However many levels were played, the stack is still Home → LevelSelect → Game.
        pressBack()
        rule.onNodeWithTag(TestTags.PAUSE_LEVELS_BUTTON).performClick()
        assertShown(TestTags.LEVEL_SELECT_SCREEN)
        pressBack()
        assertShown(TestTags.HOME_SCREEN)
        assertGone(TestTags.GAME_SCREEN)
    }
}
