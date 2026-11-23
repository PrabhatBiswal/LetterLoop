// Manual dependency wiring, used instead of Hilt (Decision log #3).
// It creates each repository once; ViewModel factories read their dependencies from here.
package com.prabhat.letterloop

import android.content.Context
import com.prabhat.letterloop.data.AssetLevelRepository
import com.prabhat.letterloop.data.LevelRepository

class AppContainer(context: Context) {
    val levelRepository: LevelRepository = AssetLevelRepository(context.assets)
}
