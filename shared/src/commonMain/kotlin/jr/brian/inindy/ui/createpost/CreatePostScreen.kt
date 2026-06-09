package jr.brian.inindy.ui.createpost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.inindy.domain.model.AddressResult
import jr.brian.inindy.domain.model.PostAudience
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.presentation.createpost.CreatePostUiState
import jr.brian.inindy.presentation.createpost.CreatePostViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.create_post_address_placeholder
import jr.brian.inindy.resources.create_post_audience_group
import jr.brian.inindy.resources.create_post_audience_neighborhood
import jr.brian.inindy.resources.create_post_chars_remaining
import jr.brian.inindy.resources.create_post_close_cd
import jr.brian.inindy.resources.create_post_create_new_group
import jr.brian.inindy.resources.create_post_decrease_cd
import jr.brian.inindy.resources.create_post_description_placeholder
import jr.brian.inindy.resources.create_post_discard_body
import jr.brian.inindy.resources.create_post_discard_confirm
import jr.brian.inindy.resources.create_post_discard_dismiss
import jr.brian.inindy.resources.create_post_discard_title
import jr.brian.inindy.resources.create_post_ends
import jr.brian.inindy.resources.create_post_increase_cd
import jr.brian.inindy.resources.create_post_max_no_limit
import jr.brian.inindy.resources.create_post_optional
import jr.brian.inindy.resources.create_post_pick_date
import jr.brian.inindy.resources.create_post_pick_group
import jr.brian.inindy.resources.create_post_section_audience
import jr.brian.inindy.resources.create_post_section_datetime
import jr.brian.inindy.resources.create_post_section_description
import jr.brian.inindy.resources.create_post_section_location
import jr.brian.inindy.resources.create_post_section_max
import jr.brian.inindy.resources.create_post_section_photos
import jr.brian.inindy.resources.create_post_section_tags
import jr.brian.inindy.resources.create_post_section_title
import jr.brian.inindy.resources.create_post_starts
import jr.brian.inindy.resources.create_post_submit
import jr.brian.inindy.resources.create_post_submitting
import jr.brian.inindy.resources.create_post_tags_helper
import jr.brian.inindy.resources.create_post_title
import jr.brian.inindy.resources.create_post_title_placeholder
import jr.brian.inindy.resources.create_post_use_location
import jr.brian.inindy.ui.components.PostImagePickerRow
import jr.brian.inindy.ui.icons.AddIcon
import jr.brian.inindy.ui.icons.CloseIcon
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.LocationOnIcon
import jr.brian.inindy.util.DateUtil
import jr.brian.inindy.util.currentTimeMillis
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(
    ExperimentalComposeUiApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun CreatePostScreen(
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showQuickGroupSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.submitted) {
        if (state.submitted) onSubmitted()
    }

    val handleClose: () -> Unit = {
        if (state.isDirty) showDiscardDialog = true else onClose()
    }

    BackHandler(onBack = handleClose)

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreatePostTopBar(
                onClose = handleClose,
                onSubmit = viewModel::submit,
                isSubmitting = state.isSubmitting
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PhotosSection(
                    images = state.images,
                    error = state.imagesError,
                    onImagesAdded = { uris -> uris.forEach(viewModel::addImage) },
                    onImageRemoved = viewModel::removeImage
                )
                TitleSection(
                    title = state.title,
                    error = state.titleError,
                    onTitleChange = viewModel::setTitle
                )
                DescriptionSection(
                    description = state.description,
                    error = state.descriptionError,
                    onDescriptionChange = viewModel::setDescription,
                    remaining = state.charactersRemaining
                )
                LocationSection(
                    address = state.address,
                    suggestions = state.addressSuggestions,
                    locationLoading = state.locationLoading,
                    error = state.addressError,
                    onUseCurrentLocation = viewModel::useCurrentLocation,
                    onAddressChange = viewModel::onAddressQueryChanged,
                    onAddressSelected = viewModel::selectAddress
                )
                DateTimeSection(
                    startsAt = state.startsAt,
                    endsAt = state.endsAt,
                    error = state.startsAtError,
                    onPickStart = viewModel::setStartsAt,
                    onPickEnd = viewModel::setEndsAt
                )
                AudienceSection(
                    state = state,
                    onSelectNeighborhood = viewModel::selectNeighborhoodAudience,
                    onSelectGroup = viewModel::selectGroupAudience,
                    onCreateGroup = { showQuickGroupSheet = true }
                )
                TagsSection(
                    tags = state.tags,
                    scrollState = scrollState,
                    onToggle = viewModel::toggleTag
                )
                MaxAttendeesSection(
                    count = state.maxAttendees,
                    noLimit = state.noLimit,
                    onSetCount = viewModel::setMaxAttendees,
                    onSetNoLimit = viewModel::setNoLimit
                )
                state.submitError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(Res.string.create_post_discard_title)) },
            text = { Text(stringResource(Res.string.create_post_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) {
                    Text(
                        text = stringResource(Res.string.create_post_discard_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(Res.string.create_post_discard_dismiss))
                }
            }
        )
    }

    if (showQuickGroupSheet) {
        QuickCreateGroupSheet(
            onDismiss = { showQuickGroupSheet = false },
            onCreate = { name, desc ->
                viewModel.quickCreateGroup(name, desc)
                showQuickGroupSheet = false
            }
        )
    }
}

