package jr.brian.inindy.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import jr.brian.inindy.data.media.ActivityProvider
import jr.brian.inindy.domain.model.AddressResult
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual class LocationProvider(
    private val context: Context,
    private val activityProvider: ActivityProvider
) {

    actual suspend fun getCurrentLocation(): AddressResult? = withContext(Dispatchers.IO) {
        val ctx = activityProvider.current() ?: context
        if (!hasLocationPermission(ctx)) return@withContext null

        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val location = fetchCurrentLocation(ctx, locationManager) ?: return@withContext null
        val formatted = reverseGeocode(ctx, location.latitude, location.longitude)
            ?: "${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
        AddressResult(formatted, location.latitude, location.longitude)
    }

    private fun hasLocationPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun fetchCurrentLocation(
        ctx: Context,
        locationManager: LocationManager
    ): Location? {
        val provider = pickProvider(locationManager) ?: return null
        return suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(ctx)
                ) { location ->
                    if (cont.isActive) {
                        cont.resume(location ?: locationManager.lastKnown(provider))
                    }
                }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun pickProvider(locationManager: LocationManager): String? {
        val providers = locationManager.getProviders(true)
        return when {
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            else -> providers.firstOrNull()
        }
    }

    private fun LocationManager.lastKnown(provider: String): Location? =
        try { getLastKnownLocation(provider) } catch (_: SecurityException) { null }

    private fun reverseGeocode(ctx: Context, lat: Double, lng: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(ctx, Locale.getDefault()).getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (_: Exception) {
            null
        }
    }
}
