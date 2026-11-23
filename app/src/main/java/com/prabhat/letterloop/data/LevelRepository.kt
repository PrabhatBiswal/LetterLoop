// Where levels come from. The interface exists so ViewModel tests can use a fake;
// the real one reads assets/levels.json once and keeps the parsed list in memory.
package com.prabhat.letterloop.data

import android.content.res.AssetManager
import com.prabhat.letterloop.game.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface LevelRepository {
    /** All levels in file order. */
    suspend fun levels(): List<Level>
}

class AssetLevelRepository(private val assets: AssetManager) : LevelRepository {
    // The Mutex makes two screens asking at the same moment share one parse instead of doing two.
    private val mutex = Mutex()
    private var cached: List<Level>? = null

    override suspend fun levels(): List<Level> = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            parseLevels(assets.open("levels.json").bufferedReader().use { it.readText() })
        }.also { cached = it }
    }
}

// The file's top-level shape: { "levels": [ ... ] }.
@Serializable
private class LevelFile(val levels: List<Level>)

// ignoreUnknownKeys lets the file gain new fields later without breaking older app versions.
private val json = Json { ignoreUnknownKeys = true }

/**
 * Parses levels.json. Kept as a plain function so it is unit tested without Android.
 * Malformed JSON or an unknown "dir" throws SerializationException; an empty list throws
 * IllegalArgumentException. It never quietly returns nothing.
 */
fun parseLevels(text: String): List<Level> {
    val levels = json.decodeFromString<LevelFile>(text).levels
    require(levels.isNotEmpty()) { "levels.json contains no levels" }
    return levels
}
