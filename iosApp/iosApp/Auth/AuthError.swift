import Foundation
import SharedLogic

enum AuthFlow {
    case login
    case register
}

/// Maps a shared-layer `DataError.Network` to a calm, concrete message in the
/// design system's voice (state what happened, offer one next action).
func authMessage(for error: DataErrorNetwork, flow: AuthFlow) -> String {
    switch error.name {
    case "UNAUTHORIZED":
        return "Couldn't log in. Check your email and password, then try again."
    case "CONFLICT":
        return "An account with that email or username already exists."
    case "NO_INTERNET":
        return "You're offline. Check your connection and try again."
    case "REQUEST_TIMEOUT", "SERVICE_UNAVAILABLE":
        return "The server is taking too long. Try again in a moment."
    case "TOO_MANY_REQUESTS":
        return "Too many attempts. Wait a moment and try again."
    case "SERVER_ERROR":
        return "Something went wrong on our end. Try again shortly."
    default:
        switch flow {
        case .login:
            return "Couldn't log in. Try again."
        case .register:
            return "Couldn't create your account. Try again."
        }
    }
}

/// Login can also fail after the server accepted the credentials, when the
/// session can't be written to the Keychain (e.g. an unsigned simulator build).
func loginMessage(for error: DataError) -> String {
    if let network = error as? DataErrorNetwork {
        return authMessage(for: network, flow: .login)
    }
    if let local = error as? DataErrorLocal, local.name == "SECURE_STORAGE" {
        return "Logged in, but couldn't save your session securely on this device. Try again."
    }
    return "Couldn't log in. Try again."
}

/// Fallback for an unexpected thrown error (e.g. cancellation, connection refused).
func genericAuthMessage(for flow: AuthFlow) -> String {
    switch flow {
    case .login:
        return "Couldn't reach the server. Check your connection and try again."
    case .register:
        return "Couldn't reach the server. Check your connection and try again."
    }
}
