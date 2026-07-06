import SwiftUI
import SharedLogic

/// A note being edited. Drives navigation: an existing note carries its current
/// values (`isNew == false`); a brand-new note starts empty (`isNew == true`).
struct NoteDraft: Hashable {
    let id: String
    let title: String
    let content: String
    let isNew: Bool
    var createdAtEpochMs: Int64 = 0
    var lastEditedAtEpochMs: Int64 = 0
}

/// Screens reachable from the notes list within the same navigation stack.
enum NotesRoute: Hashable {
    case detail(NoteDraft)
    case reader(title: String, content: String)
    case settings
}

/// The notes feature: a list that pushes the editor, reader, and settings onto a
/// `NavigationStack`. Owns the list view model so the editor can trigger a refresh.
struct NotesFlowView: View {
    let onLoggedOut: () -> Void

    @StateObject private var listVM = NotesListViewModel()
    @State private var path: [NotesRoute] = []

    var body: some View {
        NavigationStack(path: $path) {
            NotesListView(
                viewModel: listVM,
                onOpen: { note in
                    path.append(.detail(NoteDraft(
                        id: note.id,
                        title: note.title,
                        content: note.content,
                        isNew: false,
                        createdAtEpochMs: note.createdAtEpochMs,
                        lastEditedAtEpochMs: note.lastEditedAtEpochMs
                    )))
                },
                onCreate: {
                    // The id and creation time are fixed up front so debounced
                    // autosaves keep upserting the same row (M4).
                    let now = Int64(Date().timeIntervalSince1970 * 1000)
                    path.append(.detail(NoteDraft(
                        id: UUID().uuidString.lowercased(),
                        title: "",
                        content: "",
                        isNew: true,
                        createdAtEpochMs: now,
                        lastEditedAtEpochMs: now
                    )))
                },
                onOpenSettings: {
                    path.append(.settings)
                }
            )
            .navigationDestination(for: NotesRoute.self) { route in
                switch route {
                case .detail(let draft):
                    NoteDetailView(
                        draft: draft,
                        onOpenReader: { title, content in
                            path.append(.reader(title: title, content: content))
                        },
                        onDone: {
                            if !path.isEmpty { path.removeLast() }
                            // Edits are already in the local DB (autosave) —
                            // no network round-trip needed to show them.
                            Task { await listVM.refreshLocal() }
                        }
                    )
                case .reader(let title, let content):
                    ReaderView(title: title, content: content) {
                        if !path.isEmpty { path.removeLast() }
                    }
                case .settings:
                    SettingsView(
                        onBack: {
                            if !path.isEmpty { path.removeLast() }
                            // Settings can clear local data or sync — re-read
                            // the local DB, but don't trigger another sync.
                            Task { await listVM.refreshLocal() }
                        },
                        onLoggedOut: onLoggedOut
                    )
                }
            }
        }
        .task { await listVM.load() }
    }
}
