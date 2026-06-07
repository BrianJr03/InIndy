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
                .onOpenURL { url in
                    Task {
                        try? await SupabaseDeepLinksIosKt.handleSupabaseDeepLink(url: url.absoluteString)
                    }
                }
        }
    }
}
