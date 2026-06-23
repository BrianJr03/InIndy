package jr.brian.inindy.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Post
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.domain.model.VideoMedia
import jr.brian.inindy.presentation.post.PostDetailUiState
import jr.brian.inindy.presentation.post.PostDetailViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.detail_attendees_dialog_close
import jr.brian.inindy.resources.detail_attendees_dialog_title
import jr.brian.inindy.resources.detail_attendees_empty
import jr.brian.inindy.resources.detail_attendees_overflow
import jr.brian.inindy.resources.detail_back_content_description
import jr.brian.inindy.resources.detail_ends_label
import jr.brian.inindy.resources.detail_im_in_button
import jr.brian.inindy.resources.detail_image_count
import jr.brian.inindy.resources.detail_map_caption
import jr.brian.inindy.resources.detail_overview_title
import jr.brian.inindy.resources.detail_un_rsvp_confirm
import jr.brian.inindy.resources.detail_un_rsvp_content_description
import jr.brian.inindy.resources.detail_un_rsvp_dialog_message
import jr.brian.inindy.resources.detail_un_rsvp_dialog_title
import jr.brian.inindy.resources.detail_un_rsvp_dismiss
import jr.brian.inindy.resources.detail_when_where_title
import jr.brian.inindy.resources.detail_whos_in_subtitle
import jr.brian.inindy.resources.detail_whos_in_title
import jr.brian.inindy.resources.detail_youre_in_button
import jr.brian.inindy.resources.me_delete_post_confirm
import jr.brian.inindy.resources.me_delete_post_dialog_message
import jr.brian.inindy.resources.me_delete_post_dialog_title
import jr.brian.inindy.resources.me_delete_post_dismiss
import jr.brian.inindy.resources.post_author_anonymous
import jr.brian.inindy.resources.post_detail_delete
import jr.brian.inindy.resources.post_detail_edit
import jr.brian.inindy.resources.post_detail_loading
import jr.brian.inindy.resources.post_detail_menu_cd
import jr.brian.inindy.resources.post_detail_unavailable
import jr.brian.inindy.resources.post_in_count_label
import jr.brian.inindy.resources.post_in_count_label_single
import jr.brian.inindy.ui.components.FloatingBackButton
import jr.brian.inindy.ui.icons.CloseIcon
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.LocationOnIcon
import jr.brian.inindy.ui.icons.MoreVertIcon
import jr.brian.inindy.ui.icons.PersonIcon
import jr.brian.inindy.ui.icons.PlayArrowIcon
import jr.brian.inindy.ui.video.VideoPlayer
import jr.brian.inindy.util.DateUtil
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val DetailHeroHeight = 320.dp
private val DetailAvatarSize = 56.dp
private val DetailMapHeight = 180.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowHostActions: Boolean = false,
    viewModel: PostDetailViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    LaunchedEffect(state) {
        val s = state
        if (s is PostDetailUiState.Success && s.deleted) onBack()
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (val s = state) {
            is PostDetailUiState.Loading -> DetailLoadingState()
            is PostDetailUiState.Unavailable -> DetailUnavailableState(onBack = onBack)
            is PostDetailUiState.Success -> PostDetailContent(
                post = s.post,
                isHost = s.isHost && allowHostActions,
                isRsvpd = s.isRsvpd,
                attendees = s.attendees,
                attendeesLoading = s.attendeesLoading,
                onBack = onBack,
                onEdit = { onEdit(s.post.id) },
                onDelete = viewModel::delete,
                onConfirmRsvp = viewModel::rsvp,
                onUnRsvp = viewModel::cancelRsvp,
                onLoadAttendees = viewModel::loadAttendees
            )
        }
    }
}

@Composable
private fun DetailLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.post_detail_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailUnavailableState(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.post_detail_unavailable),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onBack) {
                Text(stringResource(Res.string.detail_back_content_description))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PostDetailContent(
    post: Post,
    isHost: Boolean,
    isRsvpd: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConfirmRsvp: () -> Unit,
    onUnRsvp: () -> Unit,
    modifier: Modifier = Modifier,
    nowMs: Long = rememberTickingNowMs(),
    attendees: List<User> = emptyList(),
    attendeesLoading: Boolean = false,
    onLoadAttendees: () -> Unit = {}
) {
    val accent = tagColor(post.tags.firstOrNull() ?: Interest.EXPLORING)
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAttendeesDialog by remember { mutableStateOf(false) }

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
                videos = post.videos,
                primaryTag = post.tags.firstOrNull() ?: Interest.EXPLORING,
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
                    accent = accent,
                    attendees = attendees,
                    onClick = {
                        showAttendeesDialog = true
                        onLoadAttendees()
                    }
                )
            }
        }

        FloatingBackButton(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (isHost) {
            HostActionsMenu(
                onEdit = onEdit,
                onDelete = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.TopEnd)
            )
            HostActionBar(
                accent = accent,
                onEdit = onEdit,
                onDelete = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            StickyRsvpBar(
                isRsvpd = isRsvpd,
                accent = accent,
                onConfirm = onConfirmRsvp,
                onUnRsvp = onUnRsvp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.me_delete_post_dialog_title)) },
            text = { Text(stringResource(Res.string.me_delete_post_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(Res.string.me_delete_post_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.me_delete_post_dismiss))
                }
            }
        )
    }

    if (showAttendeesDialog) {
        AttendeesDialog(
            attendees = attendees,
            loading = attendeesLoading,
            onDismiss = { showAttendeesDialog = false }
        )
    }
}

