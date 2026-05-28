package jr.brian.inindy.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.PostTag
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.detail_ends_label
import jr.brian.inindy.resources.detail_image_count
import jr.brian.inindy.resources.detail_map_caption
import jr.brian.inindy.resources.detail_overview_title
import jr.brian.inindy.resources.detail_when_where_title
import jr.brian.inindy.resources.detail_whos_in_subtitle
import jr.brian.inindy.resources.detail_whos_in_title
import jr.brian.inindy.resources.detail_im_in_button
import jr.brian.inindy.resources.detail_un_rsvp_confirm
import jr.brian.inindy.resources.detail_un_rsvp_content_description
import jr.brian.inindy.resources.detail_un_rsvp_dialog_message
import jr.brian.inindy.resources.detail_un_rsvp_dialog_title
import jr.brian.inindy.resources.detail_un_rsvp_dismiss
import jr.brian.inindy.resources.detail_youre_in_button
import jr.brian.inindy.resources.post_author_anonymous
import jr.brian.inindy.ui.components.FloatingBackButton
import jr.brian.inindy.ui.icons.CloseIcon
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.LocationOnIcon
import jr.brian.inindy.ui.icons.PersonIcon
import jr.brian.inindy.util.DateUtil
import org.jetbrains.compose.resources.stringResource

