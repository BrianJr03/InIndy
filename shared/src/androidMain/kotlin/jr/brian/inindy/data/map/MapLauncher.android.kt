package jr.brian.inindy.data.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import jr.brian.inindy.data.media.ActivityProvider
import org.koin.mp.KoinPlatformTools

actual fun openMap(address: String) {
    val query = address.trim().ifEmpty { return }
    val koin = KoinPlatformTools.defaultContext().getOrNull() ?: return
    val activity = koin.get<ActivityProvider>().current()
    val context: Context = activity ?: koin.get()

    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
    val intent = Intent(Intent.ACTION_VIEW, geoUri)
    if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        return
    }

    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
    if (activity == null) webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(webIntent)
}
