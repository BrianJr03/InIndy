package jr.brian.inindy.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.onboarding_continue
import jr.brian.inindy.resources.onboarding_profile_avatar_add
import jr.brian.inindy.resources.onboarding_profile_name_label
import jr.brian.inindy.resources.onboarding_profile_subtitle
import jr.brian.inindy.resources.onboarding_profile_title
import jr.brian.inindy.ui.auth.AuthHeading
import jr.brian.inindy.ui.brand.BrandMark
import jr.brian.inindy.ui.icons.PersonIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileStepScreen(
    error: String?,
    onContinue: (fullName: String, avatarUri: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    val avatarUri by remember { mutableStateOf<String?>(null) }
    val canContinue = name.isNotBlank()

    OnboardingBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
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
            OnboardingStepIndicator(step = 1, totalSteps = 3)
            Spacer(modifier = Modifier.height(24.dp))
            AuthHeading(
                title = stringResource(Res.string.onboarding_profile_title),
                subtitle = stringResource(Res.string.onboarding_profile_subtitle)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AvatarPicker(avatarUri = avatarUri)
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.onboarding_profile_name_label)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = error != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { onContinue(name, avatarUri) },
                enabled = canContinue,
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
private fun AvatarPicker(
    avatarUri: String?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.18f),
                            tertiary.copy(alpha = 0.18f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = primary.copy(alpha = 0.30f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PersonIcon,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = stringResource(Res.string.onboarding_profile_avatar_add),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview
@Composable
private fun ProfileStepScreenPreview() {
    MaterialTheme {
        ProfileStepScreen(error = null, onContinue = { _, _ -> })
    }
}

@Preview
@Composable
private fun ProfileStepScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        ProfileStepScreen(error = null, onContinue = { _, _ -> })
    }
}
