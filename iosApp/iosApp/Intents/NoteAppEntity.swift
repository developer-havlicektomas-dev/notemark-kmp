import AppIntents
import Foundation
import SharedLogic

/// A NoteMark note as Siri, Shortcuts and Apple Intelligence see it.
///
/// `id` is the note's client-generated UUID — the same id the backend and the
/// local SQLDelight DB use — so it stays stable across launches and devices.
struct NoteAppEntity: AppEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(
        name: "Note",
        numericFormat: "\(placeholder: .int) notes"
    )
    static let defaultQuery = NoteAppEntityQuery()

    let id: String

    @Property(title: "Title")
    var title: String

    @Property(title: "Content")
    var content: String

    @Property(title: "Created")
    var createdAt: Date

    @Property(title: "Last Edited")
    var lastEditedAt: Date

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(title)",
            subtitle: content.isEmpty ? nil : "\(String(content.prefix(80)))",
            image: .init(systemName: "doc.text")
        )
    }

    init(_ note: IosNote) {
        id = note.id
        title = note.title
        content = note.content
        createdAt = Date(timeIntervalSince1970: TimeInterval(note.createdAtEpochMs) / 1000)
        lastEditedAt = Date(timeIntervalSince1970: TimeInterval(note.lastEditedAtEpochMs) / 1000)
    }
}

/// Resolves notes for the system from the local (offline-first) store via the
/// shared `NoteCommands`, scoped to the logged-in user.
struct NoteAppEntityQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [NoteAppEntity] {
        let result = try await NoteIntentBridge.commands().getNotes(ids: identifiers)
        try NoteIntentError.throwIfNeeded(result.error)
        return result.notes.map(NoteAppEntity.init)
    }

    func entities(matching string: String) async throws -> [NoteAppEntity] {
        try await find(query: string, limit: 25)
    }

    func suggestedEntities() async throws -> [NoteAppEntity] {
        // Most recently edited notes; kept small because the system calls this
        // opportunistically to fill pickers.
        try await find(query: nil, limit: 10)
    }

    private func find(query: String?, limit: Int) async throws -> [NoteAppEntity] {
        let result = try await NoteIntentBridge.commands().findNotes(query: query, limit: Int32(limit))
        try NoteIntentError.throwIfNeeded(result.error)
        return result.notes.map(NoteAppEntity.init)
    }
}
