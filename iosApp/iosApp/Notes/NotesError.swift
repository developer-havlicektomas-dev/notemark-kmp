import Foundation
import SharedLogic

/// Maps a shared-layer `DataError.Network` to a calm, concrete message —
/// the notes counterpart to `authMessage(for:flow:)`.
func notesMessage(for error: DataErrorNetwork) -> String {
    switch error.name {
    case "NO_INTERNET":
        return "You're offline. Check your connection and try again."
    case "UNAUTHORIZED":
        return "Your session expired. Please log in again."
    case "REQUEST_TIMEOUT", "SERVICE_UNAVAILABLE":
        return "The server is taking too long. Try again in a moment."
    case "TOO_MANY_REQUESTS":
        return "Too many requests. Wait a moment and try again."
    case "SERVER_ERROR":
        return "Something went wrong on our end. Try again shortly."
    case "NOT_FOUND":
        return "That note couldn't be found."
    default:
        return "Something went wrong. Try again."
    }
}
