package jr.brian.inindy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.photo_picker_post_add_cd
import jr.brian.inindy.resources.photo_picker_post_remove_cd
import jr.brian.inindy.ui.icons.AddAPhotoIcon
import jr.brian.inindy.ui.icons.CloseIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun PostImagePickerRow(
    images: List<String>,
    maxImages: Int,
    onImagesAdded: (List<String>) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val canAddMore = images.size < maxImages

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canAddMore) {
            item(key = "__add_button__") {
                AddImageButton(onClick = { showSheet = true })
            }
        }
        items(images, key = { it }) { uri ->
            ImageThumbnail(uri = uri, onRemove = { onImageRemoved(uri) })
        }
    }

    if (showSheet) {
        val remaining = (maxImages - images.size).coerceAtLeast(1)
        PhotoPickerSheet(
            mode = if (remaining == 1) PhotoPickerMode.Single
            else PhotoPickerMode.Multiple(remaining),
            onPhotoSelected = {
                onImagesAdded(listOf(it))
                showSheet = false
            },
            onPhotosSelected = {
                onImagesAdded(it)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun AddImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AddAPhotoIcon,
            contentDescription = stringResource(Res.string.photo_picker_post_add_cd),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ImageThumbnail(uri: String, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
        )
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clickable(onClick = onRemove)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = stringResource(Res.string.photo_picker_post_remove_cd),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
