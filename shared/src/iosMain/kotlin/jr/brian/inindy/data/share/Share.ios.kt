package jr.brian.inindy.data.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareText(text: String, title: String) {
    val activityItems = listOf(text)
    val activityVC = UIActivityViewController(
        activityItems = activityItems,
        applicationActivities = null
    )
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(activityVC, animated = true, completion = null)
}
