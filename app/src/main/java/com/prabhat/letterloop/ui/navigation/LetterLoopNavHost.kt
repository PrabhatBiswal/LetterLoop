// The single NavHost: every screen, the transitions between them, and the back-stack rules from
// section 8. Navigation is only ever triggered here, through navigateFrom() and popFrom().
package com.prabhat.letterloop.ui.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.prabhat.letterloop.ui.game.GameScreen
import com.prabhat.letterloop.ui.game.GameViewModel
import com.prabhat.letterloop.ui.game.PauseDialog
import com.prabhat.letterloop.ui.home.HomeScreen
import com.prabhat.letterloop.ui.levels.LevelSelectScreen
import com.prabhat.letterloop.ui.components.Scene
import com.prabhat.letterloop.ui.components.SceneBackdrop
import com.prabhat.letterloop.ui.components.sceneFor
import androidx.compose.runtime.getValue

// The old content takes 120ms to fade out; the new content then takes 260ms to arrive.
private val ExitSpec = tween<Float>(durationMillis = 120, easing = FastOutLinearInEasing)
private val EnterSpec = tween<Float>(durationMillis = 260, delayMillis = 120, easing = LinearOutSlowInEasing)

/**
 * Navigates only if [from] (the screen asking) is still the top entry and RESUMED. While a
 * transition runs, the new screen is not RESUMED yet and the old one is no longer on top, so a
 * double tap or a tap mid-transition does nothing. launchSingleTop stops the same screen being
 * stacked twice even if two taps did get through.
 */
fun NavController.navigateFrom(from: NavBackStackEntry, route: Any) {
    if (isActiveTop(from)) navigate(route) { launchSingleTop = true }
}

/** Same guard for going back, so a double tap on "Resume" cannot pop the game as well. */
fun NavController.popFrom(from: NavBackStackEntry, pop: NavController.() -> Unit) {
    if (isActiveTop(from)) pop()
}

private fun NavController.isActiveTop(entry: NavBackStackEntry) =
    currentBackStackEntry == entry && entry.lifecycle.currentState == Lifecycle.State.RESUMED

@Composable
fun LetterLoopNavHost(navController: NavHostController = rememberNavController()) {
    // The scenery behind every screen. Each screen reports its own scene when it appears; the
    // backdrop itself never moves, so a screen change only animates the content on top of it.
    // Saved as the chapter's letter count, so a rotation does not fade the scenery in again.
    var sceneLetters by rememberSaveable { mutableStateOf<Int?>(null) }
    val onScene: (Scene) -> Unit = { sceneLetters = it.letterCount }

    SceneBackdrop(sceneLetters?.let { sceneFor(it) }, Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.fillMaxSize(),
            // "Fade through": the old screen's content fades out quickly, then the new content
            // fades in while growing slightly. The two never overlap half-visible, which is what
            // makes it read as smooth. Going back, the new content settles in from slightly larger.
            enterTransition = { fadeIn(EnterSpec) + scaleIn(EnterSpec, initialScale = 0.94f) },
            exitTransition = { fadeOut(ExitSpec) },
            popEnterTransition = { fadeIn(EnterSpec) + scaleIn(EnterSpec, initialScale = 1.05f) },
            popExitTransition = { fadeOut(ExitSpec) },
        ) {
            composable<Home> { entry ->
                HomeScreen(onPlay = { navController.navigateFrom(entry, LevelSelect) }, onScene = onScene)
            }
            composable<LevelSelect> { entry ->
                LevelSelectScreen(
                    onBack = { navController.popFrom(entry) { popBackStack() } },
                    onLevelClick = { id -> navController.navigateFrom(entry, Game(id)) },
                    onScene = onScene,
                )
            }
            composable<Game> { entry ->
                GameScreen(
                    onPause = { navController.navigateFrom(entry, Pause) },
                    onBackToLevels = { navController.popFrom(entry) { popBackStack(LevelSelect, inclusive = false) } },
                    onScene = onScene,
                )
            }
            // A dialog destination: back closes it, and it survives rotation and process death
            // because it is part of the saved back stack. No custom code needed for any of that.
            dialog<Pause>(
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) { entry ->
                // Pause shows the live game state, so it uses the Game entry's own ViewModel.
                val gameEntry = remember(entry) { navController.getBackStackEntry<Game>() }
                val gameViewModel: GameViewModel = viewModel(gameEntry, factory = GameViewModel.Factory)
                val state by gameViewModel.uiState.collectAsStateWithLifecycle()
                // Pause pops back to entries that already exist; it never navigates forward to
                // Home or LevelSelect, which is how duplicate screens get created.
                PauseDialog(
                    state = state,
                    onResume = { navController.popFrom(entry) { popBackStack() } },
                    onLevels = { navController.popFrom(entry) { popBackStack(LevelSelect, inclusive = false) } },
                    onHome = { navController.popFrom(entry) { popBackStack(Home, inclusive = false) } },
                )
            }
        }
    }
}
