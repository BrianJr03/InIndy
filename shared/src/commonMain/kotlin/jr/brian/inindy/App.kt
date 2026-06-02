package jr.brian.inindy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import jr.brian.inindy.navigation.RootNavGraph

@Composable
@Preview
fun App() {
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
    ) {
        RootNavGraph(
            isDarkMode = isDarkMode,
            onToggleDarkMode = { isDarkMode = it }
        )
    }
}
