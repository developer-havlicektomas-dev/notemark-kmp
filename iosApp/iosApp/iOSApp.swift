import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Wire up the shared domain + data layer (Koin) once at launch.
        KoinIosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .onChange(of: scenePhase) { phase in
                    // M4: the periodic sync timer only lives while the app runs,
                    // so re-arm it and drain any queued changes every time the
                    // app comes to the foreground (covers launch too).
                    guard phase == .active else { return }
                    Task {
                        let auth = IosAuthService()
                        guard (try? await auth.hasSession()) == true else { return }
                        let sync = IosSyncService()
                        try? await sync.applyStoredSchedule()
                        sync.requestBackgroundSync()
                    }
                }
        }
    }
}
