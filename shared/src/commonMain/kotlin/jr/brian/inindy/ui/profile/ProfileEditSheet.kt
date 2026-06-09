package jr.brian.inindy.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.domain.model.Neighborhood
import jr.brian.inindy.presentation.profileedit.ProfileEditIntent
import jr.brian.inindy.presentation.profileedit.ProfileEditUiState
import jr.brian.inindy.presentation.profileedit.ProfileEditViewModel
import jr.brian.inindy.presentation.profileedit.hasChanges
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.profile_edit_interests_hint
import jr.brian.inindy.resources.profile_edit_interests_label
import jr.brian.inindy.resources.profile_edit_name_label
import jr.brian.inindy.resources.profile_edit_name_required
import jr.brian.inindy.resources.profile_edit_neighborhood_label
import jr.brian.inindy.resources.profile_edit_save
import jr.brian.inindy.resources.profile_edit_selected_cd
import jr.brian.inindy.resources.profile_edit_title
import jr.brian.inindy.ui.components.AvatarPickerSection
import jr.brian.inindy.ui.icons.CheckIcon
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(
    onDismiss: () -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dismiss = {
        onDismiss()
        viewModel.onIntent(ProfileEditIntent.Dismiss)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            dismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ProfileEditSheetContent(
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}

@Composable
private fun ProfileEditSheetContent(
    state: ProfileEditUiState,
    onIntent: (ProfileEditIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(Res.string.profile_edit_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        AvatarPickerSection(
            currentImageUrl = state.currentAvatarUrl,
            newImageUri = state.newAvatarUri,
            onImageSelected = { onIntent(ProfileEditIntent.AvatarSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
            initialFallback = state.fullName.firstOrNull()?.uppercase() ?: "?"
        )

        NameSection(
            name = state.fullName,
            error = state.nameError,
            onChange = { onIntent(ProfileEditIntent.NameChanged(it)) }
        )

        NeighborhoodSection(
            neighborhoods = state.neighborhoods,
            selectedId = state.neighborhoodId,
            onSelect = { onIntent(ProfileEditIntent.NeighborhoodSelected(it)) }
        )

        InterestsSection(
            selected = state.selectedInterests,
            onToggle = { onIntent(ProfileEditIntent.ToggleInterest(it)) }
        )

        if (state.saveError != null) {
            Text(
                text = state.saveError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }

        SaveButton(
            isSaving = state.isSaving,
            hasChanges = state.hasChanges,
            onSave = { onIntent(ProfileEditIntent.Save) }
        )
    }
}

@Composable
private fun NameSection(
    name: String,
    error: String?,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(
            label = stringResource(Res.string.profile_edit_name_label),
        )
        OutlinedTextField(
            value = name,
            onValueChange = onChange,
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeighborhoodSection(
    neighborhoods: List<Neighborhood>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = neighborhoods.firstOrNull { it.id == selectedId }?.name ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(
            label = stringResource(Res.string.profile_edit_neighborhood_label),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                neighborhoods.forEach { neighborhood ->
                    val isSelected = neighborhood.id == selectedId
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = neighborhood.name,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = CheckIcon,
                                    contentDescription = stringResource(Res.string.profile_edit_selected_cd),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onSelect(neighborhood.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsSection(
    selected: Set<Interest>,
    onToggle: (Interest) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(
            label = stringResource(Res.string.profile_edit_interests_label),
        )
        if (selected.isEmpty()) {
            Text(
                text = stringResource(Res.string.profile_edit_interests_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Interest.entries.forEach { interest ->
                InterestChip(
                    interest = interest,
                    selected = interest in selected,
                    onClick = { onToggle(interest) }
                )
            }
        }
    }
}

@Composable
private fun InterestChip(
    interest: Interest,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .background(container, shape = RoundedCornerShape(20.dp))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .pointerInput(interest) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = interest.displayName,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = contentColor
        )
    }
}

@Composable
private fun SaveButton(
    isSaving: Boolean,
    hasChanges: Boolean,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (hasChanges) 1f else 0.5f)
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(Res.string.profile_edit_save),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.4.sp
        )
        Text(
            text = stringResource(Res.string.profile_edit_name_required),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 4.dp)
        )

    }
}

