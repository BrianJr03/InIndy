package jr.brian.inindy.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.presentation.explore.ExploreUiState

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onRsvpClick: (String) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is ExploreUiState.Loading -> ExploreLoadingContent(Modifier.align(Alignment.Center))
            is ExploreUiState.Success -> ExplorePostFeed(
                posts = uiState.posts,
                onRsvpClick = onRsvpClick
            )
            is ExploreUiState.Error -> ExploreErrorContent(
                message = uiState.message,
                onRetry = onRefresh
            )
        }
    }
}

@Composable
private fun ExplorePostFeed(
    posts: List<Post>,
    onRsvpClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = posts, key = { it.id }) { post ->
            PostCard(post = post, onRsvpClick = onRsvpClick)
        }
    }
}

@Composable
private fun ExploreLoadingContent(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
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
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Something went wrong",
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
            Text("Try again")
        }
    }
}

@Preview
@Composable
private fun ExploreScreenLoadingPreview() {
    MaterialTheme {
        ExploreScreen(
            uiState = ExploreUiState.Loading,
            onRefresh = {},
            onRsvpClick = {}
        )
    }
}

@Preview
@Composable
private fun ExploreScreenErrorPreview() {
    MaterialTheme {
        ExploreScreen(
            uiState = ExploreUiState.Error("Unable to load posts"),
            onRefresh = {},
            onRsvpClick = {}
        )
    }
}
