package com.diary.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.diary.app.ui.media.MediaScreen
import com.diary.app.ui.stats.StatsScreen

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "日记")
    object Media : Screen("media", "媒体库")
    object Map : Screen("map", "地图")
    object Stats : Screen("stats", "统计")
    object Editor : Screen("editor?diaryId={diaryId}", "编辑日记") {
        fun createRoute(diaryId: Long? = null): String {
            return if (diaryId != null) "editor?diaryId=$diaryId" else "editor"
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val icon: @Composable () -> Unit
)

@Composable
fun DiaryNavHost() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home) { Icon(Icons.Default.DateRange, contentDescription = "日记") },
        BottomNavItem(Screen.Media) { Icon(Icons.Default.PhotoLibrary, contentDescription = "媒体库") },
        BottomNavItem(Screen.Map) { Icon(Icons.Default.Map, contentDescription = "地图") },
        BottomNavItem(Screen.Stats) { Icon(Icons.Default.BarChart, contentDescription = "统计") }
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // 只在主页面显示底部导航栏
            val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToEditor = { diaryId ->
                        navController.navigate(Screen.Editor.createRoute(diaryId))
                    }
                )
            }
            composable(Screen.Media.route) {
                MediaScreen()
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("diaryId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
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
