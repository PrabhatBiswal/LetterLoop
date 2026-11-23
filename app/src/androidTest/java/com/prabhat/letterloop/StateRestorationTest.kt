// Device tests for state surviving a configuration change (R7). ActivityScenario.recreate() destroys
// and rebuilds the Activity exactly as a rotation does. Real process death cannot be produced from
// inside a test; it is checked by hand (section 11).
package com.prabhat.letterloop

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prabhat.letterloop.ui.TestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateRestorationTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun launch(highestUnlocked: Int = 1) {
        resetSavedProgress(highestUnlocked)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() = scenario.close()

    private fun openLevel(id: Int) {
        rule.onNodeWithTag(TestTags.PLAY_BUTTON).performClick()
        rule.waitForTag(TestTags.levelTile(id))
        rule.onNodeWithTag(TestTags.levelTile(id)).performClick()
        rule.waitForTag(TestTags.wheelLetter(0))
    }

    private fun tileState(row: Int, col: Int): String =
        rule.onNodeWithTag(TestTags.gridCell(row, col)).fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    @Test
    fun foundWord_survivesRecreate_sameLevel_sameBonusCount() {
        launch(highestUnlocked = 4)
        openLevel(4)
        rule.swipeWord("STOP") // grid word: row 0
        rule.swipeWord("SPOT") // bonus word
        rule.onNodeWithTag(TestTags.BONUS_COUNT).assertTextEquals("1")

        scenario.recreate()

        rule.waitForTag(TestTags.wheelLetter(0))
        rule.onNodeWithTag(TestTags.LEVEL_TITLE).assertTextEquals("Level 4")
        (0..3).forEach { col -> assertEquals("Revealed", tileState(0, col)) }
        assertEquals("Hidden", tileState(3, 0))
        rule.onNodeWithTag(TestTags.BONUS_COUNT).assertTextEquals("1")
    }

    @Test
    fun pauseOpen_survivesRecreate() {
        launch()
        openLevel(1)
        pressBack()
        rule.onNodeWithTag(TestTags.PAUSE_DIALOG).assertIsDisplayed()

        scenario.recreate()

        rule.waitForTag(TestTags.PAUSE_DIALOG)
        rule.onNodeWithTag(TestTags.PAUSE_DIALOG).assertIsDisplayed()
    }

    @Test
    fun shuffledWheelOrder_survivesRecreate() {
        launch(highestUnlocked = 9)
        openLevel(9)
        val original = rule.wheelLetters()

        rule.onNodeWithTag(TestTags.SHUFFLE_BUTTON).performClick()
        rule.waitForIdle()
        val shuffled = rule.wheelLetters()
        assertNotEquals(original, shuffled)

        scenario.recreate()

        rule.waitForTag(TestTags.wheelLetter(0))
        assertEquals(shuffled, rule.wheelLetters())
    }
}
