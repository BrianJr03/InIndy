package jr.brian.inindy.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import jr.brian.inindy.domain.model.isOnboardingComplete
import jr.brian.inindy.presentation.app.AppDestination
import jr.brian.inindy.presentation.app.AppViewModel
import jr.brian.inindy.ui.auth.AuthNavHost
import jr.brian.inindy.ui.chat.GroupChatScreen
import jr.brian.inindy.ui.creategroup.CreateGroupScreen
import jr.brian.inindy.ui.createpost.CreatePostScreen
import jr.brian.inindy.ui.explore.PostDetailScreen
import jr.brian.inindy.ui.main.MainScreen
import jr.brian.inindy.ui.me.GroupInviteSheet
import jr.brian.inindy.ui.me.GroupManagementScreen
import jr.brian.inindy.ui.notifications.NotificationsScreen
import jr.brian.inindy.ui.onboarding.OnboardingNavHost
import jr.brian.inindy.ui.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel

object RootRoutes {
    const val AUTH_GRAPH = "auth_graph"
    const val AUTH_ROOT = "auth_root"
    const val ONBOARDING_GRAPH = "onboarding_graph"
    const val ONBOARDING_ROOT = "onboarding_root"
    const val MAIN_GRAPH = "main_graph"
    const val MAIN = "main"
    // Route pattern with an optional query param — callers should use
    // createPost(groupId) rather than navigating to this string directly.
    const val CREATE_POST = "create_post?groupId={groupId}"
    const val CREATE_GROUP = "create_group"
    const val EDIT_POST = "edit_post/{postId}"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val POST_DETAIL = "post_detail/{postId}"
    const val GROUP_MANAGEMENT = "group_management/{groupId}"
    const val GROUP_CHAT = "group_chat/{groupId}"

    fun postDetail(postId: String) = "post_detail/$postId"
    fun editPost(postId: String) = "edit_post/$postId"
    fun groupManagement(groupId: String) = "group_management/$groupId"
    fun groupChat(groupId: String) = "group_chat/$groupId"
    // The optional query param can be omitted entirely; the composable's
    // nullable arg + defaultValue = null makes both forms match the same route.
    fun createPost(groupId: String? = null): String =
        if (groupId == null) "create_post" else "create_post?groupId=$groupId"
}

