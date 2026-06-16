package com.diary.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.diary.app.DiaryApplication
import com.diary.app.ui.backup.BackupScreen
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.capsule.CreateCapsuleScreen
import com.diary.app.ui.capsule.ReadCapsuleScreen
import com.diary.app.ui.capsule.TimeCapsuleScreen
import com.diary.app.ui.notification.NotificationScreen
import com.diary.app.ui.countdown.CountDownScreen
import com.diary.app.ui.detail.DiaryDetailScreen
import com.diary.app.ui.editor.EditorScreen
import com.diary.app.ui.experimental.ExperimentalFeaturesScreen
import com.diary.app.ui.experimental.resolveMainScreenSwipeTarget
import com.diary.app.ui.favorites.FavoritesScreen
import com.diary.app.ui.home.HomeScreen
import com.diary.app.ui.media.MediaLibraryScreen
import com.diary.app.ui.profile.ProfileScreen
import com.diary.app.ui.profile.TagManagementScreen
import com.diary.app.ui.settings.SettingsScreen
import com.diary.app.ui.stats.StatsScreen
import com.diary.app.ui.timeline.TimelineScreen
import com.diary.app.ui.todo.TodoScreen
import com.diary.app.ui.trash.TrashScreen
import com.diary.app.ui.monthlyreport.MonthlyReportScreen
import com.diary.app.ui.annualreport.AnnualReportScreen
import com.diary.app.ui.health.HealthScreen
import com.diary.app.ui.map.DiaryMapScreen
import com.diary.app.ui.biography.BiographyScreen
import com.diary.app.update.ChangelogScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Timeline : Screen("timeline", "时间线", Icons.Default.CalendarMonth)
    object Todo : Screen("todo", "待办", Icons.Default.CheckBox)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    object ExperimentalFeatures : Screen("experimental_features", "实验功能", Icons.Default.Home)
    object Editor : Screen("editor?diaryId={diaryId}&draftId={draftId}", "编辑日记", Icons.Default.Home) {
        fun createRoute(diaryId: Long? = null, draftId: String? = null): String {
            val params = mutableListOf<String>()
            if (diaryId != null) params.add("diaryId=$diaryId")
            if (draftId != null) params.add("draftId=$draftId")
            return if (params.isEmpty()) "editor" else "editor?${params.joinToString("&")}"
        }
    }

    object Detail : Screen("detail/{diaryId}", "日记详情", Icons.Default.Home) {
        fun createRoute(diaryId: Long): String = "detail/$diaryId"
    }

    object Changelog : Screen("changelog", "更新日志", Icons.Default.Home)
    object TagManagement : Screen("tag_management", "分类管理", Icons.Default.Home)
    object Review : Screen("review", "日记回顾", Icons.Default.Home)
    object Settings : Screen("settings", "设置", Icons.Default.Home)
    object Backup : Screen("backup", "备份", Icons.Default.Home)
    object Favorites : Screen("favorites", "收藏夹", Icons.Default.Home)
    object MediaLibrary : Screen("media_library", "媒体库", Icons.Default.Home)
    object Trash : Screen("trash", "回收站", Icons.Default.Home)
    object CountDown : Screen("countdown", "倒数日", Icons.Default.Home)
    object TimeCapsule : Screen("time_capsule", "时间胶囊", Icons.Default.Home)
    object CreateCapsule : Screen("create_capsule", "写胶囊", Icons.Default.Home)
    object ReadCapsule : Screen("read_capsule/{capsuleId}", "读胶囊", Icons.Default.Home) {
        fun createRoute(capsuleId: Long): String = "read_capsule/$capsuleId"
    }
    object Notifications : Screen("notifications", "消息", Icons.Default.Home)
    object AiAssistant : Screen("ai_assistant", "小墨", Icons.Default.Home)
    object MonthlyReport : Screen("monthly_report/{year}/{month}", "月度报告", Icons.Default.Home) {
        fun createRoute(year: Int, month: Int): String = "monthly_report/$year/$month"
    }
    object AnnualReport : Screen("annual_report", "年度报告", Icons.Default.Home)
    object Health : Screen("health", "健康数据", Icons.Default.Home)
    object DiaryMap : Screen("diary_map", "日记地图", Icons.Default.Home)
    object Biography : Screen("biography", "AI 传记", Icons.Default.Home)
}

data class BottomNavItem(
    val screen: Screen,
    val badgeCount: Int = 0,
    val showBadge: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home),
    BottomNavItem(Screen.Timeline),
    BottomNavItem(Screen.Todo),
    BottomNavItem(Screen.Profile)
)

