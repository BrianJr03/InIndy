package jr.brian.inindy.data.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

actual class AppSettingsOpener(private val context: Context) {
    actual fun open() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
