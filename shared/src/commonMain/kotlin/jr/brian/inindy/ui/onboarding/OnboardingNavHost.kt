package jr.brian.inindy.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.inindy.presentation.onboarding.OnboardingIntent
import jr.brian.inindy.presentation.onboarding.OnboardingUiState
import jr.brian.inindy.presentation.onboarding.OnboardingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingNavHost(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is OnboardingUiState.Complete) {
            onComplete()
        }
    }

    when (val current = state) {
        is OnboardingUiState.ProfileStep -> ProfileStepScreen(
            error = current.error,
            onContinue = { name, avatar ->
                viewModel.onIntent(OnboardingIntent.SubmitProfile(name, avatar))
            },
            modifier = modifier
        )
        is OnboardingUiState.NeighborhoodStep -> NeighborhoodStepScreen(
            neighborhoods = current.neighborhoods,
            selectedId = current.selected,
            error = current.error,
            onSelect = { viewModel.onIntent(OnboardingIntent.SelectNeighborhood(it)) },
            onContinue = { viewModel.onIntent(OnboardingIntent.ConfirmNeighborhood) },
            modifier = modifier
        )
        is OnboardingUiState.InterestsStep -> InterestsStepScreen(
            interests = current.interests,
            selected = current.selected,
            error = current.error,
            onToggle = { viewModel.onIntent(OnboardingIntent.ToggleInterest(it)) },
            onFinish = { viewModel.onIntent(OnboardingIntent.CompleteOnboarding) },
            modifier = modifier
        )
        OnboardingUiState.Loading -> OnboardingLoading(modifier)
        OnboardingUiState.Complete -> OnboardingLoading(modifier)
        is OnboardingUiState.Error -> OnboardingError(current.message, modifier)
    }
}

@Composable
private fun OnboardingLoading(modifier: Modifier = Modifier) {
    OnboardingBackground(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OnboardingError(message: String, modifier: Modifier = Modifier) {
    OnboardingBackground(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
