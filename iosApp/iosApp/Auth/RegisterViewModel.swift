import Foundation
import SharedLogic

@MainActor
final class RegisterViewModel: ObservableObject {
    @Published var username: String = ""
    @Published var email: String = ""
    @Published var password: String = ""
    @Published var repeatPassword: String = ""
    @Published var isRegistering: Bool = false
    @Published var errorMessage: String?

    private let auth = IosAuthService()

    var isUsernameValid: Bool { auth.isValidUsername(username: username) }
    var isEmailValid: Bool { auth.isValidEmail(email: email) }
    var isPasswordValid: Bool { auth.validatePassword(password: password).isValid }
    var passwordsMatch: Bool { !password.isEmpty && password == repeatPassword }

    var canRegister: Bool {
        isUsernameValid && isEmailValid && isPasswordValid && passwordsMatch && !isRegistering
    }

    func register(onSuccess: @escaping (_ email: String) -> Void) {
        guard canRegister else { return }
        isRegistering = true
        errorMessage = nil
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        Task {
            defer { isRegistering = false }
            do {
                if let error = try await auth.register(
                    username: username,
                    email: email,
                    password: password
                ) {
                    errorMessage = authMessage(for: error, flow: .register)
                } else {
                    onSuccess(trimmedEmail)
                }
            } catch {
                errorMessage = genericAuthMessage(for: .register)
            }
        }
    }
}
