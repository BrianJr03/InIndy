package jr.brian.inindy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.presentation.explore.ExploreUiState
import jr.brian.inindy.presentation.explore.ExploreViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.home_placeholder_create
import jr.brian.inindy.resources.home_placeholder_events
import jr.brian.inindy.resources.nav_create
import jr.brian.inindy.resources.nav_events
import jr.brian.inindy.resources.nav_explore
import jr.brian.inindy.ui.explore.ExploreScreen
import jr.brian.inindy.ui.explore.PostDetailScreen
import jr.brian.inindy.ui.icons.AddIcon
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.SearchIcon
import jr.brian.inindy.ui.settings.SettingsScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class HomeTab { CREATE, EXPLORE, EVENTS }

@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    exploreViewModel: ExploreViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(HomeTab.EXPLORE) }
    var settingsOpen by remember { mutableStateOf(false) }
    var detailPostId by remember { mutableStateOf<String?>(null) }
    val exploreUiState by exploreViewModel.uiState.collectAsState()

    if (settingsOpen) {
        SettingsScreen(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            onBack = { settingsOpen = false }
        )
        return
    }

    val activePost = detailPostId?.let { id ->
        (exploreUiState as? ExploreUiState.Success)?.posts?.firstOrNull { it.id == id }
    }
    if (activePost != null) {
        PostDetailScreen(
            post = activePost,
            isRsvpd = exploreViewModel.isRsvpd(activePost.id),
            onBack = { detailPostId = null },
            onConfirmRsvp = { exploreViewModel.rsvp(activePost.id) }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.height(64.dp)) {
                Spacer(Modifier.height(8.dp))
                HomeNavItem(
                    selected = selectedTab == HomeTab.CREATE,
                    onClick = { selectedTab = HomeTab.CREATE },
                    icon = AddIcon,
                    label = Res.string.nav_create
                )
                HomeNavItem(
                    selected = selectedTab == HomeTab.EXPLORE,
                    onClick = { selectedTab = HomeTab.EXPLORE },
                    icon = SearchIcon,
                    label = Res.string.nav_explore
                )
                HomeNavItem(
                    selected = selectedTab == HomeTab.EVENTS,
                    onClick = { selectedTab = HomeTab.EVENTS },
                    icon = DateRangeIcon,
                    label = Res.string.nav_events
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.CREATE -> PlaceholderContent(
                icon = AddIcon,
                label = Res.string.home_placeholder_create,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            HomeTab.EXPLORE -> ExploreScreen(
                uiState = exploreUiState,
                onRefresh = exploreViewModel::loadPosts,
                onRsvpClick = { postId -> detailPostId = postId },
                onSettingsClick = { settingsOpen = true },
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
