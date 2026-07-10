package jr.brian.inindy.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.presentation.explore.ExploreIntent
import jr.brian.inindy.presentation.explore.ExploreUiState
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.explore_error_retry
import jr.brian.inindy.resources.explore_error_title
import jr.brian.inindy.resources.explore_feed_empty_all
import jr.brian.inindy.resources.explore_feed_empty_group
import jr.brian.inindy.resources.explore_feed_empty_neighborhood
import jr.brian.inindy.resources.explore_settings_content_description
import jr.brian.inindy.resources.notifications_bell_cd
import jr.brian.inindy.resources.notifications_bell_unread_cd
import jr.brian.inindy.ui.icons.NotificationsIcon
import jr.brian.inindy.ui.icons.SettingsIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    modifier: Modifier = Modifier,
    onIntent: (ExploreIntent) -> Unit,
    onRefresh: () -> Unit,
    onRsvpClick: (String) -> Unit,
    isRsvpd: (String) -> Boolean = { false },
    isOwnPost: (Post) -> Boolean = { false },
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    listState: LazyListState = rememberLazyListState(),
    refreshTrigger: Int = 0
) {
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            onIntent(ExploreIntent.Refresh)
            listState.animateScrollToItem(0)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ExploreHeader(
                uiState = uiState,
                onIntent = onIntent,
                onSettingsClick = onSettingsClick,
                onNotificationsClick = onNotificationsClick,
                unreadNotificationCount = unreadNotificationCount
            )
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onIntent(ExploreIntent.Refresh) },
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = FeedContentKey(uiState.feed, uiState.activeFilter),
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 200)) togetherWith
                            fadeOut(tween(durationMillis = 150))
                    },
                    label = "feedTransition",
                    modifier = Modifier.fillMaxSize()
                ) { key ->
                    when (val feed = key.feed) {
                        ExploreUiState.FeedState.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        is ExploreUiState.FeedState.Success -> if (feed.posts.isEmpty()) {
                            ExploreEmptyState(
                                filter = key.filter,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ExplorePostFeedList(
                                posts = feed.posts,
                                onRsvpClick = onRsvpClick,
                                isRsvpd = isRsvpd,
                                isOwnPost = isOwnPost,
                                listState = listState
                            )
                        }
                        is ExploreUiState.FeedState.Error -> ExploreErrorContent(
                            message = feed.message,
                            onRetry = onRefresh,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (uiState.isGroupSearchSheetVisible) {
            GroupSearchSheet(
                query = uiState.groupSearchQuery,
                userGroups = uiState.userGroups,
                searchedGroups = uiState.searchedGroups,
                isSearching = uiState.isSearchingGroups,
                onQueryChanged = { onIntent(ExploreIntent.GroupSearchQueryChanged(it)) },
                onGroupSelected = { onIntent(ExploreIntent.SelectFilterGroup(it)) },
                onDismiss = { onIntent(ExploreIntent.DismissGroupSearch) }
            )
        }
    }
}

private data class FeedContentKey(
    val feed: ExploreUiState.FeedState,
    val filter: ExploreFilter
)

@Composable
private fun ExplorePostFeedList(
    posts: List<Post>,
    onRsvpClick: (String) -> Unit,
    isRsvpd: (String) -> Boolean,
    isOwnPost: (Post) -> Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(
            items = posts,
            key = { it.id }
        ) { post ->
            PostCard(
                post = post,
                isRsvpd = isRsvpd(post.id),
                onRsvpClick = onRsvpClick,
                isOwnPost = isOwnPost(post),
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(durationMillis = 250),
                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    fadeOutSpec = tween(durationMillis = 250)
                )
            )
        }
    }
}

@Composable
private fun ExploreEmptyState(
    filter: ExploreFilter,
    modifier: Modifier = Modifier
) {
    val messageRes = when (filter) {
        is ExploreFilter.All -> Res.string.explore_feed_empty_all
        is ExploreFilter.Neighborhood -> Res.string.explore_feed_empty_neighborhood
        is ExploreFilter.Group -> Res.string.explore_feed_empty_group
    }
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExploreHeader(
    uiState: ExploreUiState,
    onIntent: (ExploreIntent) -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedContent(
                targetState = uiState.brandMarkText,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 150)) togetherWith
                        fadeOut(tween(durationMillis = 150))
                },
                label = "brandMarkText",
                modifier = Modifier.widthIn(max = 280.dp)
            ) { text ->
                FilterableBrandMark(
                    text = text,
                    activeFilter = uiState.activeFilter,
                    isDropdownOpen = uiState.isFilterDropdownVisible,
                    onArrowClick = { onIntent(ExploreIntent.ToggleFilterDropdown) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                NotificationsBell(
                    unreadCount = unreadNotificationCount,
                    onClick = onNotificationsClick
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = SettingsIcon,
                        contentDescription = stringResource(Res.string.explore_settings_content_description),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(start = 16.dp)) {
            FilterDropdown(
                expanded = uiState.isFilterDropdownVisible,
                activeFilter = uiState.activeFilter,
                neighborhoodName = uiState.neighborhoodName,
                lastSelectedGroup = uiState.lastSelectedGroup,
                onSelectAll = { onIntent(ExploreIntent.SelectFilterAll) },
                onSelectNeighborhood = { onIntent(ExploreIntent.SelectFilterNeighborhood) },
                onSelectLastGroup = { onIntent(ExploreIntent.SelectFilterGroup(it)) },
                onSearchGroups = { onIntent(ExploreIntent.OpenGroupSearch) },
                onDismiss = { onIntent(ExploreIntent.DismissFilterDropdown) }
            )
        }
    }
}

@Composable
private fun NotificationsBell(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentDescription = if (unreadCount > 0) {
        stringResource(Res.string.notifications_bell_unread_cd, unreadCount)
    } else {
        stringResource(Res.string.notifications_bell_cd)
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = NotificationsIcon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExploreErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.explore_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.explore_error_retry))
        }
    }
}

@Preview
@Composable
private fun ExploreScreenLoadingPreview() {
    MaterialTheme {
        ExploreScreen(
            uiState = ExploreUiState(neighborhoodName = "Broad Ripple"),
            onIntent = {},
            onRefresh = {},
            onRsvpClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview
@Composable
private fun ExploreScreenErrorPreview() {
    MaterialTheme {
        ExploreScreen(
            uiState = ExploreUiState(
                feed = ExploreUiState.FeedState.Error("Unable to load posts"),
                neighborhoodName = "Broad Ripple"
            ),
            onIntent = {},
            onRefresh = {},
            onRsvpClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview
@Composable
private fun ExploreScreenHeaderPreview() {
    MaterialTheme {
        ExploreHeader(
            uiState = ExploreUiState(
                neighborhoodName = "Broad Ripple",
                activeFilter = ExploreFilter.Neighborhood,
                brandMarkText = "InBroadRipple"
            ),
            onIntent = {},
            onSettingsClick = {},
            onNotificationsClick = {},
            unreadNotificationCount = 3
        )
    }
}