@Composable
private fun CreatePostTopBar(
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = CloseIcon,
                contentDescription = stringResource(Res.string.create_post_close_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(Res.string.create_post_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(
            onClick = onSubmit,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(
                    if (isSubmitting) Res.string.create_post_submitting else Res.string.create_post_submit
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.4.sp
    )
}

@Composable
private fun FieldError(message: String?) {
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PhotosSection(
    images: List<String>,
    error: String?,
    onImagesAdded: (List<String>) -> Unit,
    onImageRemoved: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_photos))
        PostImagePickerRow(
            images = images,
            maxImages = CreatePostUiState.MAX_IMAGES,
            onImagesAdded = onImagesAdded,
            onImageRemoved = onImageRemoved
        )
        FieldError(error)
    }
}

@Composable
private fun TitleSection(
    title: String,
    error: String?,
    onTitleChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_title))
        InputBox {
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.create_post_title_placeholder),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
        }
        FieldError(error)
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    error: String?,
    onDescriptionChange: (String) -> Unit,
    remaining: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionLabel(stringResource(Res.string.create_post_section_description))
            Text(
                text = stringResource(Res.string.create_post_chars_remaining, remaining),
                style = MaterialTheme.typography.labelSmall,
                color = if (remaining < 20) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        InputBox(minHeight = 120.dp) {
            BasicTextField(
                value = description,
                onValueChange = onDescriptionChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (description.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.create_post_description_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
        }
        FieldError(error)
    }
}

@Composable
private fun LocationSection(
    address: String,
    suggestions: List<AddressResult>,
    locationLoading: Boolean,
    error: String?,
    onUseCurrentLocation: () -> Unit,
    onAddressChange: (String) -> Unit,
    onAddressSelected: (AddressResult) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_location))
        ActionChip(
            label = stringResource(Res.string.create_post_use_location),
            icon = LocationOnIcon,
            loading = locationLoading,
            onClick = onUseCurrentLocation
        )
        InputBox {
            BasicTextField(
                value = address,
                onValueChange = onAddressChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (address.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.create_post_address_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
        }
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddressSelected(suggestion) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = LocationOnIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = suggestion.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        FieldError(error)
    }
}

@Composable
private fun DateTimeSection(
    startsAt: Long?,
    endsAt: Long?,
    error: String?,
    onPickStart: (Long) -> Unit,
    onPickEnd: (Long?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_datetime))
        DateChip(
            prefix = stringResource(Res.string.create_post_starts),
            value = startsAt,
            isOptional = false,
            onClick = {
                val target = startsAt ?: (currentTimeMillis() + DEFAULT_START_OFFSET_MS)
                onPickStart(target)
            }
        )
        DateChip(
            prefix = stringResource(Res.string.create_post_ends),
            value = endsAt,
            isOptional = true,
            onClick = {
                val target = endsAt ?: ((startsAt ?: currentTimeMillis()) + 2 * 3_600_000L)
                onPickEnd(target)
            }
        )
        FieldError(error)
    }
}

