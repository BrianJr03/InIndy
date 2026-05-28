package jr.brian.inindy.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.domain.model.CountryCode
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.phone_continue
import jr.brian.inindy.resources.phone_field_label
import jr.brian.inindy.resources.phone_invalid
import jr.brian.inindy.resources.phone_subtitle
import jr.brian.inindy.resources.phone_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhoneScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    val country = CountryCode.DEFAULT
    val invalidLocal = stringResource(Res.string.phone_invalid)
    val sanitized = phone.filter { it.isDigit() }
    val isValid = sanitized.length == 10
    val displayError = errorMessage ?: if (phone.isNotEmpty() && !isValid) invalidLocal else null

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
                    title = stringResource(Res.string.phone_title),
                    subtitle = stringResource(Res.string.phone_subtitle)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CountryDialChip(country = country)
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter { it.isDigit() }.take(10)
                        },
                        label = { Text(stringResource(Res.string.phone_field_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = displayError != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    )
                }
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
                    onClick = { onContinue("${country.dialCode}$sanitized") },
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
                            text = stringResource(Res.string.phone_continue),
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

@Composable
private fun CountryDialChip(country: CountryCode, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(64.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = country.dialCode,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.width(96.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Preview
@Composable
private fun PhoneScreenPreview() {
    MaterialTheme {
        PhoneScreen(
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onContinue = {}
        )
    }
}

@Preview
@Composable
private fun PhoneScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        PhoneScreen(
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onContinue = {}
        )
    }
}
