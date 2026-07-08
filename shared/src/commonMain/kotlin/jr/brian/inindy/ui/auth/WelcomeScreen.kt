package jr.brian.inindy.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.welcome_continue_with_apple
import jr.brian.inindy.resources.welcome_continue_with_google
import jr.brian.inindy.resources.welcome_create_account
import jr.brian.inindy.resources.welcome_legal
import jr.brian.inindy.resources.welcome_or
import jr.brian.inindy.resources.welcome_sign_in
import jr.brian.inindy.resources.welcome_tagline
import jr.brian.inindy.ui.brand.BrandMark
import jr.brian.inindy.ui.brand.BrandWordmark
import jr.brian.inindy.ui.icons.AppleIcon
import jr.brian.inindy.ui.icons.GoogleIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                BrandMark()
                Spacer(modifier = Modifier.height(20.dp))
                BrandWordmark(
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp,
                        letterSpacing = (-1).sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.welcome_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateAccount,
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
                        text = stringResource(Res.string.welcome_create_account),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    )
                }

                TextButton(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.welcome_sign_in),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
//                    Text(
//                        text = stringResource(Res.string.welcome_or),
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.padding(horizontal = 12.dp)
//                    )
//                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

//                SocialButton(
//                    label = stringResource(Res.string.welcome_continue_with_google),
//                    iconContent = {
//                        Icon(
//                            imageVector = GoogleIcon,
//                            contentDescription = null,
//                            tint = Color.Unspecified,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    },
//                    onClick = onGoogle
//                )
//
//                SocialButton(
//                    label = stringResource(Res.string.welcome_continue_with_apple),
//                    iconContent = {
//                        Icon(
//                            imageVector = AppleIcon,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.onSurface,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    },
//                    onClick = onApple
//                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.welcome_legal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SocialButton(
    label: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        iconContent()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun WelcomeScreenLightPreview() {
    MaterialTheme {
        WelcomeScreen(
            onCreateAccount = {},
            onSignIn = {},
            onGoogle = {},
            onApple = {}
        )
    }
}

@Preview
@Composable
private fun WelcomeScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        WelcomeScreen(
            onCreateAccount = {},
            onSignIn = {},
            onGoogle = {},
            onApple = {}
        )
    }
}
