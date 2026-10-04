import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Koin bootstrap. Kotlin/Native exports `initKoin()` as `doInitKoin()` (Obj-C forbids the `init` prefix).
        InitKoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Supabase auth callback (email confirmation, Google OAuth): womenriskmap://login-callback
                .onOpenURL { url in DeepLinksKt.handleAuthDeepLink(url: url) }
        }
    }
}
