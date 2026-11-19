import AppIntents
import Foundation
import Observation
import SharedLogic

/// Entry point to the shared Kotlin note commands for App Intents.
enum NoteIntentBridge {
    /// Intents can run while the app is launched in the background, possibly
    /// before `iOSApp.init` has wired Koin — `initKoinIos()` is idempotent, so
    /// always call it first.
    static func commands() -> IosNoteCommands {
        KoinIosKt.doInitKoinIos()
        return IosNoteCommands()
    }
}

/// User-facing failures from the shared `NoteCommandError`. Conforms to
/// `CustomLocalizedStringResourceConvertible` so Siri and Shortcuts show the
/// message instead of a generic failure.
// `Swift.Error`: SharedLogic exports its own `Error` protocol.
enum NoteIntentError: Swift.Error, CustomLocalizedStringResourceConvertible {
    case notLoggedIn
    case blankTitle
    case nothingToUpdate
    case noteNotFound
    case unknown

    init(_ error: NoteCommandError) {
        switch error.name {
        case "NOT_LOGGED_IN": self = .notLoggedIn
        case "BLANK_TITLE": self = .blankTitle
        case "NOTHING_TO_UPDATE": self = .nothingToUpdate
        case "NOTE_NOT_FOUND": self = .noteNotFound
        default: self = .unknown
        }
    }

    static func throwIfNeeded(_ error: NoteCommandError?) throws {
        if let error { throw NoteIntentError(error) }
    }

    var localizedStringResource: LocalizedStringResource {
        switch self {
        case .notLoggedIn:
            "You're not logged in to NoteMark. Open the app and log in, then try again."
        case .blankTitle:
            "A note needs a title."
        case .nothingToUpdate:
            "Give a new title or new content for the note."
        case .noteNotFound:
            "That note couldn't be found. It may have been deleted."
        case .unknown:
            "Something went wrong with NoteMark. Try again."
        }
    }
}

/// Hands a note to open from `OpenNoteIntent` to the SwiftUI notes screen.
@MainActor
@Observable
final class NoteNavigator {
    static let shared = NoteNavigator()

    /// Set by the intent; the notes screen opens it and clears it.
    var pendingNoteID: String?

    /// Bumped whenever an intent changes notes, so a visible list re-reads.
    private(set) var notesChangedToken = 0

    func open(noteID: String) { pendingNoteID = noteID }

    func notesChanged() { notesChangedToken += 1 }
}
