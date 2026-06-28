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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.diary.app.ui.favorites.FavoritesScreen
import com.diary.app.ui.home.HomeScreen
import com.diary.app.ui.media.MediaLibraryScreen
import com.diary.app.ui.profile.ProfileScreen
import com.diary.app.ui.profile.TagManagementScreen
import com.diary.app.ui.stats.StatsScreen
import com.diary.app.ui.timeline.TimelineScreen
import com.diary.app.ui.todo.TodoScreen
import com.diary.app.ui.trash.TrashScreen
import com.diary.app.ui.monthlyreport.MonthlyReportScreen
import com.diary.app.ui.annualreport.AnnualReportScreen
import com.diary.app.ui.map.DiaryMapScreen
import com.diary.app.ui.achievement.AchievementScreen
import com.diary.app.ui.achievement.AchievementDetailScreen
import com.diary.app.ui.achievement.AchievementViewModel
import com.diary.app.ui.biography.BiographyScreen
import com.diary.app.ui.writingcoach.WritingCoachScreen
import com.diary.app.ui.voicerecording.VoiceRecordingScreen
import com.diary.app.ui.diarysummary.DiarySummaryScreen
import com.diary.app.ui.smallwins.SmallWinsScreen
import com.diary.app.ui.quickcheckin.QuickCheckinScreen
import com.diary.app.ui.goals.GoalsScreen
import com.diary.app.ui.emotionradar.EmotionRadarScreen
import com.diary.app.ui.textmicroscope.TextMicroscopeScreen
import com.diary.app.ui.memoryanchors.MemoryAnchorsScreen
import com.diary.app.ui.writingfingerprint.WritingFingerprintScreen
import com.diary.app.ui.emotionforecast.EmotionForecastScreen
import com.diary.app.ui.relationships.RelationshipTrackingScreen
import com.diary.app.ui.decisions.DecisionAnalysisScreen
import com.diary.app.ui.values.ValuesExtractionScreen
import com.diary.app.ui.writinglab.WritingLabScreen
import com.diary.app.ui.eastereggs.EasterEggsScreen
import com.diary.app.ui.monthlychallenge.MonthlyChallengeScreen
import com.diary.app.ui.streakshield.StreakShieldScreen
import com.diary.app.ui.quietcompanion.QuietCompanionScreen
import com.diary.app.ui.ambienttheme.AmbientThemeScreen
import com.diary.app.ui.gentlenotification.GentleNotificationScreen
import com.diary.app.ui.outline.OutlineViewScreen
import com.diary.app.ui.tools.ToolsScreen
import com.diary.app.update.ChangelogScreen
import kotlinx.coroutines.launch

// Sub-page transition specs: smooth slide without bounce
private fun subPageEnterTransition() = slideInHorizontally(
    initialOffsetX = { it / 3 },
    animationSpec = tween(280)
) + fadeIn(animationSpec = tween(240))

private fun subPageExitTransition() = slideOutHorizontally(
    targetOffsetX = { -it / 4 },
    animationSpec = tween(280)
) + fadeOut(animationSpec = tween(200))

private fun subPagePopEnterTransition() = slideInHorizontally(
    initialOffsetX = { -it / 4 },
    animationSpec = tween(280)
) + fadeIn(animationSpec = tween(240))

