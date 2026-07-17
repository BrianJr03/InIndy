package jr.brian.inindy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Displays an image at its native aspect ratio (ContentScale.Fit) layered
 * over a blurred, cropped copy of the same image that fills any letterbox
 * space. Portrait and landscape source photos both render fully — the blurred
 * backdrop makes letterboxing feel intentional rather than empty (Instagram /
 * Twitter photo-viewer pattern).
 *
 * Fallback behavior: Modifier.blur() is a no-op on Android API 30 and below.
 * On those devices the backdrop is unblurred but still fills the frame, which
 * still looks better than pure black bars. On API 31+ and iOS the blur runs
 * GPU-side and is essentially free.
 */
@Composable
fun AdaptiveMediaFrame(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    blurRadius: Dp = 24.dp,
    backdropColor: Color = Color.Black
) {
    Box(modifier = modifier.background(backdropColor)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        )
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
