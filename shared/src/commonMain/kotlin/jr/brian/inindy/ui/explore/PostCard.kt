package jr.brian.inindy.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.model.VideoMedia
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.post_author_anonymous
import jr.brian.inindy.resources.post_im_in_button
import jr.brian.inindy.resources.post_in_count_label
import jr.brian.inindy.resources.post_in_count_label_single
import jr.brian.inindy.resources.post_interested_button
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.LocationOnIcon
import jr.brian.inindy.ui.icons.PlayArrowIcon
import jr.brian.inindy.ui.video.VideoPlayer
import jr.brian.inindy.util.DateUtil
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val MAX_POST_IMAGES = 3
private const val MAX_POST_VIDEOS = 2
private const val MAX_FOOTER_TAGS = 2
private const val RELATIVE_TIME_REFRESH_MS = 60_000L
private val AvatarSize = 36.dp

private const val HERO_ASPECT_RATIO = 16f / 12f

@Composable
fun PostCard(
    post: Post,
    isRsvpd: Boolean,
    onRsvpClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = onRsvpClick,
    isOwnPost: Boolean = false,
    nowMs: Long = rememberTickingNowMs()
) {
    val displayName = post.author?.fullName
        ?: stringResource(Res.string.post_author_anonymous)
    val firstName = firstTokenOf(displayName)
    val relativeTime = DateUtil.formatRelativeDate(post.createdAt, nowMs)
    val media = post.images.take(MAX_POST_IMAGES).map { HeroMedia.Image(it) } +
            post.videos.take(MAX_POST_VIDEOS).map { HeroMedia.Video(it.url, it.thumbnailUrl) }
    val primaryTag = post.tags.firstOrNull() ?: Interest.EXPLORING

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCardClick(post.id) },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            PostHero(
                media = media,
                primaryTag = primaryTag,
                contentDescription = post.title
            )

            Column(modifier = Modifier.padding(14.dp)) {

                PostHeader(
                    name = firstName,
                    avatarUrl = post.author?.avatarUrl,
                    relativeTime = relativeTime,
                    rsvpCount = post.rsvpCount,
                    attendees = post.previewAttendees
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (post.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(10.dp))

                MetaRow(
                    location = post.address,
                    date = DateUtil.formatEventDate(post.startsAt)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FooterRow(
                    tags = post.tags,
                    neighborhoodName = post.neighborhoodName,
                    isRsvpd = isRsvpd,
                    isOwnPost = isOwnPost,
                    onRsvpClick = { onRsvpClick(post.id) }
                )
            }
        }
    }
}

@Composable
private fun PostHeader(
    name: String,
    avatarUrl: String?,
    relativeTime: String,
    rsvpCount: Int,
    attendees: List<User>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(name = name, avatarUrl = avatarUrl)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        InCountStat(rsvpCount = rsvpCount, attendees = attendees)
    }
}

