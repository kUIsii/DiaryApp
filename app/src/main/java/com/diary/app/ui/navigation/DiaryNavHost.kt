package com.diary.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diary.app.ui.detail.DiaryDetailScreen
import com.diary.app.ui.editor.EditorScreen
import com.diary.app.ui.home.HomeScreen
import com.diary.app.ui.map.MapScreen
import com.diary.app.ui.profile.ProfileScreen
import com.diary.app.ui.profile.TagManagementScreen
import com.diary.app.ui.stats.StatsScreen
import com.diary.app.update.ChangelogScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.DateRange)
    object Map : Screen("map", "时间线", Icons.Default.Timeline)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    object Editor : Screen("editor?diaryId={diaryId}", "编辑日记", Icons.Default.DateRange) {
        fun createRoute(diaryId: Long? = null): String {
            return if (diaryId != null) "editor?diaryId=$diaryId" else "editor"
        }
    }
    object Detail : Screen("detail/{diaryId}", "日记详情", Icons.Default.DateRange) {
        fun createRoute(diaryId: Long): String = "detail/$diaryId"
    }
    object Changelog : Screen("changelog", "更新日志", Icons.Default.DateRange)
    object TagManagement : Screen("tag_management", "分类管理", Icons.Default.DateRange)
}

val bottomNavItems = listOf(Screen.Home, Screen.Map, Screen.Stats, Screen.Profile)

@Composable
fun DiaryNavHost(navigateTo: String? = null, onNavigateHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.12f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                ) {
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
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LaunchedEffect(navigateTo) {
            if (navigateTo == "editor") {
                navController.navigate(Screen.Editor.createRoute())
                onNavigateHandled()
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) }
                )
            }
            composable(Screen.Map.route) {
                MapScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) }
                )
            }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) }
                )
            }
            composable(Screen.Changelog.route) {
                ChangelogScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.TagManagement.route) {
                TagManagementScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("diaryId") { type = NavType.LongType; defaultValue = -1L }),
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: -1L
                EditorScreen(
                    diaryId = if (diaryId == -1L) null else diaryId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("diaryId") { type = NavType.LongType }),
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: -1L
                DiaryDetailScreen(
                    diaryId = diaryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditor = { id ->
                        navController.navigate(Screen.Editor.createRoute(id))
                    }
                )
            }
        }
    }
}
