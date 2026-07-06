import Foundation
import SharedLogic

@MainActor
final class LoginViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var password: String = ""
    @Published var isLoggingIn: Bool = false
    @Published var errorMessage: String?

    private let auth = IosAuthService()

    var canLogin: Bool {
        auth.isValidEmail(email: email) && !password.isEmpty
    }

    func login(onSuccess: @escaping () -> Void) {
        guard canLogin, !isLoggingIn else { return }
        isLoggingIn = true
        errorMessage = nil
        Task {
            defer { isLoggingIn = false }
            do {
                if let error = try await auth.login(email: email, password: password) {
                    errorMessage = authMessage(for: error, flow: .login)
                } else {
                    onSuccess()
                }
            } catch {
                errorMessage = genericAuthMessage(for: .login)
            }
        }
    }
}