private fun subPagePopExitTransition() = slideOutHorizontally(
    targetOffsetX = { it / 3 },
    animationSpec = tween(280)
) + fadeOut(animationSpec = tween(200))

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Timeline : Screen("timeline", "时间轴", Icons.Default.CalendarMonth)
    object Tools : Screen("tools", "工具", Icons.Default.Widgets)
    object Todo : Screen("todo", "待办", Icons.Default.CheckBox)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    object ExperimentalFeatures : Screen("experimental_features", "实验功能", Icons.Default.Science)
    object Editor : Screen("editor?diaryId={diaryId}&draftId={draftId}", "编辑日记", Icons.Default.Edit) {
        fun createRoute(diaryId: Long? = null, draftId: String? = null): String {
            val params = mutableListOf<String>()
            if (diaryId != null) params.add("diaryId=$diaryId")
            if (draftId != null) params.add("draftId=$draftId")
            return if (params.isEmpty()) "editor" else "editor?${params.joinToString("&")}"
        }
    }

    object Detail : Screen("detail/{diaryId}", "详情", Icons.Default.Article) {
        fun createRoute(diaryId: Long): String = "detail/$diaryId"
    }

    object Changelog : Screen("changelog", "更新日志", Icons.Default.History)
    object TagManagement : Screen("tag_management", "标签管理", Icons.Default.Label)
    object Backup : Screen("backup", "备份", Icons.Default.Backup)
    object Favorites : Screen("favorites", "收藏", Icons.Default.Favorite)
    object MediaLibrary : Screen("media_library", "媒体库", Icons.Default.Image)
    object Trash : Screen("trash", "回收站", Icons.Default.Delete)
    object CountDown : Screen("countdown", "倒数日", Icons.Default.Timer)
    object TimeCapsule : Screen("time_capsule", "时间胶囊", Icons.Default.Schedule)
    object CreateCapsule : Screen("create_capsule", "创建胶囊", Icons.Default.Schedule)
    object ReadCapsule : Screen("read_capsule/{capsuleId}", "阅读胶囊", Icons.Default.Schedule) {
        fun createRoute(capsuleId: Long): String = "read_capsule/$capsuleId"
    }
    object Notifications : Screen("notifications", "通知", Icons.Default.Notifications)
    object AiAssistant : Screen("ai_assistant", "AI 助手", Icons.Default.AutoAwesome)
    object AiManagement : Screen("ai_management", "AI 管理", Icons.Default.SmartToy)
    object MonthlyReport : Screen("monthly_report/{year}/{month}", "月度报告", Icons.Default.CalendarMonth) {
        fun createRoute(year: Int, month: Int): String = "monthly_report/$year/$month"
    }
    object AnnualReport : Screen("annual_report", "年度报告", Icons.Default.BarChart)
    object DiaryMap : Screen("diary_map", "日记地图", Icons.Default.LocationOn)
    object Biography : Screen("biography", "AI 传记", Icons.Default.AutoAwesome)
    object Achievements : Screen("achievements", "成就", Icons.Default.EmojiEvents)
    object AchievementDetail : Screen("achievement_detail/{key}", "成就详情", Icons.Default.EmojiEvents) {
        fun createRoute(key: String): String = "achievement_detail/$key"
    }
    object Storage : Screen("storage", "存储管理", Icons.Default.Memory)
    object WeatherDetail : Screen("weather_detail", "天气详情", Icons.Default.LocationOn)
    object SmallWins : Screen("small_wins", "小确幸", Icons.Default.Favorite)
    object QuickCheckin : Screen("quick_checkin", "快速签到", Icons.Default.Edit)
    object Goals : Screen("goals", "目标追踪", Icons.Default.BarChart)
    object EmotionRadar : Screen("emotion_radar", "情绪雷达", Icons.Default.BarChart)
    object TextMicroscope : Screen("text_microscope", "文字显微镜", Icons.Default.Article)
    object MemoryAnchors : Screen("memory_anchors", "记忆锚点", Icons.Default.Label)
    object WritingFingerprint : Screen("writing_fingerprint", "写作指纹", Icons.Default.Edit)
    object EmotionForecast : Screen("emotion_forecast", "情绪预报", Icons.Default.Home)
    object RelationshipTracking : Screen("relationship_tracking", "关系追踪", Icons.Default.Person)
    object DecisionAnalysis : Screen("decision_analysis", "决策追踪", Icons.Default.CheckBox)
    object ValuesExtraction : Screen("values_extraction", "价值观", Icons.Default.Favorite)
    object WritingLab : Screen("writing_lab", "写作实验室", Icons.Default.Science)
    object WritingCoach : Screen("writing_coach", "写作教练", Icons.Default.AutoAwesome)
    object VoiceRecording : Screen("voice_recording", "语音备忘录", Icons.Default.Mic)
    object EasterEggs : Screen("easter_eggs", "隐藏彩蛋", Icons.Default.EmojiEvents)
    object MonthlyChallenge : Screen("monthly_challenge", "月度挑战", Icons.Default.CalendarMonth)
    object StreakShield : Screen("streak_shield", "连续保护罩", Icons.Default.EmojiEvents)
    object QuietCompanion : Screen("quiet_companion", "安静陪伴", Icons.Default.Home)
    object AmbientTheme : Screen("ambient_theme", "环境感知主题", Icons.Default.Image)
    object GentleNotification : Screen("gentle_notification", "温柔通知", Icons.Default.Notifications)
    object OutlineView : Screen("outline_view", "大纲视图", Icons.Default.Article)
}

data class BottomNavItem(
    val screen: Screen,
    val badgeCount: Int = 0,
    val showBadge: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home),
    BottomNavItem(Screen.Timeline),
    BottomNavItem(Screen.Tools),
    BottomNavItem(Screen.Todo),
    BottomNavItem(Screen.Profile)
)

