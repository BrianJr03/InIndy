package jr.brian.inindy.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import jr.brian.inindy.ui.motion.LocalReducedMotion
import jr.brian.inindy.ui.motion.Motion
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
    val pendingPostId by appViewModel.pendingPostId.collectAsStateWithLifecycle()
    var exploreRefreshTrigger by remember { mutableIntStateOf(0) }
    var meRefreshTrigger by remember { mutableIntStateOf(0) }
    // Bumped when returning from EDIT_POST so PostDetailScreen re-fetches the
    // post it's showing instead of holding the stale pre-edit copy.
    var postDetailRefreshTrigger by remember { mutableIntStateOf(0) }
    // Bumped when returning from GROUP_CHAT so GroupManagementScreen re-fetches
    // the unread count (which the chat itself just cleared).
    var groupManagementRefreshTrigger by remember { mutableIntStateOf(0) }

    // Captured once here because nav-compose transition lambdas are not
    // @Composable and can't call LocalReducedMotion.current themselves.
    val reducedMotion = LocalReducedMotion.current

    // AnimatedContent (not the old `return`) lets the splash dissolve into the
    // NavHost. hasResolved only flips true once and never flips back — even if
    // the auth repository re-emits Initializing on resume, this branch stays on
    // the NavHost side and the mounted screens are preserved.
    AnimatedContent(
        targetState = state.hasResolved,
        transitionSpec = {
            fadeIn(tween(Motion.Duration.Emphasized, easing = Motion.Standard)) togetherWith
                fadeOut(tween(Motion.Duration.Fast, easing = Motion.Standard))
        },
        contentKey = { it },
        label = "root-splash-nav"
    ) { hasResolved ->
        if (!hasResolved) {
            SplashScreen()
        } else {
            val startDestination = when (state.destination) {
                AppDestination.Auth -> RootRoutes.AUTH_GRAPH
                AppDestination.Onboarding -> RootRoutes.ONBOARDING_GRAPH
                AppDestination.Main -> RootRoutes.MAIN_GRAPH
            }

            // Reactive redirect: when the session ends (e.g. sign-out, account
            // deletion) AppViewModel flips destination to Auth. If we're
            // currently anywhere other than the auth graph, blow away the back
            // stack and go to auth. Guarded on currentBackStackEntry != null so
            // the initial cold-start composition (where NavHost has just been
            // set up at startDestination) doesn't double-navigate.
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

            // Default transitions across the NavHost are drill-down "push".
            // Individual composables override to modal (create/edit) or fade
            // (graph roots — the swap between auth/onboarding/main has no
            // spatial relationship, so a horizontal slide would lie).
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { Motion.pushEnter(reducedMotion) },
                exitTransition = { Motion.pushExit(reducedMotion) },
                popEnterTransition = { Motion.popEnter(reducedMotion) },
                popExitTransition = { Motion.popExit(reducedMotion) }
            ) {
                navigation(
                    startDestination = RootRoutes.AUTH_ROOT,
                    route = RootRoutes.AUTH_GRAPH
                ) {
                    composable(
                        route = RootRoutes.AUTH_ROOT,
                        enterTransition = { Motion.fadeEnter(reducedMotion) },
                        exitTransition = { Motion.fadeExit(reducedMotion) },
                        popEnterTransition = { Motion.fadeEnter(reducedMotion) },
                        popExitTransition = { Motion.fadeExit(reducedMotion) }
                    ) {
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
                    composable(
                        route = RootRoutes.ONBOARDING_ROOT,
                        enterTransition = { Motion.fadeEnter(reducedMotion) },
                        exitTransition = { Motion.fadeExit(reducedMotion) },
                        popEnterTransition = { Motion.fadeEnter(reducedMotion) },
                        popExitTransition = { Motion.fadeExit(reducedMotion) }
                    ) {
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
                    composable(
                        route = RootRoutes.MAIN,
                        enterTransition = { Motion.fadeEnter(reducedMotion) },
                        exitTransition = { Motion.fadeExit(reducedMotion) },
                        popEnterTransition = { Motion.fadeEnter(reducedMotion) },
                        popExitTransition = { Motion.fadeExit(reducedMotion) }
                    ) {
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
                        ),
                        enterTransition = { Motion.modalEnter(reducedMotion) },
                        exitTransition = { Motion.modalExit(reducedMotion) },
                        popEnterTransition = { Motion.modalPopEnter(reducedMotion) },
                        popExitTransition = { Motion.modalPopExit(reducedMotion) }
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
                    composable(
                        route = RootRoutes.EDIT_POST,
                        enterTransition = { Motion.modalEnter(reducedMotion) },
                        exitTransition = { Motion.modalExit(reducedMotion) },
                        popEnterTransition = { Motion.modalPopEnter(reducedMotion) },
                        popExitTransition = { Motion.modalPopExit(reducedMotion) }
                    ) { backStackEntry ->
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
                    composable(
                        route = RootRoutes.CREATE_GROUP,
                        enterTransition = { Motion.modalEnter(reducedMotion) },
                        exitTransition = { Motion.modalExit(reducedMotion) },
                        popEnterTransition = { Motion.modalPopEnter(reducedMotion) },
                        popExitTransition = { Motion.modalPopExit(reducedMotion) }
                    ) {
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

    // Push-notification tap opens the target post. Queued on the bus until the
    // app finishes routing to Main (cold-start-from-push case: the session is
    // still Initializing when the tap-launched intent lands).
    LaunchedEffect(pendingPostId, state.destination) {
        val postId = pendingPostId
        if (postId != null && state.destination == AppDestination.Main) {
            appViewModel.consumePostId()
            navController.navigate(RootRoutes.postDetail(postId))
        }
    }
}

// Brand splash — solid primary background with the wordmark and a subtle
// indicator. Same visual language as the Android launch theme and iOS launch
// screen so the OS splash hands off to Compose without a color flicker.
@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SplashWordmark()
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SplashWordmark() {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = onPrimary, fontWeight = FontWeight.ExtraBold)) {
            append("In")
        }
        withStyle(
            SpanStyle(
                color = onPrimary.copy(alpha = 0.85f),
                fontWeight = FontWeight.ExtraBold
            )
        ) {
            append("Indy")
        }
    }
    Text(
        text = wordmark,
        style = MaterialTheme.typography.displaySmall.copy(letterSpacing = (-0.5).sp)
    )
}
