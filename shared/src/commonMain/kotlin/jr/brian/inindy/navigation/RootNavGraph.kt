package jr.brian.inindy.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import jr.brian.inindy.domain.model.isOnboardingComplete
import jr.brian.inindy.presentation.app.AppDestination
import jr.brian.inindy.presentation.app.AppViewModel
import jr.brian.inindy.ui.auth.AuthNavHost
import jr.brian.inindy.ui.creategroup.CreateGroupScreen
import jr.brian.inindy.ui.createpost.CreatePostScreen
import jr.brian.inindy.ui.explore.PostDetailScreen
import jr.brian.inindy.ui.main.MainScreen
import jr.brian.inindy.ui.me.GroupManagementScreen
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
    const val CREATE_POST = "create_post"
    const val CREATE_GROUP = "create_group"
    const val SETTINGS = "settings"
    const val POST_DETAIL = "post_detail/{postId}"
    const val GROUP_MANAGEMENT = "group_management/{groupId}"

    fun postDetail(postId: String) = "post_detail/$postId"
    fun groupManagement(groupId: String) = "group_management/$groupId"
}

@Composable
fun RootNavGraph(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    appViewModel: AppViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    var exploreRefreshTrigger by remember { mutableIntStateOf(0) }

    if (state.isLoading) {
        SplashScreen()
        return
    }

    val startDestination = when (state.destination) {
        AppDestination.Auth -> RootRoutes.AUTH_GRAPH
        AppDestination.Onboarding -> RootRoutes.ONBOARDING_GRAPH
        AppDestination.Main -> RootRoutes.MAIN_GRAPH
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
                    exploreRefreshTrigger = exploreRefreshTrigger
                )
            }
            composable(RootRoutes.CREATE_POST) {
                CreatePostScreen(
                    onClose = { navController.popBackStack() },
                    onSubmitted = {
                        exploreRefreshTrigger++
                        navController.popBackStack()
                    }
                )
            }
            composable(RootRoutes.CREATE_GROUP) {
                CreateGroupScreen(
                    onClose = { navController.popBackStack() },
                    onCreated = { newGroupId ->
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
            composable(RootRoutes.POST_DETAIL) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.popBackStack() },
                    allowHostActions = true
                )
            }
            composable(RootRoutes.GROUP_MANAGEMENT) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()
                GroupManagementScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onPostClick = { postId ->
                        navController.navigate(RootRoutes.postDetail(postId))
                    }
                )
            }
        }
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
