package jr.brian.inindy.data.location

expect class LocationPermissionManager {
    suspend fun requestPermission(): LocationPermissionResult
    fun hasPermission(): Boolean
}

enum class LocationPermissionResult {
    Granted, Denied, PermanentlyDenied
}
