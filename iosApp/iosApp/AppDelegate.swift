import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import Shared

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // GitLive's initFirebase() may already have configured Firebase via
        // KoinInit. Guard against a double configure, which throws.
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()

        // Kick supabase-kt's realtime socket back to life when we foreground —
        // NSURLSession tears the socket down while suspended and doesn't
        // deliver the close callback to Ktor's Darwin engine.
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleAppForegrounded),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        return true
    }

    @objc private func handleAppForegrounded() {
        AppForegroundIosKt.notifyAppForegrounded()
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs registration failed: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("FCM token: \(fcmToken ?? "nil")")
    }

    // Show notifications while the app is foregrounded
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound, .badge])
    }
}
