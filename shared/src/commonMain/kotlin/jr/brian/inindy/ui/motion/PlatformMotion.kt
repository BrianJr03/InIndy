package jr.brian.inindy.ui.motion

import androidx.compose.runtime.Composable

// Reads the platform's system-level reduced-motion accessibility setting.
// Provided into LocalReducedMotion at the app root.
//   Android: Settings.Global.ANIMATOR_DURATION_SCALE == 0f
//   iOS:     UIAccessibilityIsReduceMotionEnabled()
@Composable
expect fun rememberSystemReducedMotion(): Boolean
