package jr.brian.inindy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.photo_picker_cover_cd
import jr.brian.inindy.resources.photo_picker_cover_change
import jr.brian.inindy.resources.photo_picker_cover_cta
import jr.brian.inindy.resources.photo_picker_cover_remove_cd
import jr.brian.inindy.ui.icons.AddAPhotoIcon
import jr.brian.inindy.ui.icons.CloseIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoverPhotoPickerSection(
    currentImageUrl: String?,
    newImageUri: String?,
    onImageSelected: (String) -> Unit,
    onImageRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val displayModel = newImageUri ?: currentImageUrl?.let(::coverVariantUrl)
    val coverCd = stringResource(Res.string.photo_picker_cover_cd)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { showSheet = true }
    ) {
        if (displayModel != null) {
            AsyncImage(
                model = displayModel,
                contentDescription = coverCd,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .clickable {
                        onImageRemoved()
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CloseIcon,
                        contentDescription = stringResource(Res.string.photo_picker_cover_remove_cd),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.photo_picker_cover_change),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AddAPhotoIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.photo_picker_cover_cta),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
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

private fun coverVariantUrl(cdnUrl: String): String =
    if (cdnUrl.contains("?")) cdnUrl else "$cdnUrl?width=800&fit=cover"
