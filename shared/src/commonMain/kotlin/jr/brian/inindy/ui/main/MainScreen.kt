package jr.brian.inindy.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.navigation.RootRoutes
import jr.brian.inindy.presentation.explore.ExploreViewModel
import jr.brian.inindy.presentation.notifications.NotificationsViewModel
import jr.brian.inindy.ui.explore.ExploreScreen
import jr.brian.inindy.ui.me.MeScreen
import jr.brian.inindy.ui.motion.LocalReducedMotion
import jr.brian.inindy.ui.motion.Motion
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
    tabNavController: NavHostController = rememberNavController(),
    exploreViewModel: ExploreViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    exploreRefreshTrigger: Int = 0,
    meRefreshTrigger: Int = 0
) {
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedIndex = when (currentRoute) {
        ROUTE_TAB_EXPLORE -> 0
        ROUTE_TAB_ME -> 1
        else -> 1
    }

    val exploreState by exploreViewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotion.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    val route = when (index) {
                        0 -> ROUTE_TAB_EXPLORE
                        else -> ROUTE_TAB_ME
                    }
                    if (route == currentRoute) return@BottomNavBar
                    tabNavController.navigate(route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCreateClick = {
                    val groupId = (exploreState.activeFilter as? ExploreFilter.Group)?.groupId
                    rootNavController.navigate(RootRoutes.createPost(groupId))
                }
            )
        }
    ) { padding ->
        // Tabs cross-fade — Explore ↔ Me have no spatial relationship, so a
        // directional slide would imply an ordering that isn't real.
        NavHost(
            navController = tabNavController,
            startDestination = ROUTE_TAB_EXPLORE,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = { Motion.fadeEnter(reducedMotion) },
            exitTransition = { Motion.fadeExit(reducedMotion) },
            popEnterTransition = { Motion.fadeEnter(reducedMotion) },
            popExitTransition = { Motion.fadeExit(reducedMotion) }
        ) {
            composable(ROUTE_TAB_EXPLORE) {
                val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
                val listState = rememberLazyListState()
                ExploreScreen(
                    uiState = exploreState,
                    onIntent = exploreViewModel::onIntent,
                    onRefresh = exploreViewModel::loadPosts,
                    onRsvpClick = { postId ->
                        rootNavController.navigate(RootRoutes.postDetail(postId))
                    },
                    isRsvpd = exploreViewModel::isRsvpd,
                    isOwnPost = exploreViewModel::isOwnPost,
                    onSettingsClick = { rootNavController.navigate(RootRoutes.SETTINGS) },
                    onNotificationsClick = {
                        rootNavController.navigate(RootRoutes.NOTIFICATIONS)
                    },
                    unreadNotificationCount = notificationsState.unreadCount,
                    listState = listState,
                    refreshTrigger = exploreRefreshTrigger
                )
            }
            composable(ROUTE_TAB_ME) {
                MeScreen(
                    onCreatePostClick = { rootNavController.navigate(RootRoutes.createPost()) },
                    onCreateGroupClick = { rootNavController.navigate(RootRoutes.CREATE_GROUP) },
                    onPostClick = { postId ->
                        rootNavController.navigate(RootRoutes.postDetail(postId))
                    },
                    onGroupClick = { groupId ->
                        rootNavController.navigate(RootRoutes.groupManagement(groupId))
                    },
                    onSettingsClick = { rootNavController.navigate(RootRoutes.SETTINGS) },
                    refreshTrigger = meRefreshTrigger
                )
            }
        }
    }
}
