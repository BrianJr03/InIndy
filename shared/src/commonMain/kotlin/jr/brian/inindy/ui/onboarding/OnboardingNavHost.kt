package jr.brian.inindy.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
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
import jr.brian.inindy.ui.motion.LocalReducedMotion
import jr.brian.inindy.ui.motion.Motion
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingNavHost(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(state) {
        if (state is OnboardingUiState.Complete) {
            onComplete()
        }
    }

    // Onboarding is forward-only through Profile → Neighborhood → Interests;
    // push transitions match that linear progression. Loading/Error/Complete
    // land at ordinal 4 so they fade in from the last step instead of sliding.
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            if (stepOf(targetState) >= stepOf(initialState)) {
                Motion.pushEnter(reducedMotion) togetherWith Motion.pushExit(reducedMotion)
            } else {
                Motion.popEnter(reducedMotion) togetherWith Motion.popExit(reducedMotion)
            }
        },
        contentKey = { it::class.simpleName ?: it.toString() },
        label = "onboarding-nav"
    ) { current ->
        when (current) {
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
}

private fun stepOf(state: OnboardingUiState): Int = when (state) {
    OnboardingUiState.Loading -> 0
    is OnboardingUiState.ProfileStep -> 1
    is OnboardingUiState.NeighborhoodStep -> 2
    is OnboardingUiState.InterestsStep -> 3
    OnboardingUiState.Complete -> 4
    is OnboardingUiState.Error -> 4
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
