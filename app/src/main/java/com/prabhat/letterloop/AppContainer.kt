// Manual dependency wiring, used instead of Hilt (Decision log #3).
// It creates each repository once; ViewModel factories read their dependencies from here.
package com.prabhat.letterloop

import android.content.Context
import com.prabhat.letterloop.data.AssetLevelRepository
import com.prabhat.letterloop.data.DataStoreProgressRepository
import com.prabhat.letterloop.data.LEVEL_COUNT
import com.prabhat.letterloop.data.LevelRepository
import com.prabhat.letterloop.data.ProgressRepository
import com.prabhat.letterloop.data.progressDataStore

class AppContainer(context: Context) {
    val levelRepository: LevelRepository = AssetLevelRepository(context.assets)
    val progressRepository: ProgressRepository =
        DataStoreProgressRepository(context.progressDataStore, LEVEL_COUNT)
}
