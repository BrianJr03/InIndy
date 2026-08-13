package jr.brian.inindy.data.map

import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication

actual fun openMap(address: String) {
    val query = address.trim().ifEmpty { return }
    val components = NSURLComponents().apply {
        scheme = "https"
        host = "maps.apple.com"
        queryItems = listOf(NSURLQueryItem.queryItemWithName("q", value = query))
    }
    val url = components.URL ?: return
    UIApplication.sharedApplication.openURL(url)
}
