package jr.brian.inindy.data.location

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted

actual class LocationPermissionManager {

    private val locationManager = CLLocationManager()

    actual suspend fun requestPermission(): LocationPermissionResult =
        suspendCancellableCoroutine { cont ->
            val status = CLLocationManager.authorizationStatus()
            when (status) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways ->
                    cont.resume(LocationPermissionResult.Granted)
                kCLAuthorizationStatusDenied,
                kCLAuthorizationStatusRestricted ->
                    cont.resume(LocationPermissionResult.PermanentlyDenied)
                else -> {
                    // kCLAuthorizationStatusNotDetermined — request it.
                    // iOS does not expose a direct KMP callback; LocationProvider
                    // will surface the real availability when fetching coordinates.
                    locationManager.requestWhenInUseAuthorization()
                    cont.resume(LocationPermissionResult.Granted)
                }
            }
        }

    actual fun hasPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }
}
