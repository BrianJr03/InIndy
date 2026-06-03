package jr.brian.inindy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.photo_picker_avatar_add
import jr.brian.inindy.resources.photo_picker_avatar_cd
import jr.brian.inindy.resources.photo_picker_avatar_change
import jr.brian.inindy.ui.icons.CameraAltIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun AvatarPickerSection(
    currentImageUrl: String?,
    newImageUri: String?,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialFallback: String = "?"
) {
    var showSheet by remember { mutableStateOf(false) }
    val avatarCdDescription = stringResource(Res.string.photo_picker_avatar_cd)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val displayModel = newImageUri ?: currentImageUrl?.let(::avatarVariantUrl)
            if (!displayModel.isNullOrBlank()) {
                AsyncImage(
                    model = displayModel,
                    contentDescription = avatarCdDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable { showSheet = true }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .clickable { showSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialFallback,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp,
                modifier = Modifier.size(28.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CameraAltIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp).padding(2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val label = if (newImageUri != null || currentImageUrl != null) {
            stringResource(Res.string.photo_picker_avatar_change)
        } else {
            stringResource(Res.string.photo_picker_avatar_add)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { showSheet = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    if (showSheet) {
        PhotoPickerSheet(
            mode = PhotoPickerMode.Single,
            onPhotoSelected = {
                onImageSelected(it)
                showSheet = false
            },
            onPhotosSelected = { },
            onDismiss = { showSheet = false }
        )
    }
}

private fun avatarVariantUrl(cdnUrl: String): String =
    if (cdnUrl.contains("?")) cdnUrl else "$cdnUrl?width=200&height=200&fit=cover"