@Composable
private fun InCountStat(
    rsvpCount: Int,
    attendees: List<User>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (attendees.isNotEmpty()) {
            OverlappingAvatars(
                attendees = attendees,
                totalCount = attendees.size,
                avatarSize = 24.dp,
                maxVisible = 3,
                ringColor = MaterialTheme.colorScheme.surface
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = rsvpCount,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "post-card-rsvp-count"
            ) { count ->
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Text(
                text = if (rsvpCount == 1) {
                    stringResource(Res.string.post_in_count_label_single)
                }
                else {
                    stringResource(Res.string.post_in_count_label)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Avatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(AvatarSize)
                .clip(CircleShape)
        )
    } else {
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        Box(
            modifier = modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PostHero(
    media: List<HeroMedia>,
    primaryTag: Interest,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(HERO_ASPECT_RATIO)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        if (media.isEmpty()) {
            TagGradientBackground(tag = primaryTag, modifier = Modifier.fillMaxSize())
        } else {
            val pagerState = rememberPagerState(pageCount = { media.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (val item = media[page]) {
                    is HeroMedia.Image -> AsyncImage(
                        model = item.url,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    is HeroMedia.Video -> VideoPage(
                        video = item,
                        contentDescription = contentDescription,
                        primaryTag = primaryTag
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.25f)
                        )
                    )
            )

            if (media.size > 1) {
                PageIndicator(
                    pageCount = media.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }
        }
    }
}

private sealed interface HeroMedia {
    val url: String

    data class Image(override val url: String) : HeroMedia
    data class Video(override val url: String, val thumbnailUrl: String?) : HeroMedia
}

@Composable
private fun VideoPage(
    video: HeroMedia.Video,
    contentDescription: String,
    primaryTag: Interest
) {
    var playing by remember(video.url) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playing) {
            VideoPlayer(
                url = video.url,
                autoPlay = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            if (video.thumbnailUrl != null) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                TagGradientBackground(tag = primaryTag, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable { playing = true },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Icon(
                        imageVector = PlayArrowIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagGradientBackground(tag: Interest, modifier: Modifier = Modifier) {
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
            text = tag.displayName.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (isActive) 16.dp else 5.dp, height = 5.dp)
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
private fun MetaRow(
    location: String,
    date: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row {
            Icon(
                imageVector = LocationOnIcon,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp).size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(Modifier.weight(1f))

        Row {
            Icon(
                imageVector = DateRangeIcon,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp).size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FooterRow(
    tags: List<Interest>,
    neighborhoodName: String?,
    isRsvpd: Boolean,
    isOwnPost: Boolean,
    onRsvpClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tags.take(MAX_FOOTER_TAGS).forEach { tag ->
                TagChip(tag = tag)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
//            if (!neighborhoodName.isNullOrBlank()) {
//                Button(
//                    onClick = {},
//                    shape = RoundedCornerShape(12.dp),
//                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = if (isRsvpd)
//                            MaterialTheme.colorScheme.secondaryContainer
//                        else
//                            MaterialTheme.colorScheme.primary,
//                        contentColor = if (isRsvpd)
//                            MaterialTheme.colorScheme.onSecondaryContainer
//                        else
//                            MaterialTheme.colorScheme.onPrimary
//                    )
//                ) {
//                    Row(
//                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Icon(
//                            imageVector = LocationOnIcon,
//                            contentDescription = null,
//                            modifier = Modifier.size(12.dp),
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Text(
//                            text = neighborhoodName,
//                            style = MaterialTheme.typography.labelLarge,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                    }
//                }
//            }

            Spacer(modifier = Modifier.weight(1f))

            if (!isOwnPost) {
                Button(
                    onClick = onRsvpClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRsvpd)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primary,
                        contentColor = if (isRsvpd)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (isRsvpd) Res.string.post_interested_button
                            else Res.string.post_im_in_button
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: Interest,
    modifier: Modifier = Modifier
) {
    val color = tagColor(tag)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = tag.displayName,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun rememberTickingNowMs(refreshIntervalMs: Long = RELATIVE_TIME_REFRESH_MS): Long {
    val now by produceState(initialValue = currentTimeMillis()) {
        while (true) {
            delay(refreshIntervalMs)
            value = currentTimeMillis()
        }
    }
    return now
}

internal fun tagColor(tag: Interest): Color = when (tag) {
    Interest.RUNNING,
    Interest.HIKING,
    Interest.CYCLING,
    Interest.WALKING,
    Interest.YOGA,
    Interest.SPORTS,
    Interest.SWIMMING,
    Interest.SKATING -> Color(0xFF4CAF50)

    Interest.PICNICS,
    Interest.BONFIRES,
    Interest.GAME_NIGHTS,
    Interest.COFFEE,
    Interest.FOOD,
    Interest.VOLUNTEERING -> Color(0xFFFFC107)

    Interest.PHOTOGRAPHY,
    Interest.DRAWING,
    Interest.READING,
    Interest.MUSIC,
    Interest.CRAFTS,
    Interest.WRITING -> Color(0xFFE91E63)

    Interest.EXPLORING,
    Interest.BIRDWATCHING,
    Interest.GARDENING,
    Interest.STARGAZING,
    Interest.NATURE -> Color(0xFF9C27B0)

    Interest.DOG_WALKS,
    Interest.PET_FRIENDLY -> Color(0xFF009688)
}

internal fun firstTokenOf(displayName: String): String {
    val trimmed = displayName.trim()
    if (trimmed.isEmpty()) return displayName
    val space = trimmed.indexOf(' ')
    return if (space > 0) trimmed.substring(0, space) else trimmed
}

@Preview
@Composable
private fun PostCardPreview() {
    val createdAt = 1_779_836_400_000L
    MaterialTheme {
        PostCard(
            post = Post(
                id = "1",
                userId = "u1",
                title = "Morning hike at Eagle Creek",
                description = "Anyone down for an easy 5-mile loop Saturday morning? All paces welcome — bringing coffee.",
                latitude = 39.8283,
                longitude = -86.2779,
                address = "Eagle Creek Park, Indianapolis",
                startsAt = 1_780_045_200_000L,
                endsAt = null,
                createdAt = createdAt,
                tags = listOf(Interest.HIKING, Interest.WALKING),
                images = listOf(
                    "https://example.com/photo1.jpg",
                    "https://example.com/photo2.jpg",
                    "https://example.com/photo3.jpg"
                ),
                videos = listOf(
                    VideoMedia(
                        url = "https://example.com/clip1.mp4",
                        thumbnailUrl = "https://example.com/clip1-thumb.jpg"
                    )
                ),
                rsvpCount = 12,
                author = User("u1", "Sarah M.", null),
                neighborhoodName = "Broad Ripple",
                previewAttendees = listOf(
                    User("a1", "Alex P.", null),
                    User("a2", "Bree K.", null),
                    User("a3", "Carlos R.", null)
                )
            ),
            isRsvpd = false,
            onRsvpClick = {},
            nowMs = createdAt + 20 * 60_000L
        )
    }
}

@Preview
@Composable
private fun PostCardNoImagesPreview() {
    val createdAt = 1_779_922_800_000L
    MaterialTheme {
        PostCard(
            post = Post(
                id = "2",
                userId = "u2",
                title = "Sunset picnic at White River",
                description = "Bring a blanket and a snack — should be a perfect golden hour.",
                latitude = 39.7684,
                longitude = -86.1581,
                address = "White River State Park",
                startsAt = 1_780_131_600_000L,
                endsAt = null,
                createdAt = createdAt,
                tags = listOf(Interest.PICNICS),
                images = emptyList(),
                videos = emptyList(),
                rsvpCount = 4,
                author = User("u2", "Jordan T.", null),
                neighborhoodName = "Downtown"
            ),
            isRsvpd = true,
            onRsvpClick = {},
            nowMs = createdAt + 3 * 3_600_000L
        )
    }
}

@Preview
@Composable
private fun PostCardRsvpdPreview() {
    val createdAt = 1_779_950_000_000L
    MaterialTheme {
        PostCard(
            post = Post(
                id = "3",
                userId = "u3",
                title = "Pickup soccer at Garfield Park",
                description = "Casual game, no skill required. Show up and we'll split teams.",
                latitude = 39.7261,
                longitude = -86.1349,
                address = "Garfield Park, Indianapolis",
                startsAt = 1_780_200_000_000L,
                endsAt = null,
                createdAt = createdAt,
                tags = listOf(Interest.SPORTS, Interest.RUNNING),
                images = emptyList(),
                videos = emptyList(),
                rsvpCount = 7,
                author = null,
                neighborhoodName = "Fountain Square"
            ),
            isRsvpd = true,
            onRsvpClick = {},
            nowMs = createdAt + 45 * 60_000L
        )
    }
}
