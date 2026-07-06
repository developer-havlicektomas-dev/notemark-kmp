import SwiftUI
import SharedLogic

/// Shows branding while it checks for a persisted session, then hands control
/// back: authenticated users go to Notes, everyone else to the auth flow.
struct SplashView: View {
    /// Called once the session check completes. `true` = a session exists.
    let onChecked: (Bool) -> Void

    private let auth = IosAuthService()

    var body: some View {
        VStack(spacing: Space.lg) {
            Wordmark(size: 34, dotSize: 9)
            Text("Your notes, in sync.")
                .font(.nmSubhead)
                .foregroundStyle(AppColor.fgSubtle)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppColor.bg.ignoresSafeArea())
        .task {
            let loggedIn = (try? await auth.hasSession())?.boolValue ?? false
            onChecked(loggedIn)
        }
    }
}
