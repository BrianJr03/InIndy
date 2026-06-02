package jr.brian.inindy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import jr.brian.inindy.presentation.explore.ExploreViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.home_placeholder_events
import jr.brian.inindy.resources.nav_me
import jr.brian.inindy.resources.nav_events
import jr.brian.inindy.resources.nav_explore
import jr.brian.inindy.ui.creategroup.CreateGroupScreen
import jr.brian.inindy.ui.createpost.CreatePostScreen
import jr.brian.inindy.ui.explore.ExploreScreen
import jr.brian.inindy.ui.explore.PostDetailScreen
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.PersonIcon
import jr.brian.inindy.ui.icons.SearchIcon
import jr.brian.inindy.ui.me.GroupManagementScreen
import jr.brian.inindy.ui.me.MeScreen
import jr.brian.inindy.ui.settings.SettingsScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class HomeTab { ME, EXPLORE, EVENTS }

@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    exploreViewModel: ExploreViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(HomeTab.EXPLORE) }
    var settingsOpen by remember { mutableStateOf(false) }
    var detailPostId by remember { mutableStateOf<String?>(null) }
    var detailFromMe by remember { mutableStateOf(false) }
    var createPostOpen by remember { mutableStateOf(false) }
    var createGroupOpen by remember { mutableStateOf(false) }
    var managedGroupId by remember { mutableStateOf<String?>(null) }
    val exploreUiState by exploreViewModel.uiState.collectAsState()
    val exploreListState = rememberLazyListState()

    if (settingsOpen) {
        ScopedScreen {
            SettingsScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onBack = { settingsOpen = false }
            )
        }
        return
    }

    if (createPostOpen) {
        ScopedScreen {
            CreatePostScreen(
                onClose = { createPostOpen = false },
                onSubmitted = { createPostOpen = false }
            )
        }
        return
    }

    if (createGroupOpen) {
        ScopedScreen {
            CreateGroupScreen(
                onClose = { createGroupOpen = false },
                onCreated = { newGroupId ->
                    createGroupOpen = false
                    managedGroupId = newGroupId
                }
            )
        }
        return
    }

    val groupId = managedGroupId
    if (groupId != null) {
        ScopedScreen {
            GroupManagementScreen(
                groupId = groupId,
                onBack = { managedGroupId = null },
                onPostClick = { postId ->
                    detailFromMe = true
                    detailPostId = postId
                }
            )
        }
        return
    }

    val activePostId = detailPostId
    if (activePostId != null) {
        ScopedScreen {
            PostDetailScreen(
                postId = activePostId,
                onBack = { detailPostId = null },
                onEdit = { detailPostId = null },
                allowHostActions = detailFromMe
            )
        }
        return
    }

    Scaffold(
        bottomBar = {
            HomeBottomNavBar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.ME -> MeScreen(
                onCreatePostClick = { createPostOpen = true },
                onCreateGroupClick = { createGroupOpen = true },
                onPostClick = { postId ->
                    detailFromMe = true
                    detailPostId = postId
                },
                onGroupClick = { id -> managedGroupId = id },
                onSettingsClick = { settingsOpen = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            HomeTab.EXPLORE -> ExploreScreen(
                uiState = exploreUiState,
                onIntent = exploreViewModel::onIntent,
                onRefresh = exploreViewModel::loadPosts,
                onRsvpClick = { postId ->
                    detailFromMe = false
                    detailPostId = postId
                },
                isRsvpd = exploreViewModel::isRsvpd,
                onSettingsClick = { settingsOpen = true },
                listState = exploreListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            HomeTab.EVENTS -> PlaceholderContent(
                icon = DateRangeIcon,
                label = Res.string.home_placeholder_events,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun HomeBottomNavBar(
    selectedTab: HomeTab,
    onSelect: (HomeTab) -> Unit
) {
    NavigationBar {
        HomeNavItem(
            selected = selectedTab == HomeTab.EXPLORE,
            onClick = { onSelect(HomeTab.EXPLORE) },
            icon = SearchIcon,
            label = Res.string.nav_explore
        )

        HomeNavItem(
            selected = selectedTab == HomeTab.ME,
            onClick = { onSelect(HomeTab.ME) },
            icon = PersonIcon,
            label = Res.string.nav_me
        )

//        HomeNavItem(
//            selected = selectedTab == HomeTab.EVENTS,
//            onClick = { onSelect(HomeTab.EVENTS) },
//            icon = DateRangeIcon,
//            label = Res.string.nav_events
//        )
    }
}

@Composable
private fun RowScope.HomeNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: StringResource
) {
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = unselectedColor
            )
        },
        label = {
            Text(
                text = stringResource(label),
                color = if (selected) selectedColor else unselectedColor,
                fontSize = if (selected) 14.sp else 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = unselectedColor,
            unselectedIconColor = unselectedColor,
            indicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun PlaceholderContent(
    icon: ImageVector,
    label: StringResource,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScopedScreen(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(Unit) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

@Preview
@Composable
private fun PlaceholderContentPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            PlaceholderContent(
                icon = DateRangeIcon,
                label = Res.string.home_placeholder_events,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
