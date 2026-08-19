package jr.brian.inindy.data.notification

expect class NotificationPermissionManager {
    suspend fun requestPermission(): NotificationPermissionResult
    suspend fun hasPermission(): Boolean
}

enum class NotificationPermissionResult {
    Granted, Denied, PermanentlyDenied
}
