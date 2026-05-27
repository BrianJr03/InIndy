package jr.brian.inindy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.tooling.preview.Preview
import jr.brian.inindy.presentation.explore.ExploreViewModel
import jr.brian.inindy.ui.explore.ExploreScreen
import jr.brian.inindy.ui.icons.AddIcon
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.SearchIcon
import org.koin.compose.viewmodel.koinViewModel

private enum class HomeTab { EXPLORE, EVENTS }

@Composable
fun HomeScreen(
    exploreViewModel: ExploreViewModel = koinViewModel()
) {
    var selectedTab by remember { mutableStateOf(HomeTab.EXPLORE) }
    val exploreUiState by exploreViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.EXPLORE,
                    onClick = { selectedTab = HomeTab.EXPLORE },
                    icon = { Icon(SearchIcon, contentDescription = null) },
                    label = { Text("Explore") }
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.EVENTS,
                    onClick = { selectedTab = HomeTab.EVENTS },
                    icon = { Icon(DateRangeIcon, contentDescription = null) },
                    label = { Text("Events") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.EXPLORE) {
                FloatingActionButton(onClick = { /* TODO: navigate to create post */ }) {
                    Icon(AddIcon, contentDescription = "Create post")
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.EXPLORE -> ExploreScreen(
                uiState = exploreUiState,
                onRefresh = exploreViewModel::loadPosts,
                onRsvpClick = { /* TODO: handle RSVP */ },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            HomeTab.EVENTS -> EventsPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun EventsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = SearchIcon, // TODO: DateRangeIcon
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Events coming soon",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun EventsPlaceholderPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            EventsPlaceholder(modifier = Modifier.fillMaxSize())
        }
    }
}
