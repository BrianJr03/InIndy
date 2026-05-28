package jr.brian.inindy.ui.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.otp_hint
import jr.brian.inindy.resources.otp_resend
import jr.brian.inindy.resources.otp_resend_in
import jr.brian.inindy.resources.otp_subtitle
import jr.brian.inindy.resources.otp_title
import jr.brian.inindy.resources.otp_verify
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val OTP_LENGTH = 6
private const val RESEND_SECONDS = 60

@Composable
fun OtpScreen(
    phone: String,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onVerify: (code: String) -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var resendIn by remember { mutableStateOf(RESEND_SECONDS) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(resendIn) {
        if (resendIn > 0) {
            delay(1_000L)
            resendIn -= 1
        }
    }

    LaunchedEffect(code) {
        if (code.length == OTP_LENGTH) {
            onVerify(code)
        }
    }

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
                    title = stringResource(Res.string.otp_title),
                    subtitle = stringResource(Res.string.otp_subtitle, phone.takeLast(4))
                )
                Spacer(modifier = Modifier.height(32.dp))

                BasicTextField(
                    value = code,
                    onValueChange = { input ->
                        code = input.filter { it.isDigit() }.take(OTP_LENGTH)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle.Default,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(OTP_LENGTH) { index ->
                                OtpBox(
                                    value = code.getOrNull(index)?.toString().orEmpty(),
                                    isError = errorMessage != null,
                                    isFocused = index == code.length
                                )
                            }
                        }
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.otp_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onVerify(code) },
                    enabled = code.length == OTP_LENGTH && !isLoading,
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
                            text = stringResource(Res.string.otp_verify),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (resendIn > 0) {
                        Text(
                            text = stringResource(Res.string.otp_resend_in, resendIn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        TextButton(
                            onClick = {
                                onResend()
                                resendIn = RESEND_SECONDS
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.otp_resend),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpBox(
    value: String,
    isError: Boolean,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 56.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun OtpScreenPreview() {
    MaterialTheme {
        OtpScreen(
            phone = "+13175551234",
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onVerify = {},
            onResend = {}
        )
    }
}

@Preview
@Composable
private fun OtpScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        OtpScreen(
            phone = "+13175551234",
            isLoading = false,
            errorMessage = "That code didn't work. Try again.",
            onBack = {},
            onVerify = {},
            onResend = {}
        )
    }
}
