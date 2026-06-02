import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
