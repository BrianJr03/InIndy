package jr.brian.inindy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jr.brian.inindy.data.media.AppSettingsOpener
import jr.brian.inindy.data.media.CameraCapture
import jr.brian.inindy.data.media.CameraResult
import jr.brian.inindy.data.media.ImageCompressor
import jr.brian.inindy.data.media.ImagePicker
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.photo_picker_camera_icon_cd
import jr.brian.inindy.resources.photo_picker_choose_gallery
import jr.brian.inindy.resources.photo_picker_choose_gallery_with_count
import jr.brian.inindy.resources.photo_picker_error_generic
import jr.brian.inindy.resources.photo_picker_gallery_icon_cd
import jr.brian.inindy.resources.photo_picker_open_settings
import jr.brian.inindy.resources.photo_picker_permission_blocked_body
import jr.brian.inindy.resources.photo_picker_permission_blocked_title
import jr.brian.inindy.resources.photo_picker_permission_cancel
import jr.brian.inindy.resources.photo_picker_permission_denied
import jr.brian.inindy.resources.photo_picker_take_photo
import jr.brian.inindy.ui.icons.CameraAltIcon
import jr.brian.inindy.ui.icons.PhotoLibraryIcon
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

sealed class PhotoPickerMode {
    data object Single : PhotoPickerMode()
    data class Multiple(val max: Int) : PhotoPickerMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerSheet(
    mode: PhotoPickerMode,
    onPhotoSelected: (String) -> Unit,
    onPhotosSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cameraCapture: CameraCapture = koinInject(),
    imagePicker: ImagePicker = koinInject(),
    imageCompressor: ImageCompressor = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingLaunch by remember { mutableStateOf<PendingLaunch?>(null) }
    var sheetVisible by remember { mutableStateOf(true) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var transientMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val permissionDeniedText = stringResource(Res.string.photo_picker_permission_denied)
    val genericErrorText = stringResource(Res.string.photo_picker_error_generic)

    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetVisible = false
                onDismiss()
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier
        ) {
            Column(modifier = Modifier.navigationBarsPadding().fillMaxWidth()) {
//                ListItem(
//                    headlineContent = { Text(stringResource(Res.string.photo_picker_take_photo)) },
//                    leadingContent = {
//                        Icon(
//                            imageVector = CameraAltIcon,
//                            contentDescription = stringResource(Res.string.photo_picker_camera_icon_cd),
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    },
//                    colors = ListItemDefaults.colors(
//                        containerColor = MaterialTheme.colorScheme.surface
//                    ),
//                    modifier = Modifier.clickable(enabled = !isProcessing) {
//                        pendingLaunch = PendingLaunch.Camera
//                        sheetVisible = false
//                    }
//                )
//                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                val galleryLabel = when (mode) {
                    is PhotoPickerMode.Single -> stringResource(Res.string.photo_picker_choose_gallery)
                    is PhotoPickerMode.Multiple -> stringResource(
                        Res.string.photo_picker_choose_gallery_with_count,
                        mode.max
                    )
                }
                ListItem(
                    headlineContent = { Text(galleryLabel) },
                    leadingContent = {
                        Icon(
                            imageVector = PhotoLibraryIcon,
                            contentDescription = stringResource(Res.string.photo_picker_gallery_icon_cd),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.clickable(enabled = !isProcessing) {
                        pendingLaunch = PendingLaunch.Gallery
                        sheetVisible = false
                    }
                )
                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pendingLaunch }
            .filterNotNull()
            .collect { launch ->
                pendingLaunch = null
                isProcessing = true
                try {
                    when (launch) {
                        PendingLaunch.Camera -> handleCamera(
                            mode = mode,
                            cameraCapture = cameraCapture,
                            imageCompressor = imageCompressor,
                            onPhotoSelected = onPhotoSelected,
                            onPhotosSelected = onPhotosSelected,
                            onDone = { onDismiss() },
                            reopenSheet = { sheetVisible = true },
                            showSnackbar = { msg ->
                                sheetVisible = true
                                transientMessage = msg
                            },
                            showBlocked = {
                                sheetVisible = true
                                showBlockedDialog = true
                            },
                            permissionDeniedText = permissionDeniedText,
                            genericErrorText = genericErrorText
                        )
                        PendingLaunch.Gallery -> handleGallery(
                            mode = mode,
                            imagePicker = imagePicker,
                            imageCompressor = imageCompressor,
                            onPhotoSelected = onPhotoSelected,
                            onPhotosSelected = onPhotosSelected,
                            onDone = { onDismiss() },
                            reopenSheet = { sheetVisible = true },
                            showSnackbar = { msg ->
                                sheetVisible = true
                                transientMessage = msg
                            },
                            genericErrorText = genericErrorText
                        )
                    }
                } finally {
                    isProcessing = false
                }
            }
    }

    transientMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { transientMessage = null },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { transientMessage = null }) {
                    Text(stringResource(Res.string.photo_picker_permission_cancel))
                }
            }
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(stringResource(Res.string.photo_picker_permission_blocked_title)) },
            text = { Text(stringResource(Res.string.photo_picker_permission_blocked_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBlockedDialog = false
                    appSettingsOpener.open()
                }) {
                    Text(stringResource(Res.string.photo_picker_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedDialog = false }) {
                    Text(stringResource(Res.string.photo_picker_permission_cancel))
                }
            }
        )
    }
}

