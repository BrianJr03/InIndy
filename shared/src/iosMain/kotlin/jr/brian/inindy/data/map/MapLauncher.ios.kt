package jr.brian.inindy.data.map

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication

actual fun openMap(address: String) {
    val query = address.trim().ifEmpty { return }
    val encoded = query.stringByAddingPercentEncodingWithAllowedCharacters(
        NSCharacterSet.URLQueryAllowedCharacterSet
    ) ?: return
    val url = NSURL.URLWithString("https://maps.apple.com/?q=$encoded") ?: return
    UIApplication.sharedApplication.openURL(url)
}
