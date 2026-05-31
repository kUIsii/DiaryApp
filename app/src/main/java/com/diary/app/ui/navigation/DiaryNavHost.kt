package com.diary.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diary.app.ui.backup.BackupScreen
import com.diary.app.ui.detail.DiaryDetailScreen
import com.diary.app.ui.editor.EditorScreen
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.home.HomeScreen
import com.diary.app.ui.review.DiaryReviewScreen
import com.diary.app.ui.map.MapScreen
import com.diary.app.ui.profile.ProfileScreen
import com.diary.app.ui.profile.TagManagementScreen
import com.diary.app.ui.settings.SettingsScreen
import com.diary.app.ui.stats.StatsScreen
import com.diary.app.ui.todo.TodoScreen
import com.diary.app.update.ChangelogScreen

// region Screen definitions

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "\u9996\u9875", Icons.Default.Home)
    object Map : Screen("map", "\u65e5\u5386", Icons.Default.CalendarMonth)
    object Todo : Screen("todo", "\u5f85\u529e", Icons.Default.CheckBox)
    object Stats : Screen("stats", "\u7edf\u8ba1", Icons.Default.BarChart)
    object Profile : Screen("profile", "\u6211\u7684", Icons.Default.Person)
    object Editor : Screen("editor?diaryId={diaryId}", "\u7f16\u8f91\u65e5\u8bb0", Icons.Default.Home) {
        fun createRoute(diaryId: Long? = null): String {
            return if (diaryId != null) "editor?diaryId=$diaryId" else "editor"
        }
    }
    object Detail : Screen("detail/{diaryId}", "\u65e5\u8bb0\u8be6\u60c5", Icons.Default.Home) {
        fun createRoute(diaryId: Long): String = "detail/$diaryId"
    }
    object Changelog : Screen("changelog", "\u66f4\u65b0\u65e5\u5fd7", Icons.Default.Home)
    object TagManagement : Screen("tag_management", "\u5206\u7c7b\u7ba1\u7406", Icons.Default.Home)
    object Review : Screen("review", "\u65e5\u8bb0\u56de\u987e", Icons.Default.Home)
    object Settings : Screen("settings", "\u8bbe\u7f6e", Icons.Default.Home)
    object Backup : Screen("backup", "\u5907\u4efd", Icons.Default.Home)
}

// endregion

// region Bottom navigation item with badge support

data class BottomNavItem(
    val screen: Screen,
    val badgeCount: Int = 0,
    val showBadge: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home),
    BottomNavItem(Screen.Map),
    BottomNavItem(Screen.Todo),
    BottomNavItem(Screen.Stats),
    BottomNavItem(Screen.Profile)
)

// endregion

@Composable
fun DiaryNavHost(navigateTo: String? = null, onNavigateHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val haptic = rememberHapticFeedback()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

            if (showBottomBar) {
                DiaryBottomNavigationBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.route,
                    onNavigate = { route ->
                        haptic.click()
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LaunchedEffect(navigateTo) {
            if (navigateTo == "editor") {
                navController.navigate(Screen.Editor.createRoute())
                onNavigateHandled()
            } else if (navigateTo == "todo") {
                navController.navigate(Screen.Todo.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onNavigateHandled()
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // region Bottom nav destinations

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) },
                    onNavigateToReview = { navController.navigate(Screen.Review.route) }
                )
            }
            composable(Screen.Map.route) {
                MapScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) }
                )
            }
            composable(Screen.Todo.route) { TodoScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) }
                )
            }

            // endregion

            // region Secondary destinations

            composable(Screen.Changelog.route) {
                ChangelogScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.TagManagement.route) {
                TagManagementScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Review.route) {
                DiaryReviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) },
                    onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) }
                )
            }
            composable(Screen.Backup.route) {
                BackupScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // endregion

            // region Full-screen slide destinations (Editor & Detail)

            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("diaryId") { type = NavType.LongType; defaultValue = -1L }),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeOut(animationSpec = tween(200))
                }
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
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: -1L
                DiaryDetailScreen(
                    diaryId = diaryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditor = { id ->
                        navController.navigate(Screen.Editor.createRoute(id))
                    },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }

            // endregion
        }
    }
}

// region Custom bottom navigation bar

@Composable
private fun DiaryBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .drawBehind {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.05f),
                                primaryColor.copy(alpha = 0.35f),
                                primaryColor.copy(alpha = 0.05f)
                            )
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    DiaryBottomNavItem(
                        item = item,
                        isSelected = currentRoute == item.screen.route,
                        onClick = { onNavigate(item.screen.route) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "iconScale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else onSurfaceVariant,
        animationSpec = tween(200),
        label = "iconColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else onSurfaceVariant,
        animationSpec = tween(200),
        label = "textColor"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "indicatorWidth"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = item.screen.icon,
                    contentDescription = item.screen.title,
                    tint = iconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )

                // Badge support
                if (item.showBadge) {
                    Badge {
                        if (item.badgeCount > 0) {
                            Text(
                                text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.screen.title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Animated indicator bar
            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(primaryColor)
            )
        }
    }
}

// endregion
