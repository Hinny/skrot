package dev.hinny.skrot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.body.BodyScreen
import dev.hinny.skrot.ui.exercises.ExerciseDetailScreen
import dev.hinny.skrot.ui.exercises.ExercisesScreen
import dev.hinny.skrot.ui.gyms.GymsScreen
import dev.hinny.skrot.ui.home.HomeScreen
import dev.hinny.skrot.ui.importexport.BackupScreen
import dev.hinny.skrot.ui.library.LibraryScreen
import dev.hinny.skrot.ui.logging.SessionSummaryScreen
import dev.hinny.skrot.ui.logging.WorkoutScreen
import dev.hinny.skrot.ui.more.AboutScreen
import dev.hinny.skrot.ui.more.MoreScreen
import dev.hinny.skrot.ui.routines.DayEditorScreen
import dev.hinny.skrot.ui.routines.ProgramEditorScreen
import dev.hinny.skrot.ui.routines.ProgramsScreen
import dev.hinny.skrot.ui.session.SessionScreen
import dev.hinny.skrot.ui.settings.SettingsScreen
import dev.hinny.skrot.ui.stats.StatsScreen

/** Creates a ViewModel wired to the app container, with a SavedStateHandle. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    container: AppContainer,
    key: String? = null,
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer { create(container, createSavedStateHandle()) }
    },
)

object Routes {
    const val HOME = "home"
    const val SESSION = "session"
    const val LIBRARY = "library"
    const val PROGRAMS = "programs"
    const val EXERCISES = "exercises"
    const val STATS = "stats"
    const val MORE = "more"
    const val GYMS = "gyms"
    const val BODY = "body"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val ABOUT = "about"
    fun program(id: Long) = "program/$id"
    fun day(id: Long) = "day/$id"
    fun workout(id: Long) = "workout/$id"
    fun summary(id: Long) = "summary/$id"
    fun exercise(id: Long) = "exercise/$id"

    /** Maps any route to the bottom-bar tab it belongs under (for highlighting). */
    fun tabFor(route: String?): String? = when {
        route == null -> null
        route == HOME -> HOME
        route == SESSION || route.startsWith("workout/") || route.startsWith("summary/") -> SESSION
        route == LIBRARY || route == PROGRAMS || route == EXERCISES || route == GYMS ||
            route.startsWith("program/") || route.startsWith("day/") ||
            route.startsWith("exercise/") -> LIBRARY

        route == STATS -> STATS
        else -> MORE
    }
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun SkrotApp(container: AppContainer, settings: Settings) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val openSession by container.db.sessionDao().observeOpenSession()
        .collectAsStateWithLifecycle(initialValue = null)

    val tabs = listOf(
        Tab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
        Tab(Routes.SESSION, R.string.tab_session, Icons.Filled.PlayArrow),
        Tab(Routes.LIBRARY, R.string.tab_library, Icons.Filled.MenuBook),
        Tab(Routes.STATS, R.string.tab_stats, Icons.Filled.BarChart),
        Tab(Routes.MORE, R.string.tab_more, Icons.Filled.MoreHoriz),
    )
    val selectedTab = Routes.tabFor(currentRoute)
    val hideBars = currentRoute?.startsWith("workout/") == true ||
        currentRoute?.startsWith("summary/") == true

    Scaffold(
        bottomBar = {
            Column {
                // When the nav bar is hidden (during a workout) the timer bar is the
                // bottom-most element and must clear the system navigation bar itself.
                RestTimerBar(container, settings, applyNavInsets = hideBars)
                if (!hideBars) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab.route,
                                onClick = {
                                    // Always land on the tab's first page (never a sub-page).
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME)
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    if (tab.route == Routes.SESSION && openSession != null) {
                                        BadgedBox(badge = { Badge() }) { Icon(tab.icon, null) }
                                    } else {
                                        Icon(tab.icon, null)
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { HomeScreen(container, settings, navController) }
            composable(Routes.SESSION) { SessionScreen(container, navController) }
            composable(Routes.LIBRARY) { LibraryScreen(navController) }
            composable(Routes.PROGRAMS) { ProgramsScreen(container, navController) }
            composable(
                "program/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { ProgramEditorScreen(container, navController, it.arguments!!.getLong("id")) }
            composable(
                "day/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { DayEditorScreen(container, settings, navController, it.arguments!!.getLong("id")) }
            composable(
                "workout/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { WorkoutScreen(container, settings, navController, it.arguments!!.getLong("id")) }
            composable(
                "summary/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { SessionSummaryScreen(container, settings, navController, it.arguments!!.getLong("id")) }
            composable(Routes.EXERCISES) { ExercisesScreen(container, navController) }
            composable(
                "exercise/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { ExerciseDetailScreen(container, settings, navController, it.arguments!!.getLong("id")) }
            composable(Routes.STATS) { StatsScreen(container, settings) }
            composable(Routes.MORE) { MoreScreen(navController) }
            composable(Routes.GYMS) { GymsScreen(container) }
            composable(Routes.BODY) { BodyScreen(container, settings) }
            composable(Routes.SETTINGS) { SettingsScreen(container, settings, navController) }
            composable(Routes.BACKUP) { BackupScreen(container) }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}

@Composable
private fun RestTimerBar(
    container: AppContainer,
    settings: Settings,
    applyNavInsets: Boolean,
) {
    val timerState by container.restTimer.state.collectAsStateWithLifecycle()
    val state = timerState ?: return
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (applyNavInsets) {
                        Modifier.windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "%d:%02d · %s".format(
                    state.remainingSec / 60,
                    state.remainingSec % 60,
                    state.exerciseName,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { container.restTimer.adjust(-settings.timerAdjustStepSec) }) {
                    Text("-${settings.timerAdjustStepSec}s")
                }
                TextButton(onClick = { container.restTimer.adjust(settings.timerAdjustStepSec) }) {
                    Text("+${settings.timerAdjustStepSec}s")
                }
                IconButton(onClick = { container.restTimer.skip() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.skip))
                }
            }
        }
    }
}
