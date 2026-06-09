package jr.brian.inindy.ui.creategroup

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.inindy.presentation.creategroup.CreateGroupIntent
import jr.brian.inindy.presentation.creategroup.CreateGroupUiState
import jr.brian.inindy.presentation.creategroup.CreateGroupViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.create_group_char_counter
import jr.brian.inindy.resources.create_group_close_cd
import jr.brian.inindy.resources.create_group_cover_label
import jr.brian.inindy.resources.create_group_cover_optional
import jr.brian.inindy.resources.create_group_description_label
import jr.brian.inindy.resources.create_group_description_optional
import jr.brian.inindy.resources.create_group_description_placeholder
import jr.brian.inindy.resources.create_group_loading_label
import jr.brian.inindy.resources.create_group_name_label
import jr.brian.inindy.resources.create_group_name_placeholder
import jr.brian.inindy.resources.create_group_name_required
import jr.brian.inindy.resources.create_group_submit
import jr.brian.inindy.resources.create_group_submitting
import jr.brian.inindy.resources.create_group_title
import jr.brian.inindy.ui.components.CoverPhotoPickerSection
import jr.brian.inindy.ui.icons.CloseIcon
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupScreen(
    onClose: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdGroupId) {
        state.createdGroupId?.let(onCreated)
    }

    CreateGroupScreenContent(
        state = state,
        onClose = onClose,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun CreateGroupScreenContent(
    state: CreateGroupUiState,
    onClose: () -> Unit,
    onIntent: (CreateGroupIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateGroupTopBar(
                isSubmitting = state.isSubmitting,
                onClose = onClose,
                onSubmit = { onIntent(CreateGroupIntent.Submit) }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CoverPhotoSection(
                    coverImageUri = state.coverImageUri,
                    onImageSelected = { onIntent(CreateGroupIntent.CoverImageSelected(it)) },
                    onRemove = { onIntent(CreateGroupIntent.RemoveCoverImage) }
                )
                NameSection(
                    name = state.name,
                    nameError = state.nameError,
                    onNameChange = { onIntent(CreateGroupIntent.NameChanged(it)) }
                )
                DescriptionSection(
                    description = state.description,
                    onDescriptionChange = { onIntent(CreateGroupIntent.DescriptionChanged(it)) }
                )
                state.submitError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (state.isSubmitting) {
            SubmittingOverlay()
        }
    }
}

@Composable
private fun CreateGroupTopBar(
    isSubmitting: Boolean,
    onClose: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onClose, enabled = !isSubmitting) {
            Icon(
                imageVector = CloseIcon,
                contentDescription = stringResource(Res.string.create_group_close_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(Res.string.create_group_title),
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
                    if (isSubmitting) Res.string.create_group_submitting
                    else Res.string.create_group_submit
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.4.sp
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CoverPhotoSection(
    coverImageUri: String?,
    onImageSelected: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(
            text = stringResource(Res.string.create_group_cover_label),
            trailing = stringResource(Res.string.create_group_cover_optional)
        )
        CoverPhotoPickerSection(
            currentImageUrl = null,
            newImageUri = coverImageUri,
            onImageSelected = onImageSelected,
            onImageRemoved = onRemove
        )
    }
}

@Composable
private fun NameSection(
    name: String,
    nameError: String?,
    onNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.create_group_name_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = stringResource(Res.string.create_group_name_required),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            CharacterCounter(
                current = name.length,
                max = CreateGroupUiState.NAME_MAX_LENGTH
            )
        }
        InputBox {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.create_group_name_placeholder),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
        }
        if (nameError != null) {
            Text(
                text = nameError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.create_group_description_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = stringResource(Res.string.create_group_description_optional),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            CharacterCounter(
                current = description.length,
                max = CreateGroupUiState.DESCRIPTION_MAX_LENGTH
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
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (description.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.create_group_description_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun CharacterCounter(current: Int, max: Int) {
    val nearLimit = current >= (max * 0.9f).toInt()
    Text(
        text = stringResource(Res.string.create_group_char_counter, current, max),
        style = MaterialTheme.typography.labelSmall,
        color = if (nearLimit) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun InputBox(
    minHeight: Dp = 56.dp,
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
private fun SubmittingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(Res.string.create_group_loading_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview
@Composable
private fun CreateGroupScreenEmptyPreview() {
    MaterialTheme {
        CreateGroupScreenContent(
            state = CreateGroupUiState(),
            onClose = {},
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun CreateGroupScreenFilledPreview() {
    MaterialTheme {
        CreateGroupScreenContent(
            state = CreateGroupUiState(
                coverImageUri = "https://picsum.photos/seed/group/800/400",
                name = "Broad Ripple Runners",
                description = "Weekly Saturday morning runs on the Monon."
            ),
            onClose = {},
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun CreateGroupScreenSubmittingPreview() {
    MaterialTheme {
        CreateGroupScreenContent(
            state = CreateGroupUiState(
                name = "Broad Ripple Runners",
                isSubmitting = true
            ),
            onClose = {},
            onIntent = {}
        )
    }
}
