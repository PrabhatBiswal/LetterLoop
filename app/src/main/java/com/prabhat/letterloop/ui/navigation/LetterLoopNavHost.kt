// The single NavHost: every screen, the transitions between them, and the back-stack rules from
// section 8. Navigation is only ever triggered here, through navigateFrom() and popFrom().
package com.prabhat.letterloop.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.prabhat.letterloop.ui.theme.LoopColors
import androidx.compose.runtime.getValue

private const val TRANSITION_MILLIS = 300

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
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = Modifier
            .fillMaxSize()
            .background(LoopColors.Ground),
        // Fade plus a short slide; popping (back) runs the mirror image.
        enterTransition = {
            fadeIn(tween(TRANSITION_MILLIS)) +
                slideIntoContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) { it / 8 }
        },
        exitTransition = {
            fadeOut(tween(TRANSITION_MILLIS)) +
                slideOutOfContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) { it / 8 }
        },
        popEnterTransition = {
            fadeIn(tween(TRANSITION_MILLIS)) +
                slideIntoContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) { it / 8 }
        },
        popExitTransition = {
            fadeOut(tween(TRANSITION_MILLIS)) +
                slideOutOfContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) { it / 8 }
        },
    ) {
        composable<Home> { entry ->
            HomeScreen(onPlay = { navController.navigateFrom(entry, LevelSelect) })
        }
        composable<LevelSelect> { entry ->
            LevelSelectScreen(
                onBack = { navController.popFrom(entry) { popBackStack() } },
                onLevelClick = { id -> navController.navigateFrom(entry, Game(id)) },
            )
        }
        composable<Game> { entry ->
            GameScreen(
                onPause = { navController.navigateFrom(entry, Pause) },
                onBackToLevels = { navController.popFrom(entry) { popBackStack(LevelSelect, inclusive = false) } },
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
