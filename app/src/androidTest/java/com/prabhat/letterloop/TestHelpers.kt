// Shared helpers for the device tests that drive the whole app: resetting saved data, waiting for
// async content, and swiping a word on the real wheel using the same geometry the app uses.
package com.prabhat.letterloop

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.prabhat.letterloop.data.progressDataStore
import com.prabhat.letterloop.ui.TestTags
import com.prabhat.letterloop.ui.game.letterCenters
import kotlinx.coroutines.runBlocking
import kotlin.math.min

/** Every device test starts from clean saved data; optionally with some levels already unlocked. */
fun resetSavedProgress(highestUnlocked: Int = 1) = runBlocking {
    ApplicationProvider.getApplicationContext<Context>().progressDataStore.edit {
        it.clear()
        if (highestUnlocked > 1) it[intPreferencesKey("highest_unlocked")] = highestUnlocked
    }
}

/** Waits (polling, up to [timeoutMillis]) for a node with [tag] to exist, e.g. after async loading. */
fun ComposeTestRule.waitForTag(tag: String, timeoutMillis: Long = 5_000) =
    waitUntil(timeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

/** The wheel's letters in their on-screen order, read from the letter nodes. */
fun ComposeTestRule.wheelLetters(): String =
    (0 until 5).mapNotNull { i ->
        onAllNodesWithTag(TestTags.wheelLetter(i)).fetchSemanticsNodes().firstOrNull()
            ?.config?.get(SemanticsProperties.Text)?.first()?.text
    }.joinToString("")

/**
 * Swipes [word] on the wheel. Letters are picked by index, using each copy of a repeated letter
 * once. Straight moves between any two letters never cross a third letter's hit circle.
 */
fun ComposeTestRule.swipeWord(word: String) {
    waitForTag(TestTags.wheelLetter(0))
    // Let any level-change or shuffle animation finish, so only one wheel is on screen.
    waitForIdle()
    val letters = wheelLetters()
    val used = mutableSetOf<Int>()
    val indices = word.map { ch -> letters.indices.first { letters[it] == ch && it !in used }.also { used += it } }
    onNodeWithTag(TestTags.WHEEL).performTouchInput {
        val centers = letterCenters(letters.length, Offset(width / 2f, height / 2f), min(width, height) / 2f)
        down(centers[indices.first()])
        indices.drop(1).forEach { moveTo(centers[it]) }
        up()
    }
}