@Composable
fun DiaryNavHost(navigateTo: String? = null, onNavigateHandled: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as DiaryApplication
    val navController = rememberNavController()
    val haptic = rememberHapticFeedback()
    val experimentalFeatures by app.experimentalFeatures.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    fun navigateToBottomRoute(route: String) {
        haptic.click()
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val rootSwipeModifier = if (showBottomBar && currentRoute != Screen.Todo.route) {
        Modifier.pointerInput(currentRoute, experimentalFeatures.mainScreenSwipeEnabled) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    val targetRoute = resolveMainScreenSwipeTarget(
                        currentRoute = currentRoute,
                        totalDrag = totalDrag,
                        enabled = experimentalFeatures.mainScreenSwipeEnabled
                    )
                    if (targetRoute != null) {
                        navigateToBottomRoute(targetRoute)
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (showBottomBar) {
                DiaryBottomNavigationBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = ::navigateToBottomRoute
                )
            }
        }
    ) { innerPadding ->
        LaunchedEffect(navigateTo) {
            if (navigateTo == "editor") {
                navController.navigate(Screen.Editor.createRoute())
                onNavigateHandled()
            } else if (navigateTo == "todo") {
                navigateToBottomRoute(Screen.Todo.route)
                onNavigateHandled()
            }
        }

        val navHostModifier = if (showBottomBar) {
            Modifier.padding(innerPadding)
        } else {
            Modifier.padding(top = innerPadding.calculateTopPadding())
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = navHostModifier.then(rootSwipeModifier)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) },
                    onNavigateToReview = { navController.navigate(Screen.Review.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToCountDown = { navController.navigate(Screen.CountDown.route) },
                    onNavigateToTimeline = { navController.navigate(Screen.Timeline.route) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToMediaLibrary = { navController.navigate(Screen.MediaLibrary.route) },
                    onNavigateToExperimentalFeatures = { navController.navigate(Screen.ExperimentalFeatures.route) },
                    onNavigateToTimeCapsule = { navController.navigate(Screen.TimeCapsule.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToAiAssistant = { navController.navigate(Screen.AiAssistant.route) },
                    onNavigateToHealth = { navController.navigate(Screen.Health.route) },
                    onNavigateToDiaryMap = { navController.navigate(Screen.DiaryMap.route) },
                    onNavigateToBiography = { navController.navigate(Screen.Biography.route) }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(Screen.Todo.route) {
                TodoScreen(
                    onMainScreenSwipe = { dragAmount ->
                        val targetRoute = resolveMainScreenSwipeTarget(
                            currentRoute = Screen.Todo.route,
                            totalDrag = dragAmount,
                            enabled = experimentalFeatures.mainScreenSwipeEnabled
                        )
                        if (targetRoute != null) {
                            navigateToBottomRoute(targetRoute)
                        }
                    }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigateToHealth = { navController.navigate(Screen.Health.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
                )
            }

            composable(Screen.Changelog.route) {
                ChangelogScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.TagManagement.route) {
                TagManagementScreen(onNavigateBack = { navController.popBackStack() })
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
                BackupScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(Screen.MediaLibrary.route) {
                MediaLibraryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(Screen.Trash.route) {
                TrashScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(Screen.CountDown.route) {
                CountDownScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Notifications.route) {
                NotificationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCapsule = { capsuleId -> navController.navigate(Screen.ReadCapsule.createRoute(capsuleId)) },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToMonthlyReport = { year, month -> navController.navigate(Screen.MonthlyReport.createRoute(year, month)) },
                    onNavigateToAnnualReport = { navController.navigate(Screen.AnnualReport.route) }
                )
            }
            composable(Screen.AiAssistant.route) {
                val assistantViewModel: com.diary.app.ai.AiAssistantViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel()
                com.diary.app.ui.assistant.AiAssistantScreen(
                    viewModel = assistantViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.TimeCapsule.route) {
                TimeCapsuleScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(Screen.CreateCapsule.route) },
                    onNavigateToRead = { capsuleId -> navController.navigate(Screen.ReadCapsule.createRoute(capsuleId)) }
                )
            }
            composable(Screen.CreateCapsule.route) {
                val capsuleViewModel: com.diary.app.ui.capsule.TimeCapsuleViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel()
                CreateCapsuleScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCreateCapsule = { title, content, unlockDate, theme, imageUri, unlockHour, unlockMinute ->
                        capsuleViewModel.createCapsule(title, content, unlockDate, theme, imageUri, unlockHour, unlockMinute)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.ReadCapsule.route,
                arguments = listOf(navArgument("capsuleId") { type = NavType.LongType })
            ) { backStackEntry ->
                val capsuleId = backStackEntry.arguments?.getLong("capsuleId") ?: -1L
                val capsuleViewModel: com.diary.app.ui.capsule.TimeCapsuleViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel()
                ReadCapsuleScreen(
                    capsuleId = capsuleId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = capsuleViewModel
                )
            }
            composable(Screen.ExperimentalFeatures.route) {
                ExperimentalFeaturesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.MonthlyReport.route,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 2026
                val month = backStackEntry.arguments?.getInt("month") ?: 1
                MonthlyReportScreen(
                    year = year,
                    month = month,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AnnualReport.route) {
                AnnualReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Health.route) {
                HealthScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DiaryMap.route) {
                DiaryMapScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(Screen.Biography.route) {
                BiographyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("diaryId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("draftId") { type = NavType.StringType; defaultValue = "" }
                ),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: -1L
                val draftId = backStackEntry.arguments?.getString("draftId") ?: ""
                EditorScreen(
                    diaryId = if (diaryId == -1L) null else diaryId,
                    draftId = draftId.ifBlank { null },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("diaryId") { type = NavType.LongType }),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) { backStackEntry ->
                val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: -1L
                DiaryDetailScreen(
                    diaryId = diaryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditor = { id -> navController.navigate(Screen.Editor.createRoute(id)) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }
        }
    }
}

@Composable
private fun DiaryBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Surface(
        color = surfaceColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
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
        targetValue = if (isSelected) 1.1f else 1.0f,
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
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )

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

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.screen.title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
