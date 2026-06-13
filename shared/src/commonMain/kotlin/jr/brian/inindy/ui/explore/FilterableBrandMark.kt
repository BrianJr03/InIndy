package jr.brian.inindy.ui.explore

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.explore_filter_arrow_cd
import jr.brian.inindy.ui.icons.KeyboardArrowDownIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilterableBrandMark(
    text: String,
    activeFilter: ExploreFilter = ExploreFilter.All,
    isDropdownOpen: Boolean,
    onArrowClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isDropdownOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "filterArrowRotation"
    )

    val wordmark = buildAnnotatedString {
        val accentPrefix = "In"
        if (text.startsWith(accentPrefix) && text.length > accentPrefix.length) {
            withStyle(SpanStyle(color = accentColor)) { append(accentPrefix) }
            withStyle(SpanStyle(color = baseColor)) {
                append(text.removePrefix(accentPrefix))
            }
        } else {
            withStyle(SpanStyle(color = baseColor)) { append(text) }
        }
    }

    fun TextStyle.additionalProps(): TextStyle {
        return this.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
    }

    val headerTextStyle =
        if (activeFilter == ExploreFilter.All) MaterialTheme.typography.headlineLarge
        else MaterialTheme.typography.headlineSmall

    Row(
        modifier = modifier.clickable(
            onClick = onArrowClick,
            indication = null,
            interactionSource = MutableInteractionSource()
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = wordmark,
            style = headerTextStyle.additionalProps(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onArrowClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = KeyboardArrowDownIcon,
                contentDescription = stringResource(Res.string.explore_filter_arrow_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrowRotation)
            )
        }
    }
}

@Preview
@Composable
private fun FilterableBrandMarkClosedPreview() {
    MaterialTheme {
        FilterableBrandMark(
            text = "InIndy",
            isDropdownOpen = false,
            onArrowClick = {}
        )
    }
}

@Preview
@Composable
private fun FilterableBrandMarkOpenPreview() {
    MaterialTheme {
        FilterableBrandMark(
            text = "InBroadRipple",
            isDropdownOpen = true,
            onArrowClick = {}
        )
    }
}
