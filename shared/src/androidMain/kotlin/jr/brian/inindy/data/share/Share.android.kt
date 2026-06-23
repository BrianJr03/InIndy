package jr.brian.inindy.data.share

import android.content.Context
import android.content.Intent
import jr.brian.inindy.data.media.ActivityProvider
import org.koin.mp.KoinPlatformTools

actual fun shareText(text: String, title: String) {
    val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return
    val activity = koin.get<ActivityProvider>().current()
    val context: Context = activity ?: koin.get()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    val chooser = Intent.createChooser(intent, title)
    if (activity == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
