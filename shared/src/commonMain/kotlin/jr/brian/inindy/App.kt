package jr.brian.inindy

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import jr.brian.inindy.di.appModule
import jr.brian.inindy.ui.home.HomeScreen
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
    KoinApplication(application = { modules(appModule) }) {
        MaterialTheme {
            HomeScreen()
        }
    }
}
