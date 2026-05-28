package jr.brian.inindy.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.email_continue
import jr.brian.inindy.resources.email_field_label
import jr.brian.inindy.resources.email_invalid
import jr.brian.inindy.resources.email_subtitle
import jr.brian.inindy.resources.email_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmailScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    val invalidLocal = stringResource(Res.string.email_invalid)
    val isValid = email.matches(EMAIL_REGEX)
    val displayError = errorMessage ?: if (email.isNotEmpty() && !isValid) invalidLocal else null

    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            AuthTopBar(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthHeading(
                    title = stringResource(Res.string.email_title),
                    subtitle = stringResource(Res.string.email_subtitle)
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text(stringResource(Res.string.email_field_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = displayError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onContinue(email) },
                    enabled = isValid && !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.email_continue),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@Preview
@Composable
private fun EmailScreenPreview() {
    MaterialTheme {
        EmailScreen(
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onContinue = {}
        )
    }
}

@Preview
@Composable
private fun EmailScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        EmailScreen(
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onContinue = {}
        )
    }
}
