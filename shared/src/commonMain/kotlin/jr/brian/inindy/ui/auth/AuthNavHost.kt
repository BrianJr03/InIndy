package jr.brian.inindy.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.inindy.domain.model.User
import jr.brian.inindy.presentation.auth.AuthIntent
import jr.brian.inindy.presentation.auth.AuthUiState
import jr.brian.inindy.presentation.auth.AuthViewModel
import jr.brian.inindy.ui.motion.LocalReducedMotion
import jr.brian.inindy.ui.motion.Motion
import org.koin.compose.viewmodel.koinViewModel

private sealed class AuthRoute {
    // Depth in the linear auth flow — used by AnimatedContent below to pick
    // push (deeper) vs pop (shallower) transitions instead of guessing at
    // direction. Intro/Welcome/Method/Phone|Email/Otp|EmailLinkSent stack in
    // that order.
    abstract val order: Int

    data object Intro : AuthRoute() { override val order = 0 }
    data object Welcome : AuthRoute() { override val order = 1 }
    data class Method(val mode: AuthMode) : AuthRoute() { override val order = 2 }
    data class Phone(val mode: AuthMode) : AuthRoute() { override val order = 3 }
    data class Email(val mode: AuthMode) : AuthRoute() { override val order = 3 }
    data class Otp(val phone: String) : AuthRoute() { override val order = 4 }
    data class EmailLinkSent(val email: String) : AuthRoute() { override val order = 4 }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AuthNavHost(
    onAuthenticated: (User) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var route by remember { mutableStateOf<AuthRoute>(AuthRoute.Intro) }
    var pendingPhone by remember { mutableStateOf("") }
    var pendingEmail by remember { mutableStateOf("") }
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(state) {
        when (val current = state) {
            is AuthUiState.Authenticated -> {
                onAuthenticated(current.user)
                viewModel.onIntent(AuthIntent.Reset)
            }
            AuthUiState.OtpSent -> {
                route = AuthRoute.Otp(pendingPhone)
            }
            AuthUiState.EmailLinkSent -> {
                route = AuthRoute.EmailLinkSent(pendingEmail)
            }
            else -> Unit
        }
    }

    val errorMessage = (state as? AuthUiState.Error)?.message
    val isLoading = state is AuthUiState.Loading

    BackHandler(enabled = route !is AuthRoute.Intro) {
        goBack(route)?.let { route = it; viewModel.onIntent(AuthIntent.Reset) }
    }

    // Uses the route order to slide forward (push) when going deeper and
    // slide back (pop) when returning — direction inference by ordinal is
    // safer than "always fade" and matches the iOS-style spatial personality.
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            if (targetState.order >= initialState.order) {
                Motion.pushEnter(reducedMotion) togetherWith Motion.pushExit(reducedMotion)
            } else {
                Motion.popEnter(reducedMotion) togetherWith Motion.popExit(reducedMotion)
            }
        },
        contentKey = { it::class.simpleName ?: it.toString() },
        label = "auth-nav"
    ) { current ->
        when (current) {
            AuthRoute.Intro -> IntroScreen(
                onFinish = { route = AuthRoute.Welcome },
                modifier = modifier
            )
            AuthRoute.Welcome -> WelcomeScreen(
                onCreateAccount = { route = AuthRoute.Method(AuthMode.SIGN_UP) },
                onSignIn = { route = AuthRoute.Method(AuthMode.SIGN_IN) },
                onGoogle = { viewModel.onIntent(AuthIntent.SignInWithGoogle) },
                onApple = { viewModel.onIntent(AuthIntent.SignInWithApple) },
                modifier = modifier
            )
            is AuthRoute.Method -> AuthMethodScreen(
                mode = current.mode,
                onBack = { route = AuthRoute.Welcome; viewModel.onIntent(AuthIntent.Reset) },
                onPhone = { route = AuthRoute.Phone(current.mode) },
                onEmail = { route = AuthRoute.Email(current.mode) },
                modifier = modifier
            )
            is AuthRoute.Phone -> PhoneScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { route = AuthRoute.Method(current.mode); viewModel.onIntent(AuthIntent.Reset) },
                onContinue = { phone ->
                    pendingPhone = phone
                    viewModel.onIntent(AuthIntent.SignUpPhone(phone))
                },
                modifier = modifier
            )
            is AuthRoute.Email -> EmailScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { route = AuthRoute.Method(current.mode); viewModel.onIntent(AuthIntent.Reset) },
                onContinue = { email ->
                    pendingEmail = email
                    viewModel.onIntent(AuthIntent.SignUpEmail(email))
                },
                modifier = modifier
            )
            is AuthRoute.Otp -> OtpScreen(
                phone = current.phone,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { route = AuthRoute.Phone(AuthMode.SIGN_UP); viewModel.onIntent(AuthIntent.Reset) },
                onVerify = { code -> viewModel.onIntent(AuthIntent.VerifyOtp(current.phone, code)) },
                onResend = { viewModel.onIntent(AuthIntent.SignUpPhone(current.phone)) },
                modifier = modifier
            )
            is AuthRoute.EmailLinkSent -> EmailLinkSentScreen(
                email = current.email,
                onBack = { route = AuthRoute.Email(AuthMode.SIGN_UP); viewModel.onIntent(AuthIntent.Reset) },
                onResend = { viewModel.onIntent(AuthIntent.SignUpEmail(current.email)) },
                onChangeEmail = { route = AuthRoute.Email(AuthMode.SIGN_UP); viewModel.onIntent(AuthIntent.Reset) },
                modifier = modifier
            )
        }
    }
}

private fun goBack(current: AuthRoute): AuthRoute? = when (current) {
    AuthRoute.Intro -> null
    AuthRoute.Welcome -> AuthRoute.Intro
    is AuthRoute.Method -> AuthRoute.Welcome
    is AuthRoute.Phone -> AuthRoute.Method(current.mode)
    is AuthRoute.Email -> AuthRoute.Method(current.mode)
    is AuthRoute.Otp -> AuthRoute.Phone(AuthMode.SIGN_UP)
    is AuthRoute.EmailLinkSent -> AuthRoute.Email(AuthMode.SIGN_UP)
}
