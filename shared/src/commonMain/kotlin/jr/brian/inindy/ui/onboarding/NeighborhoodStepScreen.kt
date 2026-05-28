package jr.brian.inindy.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import jr.brian.inindy.domain.model.Neighborhood
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.onboarding_continue
import jr.brian.inindy.resources.onboarding_neighborhood_subtitle
import jr.brian.inindy.resources.onboarding_neighborhood_title
import jr.brian.inindy.ui.auth.AuthHeading
import jr.brian.inindy.ui.brand.BrandMark
import androidx.compose.foundation.gestures.detectTapGestures
import org.jetbrains.compose.resources.stringResource

@Composable
fun NeighborhoodStepScreen(
    neighborhoods: List<Neighborhood>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            OnboardingStepIndicator(step = 2, totalSteps = 3)
            Spacer(modifier = Modifier.height(24.dp))
            AuthHeading(
                title = stringResource(Res.string.onboarding_neighborhood_title),
                subtitle = stringResource(Res.string.onboarding_neighborhood_subtitle)
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(neighborhoods, key = { it.id }) { hood ->
                    NeighborhoodRow(
                        neighborhood = hood,
                        selected = hood.id == selectedId,
                        onClick = { onSelect(hood.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                enabled = selectedId != null,
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
                    text = stringResource(Res.string.onboarding_continue),
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
private fun NeighborhoodRow(
    neighborhood: Neighborhood,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(containerColor, shape = RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .pointerInput(neighborhood.id) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = neighborhood.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
                )
            }
        }
    }
}

@Preview
@Composable
private fun NeighborhoodStepScreenPreview() {
    MaterialTheme {
        NeighborhoodStepScreen(
            neighborhoods = listOf(
                Neighborhood("1", "Broad Ripple", slug = "broad-ripple"),
                Neighborhood("2", "Fountain Square", slug = "fountain-square"),
                Neighborhood("3", "Mass Ave", slug = "mass-ave")
            ),
            selectedId = "2",
            error = null,
            onSelect = {},
            onContinue = {}
        )
    }
}