private enum class PendingLaunch { Camera, Gallery }

private suspend fun handleCamera(
    mode: PhotoPickerMode,
    cameraCapture: CameraCapture,
    imageCompressor: ImageCompressor,
    onPhotoSelected: (String) -> Unit,
    onPhotosSelected: (List<String>) -> Unit,
    onDone: () -> Unit,
    reopenSheet: () -> Unit,
    showSnackbar: (String) -> Unit,
    showBlocked: () -> Unit,
    permissionDeniedText: String,
    genericErrorText: String
) {
    when (val result = cameraCapture.capturePhoto()) {
        is CameraResult.Success -> {
            val compressed = runCatching { imageCompressor.compressToFile(result.uri) }.getOrNull()
            if (compressed == null) {
                showSnackbar(genericErrorText)
            } else {
                when (mode) {
                    is PhotoPickerMode.Single -> onPhotoSelected(compressed)
                    is PhotoPickerMode.Multiple -> onPhotosSelected(listOf(compressed))
                }
                onDone()
            }
        }
        CameraResult.Cancelled -> reopenSheet()
        CameraResult.PermissionDenied -> showSnackbar(permissionDeniedText)
        CameraResult.PermissionPermanentlyDenied -> showBlocked()
        is CameraResult.Error -> showSnackbar(genericErrorText)
    }
}

private suspend fun handleGallery(
    mode: PhotoPickerMode,
    imagePicker: ImagePicker,
    imageCompressor: ImageCompressor,
    onPhotoSelected: (String) -> Unit,
    onPhotosSelected: (List<String>) -> Unit,
    onDone: () -> Unit,
    reopenSheet: () -> Unit,
    showSnackbar: (String) -> Unit,
    genericErrorText: String
) {
    when (mode) {
        is PhotoPickerMode.Single -> {
            val uri = imagePicker.pickSingle()
            if (uri == null) {
                reopenSheet()
                return
            }
            val compressed = runCatching { imageCompressor.compressToFile(uri) }.getOrNull()
            if (compressed == null) {
                showSnackbar(genericErrorText)
            } else {
                onPhotoSelected(compressed)
                onDone()
            }
        }
        is PhotoPickerMode.Multiple -> {
            val uris = imagePicker.pickMultiple(mode.max)
            if (uris.isEmpty()) {
                reopenSheet()
                return
            }
            val compressed = uris.mapNotNull { raw ->
                runCatching { imageCompressor.compressToFile(raw) }.getOrNull()
            }
            if (compressed.isEmpty()) {
                showSnackbar(genericErrorText)
            } else {
                onPhotosSelected(compressed)
                onDone()
            }
        }
    }
}
