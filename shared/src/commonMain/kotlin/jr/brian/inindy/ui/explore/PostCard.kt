package jr.brian.inindy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.PostTag
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.post_author_anonymous
import jr.brian.inindy.resources.post_rsvp_button
import jr.brian.inindy.resources.post_rsvp_count_going
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.PersonIcon
import jr.brian.inindy.util.DateUtil
import org.jetbrains.compose.resources.stringResource

private const val MAX_POST_IMAGES = 3

@Composable
fun PostCard(
    post: Post,
    onRsvpClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTag = post.tags.firstOrNull() ?: PostTag.OTHER
    val images = post.images.take(MAX_POST_IMAGES)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            PostHero(
                images = images,
                primaryTag = primaryTag,
                contentDescription = post.title
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                MetaRow(date = DateUtil.formatEventDate(post.startsAt))

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(10.dp))

                FooterRow(
                    displayName = post.author?.displayName
                        ?: stringResource(Res.string.post_author_anonymous),
                    rsvpCount = post.rsvpCount,
                    onRsvpClick = { onRsvpClick(post.id) }
                )
            }
        }
    }
}

@Composable
private fun PostHero(
    images: List<String>,
    primaryTag: PostTag,
    contentDescription: String
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (images.isEmpty()) {
            TagGradientBackground(tag = primaryTag, modifier = Modifier.fillMaxSize())
        } else {
            val pagerState = rememberPagerState(pageCount = { images.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scrim improves legibility for the tag pill and indicators.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.32f)
                            )
                        )
                    )
            )

            if (images.size > 1) {
                PageIndicator(
                    pageCount = images.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }
        }

        TagPill(
            tag = primaryTag,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        )
    }
}

@Composable
private fun TagGradientBackground(tag: PostTag, modifier: Modifier = Modifier) {
    val color = tagColor(tag)
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(color, color.copy(alpha = 0.55f))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.label.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TagPill(tag: PostTag, modifier: Modifier = Modifier) {
    val color = tagColor(tag)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color
    ) {
        Text(
            text = tag.label.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (isActive) 18.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive) Color.White
                        else Color.White.copy(alpha = 0.55f)
                    )
            )
        }
    }
}

@Composable
private fun MetaRow(date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = DateRangeIcon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FooterRow(
    displayName: String,
    rsvpCount: Int,
    onRsvpClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PersonIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.post_rsvp_count_going, rsvpCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        OutlinedButton(
            onClick = onRsvpClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(Res.string.post_rsvp_button),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

internal fun tagColor(tag: PostTag): Color = when (tag) {
    PostTag.HIKE -> Color(0xFF4CAF50)
    PostTag.RUN -> Color(0xFFFF5722)
    PostTag.PICNIC -> Color(0xFFFFC107)
    PostTag.SPORT -> Color(0xFF2196F3)
    PostTag.WALK -> Color(0xFF009688)
    PostTag.EXPLORE -> Color(0xFF9C27B0)
    PostTag.OTHER -> Color(0xFF9E9E9E)
}

@Preview
@Composable
private fun PostCardPreview() {
    MaterialTheme {
        PostCard(
            post = Post(
                id = "1",
                userId = "u1",
                title = "Morning hike at Eagle Creek",
                description = "Join us for a 5-mile loop through Eagle Creek Park. All skill levels welcome!",
                latitude = 39.8283,
                longitude = -86.2779,
                address = "Eagle Creek Park, Indianapolis",
                startsAt = 1_780_045_200_000L,
                endsAt = null,
                createdAt = 1_779_836_400_000L,
                tags = listOf(PostTag.HIKE, PostTag.WALK),
                images = listOf(
                    "https://example.com/photo1.jpg",
                    "https://example.com/photo2.jpg",
                    "https://example.com/photo3.jpg"
                ),
                rsvpCount = 12,
                author = User("u1", "Sarah M.", null)
            ),
            onRsvpClick = {}
        )
    }
}

@Preview
@Composable
private fun PostCardNoImagesPreview() {
    MaterialTheme {
        PostCard(
            post = Post(
                id = "2",
                userId = "u2",
                title = "Sunset picnic at White River",
                description = "Bring a blanket and a snack.",
                latitude = 39.7684,
                longitude = -86.1581,
                address = "White River State Park",
                startsAt = 1_780_131_600_000L,
                endsAt = null,
                createdAt = 1_779_922_800_000L,
                tags = listOf(PostTag.PICNIC),
                images = emptyList(),
                rsvpCount = 4,
                author = User("u2", "Jordan T.", null)
            ),
            onRsvpClick = {}
        )
    }
}