@Composable
private fun HostActionsMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.padding(8.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 6.dp
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = MoreVertIcon,
                    contentDescription = stringResource(Res.string.post_detail_menu_cd),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.post_detail_edit)) },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(Res.string.post_detail_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun HostActionBar(
    accent: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.post_detail_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(Res.string.post_detail_edit),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DetailHero(
    images: List<String>,
    videos: List<VideoMedia>,
    primaryTag: Interest,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val media = images.map { DetailMedia.Image(it) } +
            videos.map { DetailMedia.Video(it.url, it.thumbnailUrl) }

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
        if (media.isEmpty()) {
            TagBackdrop(tag = primaryTag, modifier = Modifier.fillMaxSize())
        } else {
            val pagerState = rememberPagerState(pageCount = { media.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (val item = media[page]) {
                    is DetailMedia.Image -> AsyncImage(
                        model = item.url,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    is DetailMedia.Video -> DetailVideoPage(
                        video = item,
                        contentDescription = contentDescription,
                        primaryTag = primaryTag
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
            )

            if (media.size > 1) {
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
                            media.size
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

private sealed interface DetailMedia {
    val url: String

    data class Image(override val url: String) : DetailMedia
    data class Video(override val url: String, val thumbnailUrl: String?) : DetailMedia
}

@Composable
private fun DetailVideoPage(
    video: DetailMedia.Video,
    contentDescription: String,
    primaryTag: Interest,
    modifier: Modifier = Modifier
) {
    var playing by remember(video.url) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
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
                TagBackdrop(tag = primaryTag, modifier = Modifier.fillMaxSize())
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
private fun TagBackdrop(
    tag: Interest,
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
            text = tag.displayName.uppercase(),
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
    tags: List<Interest>,
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
    tag: Interest,
    modifier: Modifier = Modifier
) {
    val color = tagColor(tag)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = tag.displayName.uppercase(),
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
    modifier: Modifier = Modifier,
    attendees: List<User> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.10f))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
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
                text = if (rsvpCount == 1) {
                    stringResource(Res.string.post_in_count_label_single)
                } else {
                    stringResource(Res.string.post_in_count_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (attendees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OverlappingAvatars(
                    attendees = attendees,
                    totalCount = rsvpCount,
                    avatarSize = 28.dp,
                    maxVisible = 4,
                    ringColor = MaterialTheme.colorScheme.surface
                )
            }
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
internal fun OverlappingAvatars(
    attendees: List<User>,
    totalCount: Int,
    avatarSize: Dp,
    maxVisible: Int,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    if (attendees.isEmpty()) return
    val visible = attendees.take(maxVisible)
    val overflow = (totalCount - visible.size).coerceAtLeast(0)
    val overlap = avatarSize / 4

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        visible.forEachIndexed { index, user ->
            AttendeePreviewAvatar(
                user = user,
                size = avatarSize,
                ringColor = ringColor,
                modifier = Modifier.offset(x = if (index == 0) 0.dp else -overlap * index)
            )
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .offset(x = -overlap * visible.size)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = ringColor, shape = CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.detail_attendees_overflow, overflow),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AttendeePreviewAvatar(
    user: User,
    size: Dp,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val anonymous = stringResource(Res.string.post_author_anonymous)
    val displayName = user.fullName ?: anonymous
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
    if (!user.avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(width = 2.dp, color = ringColor, shape = CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(width = 2.dp, color = ringColor, shape = CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
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

@Composable
private fun AttendeesDialog(
    attendees: List<User>,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.detail_attendees_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 360.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )

                    attendees.isEmpty() -> Text(
                        text = stringResource(Res.string.detail_attendees_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(attendees, key = { it.id }) { user ->
                            AttendeeRow(user = user)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.detail_attendees_dialog_close),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun AttendeeRow(
    user: User,
    modifier: Modifier = Modifier
) {
    val anonymous = stringResource(Res.string.post_author_anonymous)
    val displayName = user.fullName ?: anonymous
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AttendeeAvatar(name = displayName, avatarUrl = user.avatarUrl)
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AttendeeAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val size = 40.dp
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Preview
@Composable
private fun PostDetailScreenPreview() {
    val createdAt = 1_779_836_400_000L
    MaterialTheme {
        PostDetailContent(
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
                tags = listOf(Interest.HIKING, Interest.WALKING),
                images = listOf(
                    "https://example.com/photo1.jpg",
                    "https://example.com/photo2.jpg"
                ),
                videos = emptyList(),
                rsvpCount = 12,
                author = User("u1", "Sarah M.", null)
            ),
            isHost = false,
            isRsvpd = false,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onConfirmRsvp = {},
            onUnRsvp = {},
            nowMs = createdAt + 20 * 60_000L
        )
    }
}

@Preview
@Composable
private fun PostDetailScreenHostPreview() {
    val createdAt = 1_779_836_400_000L
    MaterialTheme {
        PostDetailContent(
            post = Post(
                id = "me-1",
                userId = "me",
                title = "My Sunday picnic",
                description = "Bringing a blanket and snacks for everyone.",
                latitude = 39.7261,
                longitude = -86.1349,
                address = "Garfield Park, Indianapolis",
                startsAt = 1_780_200_000_000L,
                endsAt = null,
                createdAt = createdAt,
                tags = listOf(Interest.PICNICS),
                images = emptyList(),
                videos = emptyList(),
                rsvpCount = 8,
                author = User("me", "Brian", null)
            ),
            isHost = true,
            isRsvpd = false,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onConfirmRsvp = {},
            onUnRsvp = {},
            nowMs = createdAt + 45 * 60_000L
        )
    }
}
