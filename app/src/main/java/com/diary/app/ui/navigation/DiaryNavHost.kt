package com.diary.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diary.app.ui.editor.EditorScreen
import com.diary.app.ui.home.HomeScreen
import com.diary.app.ui.map.MapScreen
import com.diary.app.ui.stats.StatsScreen
import com.diary.app.ui.profile.ProfileScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.DateRange)
    object Map : Screen("map", "地图", Icons.Default.Map)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    object Editor : Screen("editor?diaryId={diaryId}", "编辑日记", Icons.Default.DateRange) {
        fun createRoute(diaryId: Long? = null): String {
            return if (diaryId != null) "editor?diaryId=$diaryId" else "editor"
        }
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Map, Screen.Stats, Screen.Profile)

@Composable
fun DiaryNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

            if (showBottomBar) {
                NavigationBar(containerColor = Color.Transparent) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) })
            }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("diaryId") { type = NavType.LongType; defaultValue = -1L }),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: -1L
                EditorScreen(
                    diaryId = if (diaryId == -1L) null else diaryId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
