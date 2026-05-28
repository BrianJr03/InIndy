package jr.brian.inindy.ui.auth

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.signin_subtitle
import jr.brian.inindy.resources.signin_title
import jr.brian.inindy.resources.signup_subtitle
import jr.brian.inindy.resources.signup_title
import jr.brian.inindy.resources.signup_with_email
import jr.brian.inindy.resources.signup_with_phone
import jr.brian.inindy.ui.icons.PersonIcon
import org.jetbrains.compose.resources.stringResource

enum class AuthMode { SIGN_UP, SIGN_IN }

@Composable
fun AuthMethodScreen(
    mode: AuthMode,
    onBack: () -> Unit,
    onPhone: () -> Unit,
    onEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            AuthTopBar(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthHeading(
                    title = stringResource(
                        if (mode == AuthMode.SIGN_UP) Res.string.signup_title
                        else Res.string.signin_title
                    ),
                    subtitle = stringResource(
                        if (mode == AuthMode.SIGN_UP) Res.string.signup_subtitle
                        else Res.string.signin_subtitle
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))
                MethodOption(
                    label = stringResource(Res.string.signup_with_phone),
                    description = "We'll text you a 6-digit code",
                    onClick = onPhone
                )
                Spacer(modifier = Modifier.height(12.dp))
                MethodOption(
                    label = stringResource(Res.string.signup_with_email),
                    description = "We'll email you a magic link",
                    onClick = onEmail
                )
            }
        }
    }
}

@Composable
private fun MethodOption(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(AuthCornerRadius),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PersonIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun AuthMethodScreenSignUpPreview() {
    MaterialTheme {
        AuthMethodScreen(
            mode = AuthMode.SIGN_UP,
            onBack = {},
            onPhone = {},
            onEmail = {}
        )
    }
}

@Preview
@Composable
private fun AuthMethodScreenSignInDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        AuthMethodScreen(
            mode = AuthMode.SIGN_IN,
            onBack = {},
            onPhone = {},
            onEmail = {}
        )
    }
}
