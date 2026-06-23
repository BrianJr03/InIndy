package jr.brian.inindy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import jr.brian.inindy.data.media.ActivityProvider
import jr.brian.inindy.data.remote.handleSupabaseDeepLink
import jr.brian.inindy.navigation.DeepLinkBus
import jr.brian.inindy.navigation.DeepLinkResult
import jr.brian.inindy.navigation.parseDeepLink
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val activityProvider: ActivityProvider by inject()
    private val deepLinkBus: DeepLinkBus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        activityProvider.attach(this)
        routeDeepLink(intent)

        setContent {
            AndroidApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeDeepLink(intent)
    }

    private fun routeDeepLink(intent: Intent?) {
        intent ?: return
        val url = intent.data?.toString()
        when (val result = url?.let(::parseDeepLink)) {
            is DeepLinkResult.GroupInvite -> deepLinkBus.postInviteToken(result.token)
            DeepLinkResult.Auth,
            DeepLinkResult.Unknown,
            null -> handleSupabaseDeepLink(intent)
        }
    }

    override fun onDestroy() {
        activityProvider.detach(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
