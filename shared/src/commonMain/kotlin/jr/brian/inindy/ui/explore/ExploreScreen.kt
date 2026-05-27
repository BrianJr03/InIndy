package jr.brian.inindy.ui.explore

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.presentation.explore.ExploreUiState
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.explore_app_title
import jr.brian.inindy.resources.explore_app_title_accent
import jr.brian.inindy.resources.explore_error_retry
import jr.brian.inindy.resources.explore_error_title
import jr.brian.inindy.resources.explore_settings_content_description
import jr.brian.inindy.ui.icons.SettingsIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onRsvpClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is ExploreUiState.Loading -> ExploreLoadingContent(Modifier.align(Alignment.Center))
            is ExploreUiState.Success -> ExplorePostFeed(
                posts = uiState.posts,
                onRsvpClick = onRsvpClick,
                onSettingsClick = onSettingsClick
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
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            ExploreHeader(onSettingsClick = onSettingsClick)
        }
        items(items = posts, key = { it.id }) { post ->
            PostCard(post = post, onRsvpClick = onRsvpClick)
        }
    }
}

@Composable
private fun ExploreHeader(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullTitle = stringResource(Res.string.explore_app_title)
    val accent = stringResource(Res.string.explore_app_title_accent)
    val accentColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val wordmark = buildAnnotatedString {
        val accentPrefix = if (fullTitle.startsWith(accent)) accent else ""
        if (accentPrefix.isNotEmpty()) {
            withStyle(SpanStyle(color = accentColor)) { append(accentPrefix) }
            append(fullTitle.removePrefix(accentPrefix))
        } else {
            withStyle(SpanStyle(color = onSurfaceColor)) { append(fullTitle) }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = wordmark,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = onSurfaceColor
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
private fun ExploreHeaderPreview() {
    MaterialTheme {
        ExploreHeader(onSettingsClick = {})
    }
}

@Preview
@Composable
private fun ExploreScreenLoadingPreview() {
    MaterialTheme {
        ExploreScreen(
            uiState = ExploreUiState.Loading,
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
            uiState = ExploreUiState.Error("Unable to load posts"),
            onRefresh = {},
            onRsvpClick = {},
            onSettingsClick = {}
        )
    }
}
