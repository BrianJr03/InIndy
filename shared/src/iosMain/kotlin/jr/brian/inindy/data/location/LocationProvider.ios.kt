package jr.brian.inindy.data.location

import jr.brian.inindy.domain.model.AddressResult
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class LocationProvider {

    actual suspend fun getCurrentLocation(): AddressResult? =
        suspendCancellableCoroutine { cont ->
            val manager = CLLocationManager()
            manager.desiredAccuracy = kCLLocationAccuracyBest

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    if (location == null) {
                        if (cont.isActive) cont.resume(null)
                        return
                    }
                    val (lat, lng) = location.coordinate.useContents { latitude to longitude }
                    CLGeocoder().reverseGeocodeLocation(location) { placemarks, _ ->
                        val address = placemarks?.firstOrNull()
                            ?.let { it as? platform.CoreLocation.CLPlacemark }
                            ?.let(::formatPlacemark)
                            ?: defaultLabel(lat, lng)
                        if (cont.isActive) cont.resume(AddressResult(address, lat, lng))
                    }
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError
                ) {
                    if (cont.isActive) cont.resume(null)
                }
            }

            manager.delegate = delegate
            cont.invokeOnCancellation { manager.delegate = null }
            manager.requestLocation()
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun formatPlacemark(placemark: platform.CoreLocation.CLPlacemark): String {
    val parts = listOfNotNull(
        listOfNotNull(placemark.subThoroughfare, placemark.thoroughfare)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" "),
        placemark.locality,
        placemark.administrativeArea
    )
    return if (parts.isEmpty()) placemark.name.orEmpty() else parts.joinToString(", ")
}

private fun defaultLabel(lat: Double, lng: Double): String {
    val latStr = ((lat * 10000).toLong().toDouble() / 10000).toString()
    val lngStr = ((lng * 10000).toLong().toDouble() / 10000).toString()
    return "$latStr, $lngStr"
}
