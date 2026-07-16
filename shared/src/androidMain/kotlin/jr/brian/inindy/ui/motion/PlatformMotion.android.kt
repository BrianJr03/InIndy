package jr.brian.inindy.ui.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        // Users toggle this via Developer options → Animator duration scale,
        // or via Accessibility → Remove animations on newer builds. Both
        // collapse the scale to 0 for our purposes.
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }
}
