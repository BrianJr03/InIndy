package jr.brian.inindy.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberSystemReducedMotion(): Boolean {
    // Reflects Settings → Accessibility → Motion → Reduce Motion. We snapshot
    // at composition; if the user toggles it while the app is running they'll
    // pick up the change on next process launch, which is fine.
    return remember { UIAccessibilityIsReduceMotionEnabled() }
}
