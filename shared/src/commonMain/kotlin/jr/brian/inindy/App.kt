package jr.brian.inindy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import jr.brian.inindy.navigation.RootNavGraph
import jr.brian.inindy.ui.motion.LocalReducedMotion
import jr.brian.inindy.ui.motion.rememberSystemReducedMotion

@Composable
@Preview
fun App() {
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }
    val reducedMotion = rememberSystemReducedMotion()

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
    ) {
        // Every navigation transition and Motion builder in the tree reads from
        // this local; providing once here means individual screens don't need
        // to call rememberSystemReducedMotion themselves.
        CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
            RootNavGraph(
                isDarkMode = isDarkMode,
                onToggleDarkMode = { isDarkMode = it }
            )
        }
    }
}