@Composable
private fun DateChip(
    prefix: String,
    value: Long?,
    isOptional: Boolean,
    onClick: () -> Unit
) {
    val pickText = stringResource(Res.string.create_post_pick_date)
    val optionalText = stringResource(Res.string.create_post_optional)
    val label = when {
        value != null -> "$prefix · ${DateUtil.formatEventDate(value)}"
        isOptional -> "$prefix · $optionalText"
        else -> "$prefix · $pickText"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = DateRangeIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AudienceSection(
    state: CreatePostUiState,
    onSelectNeighborhood: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    val isNeighborhood = state.audience is PostAudience.Neighborhood
    val selectedGroupId = (state.audience as? PostAudience.GroupAudience)?.groupId
    var groupMenuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_audience))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isNeighborhood, onClick = onSelectNeighborhood)
            Text(
                text = stringResource(Res.string.create_post_audience_neighborhood),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = !isNeighborhood,
                onClick = {
                    val first = state.userGroups.firstOrNull()
                    if (first != null) onSelectGroup(first.id) else onCreateGroup()
                }
            )
            Text(
                text = stringResource(Res.string.create_post_audience_group),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        if (!isNeighborhood) {
            Box {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { groupMenuExpanded = true }
                ) {
                    val current = state.userGroups.firstOrNull { it.id == selectedGroupId }
                    Text(
                        text = current?.name ?: stringResource(Res.string.create_post_pick_group),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                DropdownMenu(
                    expanded = groupMenuExpanded,
                    onDismissRequest = { groupMenuExpanded = false }
                ) {
                    state.userGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                onSelectGroup(group.id)
                                groupMenuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(Res.string.create_post_create_new_group),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            groupMenuExpanded = false
                            onCreateGroup()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagsSection(
    tags: Set<Interest>,
    scrollState: ScrollState,
    onToggle: (Interest) -> Unit
) {
    val all = Interest.entries
    val scope = rememberCoroutineScope()
    val maxed = tags.size >= CreatePostUiState.MAX_TAGS
    var allTagsVisible by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_tags))
        Text(
            text = stringResource(Res.string.create_post_tags_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowChipRow {
            all.subList(0, 10).forEach { tag ->
                val selected = tag in tags
                val enabled = selected || !maxed
                TagChipToggle(
                    label = tag.displayName,
                    selected = selected,
                    enabled = enabled,
                    onClick = { onToggle(tag) }
                )
            }
            TagChipToggle(
                label = if (allTagsVisible) "Show Less" else "Show More",
                selected = false,
                isVisibilityToggle = true,
                enabled = true,
                onClick = {
                    allTagsVisible = !allTagsVisible
                    if (allTagsVisible) {
                        scope.launch {
                            scrollState.animateScrollTo(Int.MAX_VALUE)
                        }
                    }
                }
            )
        }
        AnimatedVisibility(allTagsVisible) {
            FlowChipRow {
                all.subList(11, all.size - 1).forEach { tag ->
                    val selected = tag in tags
                    val enabled = selected || !maxed
                    TagChipToggle(
                        label = tag.displayName,
                        selected = selected,
                        enabled = enabled,
                        onClick = { onToggle(tag) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun TagChipToggle(
    label: String,
    selected: Boolean,
    isVisibilityToggle: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = when {
        isVisibilityToggle -> BorderStroke(
            width = 1.dp, Brush.linearGradient(
                listOf(
                    Color.Green.copy(alpha = 0.6f),
                    Color.Green.copy(alpha = 0.6f)
                )
            )
        )

        else -> null
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        border = border,
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = onClick
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MaxAttendeesSection(
    count: Int?,
    noLimit: Boolean,
    onSetCount: (Int?) -> Unit,
    onSetNoLimit: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(Res.string.create_post_section_max))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.create_post_max_no_limit),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Switch(checked = noLimit, onCheckedChange = onSetNoLimit)
        }
        if (!noLimit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StepperButton(
                    icon = CloseIcon,
                    contentDescription = stringResource(Res.string.create_post_decrease_cd),
                    onClick = { onSetCount(((count ?: 4) - 1).coerceAtLeast(1)) }
                )
                Text(
                    text = (count ?: 4).toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StepperButton(
                    icon = AddIcon,
                    contentDescription = stringResource(Res.string.create_post_increase_cd),
                    onClick = { onSetCount(((count ?: 4) + 1).coerceAtMost(500)) }
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InputBox(
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .heightIn(min = minHeight - 24.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier
            .widthIn(min = 0.dp)
            .clickable(enabled = !loading, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (loading) "Locating…" else label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private const val DEFAULT_START_OFFSET_MS = 86_400_000L + 9 * 3_600_000L

@Preview
@Composable
private fun CreatePostTopBarPreview() {
    MaterialTheme {
        CreatePostTopBar(
            onClose = {},
            onSubmit = {},
            isSubmitting = false
        )
    }
}
