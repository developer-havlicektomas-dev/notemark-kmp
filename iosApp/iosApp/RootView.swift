import SwiftUI

/// Top-level app phases. Switching phase tears down the previous navigation
/// stack entirely — which is how the spec's "clear auth flow after login" and
/// "remove landing from the back stack" rules are satisfied.
private enum AppPhase {
    case splash
    case auth
    case notes
    case editorPreview
}

private enum AuthRoute: Hashable {
    case login
    case register
}

struct RootView: View {
    @State private var phase: AppPhase

    init() {
        // Debug hooks for screenshot verification: `-screen login|register`
        // jumps into the auth flow; `-editorPreview` opens the note editor.
        if LaunchArgs.editorPreview {
            _phase = State(initialValue: .editorPreview)
        } else {
            _phase = State(initialValue: LaunchArgs.screen == nil ? .splash : .auth)
        }
    }

    var body: some View {
        Group {
            switch phase {
            case .splash:
                SplashView { isLoggedIn in
                    phase = isLoggedIn ? .notes : .auth
                }
            case .auth:
                AuthFlowView(
                    initialRoute: LaunchArgs.screen,
                    onAuthenticated: { phase = .notes }
                )
            case .notes:
                NotesFlowView(onLoggedOut: { phase = .auth })
            case .editorPreview:
                NavigationStack {
                    NoteDetailView(
                        draft: NoteDraft(
                            id: "preview",
                            title: "Weekly planning",
                            content: "Block two hours Monday for deep work and review.",
                            isNew: false,
                            createdAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000) - 86_400_000,
                            lastEditedAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000) - 120_000
                        ),
                        onDone: {}
                    )
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: phase)
    }
}

private enum LaunchArgs {
    /// `-screen login` / `-screen register` for screenshot verification.
    static var screen: AuthRoute? {
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: "-screen"), i + 1 < args.count else { return nil }
        switch args[i + 1] {
        case "login": return .login
        case "register": return .register
        default: return nil
        }
    }

    /// `-editorPreview` opens the note editor directly for screenshot verification.
    static var editorPreview: Bool {
        ProcessInfo.processInfo.arguments.contains("-editorPreview")
    }
}

private struct AuthFlowView: View {
    var initialRoute: AuthRoute? = nil
    let onAuthenticated: () -> Void
    @State private var path: [AuthRoute]

    init(initialRoute: AuthRoute? = nil, onAuthenticated: @escaping () -> Void) {
        self.initialRoute = initialRoute
        self.onAuthenticated = onAuthenticated
        _path = State(initialValue: initialRoute.map { [$0] } ?? [])
    }

    var body: some View {
        NavigationStack(path: $path) {
            LandingView(
                onGetStarted: { path.append(.register) },
                onLogIn: { path.append(.login) }
            )
            .navigationDestination(for: AuthRoute.self) { route in
                switch route {
                case .login:
                    LoginView(
                        onLoggedIn: onAuthenticated,
                        onRegister: { path.append(.register) }
                    )
                case .register:
                    RegisterView(
                        onRegistered: { _ in
                            // No tokens are issued on register, so send the user
                            // to log in — and leave a single login on the stack.
                            path = [.login]
                        }
                    )
                }
            }
        }
    }
}
