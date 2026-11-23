// Tests parseLevels(): the JSON format from section 5 parses into Level objects, and bad input
// fails loudly instead of producing an empty or half-read list.
package com.prabhat.letterloop.data

import com.prabhat.letterloop.game.Direction.ACROSS
import com.prabhat.letterloop.game.Direction.DOWN
import com.prabhat.letterloop.game.Level
import com.prabhat.letterloop.game.PlacedWord
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LevelParsingTest {

    @Test
    fun sampleJson_parsesIntoExpectedLevels() {
        val json = """
            {
              "levels": [
                {
                  "id": 1, "letters": "CAT",
                  "words": [
                    { "word": "CAT", "row": 0, "col": 0, "dir": "ACROSS" },
                    { "word": "ACT", "row": 0, "col": 1, "dir": "DOWN" }
                  ],
                  "bonus": []
                },
                {
                  "id": 2, "letters": "STOP",
                  "words": [ { "word": "STOP", "row": 0, "col": 0, "dir": "ACROSS" } ],
                  "bonus": ["POT", "TOP"]
                }
              ]
            }
        """.trimIndent()

        val expected = listOf(
            Level(1, "CAT", listOf(PlacedWord("CAT", 0, 0, ACROSS), PlacedWord("ACT", 0, 1, DOWN)), emptyList()),
            Level(2, "STOP", listOf(PlacedWord("STOP", 0, 0, ACROSS)), listOf("POT", "TOP")),
        )
        assertEquals(expected, parseLevels(json))
    }

    @Test
    fun malformedJson_throwsSerializationException() {
        assertThrows(SerializationException::class.java) {
            parseLevels("""{ "levels": [ { "id": 1, "letters": "CAT" """)
        }
    }

    @Test
    fun emptyLevelList_throwsInsteadOfReturningEmpty() {
        val error = assertThrows(IllegalArgumentException::class.java) { parseLevels("""{ "levels": [] }""") }
        assertEquals("levels.json contains no levels", error.message)
    }

    @Test
    fun unknownDir_throws() {
        val json = """
            { "levels": [ { "id": 1, "letters": "CAT",
              "words": [ { "word": "CAT", "row": 0, "col": 0, "dir": "DIAGONAL" } ] } ] }
        """.trimIndent()
        assertThrows(SerializationException::class.java) { parseLevels(json) }
    }

    @Test
    fun unknownExtraField_isIgnored() {
        val json = """
            { "version": 3,
              "levels": [ { "id": 1, "letters": "CAT", "theme": "animals",
                "words": [ { "word": "CAT", "row": 0, "col": 0, "dir": "ACROSS", "clue": "pet" } ] } ] }
        """.trimIndent()

        assertEquals(listOf(Level(1, "CAT", listOf(PlacedWord("CAT", 0, 0, ACROSS)))), parseLevels(json))
    }
}
