// Saved progress: per-level found words and the highest unlocked level, kept in DataStore so they
// survive force-stop and process death. The interface exists so ViewModel tests can use a fake.
package com.prabhat.letterloop.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface ProgressRepository {
    /** The highest level the player may open, from 1 to levelCount + 1 (+1 means all finished). */
    val highestUnlocked: Flow<Int>

    /** Saved progress for one level; empty if nothing is saved or the value is unreadable. */
    fun progress(levelId: Int): Flow<LevelProgress>

    suspend fun saveProgress(levelId: Int, progress: LevelProgress)

    suspend fun clearProgress(levelId: Int)

    /** Raises highest_unlocked to [levelId] if it is higher. It never goes down. */
    suspend fun unlockUpTo(levelId: Int)
}

// One DataStore per process, created by the standard delegate. A file that cannot be read at all
// is replaced with empty data instead of crashing the app on every start.
val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "progress",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class DataStoreProgressRepository(
    private val dataStore: DataStore<Preferences>,
    private val levelCount: Int,
) : ProgressRepository {

    private val highestKey = intPreferencesKey("highest_unlocked")
    private fun progressKey(levelId: Int) = stringPreferencesKey("progress_$levelId")

    // Saved data is not trusted: an out-of-range value is clamped to 1..levelCount + 1.
    private fun Preferences.highest(): Int = (this[highestKey] ?: 1).coerceIn(1, levelCount + 1)

    // DataStore emits whenever any key changes; distinctUntilChanged hides changes to other keys.
    override val highestUnlocked: Flow<Int> =
        dataStore.data.map { it.highest() }.distinctUntilChanged()

    override fun progress(levelId: Int): Flow<LevelProgress> =
        dataStore.data.map { decode(it[progressKey(levelId)]) }.distinctUntilChanged()

    override suspend fun saveProgress(levelId: Int, progress: LevelProgress) {
        dataStore.edit { it[progressKey(levelId)] = Json.encodeToString(progress) }
    }

    override suspend fun clearProgress(levelId: Int) {
        dataStore.edit { it.remove(progressKey(levelId)) }
    }

    override suspend fun unlockUpTo(levelId: Int) {
        // Read and write inside one edit block, which DataStore runs as a single transaction.
        dataStore.edit { it[highestKey] = maxOf(it.highest(), levelId).coerceIn(1, levelCount + 1) }
    }

    // A value that is not valid LevelProgress JSON reads as empty progress, never as a crash.
    // (Words that do not belong to the level are checked by GameViewModel, which knows the level.)
    private fun decode(raw: String?): LevelProgress =
        if (raw == null) LevelProgress()
        else try {
            Json.decodeFromString<LevelProgress>(raw)
        } catch (e: IllegalArgumentException) { // SerializationException is a subclass
            LevelProgress()
        }
}