@Composable
fun DiaryNavHost(navigateTo: String? = null, onNavigateHandled: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as? DiaryApplication ?: return
    val experimentalFeatures by app.experimentalFeatures.collectAsState()
    val navController = rememberNavController()
    val haptic = rememberHapticFeedback()
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            modifier = navHostModifier,
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                val fromIdx = bottomNavItems.indexOfFirst { it.screen.route == from }
                val toIdx = bottomNavItems.indexOfFirst { it.screen.route == to }
                if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                    val direction = if (toIdx > fromIdx) 1 else -1
                    slideInHorizontally(tween(280)) { direction * it / 3 } +
                        fadeIn(tween(200))
                } else {
                    fadeIn(animationSpec = tween(250))
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                val fromIdx = bottomNavItems.indexOfFirst { it.screen.route == from }
                val toIdx = bottomNavItems.indexOfFirst { it.screen.route == to }
                if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                    val direction = if (toIdx > fromIdx) -1 else 1
                    slideOutHorizontally(tween(280)) { direction * it / 4 } +
                        fadeOut(tween(200))
                } else {
                    fadeOut(animationSpec = tween(200))
                }
            },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToEditor = { diaryId -> navController.navigate(Screen.Editor.createRoute(diaryId)) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToTimeline = { query ->
                        if (!query.isNullOrBlank()) {
                            navController.currentBackStackEntry?.savedStateHandle?.set("timeline_query", query)
                        }
                        navController.navigate(Screen.Timeline.route)
                    },
                    onNavigateToTodo = { navigateToBottomRoute(Screen.Todo.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToAiAssistant = { navController.navigate(Screen.AiAssistant.route) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToCountDown = { navController.navigate(Screen.CountDown.route) },
                    onNavigateToTimeCapsule = { navController.navigate(Screen.TimeCapsule.route) },
                    onNavigateToMediaLibrary = { navController.navigate(Screen.MediaLibrary.route) },
                    onNavigateToDiaryMap = { navController.navigate(Screen.DiaryMap.route) },
                    onNavigateToBiography = { navController.navigate(Screen.Biography.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                    onNavigateToWeatherDetail = { navController.navigate(Screen.WeatherDetail.route) },
                    onMainScreenSwipe = { dragAmount ->
                        val targetRoute = resolveMainScreenSwipeTarget(
                            currentRoute = Screen.Home.route,
                            totalDrag = dragAmount,
                            enabled = experimentalFeatures.mainScreenSwipeEnabled
                        )
                        if (targetRoute != null) {
                            navigateToBottomRoute(targetRoute)
                        }
                    }
                )
            }
            composable(Screen.Timeline.route) {
                val initialQuery = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("timeline_query")
                TimelineScreen(
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    initialSearchQuery = initialQuery,
                    onMainScreenSwipe = { dragAmount ->
                        val targetRoute = resolveMainScreenSwipeTarget(
                            currentRoute = Screen.Timeline.route,
                            totalDrag = dragAmount,
                            enabled = experimentalFeatures.mainScreenSwipeEnabled
                        )
                        if (targetRoute != null) {
                            navigateToBottomRoute(targetRoute)
                        }
                    }
                )
            }
            composable(Screen.Tools.route) {
                val toolsScope = rememberCoroutineScope()
                ToolsScreen(
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToMediaLibrary = { navController.navigate(Screen.MediaLibrary.route) },
                    onNavigateToCountDown = { navController.navigate(Screen.CountDown.route) },
                    onNavigateToTimeCapsule = { navController.navigate(Screen.TimeCapsule.route) },
                    onNavigateToRandom = {
                        toolsScope.launch {
                            val dao = (app as? com.diary.app.DiaryApplication)?.database?.diaryDao()
                            val randomId = dao?.getRandomEntryId()
                            if (randomId != null) {
                                navController.navigate(Screen.Detail.createRoute(randomId))
                            }
                        }
                    },
                    onNavigateToDiaryMap = { navController.navigate(Screen.DiaryMap.route) },
                    onNavigateToBiography = { navController.navigate(Screen.Biography.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) },
                    onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                    onNavigateToExperimental = { navController.navigate(Screen.ExperimentalFeatures.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToAiAssistant = { navController.navigate(Screen.AiAssistant.route) },
                    onNavigateToAiManagement = { navController.navigate(Screen.AiManagement.route) },
                    onNavigateToSmallWins = { navController.navigate(Screen.SmallWins.route) },
                    onNavigateToQuickCheckin = { navController.navigate(Screen.QuickCheckin.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToWritingCoach = { navController.navigate(Screen.WritingCoach.route) },
                    onNavigateToVoiceRecording = { navController.navigate(Screen.VoiceRecording.route) },
                    onMainScreenSwipe = { dragAmount ->
                        val targetRoute = resolveMainScreenSwipeTarget(
                            currentRoute = Screen.Tools.route,
                            totalDrag = dragAmount,
                            enabled = experimentalFeatures.mainScreenSwipeEnabled
                        )
                        if (targetRoute != null) {
                            navigateToBottomRoute(targetRoute)
                        }
                    }
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
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToMonthlyReport = {
                        val now = java.time.LocalDate.now()
                        navController.navigate(Screen.MonthlyReport.createRoute(now.year, now.monthValue))
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToChangelog = { navController.navigate(Screen.Changelog.route) },
                    onNavigateToTagManagement = { navController.navigate(Screen.TagManagement.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onMainScreenSwipe = { dragAmount ->
                        val targetRoute = resolveMainScreenSwipeTarget(
                            currentRoute = Screen.Profile.route,
                            totalDrag = dragAmount,
                            enabled = experimentalFeatures.mainScreenSwipeEnabled
                        )
                        if (targetRoute != null) {
                            navigateToBottomRoute(targetRoute)
                        }
                    }
                )
            }

            composable(
                Screen.Changelog.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                ChangelogScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                Screen.TagManagement.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                TagManagementScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                Screen.Backup.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                BackupScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                Screen.Favorites.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                FavoritesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(
                Screen.MediaLibrary.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                MediaLibraryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(
                Screen.Trash.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                TrashScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(
                Screen.CountDown.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                CountDownScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                Screen.Notifications.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                NotificationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCapsule = { capsuleId -> navController.navigate(Screen.ReadCapsule.createRoute(capsuleId)) },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
                    onNavigateToMonthlyReport = { year, month -> navController.navigate(Screen.MonthlyReport.createRoute(year, month)) },
                    onNavigateToAnnualReport = { navController.navigate(Screen.AnnualReport.route) }
                )
            }
            composable(
                Screen.AiAssistant.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                val assistantViewModel: com.diary.app.ai.AiAssistantViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel()
                com.diary.app.ui.assistant.AiAssistantScreen(
                    viewModel = assistantViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.AiManagement.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                val app = LocalContext.current.applicationContext as? com.diary.app.DiaryApplication
                com.diary.app.ui.tools.AiManagementScreen(
                    aiService = app!!.aiService,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.TimeCapsule.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                TimeCapsuleScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(Screen.CreateCapsule.route) },
                    onNavigateToRead = { capsuleId -> navController.navigate(Screen.ReadCapsule.createRoute(capsuleId)) }
                )
            }
            composable(
                Screen.CreateCapsule.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
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
                arguments = listOf(navArgument("capsuleId") { type = NavType.LongType }),
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
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
            composable(
                Screen.ExperimentalFeatures.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                ExperimentalFeaturesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.MonthlyReport.route,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                ),
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: java.time.LocalDate.now().year
                val month = backStackEntry.arguments?.getInt("month") ?: 1
                MonthlyReportScreen(
                    year = year,
                    month = month,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.AnnualReport.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                AnnualReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.DiaryMap.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                DiaryMapScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) }
                )
            }
            composable(
                Screen.Biography.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                BiographyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Achievements.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                val achievementViewModel: AchievementViewModel = viewModel()
                AchievementScreen(
                    viewModel = achievementViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { key ->
                        navController.navigate(Screen.AchievementDetail.createRoute(key))
                    }
                )
            }

            composable(
                Screen.AchievementDetail.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: return@composable
                val achievementViewModel: AchievementViewModel = viewModel()
                AchievementDetailScreen(
                    achievementKey = key,
                    viewModel = achievementViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Storage.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                com.diary.app.ui.storage.StorageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.WeatherDetail.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                com.diary.app.ui.home.WeatherDetailScreen(
                    onBack = { navController.popBackStack() }
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
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(240))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(240))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(280)
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
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(240))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(240))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(280)
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

            composable(
                route = Screen.SmallWins.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                SmallWinsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.QuickCheckin.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                QuickCheckinScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Goals.route,
                enterTransition = { subPageEnterTransition() },
                exitTransition = { subPageExitTransition() },
                popEnterTransition = { subPagePopEnterTransition() },
                popExitTransition = { subPagePopExitTransition() }
            ) {
                GoalsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EmotionRadar.route) { EmotionRadarScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.TextMicroscope.route) { TextMicroscopeScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.MemoryAnchors.route) { MemoryAnchorsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.WritingFingerprint.route) { WritingFingerprintScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.EmotionForecast.route) { EmotionForecastScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.RelationshipTracking.route) { RelationshipTrackingScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.DecisionAnalysis.route) { DecisionAnalysisScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.ValuesExtraction.route) { ValuesExtractionScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.WritingLab.route) { WritingLabScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.WritingCoach.route) { WritingCoachScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.VoiceRecording.route) { VoiceRecordingScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.EasterEggs.route) { EasterEggsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.MonthlyChallenge.route) { MonthlyChallengeScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.StreakShield.route) { StreakShieldScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.QuietCompanion.route) { QuietCompanionScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.AmbientTheme.route) { AmbientThemeScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.GentleNotification.route) { GentleNotificationScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.OutlineView.route) { OutlineViewScreen(onNavigateBack = { navController.popBackStack() }) }
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
