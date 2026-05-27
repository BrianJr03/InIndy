package jr.brian.inindy.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.detail_back_content_description
import jr.brian.inindy.ui.icons.ArrowBackIcon
import org.jetbrains.compose.resources.stringResource

/**
 * Floating circular back button for screens that overlay content (e.g. a hero image).
 * Status-bar safe area + 8dp top, 4dp start — matches the vertical placement of a
 * Material3 TopAppBar nav icon, so it lines up with screens like SettingsScreen.
 */
@Composable
fun FloatingBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(Res.string.detail_back_content_description)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 6.dp, shape = CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = ArrowBackIcon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
