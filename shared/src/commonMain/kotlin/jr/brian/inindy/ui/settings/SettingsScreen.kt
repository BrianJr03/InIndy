package jr.brian.inindy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.inindy.presentation.settings.DeleteAccountState
import jr.brian.inindy.presentation.settings.SettingsViewModel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.settings_back_content_description
import jr.brian.inindy.resources.settings_dark_mode_description
import jr.brian.inindy.resources.settings_dark_mode_title
import jr.brian.inindy.resources.settings_feed_interest_ordering_description
import jr.brian.inindy.resources.settings_feed_interest_ordering_title
import jr.brian.inindy.resources.settings_section_feed
import jr.brian.inindy.resources.settings_delete_account_confirm_word
import jr.brian.inindy.resources.settings_delete_account_description
import jr.brian.inindy.resources.settings_delete_account_dialog_body
import jr.brian.inindy.resources.settings_delete_account_dialog_cancel
import jr.brian.inindy.resources.settings_delete_account_dialog_confirm
import jr.brian.inindy.resources.settings_delete_account_dialog_confirm_hint
import jr.brian.inindy.resources.settings_delete_account_dialog_title
import jr.brian.inindy.resources.settings_delete_account_error_generic
import jr.brian.inindy.resources.settings_delete_account_title
import jr.brian.inindy.resources.settings_section_account
import jr.brian.inindy.resources.settings_section_appearance
import jr.brian.inindy.resources.settings_title
import jr.brian.inindy.ui.icons.ArrowBackIcon
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        onBack = onBack,
        deleteState = uiState.deleteAccount,
        feedInterestOrderingEnabled = uiState.feedInterestOrderingEnabled,
        onSetFeedInterestOrdering = viewModel::setInterestOrdering,
        onDeleteAccountConfirmed = viewModel::deleteAccount,
        onDismissDeleteError = viewModel::dismissError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SettingsContent(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    deleteState: DeleteAccountState,
    feedInterestOrderingEnabled: Boolean,
    onSetFeedInterestOrdering: (Boolean) -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    onDismissDeleteError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isConfirmDialogVisible by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = stringResource(Res.string.settings_back_content_description),
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel(text = stringResource(Res.string.settings_section_appearance))
            SettingToggleRow(
                title = stringResource(Res.string.settings_dark_mode_title),
                description = stringResource(Res.string.settings_dark_mode_description),
                checked = isDarkMode,
                onCheckedChange = onToggleDarkMode
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel(text = stringResource(Res.string.settings_section_feed))
            SettingToggleRow(
                title = stringResource(Res.string.settings_feed_interest_ordering_title),
                description = stringResource(Res.string.settings_feed_interest_ordering_description),
                checked = feedInterestOrderingEnabled,
                onCheckedChange = onSetFeedInterestOrdering
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel(
                text = stringResource(Res.string.settings_section_account),
                tint = MaterialTheme.colorScheme.error
            )
            DeleteAccountRow(
                state = deleteState,
                onClick = {
                    if (deleteState !is DeleteAccountState.Deleting) {
                        onDismissDeleteError()
                        isConfirmDialogVisible = true
                    }
                }
            )
            val errorState = deleteState as? DeleteAccountState.Error
            if (errorState != null) {
                Text(
                    text = errorState.message.ifBlank {
                        stringResource(Res.string.settings_delete_account_error_generic)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (isConfirmDialogVisible) {
        DeleteAccountConfirmDialog(
            isDeleting = deleteState is DeleteAccountState.Deleting,
            onConfirm = onDeleteAccountConfirmed,
            onDismiss = {
                if (deleteState !is DeleteAccountState.Deleting) {
                    isConfirmDialogVisible = false
                }
            }
        )
    }
}

@Composable
private fun DeleteAccountRow(
    state: DeleteAccountState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeleting = state is DeleteAccountState.Deleting
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
            .clickable(enabled = !isDeleting, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_delete_account_title),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.settings_delete_account_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DeleteAccountConfirmDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmWord = stringResource(Res.string.settings_delete_account_confirm_word)
    var typed by remember { mutableStateOf("") }
    val isConfirmEnabled = !isDeleting && typed.trim() == confirmWord

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.settings_delete_account_dialog_title),
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(Res.string.settings_delete_account_dialog_body))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    enabled = !isDeleting,
                    label = {
                        Text(text = stringResource(Res.string.settings_delete_account_dialog_confirm_hint))
                    },
                    placeholder = { Text(text = confirmWord) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.settings_delete_account_dialog_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(text = stringResource(Res.string.settings_delete_account_dialog_cancel))
            }
        }
    )
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
        ),
        color = tint,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview
@Composable
private fun SettingsScreenLightPreview() {
    MaterialTheme {
        SettingsContent(
            isDarkMode = false,
            onToggleDarkMode = {},
            onBack = {},
            deleteState = DeleteAccountState.Idle,
            feedInterestOrderingEnabled = false,
            onSetFeedInterestOrdering = {},
            onDeleteAccountConfirmed = {},
            onDismissDeleteError = {}
        )
    }
}

@Preview
@Composable
private fun SettingsScreenDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        SettingsContent(
            isDarkMode = true,
            onToggleDarkMode = {},
            onBack = {},
            deleteState = DeleteAccountState.Idle,
            feedInterestOrderingEnabled = true,
            onSetFeedInterestOrdering = {},
            onDeleteAccountConfirmed = {},
            onDismissDeleteError = {}
        )
    }
}
