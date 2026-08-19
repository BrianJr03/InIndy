package jr.brian.inindy.data.notification

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

actual class NotificationPermissionManager {

    actual suspend fun requestPermission(): NotificationPermissionResult =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional -> {
                            if (cont.isActive) cont.resume(NotificationPermissionResult.Granted)
                        }
                        UNAuthorizationStatusDenied -> {
                            // iOS shows the system dialog once per install; a
                            // second requestAuthorization call after a denial
                            // returns immediately with no UI. Surface that as
                            // PermanentlyDenied so the caller can route the
                            // user to Settings instead.
                            if (cont.isActive) cont.resume(NotificationPermissionResult.PermanentlyDenied)
                        }
                        UNAuthorizationStatusNotDetermined -> {
                            val options = UNAuthorizationOptionAlert or
                                UNAuthorizationOptionBadge or
                                UNAuthorizationOptionSound
                            UNUserNotificationCenter.currentNotificationCenter()
                                .requestAuthorizationWithOptions(options) { granted, _ ->
                                    val result = if (granted) NotificationPermissionResult.Granted
                                    else NotificationPermissionResult.Denied
                                    if (cont.isActive) cont.resume(result)
                                }
                        }
                        else -> {
                            if (cont.isActive) cont.resume(NotificationPermissionResult.Denied)
                        }
                    }
                }
        }

    actual suspend fun hasPermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val granted = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional -> true
                        else -> false
                    }
                    if (cont.isActive) cont.resume(granted)
                }
        }
}
