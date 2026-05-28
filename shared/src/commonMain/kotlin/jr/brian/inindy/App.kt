package jr.brian.inindy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import jr.brian.inindy.di.appModule
import jr.brian.inindy.di.authModule
import jr.brian.inindy.di.platformModule
import jr.brian.inindy.di.postModule
import jr.brian.inindy.presentation.root.RootDestination
import jr.brian.inindy.presentation.root.RootViewModel
import jr.brian.inindy.ui.auth.AuthNavHost
import jr.brian.inindy.ui.home.HomeScreen
import jr.brian.inindy.ui.onboarding.OnboardingNavHost
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication as KoinApp

@Composable
@Preview
fun App(
    koinAppDeclaration: KoinApp.() -> Unit = {}
) {
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }
    KoinApplication(application = {
        koinAppDeclaration()
        modules(appModule, authModule, postModule, platformModule)
    }) {
        MaterialTheme(
            colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
        ) {
            RootHost(
                isDarkMode = isDarkMode,
                onToggleDarkMode = { isDarkMode = it }
            )
        }
    }
}

@Composable
private fun RootHost(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    rootViewModel: RootViewModel = koinViewModel()
) {
    val destination by rootViewModel.destination.collectAsState()
    when (destination) {
        RootDestination.Splash -> SplashContent()
        RootDestination.Auth -> AuthNavHost(
            onAuthenticated = { rootViewModel.onAuthenticated(it) }
        )
        RootDestination.Onboarding -> OnboardingNavHost(
            onComplete = { rootViewModel.onOnboardingComplete() }
        )
        RootDestination.Main -> HomeScreen(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode
        )
    }
}

@Composable
private fun SplashContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
