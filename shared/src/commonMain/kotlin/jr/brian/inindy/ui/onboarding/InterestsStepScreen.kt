package jr.brian.inindy.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.domain.model.Interest
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.onboarding_finish
import jr.brian.inindy.resources.onboarding_interests_min
import jr.brian.inindy.resources.onboarding_interests_subtitle
import jr.brian.inindy.resources.onboarding_interests_title
import jr.brian.inindy.ui.auth.AuthHeading
import jr.brian.inindy.ui.brand.BrandMark
import androidx.compose.foundation.gestures.detectTapGestures
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsStepScreen(
    interests: List<Interest>,
    selected: Set<Interest>,
    error: String?,
    onToggle: (Interest) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canFinish = selected.isNotEmpty()
    OnboardingBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BrandMark(size = 56.dp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            OnboardingStepIndicator(step = 3, totalSteps = 3)
            Spacer(modifier = Modifier.height(24.dp))
            AuthHeading(
                title = stringResource(Res.string.onboarding_interests_title),
                subtitle = stringResource(Res.string.onboarding_interests_subtitle)
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!canFinish) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.onboarding_interests_min),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    interests.forEach { interest ->
                        InterestChip(
                            interest = interest,
                            selected = interest in selected,
                            onClick = { onToggle(interest) }
                        )
                    }
                }
            }

            Button(
                onClick = onFinish,
                enabled = canFinish,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_finish),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InterestChip(
    interest: Interest,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .background(container, shape = RoundedCornerShape(20.dp))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .pointerInput(interest) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = interest.displayName,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = contentColor
        )
    }
}

@Preview
@Composable
private fun InterestsStepScreenPreview() {
    MaterialTheme {
        InterestsStepScreen(
            interests = Interest.entries,
            selected = setOf(Interest.HIKING, Interest.RUNNING),
            error = null,
            onToggle = {},
            onFinish = {}
        )
    }
}

@Preview
@Composable
private fun InterestsStepScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        InterestsStepScreen(
            interests = Interest.entries,
            selected = setOf(Interest.PICNICS, Interest.YOGA, Interest.BONFIRES),
            error = null,
            onToggle = {},
            onFinish = {}
        )
    }
}