@Composable
fun RootNavGraph(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    appViewModel: AppViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val pendingInviteToken by appViewModel.pendingInviteToken.collectAsStateWithLifecycle()
    var exploreRefreshTrigger by remember { mutableIntStateOf(0) }
    var meRefreshTrigger by remember { mutableIntStateOf(0) }
    // Bumped when returning from EDIT_POST so PostDetailScreen re-fetches the
    // post it's showing instead of holding the stale pre-edit copy.
    var postDetailRefreshTrigger by remember { mutableIntStateOf(0) }
    // Bumped when returning from GROUP_CHAT so GroupManagementScreen re-fetches
    // the unread count (which the chat itself just cleared).
    var groupManagementRefreshTrigger by remember { mutableIntStateOf(0) }

    if (state.isLoading) {
        SplashScreen()
        return
    }

    val startDestination = when (state.destination) {
        AppDestination.Auth -> RootRoutes.AUTH_GRAPH
        AppDestination.Onboarding -> RootRoutes.ONBOARDING_GRAPH
        AppDestination.Main -> RootRoutes.MAIN_GRAPH
    }

    // Reactive redirect: when the session ends (e.g. sign-out, account deletion)
    // AppViewModel flips destination to Auth. If we're currently anywhere other
    // than the auth graph, blow away the back stack and go to auth. Guarded on
    // currentBackStackEntry != null so the initial cold-start composition
    // (where NavHost has just been set up at startDestination) doesn't
    // double-navigate.
    LaunchedEffect(state.destination) {
        if (state.destination != AppDestination.Auth) return@LaunchedEffect
        val currentGraph = navController.currentBackStackEntry?.destination?.parent?.route
        if (currentGraph != null && currentGraph != RootRoutes.AUTH_GRAPH) {
            navController.navigate(RootRoutes.AUTH_GRAPH) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        navigation(
            startDestination = RootRoutes.AUTH_ROOT,
            route = RootRoutes.AUTH_GRAPH
        ) {
            composable(RootRoutes.AUTH_ROOT) {
                AuthNavHost(
                    onAuthenticated = { user ->
                        val target = if (user.isOnboardingComplete) {
                            RootRoutes.MAIN_GRAPH
                        } else {
                            RootRoutes.ONBOARDING_GRAPH
                        }
                        navController.navigate(target) {
                            popUpTo(RootRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    }
                )
            }
        }

        navigation(
            startDestination = RootRoutes.ONBOARDING_ROOT,
            route = RootRoutes.ONBOARDING_GRAPH
        ) {
            composable(RootRoutes.ONBOARDING_ROOT) {
                OnboardingNavHost(
                    onComplete = {
                        navController.navigate(RootRoutes.MAIN_GRAPH) {
                            popUpTo(RootRoutes.ONBOARDING_GRAPH) { inclusive = true }
                        }
                    }
                )
            }
        }

        navigation(
            startDestination = RootRoutes.MAIN,
            route = RootRoutes.MAIN_GRAPH
        ) {
            composable(RootRoutes.MAIN) {
                MainScreen(
                    rootNavController = navController,
                    exploreRefreshTrigger = exploreRefreshTrigger,
                    meRefreshTrigger = meRefreshTrigger
                )
            }
            composable(
                route = RootRoutes.CREATE_POST,
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                // getString(...) throws when the key is missing — the case
                // where the FAB was tapped without a group filter. Use the
                // Nullable-suffixed getter so the "no group" navigation path
                // just yields null.
                val initialGroupId = backStackEntry.arguments?.read {
                    getStringOrNull("groupId")
                }
                CreatePostScreen(
                    onClose = { navController.popBackStack() },
                    onSubmitted = {
                        exploreRefreshTrigger++
                        meRefreshTrigger++
                        navController.popBackStack()
                    },
                    initialGroupId = initialGroupId
                )
            }
            composable(RootRoutes.EDIT_POST) { backStackEntry ->
                val postId = backStackEntry.arguments?.read { getString("postId") }.orEmpty()
                CreatePostScreen(
                    onClose = { navController.popBackStack() },
                    onSubmitted = {
                        exploreRefreshTrigger++
                        meRefreshTrigger++
                        postDetailRefreshTrigger++
                        navController.popBackStack()
                    },
                    postId = postId
                )
            }
            composable(RootRoutes.CREATE_GROUP) {
                CreateGroupScreen(
                    onClose = { navController.popBackStack() },
                    onCreated = { newGroupId ->
                        meRefreshTrigger++
                        navController.popBackStack()
                        navController.navigate(RootRoutes.groupManagement(newGroupId))
                    }
                )
            }
            composable(RootRoutes.SETTINGS) {
                SettingsScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(RootRoutes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onNotificationClick = { notification ->
                        val postId = notification.postId
                        if (postId != null) {
                            navController.navigate(RootRoutes.postDetail(postId))
                        }
                    }
                )
            }
            composable(RootRoutes.POST_DETAIL) { backStackEntry ->
                val postId = backStackEntry.arguments?.read { getString("postId") }.orEmpty()
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onEdit = { editPostId ->
                        navController.navigate(RootRoutes.editPost(editPostId))
                    },
                    allowHostActions = true,
                    refreshTrigger = postDetailRefreshTrigger
                )
            }
            composable(RootRoutes.GROUP_MANAGEMENT) { backStackEntry ->
                val groupId = backStackEntry.arguments?.read { getString("groupId") }.orEmpty()
                GroupManagementScreen(
                    groupId = groupId,
                    onBack = {
                        meRefreshTrigger++
                        navController.popBackStack()
                    },
                    onPostClick = { postId ->
                        navController.navigate(RootRoutes.postDetail(postId))
                    },
                    onChatClick = { navController.navigate(RootRoutes.groupChat(groupId)) },
                    refreshTrigger = groupManagementRefreshTrigger
                )
            }
            composable(RootRoutes.GROUP_CHAT) { backStackEntry ->
                val groupId = backStackEntry.arguments?.read { getString("groupId") }.orEmpty()
                GroupChatScreen(
                    groupId = groupId,
                    onBack = {
                        groupManagementRefreshTrigger++
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    val activeInviteToken = pendingInviteToken
    if (activeInviteToken != null && state.destination == AppDestination.Main) {
        GroupInviteSheet(
            token = activeInviteToken,
            onDismiss = { appViewModel.consumeInviteToken() },
            onJoined = { group ->
                appViewModel.consumeInviteToken()
                meRefreshTrigger++
                navController.navigate(RootRoutes.groupManagement(group.id))
            }
        )
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
