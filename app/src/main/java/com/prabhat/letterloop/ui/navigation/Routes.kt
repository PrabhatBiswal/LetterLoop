// The app's destinations as type-safe @Serializable routes:
// Home → LevelSelect → Game(levelId) → Pause (a dialog destination).
package com.prabhat.letterloop.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object LevelSelect

/** GameViewModel reads [levelId] from its SavedStateHandle under this property's name. */
@Serializable
data class Game(val levelId: Int)

@Serializable
data object Pause
