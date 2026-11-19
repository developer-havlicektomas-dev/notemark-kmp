import AppIntents
import Foundation
import SharedLogic

// Intent type names, parameter names and NoteAppEntity ids are stored in saved
// shortcuts: add new ones freely, but don't rename or remove shipped ones.

/// Creates a note. Mirrors the Android `createNote` App Function.
struct CreateNoteIntent: AppIntent {
    static let title: LocalizedStringResource = "Create Note"
    static let description = IntentDescription(
        "Creates a new note in NoteMark. It's saved on this device and synced when online."
    )
    static var supportedModes: IntentModes { .background }

    @Parameter(title: "Title")
    var noteTitle: String

    @Parameter(title: "Content", inputOptions: String.IntentInputOptions(multiline: true))
    var content: String?

    static var parameterSummary: some ParameterSummary {
        Summary("Create note \(\.$noteTitle)") {
            \.$content
        }
    }

    func perform() async throws -> some IntentResult & ReturnsValue<NoteAppEntity> & ProvidesDialog {
        // Not idempotent across restarts of perform(): this is the only side
        // effect and it is the last thing done before returning.
        let result = try await NoteIntentBridge.commands().createNote(title: noteTitle, content: content)
        try NoteIntentError.throwIfNeeded(result.error)
        guard let note = result.note else { throw NoteIntentError.unknown }
        await MainActor.run { NoteNavigator.shared.notesChanged() }
        let entity = NoteAppEntity(note)
        return .result(value: entity, dialog: "Created “\(entity.title)”.")
    }
}

/// Replaces a note's title and/or content; a field left empty keeps its value.
/// Mirrors the Android `updateNote` App Function.
struct UpdateNoteIntent: AppIntent {
    static let title: LocalizedStringResource = "Update Note"
    static let description = IntentDescription(
        "Replaces the title or content of a NoteMark note. Fields you leave empty stay as they are."
    )
    static var supportedModes: IntentModes { .background }

    @Parameter(title: "Note")
    var note: NoteAppEntity

    @Parameter(title: "New Title")
    var newTitle: String?

    @Parameter(title: "New Content", inputOptions: String.IntentInputOptions(multiline: true))
    var newContent: String?

    static var parameterSummary: some ParameterSummary {
        Summary("Update \(\.$note)") {
            \.$newTitle
            \.$newContent
        }
    }

    func perform() async throws -> some IntentResult & ReturnsValue<NoteAppEntity> & ProvidesDialog {
        let result = try await NoteIntentBridge.commands().updateNote(
            id: note.id,
            title: newTitle,
            content: newContent
        )
        try NoteIntentError.throwIfNeeded(result.error)
        guard let updated = result.note else { throw NoteIntentError.unknown }
        await MainActor.run { NoteNavigator.shared.notesChanged() }
        let entity = NoteAppEntity(updated)
        return .result(value: entity, dialog: "Updated “\(entity.title)”.")
    }
}

/// Returns a note's current title and content. Mirrors the Android `getNote`
/// App Function.
struct GetNoteIntent: AppIntent {
    static let title: LocalizedStringResource = "Get Note"
    static let description = IntentDescription("Gets the title and content of a NoteMark note.")
    static var supportedModes: IntentModes { .background }

    @Parameter(title: "Note")
    var note: NoteAppEntity

    static var parameterSummary: some ParameterSummary {
        Summary("Get \(\.$note)")
    }

    func perform() async throws -> some IntentResult & ReturnsValue<NoteAppEntity> {
        // Re-read so the result reflects the latest local edit, not the
        // snapshot the parameter was resolved with.
        let result = try await NoteIntentBridge.commands().getNote(id: note.id)
        try NoteIntentError.throwIfNeeded(result.error)
        guard let current = result.note else { throw NoteIntentError.noteNotFound }
        return .result(value: NoteAppEntity(current))
    }
}

/// Finds notes whose title or content matches a search, newest edit first.
/// Mirrors the Android `findNotes` App Function.
struct FindNotesIntent: AppIntent {
    static let title: LocalizedStringResource = "Find Notes"
    static let description = IntentDescription(
        "Finds NoteMark notes whose title or content contains the search text. Leave the search empty for your most recently edited notes."
    )
    static var supportedModes: IntentModes { .background }

    @Parameter(title: "Search")
    var searchText: String?

    @Parameter(title: "Limit", default: 10, inclusiveRange: (1, 25))
    var limit: Int

    static var parameterSummary: some ParameterSummary {
        Summary("Find notes matching \(\.$searchText)") {
            \.$limit
        }
    }

    func perform() async throws -> some IntentResult & ReturnsValue<[NoteAppEntity]> {
        let result = try await NoteIntentBridge.commands().findNotes(query: searchText, limit: Int32(limit))
        try NoteIntentError.throwIfNeeded(result.error)
        return .result(value: result.notes.map(NoteAppEntity.init))
    }
}

/// Opens a note in the NoteMark editor.
struct OpenNoteIntent: OpenIntent {
    static let title: LocalizedStringResource = "Open Note"
    static let description = IntentDescription("Opens a note in NoteMark.")
    static var supportedModes: IntentModes { .foreground }

    @Parameter(title: "Note")
    var target: NoteAppEntity

    func perform() async throws -> some IntentResult {
        await MainActor.run { NoteNavigator.shared.open(noteID: target.id) }
        return .result()
    }
}

struct NoteMarkShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CreateNoteIntent(),
            phrases: [
                "Create a note in \(.applicationName)",
                "New \(.applicationName) note",
            ],
            shortTitle: "Create Note",
            systemImageName: "square.and.pencil"
        )
        AppShortcut(
            intent: FindNotesIntent(),
            phrases: [
                "Find notes in \(.applicationName)",
                "Search \(.applicationName)",
            ],
            shortTitle: "Find Notes",
            systemImageName: "magnifyingglass"
        )
        AppShortcut(
            intent: OpenNoteIntent(),
            phrases: [
                "Open a note in \(.applicationName)",
            ],
            shortTitle: "Open Note",
            systemImageName: "doc.text"
        )
    }
}
