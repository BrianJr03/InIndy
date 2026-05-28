package jr.brian.inindy

import android.content.Context
import androidx.compose.runtime.Composable
import org.koin.android.ext.koin.androidContext

@Composable
fun AndroidApp(context: Context) {
    App(koinAppDeclaration = { androidContext(context) })
}
