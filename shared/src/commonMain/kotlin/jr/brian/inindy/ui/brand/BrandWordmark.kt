package jr.brian.inindy.ui.brand

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.explore_app_title
import jr.brian.inindy.resources.explore_app_title_accent
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp
    )
) {
    val fullTitle = stringResource(Res.string.explore_app_title)
    val accent = stringResource(Res.string.explore_app_title_accent)

    val wordmark = buildAnnotatedString {
        if (fullTitle.startsWith(accent)) {
            withStyle(SpanStyle(color = accentColor)) { append(accent) }
            withStyle(SpanStyle(color = baseColor)) {
                append(fullTitle.removePrefix(accent))
            }
        } else {
            withStyle(SpanStyle(color = baseColor)) { append(fullTitle) }
        }
    }

    Text(
        text = wordmark,
        style = style,
        modifier = modifier
    )
}