private val DetailHeroHeight = 320.dp
private val DetailAvatarSize = 56.dp
private val DetailMapHeight = 180.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PostDetailScreen(
    post: Post,
    isRsvpd: Boolean,
    onBack: () -> Unit,
    onConfirmRsvp: () -> Unit,
    onUnRsvp: () -> Unit,
    modifier: Modifier = Modifier,
    nowMs: Long = rememberTickingNowMs()
) {
    BackHandler(onBack = onBack)
    val accent = tagColor(post.tags.firstOrNull() ?: PostTag.OTHER)
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp)
        ) {
            DetailHero(
                images = post.images,
                primaryTag = post.tags.firstOrNull() ?: PostTag.OTHER,
                contentDescription = post.title
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AuthorBlock(
                    post = post,
                    nowMs = nowMs
                )

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 34.sp
                )

                if (post.tags.isNotEmpty()) {
                    DetailTagRow(tags = post.tags)
                }

                if (post.description.isNotBlank()) {
                    SectionLabel(text = stringResource(Res.string.detail_overview_title))
                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                SectionLabel(text = stringResource(Res.string.detail_when_where_title))
                WhenWhereBlock(post = post)

                MapPreviewCard(
                    address = post.address,
                    accent = accent
                )

                SectionLabel(text = stringResource(Res.string.detail_whos_in_title))
                WhosInStat(
                    rsvpCount = post.rsvpCount,
                    accent = accent
                )
            }
        }

        FloatingBackButton(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        StickyRsvpBar(
            isRsvpd = isRsvpd,
            accent = accent,
            onConfirm = onConfirmRsvp,
            onUnRsvp = onUnRsvp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DetailHero(
    images: List<String>,
    primaryTag: PostTag,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val scrim = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.55f),
            Color.Transparent,
            Color.Transparent,
            Color.Black.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DetailHeroHeight)
    ) {
        if (images.isEmpty()) {
            TagBackdrop(tag = primaryTag, modifier = Modifier.fillMaxSize())
        } else {
            val pagerState = rememberPagerState(pageCount = { images.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
            )

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            Res.string.detail_image_count,
                            pagerState.currentPage + 1,
                            images.size
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TagBackdrop(
    tag: PostTag,
    modifier: Modifier = Modifier
) {
    val color = tagColor(tag)
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(color, color.copy(alpha = 0.6f))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tag.label.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun AuthorBlock(
    post: Post,
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    val anonymous = stringResource(Res.string.post_author_anonymous)
    val displayName = post.author?.fullName ?: anonymous
    val firstName = firstTokenOf(displayName)
    val relative = DateUtil.formatRelativeDate(post.createdAt, nowMs)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailAvatar(name = firstName, avatarUrl = post.author?.avatarUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = firstName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Posted $relative",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailAvatar(
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
                .size(DetailAvatarSize)
                .clip(CircleShape)
        )
    } else {
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        Box(
            modifier = modifier
                .size(DetailAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DetailTagRow(
    tags: List<PostTag>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            DetailTagChip(tag = tag)
        }
    }
}

@Composable
private fun DetailTagChip(
    tag: PostTag,
    modifier: Modifier = Modifier
) {
    val color = tagColor(tag)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = tag.label.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun WhenWhereBlock(
    post: Post,
    modifier: Modifier = Modifier
) {
    val endsLabel = stringResource(Res.string.detail_ends_label)
    val endsText = post.endsAt?.let { "$endsLabel ${DateUtil.formatEventDate(it)}" }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetaRow(
            icon = LocationOnIcon,
            primary = post.address,
            secondary = "Indianapolis, IN"
        )
        MetaRow(
            icon = DateRangeIcon,
            primary = DateUtil.formatEventDate(post.startsAt),
            secondary = endsText
        )
    }
}

@Composable
private fun MetaRow(
    icon: ImageVector,
    primary: String,
    secondary: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!secondary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MapPreviewCard(
    address: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            StylizedMapCanvas(
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DetailMapHeight)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = LocationOnIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(Res.string.detail_map_caption),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StylizedMapCanvas(
    accent: Color,
    modifier: Modifier = Modifier
) {
    val gridLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val baseTop = accent.copy(alpha = 0.18f)
    val baseBottom = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(colors = listOf(baseTop, baseBottom))
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepPx = with(density) { 22.dp.toPx() }
            val stroke = with(density) { 1.dp.toPx() }
            val diagonalCount = ((size.width + size.height) / stepPx).toInt() + 2
            for (i in -2..diagonalCount) {
                val x = i * stepPx
                drawLine(
                    color = gridLine,
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = stroke
                )
            }
            // Soft "route" curve across the map
            val routeColor = accent.copy(alpha = 0.55f)
            val routeStroke = with(density) { 3.dp.toPx() }
            val midY = size.height * 0.55f
            drawLine(
                brush = SolidColor(routeColor),
                start = Offset(size.width * 0.08f, midY + size.height * 0.18f),
                end = Offset(size.width * 0.45f, midY - size.height * 0.05f),
                strokeWidth = routeStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                brush = SolidColor(routeColor),
                start = Offset(size.width * 0.45f, midY - size.height * 0.05f),
                end = Offset(size.width * 0.92f, midY - size.height * 0.22f),
                strokeWidth = routeStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        MapPin(accent = accent)
    }
}

@Composable
private fun MapPin(
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring pulse
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.32f))
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LocationOnIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun WhosInStat(
    rsvpCount: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = rsvpCount,
                transitionSpec = {
                    (fadeIn() togetherWith fadeOut())
                },
                label = "rsvp-count"
            ) { count ->
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.detail_whos_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = PersonIcon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accent
        )
    }
}

@Composable
private fun StickyRsvpBar(
    isRsvpd: Boolean,
    accent: Color,
    onConfirm: () -> Unit,
    onUnRsvp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnRsvpDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = { if (!isRsvpd) onConfirm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = if (isRsvpd) 8.dp else 20.dp,
                    top = 14.dp,
                    bottom = 14.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                AnimatedContent(
                    targetState = isRsvpd,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "rsvp-button",
                    modifier = Modifier.fillMaxWidth()
                ) { rsvpd ->
                    if (rsvpd) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(Res.string.detail_youre_in_button),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable(onClick = { showUnRsvpDialog = true }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CloseIcon,
                                    contentDescription = stringResource(
                                        Res.string.detail_un_rsvp_content_description
                                    ),
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.detail_im_in_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showUnRsvpDialog) {
        AlertDialog(
            onDismissRequest = { showUnRsvpDialog = false },
            title = {
                Text(text = stringResource(Res.string.detail_un_rsvp_dialog_title))
            },
            text = {
                Text(text = stringResource(Res.string.detail_un_rsvp_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnRsvpDialog = false
                        onUnRsvp()
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.detail_un_rsvp_confirm),
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnRsvpDialog = false }) {
                    Text(text = stringResource(Res.string.detail_un_rsvp_dismiss))
                }
            }
        )
    }
}

@Preview
@Composable
private fun PostDetailScreenPreview() {
    val createdAt = 1_779_836_400_000L
    MaterialTheme {
        PostDetailScreen(
            post = Post(
                id = "1",
                userId = "u1",
                title = "Morning hike at Eagle Creek",
                description = "Anyone down for an easy 5-mile loop Saturday morning? All paces welcome — bringing coffee and a couple of breakfast bars to share at the trailhead.",
                latitude = 39.8283,
                longitude = -86.2779,
                address = "Eagle Creek Park, Indianapolis",
                startsAt = 1_780_045_200_000L,
                endsAt = 1_780_056_000_000L,
                createdAt = createdAt,
                tags = listOf(PostTag.HIKE, PostTag.WALK),
                images = listOf(
                    "https://example.com/photo1.jpg",
                    "https://example.com/photo2.jpg"
                ),
                videos = emptyList(),
                rsvpCount = 12,
                author = User("u1", "Sarah M.", null)
            ),
            isRsvpd = false,
            onBack = {},
            onConfirmRsvp = {},
            onUnRsvp = {},
            nowMs = createdAt + 20 * 60_000L
        )
    }
}

@Preview
@Composable
private fun PostDetailScreenConfirmedPreview() {
    val createdAt = 1_779_836_400_000L
    MaterialTheme {
        PostDetailScreen(
            post = Post(
                id = "2",
                userId = "u2",
                title = "Pickup soccer at Garfield Park",
                description = "Casual game, no skill required. Show up and we'll split teams.",
                latitude = 39.7261,
                longitude = -86.1349,
                address = "Garfield Park, Indianapolis",
                startsAt = 1_780_200_000_000L,
                endsAt = null,
                createdAt = createdAt,
                tags = listOf(PostTag.SPORT),
                images = emptyList(),
                videos = emptyList(),
                rsvpCount = 8,
                author = null
            ),
            isRsvpd = true,
            onBack = {},
            onConfirmRsvp = {},
            onUnRsvp = {},
            nowMs = createdAt + 45 * 60_000L
        )
    }
}
