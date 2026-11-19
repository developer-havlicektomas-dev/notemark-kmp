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

/// What the detail column shows. `instance` gives every opening of a note a
/// fresh `NoteDetailView` (and view model), even when it is the same note.
enum NotesDetail: Hashable {
    case note(NoteDraft, instance: UUID)
    case settings
}

/// Screens pushed on top of the detail column.
enum NotesRoute: Hashable {
    case reader(title: String, content: String)
}

/// The notes feature as an adaptive list-detail layout.
///
/// Compact widths (iPhone, a folded iPhone Duo) collapse the split view into a
/// single stack: the list, with the note or Settings pushed on top. Regular
/// widths (an unfolded iPhone Duo, iPad) show the list and the open note side
/// by side. Owns the list view model so the detail can trigger a refresh.
struct NotesFlowView: View {
    let onLoggedOut: () -> Void

    @StateObject private var listVM = NotesListViewModel()
    @State private var detail: NotesDetail?
    @State private var detailPath: [NotesRoute] = []
    @State private var preferredColumn: NavigationSplitViewColumn = .sidebar
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.scenePhase) private var scenePhase
    /// Notes opened or changed by App Intents (Siri, Shortcuts).
    private let navigator = NoteNavigator.shared

    private var isCollapsed: Bool { horizontalSizeClass == .compact }

    var body: some View {
        NavigationSplitView(preferredCompactColumn: $preferredColumn) {
            NotesListView(
                viewModel: listVM,
                selectedNoteID: isCollapsed ? nil : selectedNoteID,
                onOpen: { note in
                    show(.note(NoteDraft(
                        id: note.id,
                        title: note.title,
                        content: note.content,
                        isNew: false,
                        createdAtEpochMs: note.createdAtEpochMs,
                        lastEditedAtEpochMs: note.lastEditedAtEpochMs
                    ), instance: UUID()))
                },
                onCreate: {
                    // The id and creation time are fixed up front so debounced
                    // autosaves keep upserting the same row (M4).
                    let now = Int64(Date().timeIntervalSince1970 * 1000)
                    show(.note(NoteDraft(
                        id: UUID().uuidString.lowercased(),
                        title: "",
                        content: "",
                        isNew: true,
                        createdAtEpochMs: now,
                        lastEditedAtEpochMs: now
                    ), instance: UUID()))
                },
                onOpenSettings: { show(.settings) }
            )
            .navigationSplitViewColumnWidth(min: 300, ideal: 380, max: 460)
        } detail: {
            NavigationStack(path: $detailPath) {
                detailRoot
                    .navigationDestination(for: NotesRoute.self) { route in
                        switch route {
                        case .reader(let title, let content):
                            ReaderView(title: title, content: content) {
                                if !detailPath.isEmpty { detailPath.removeLast() }
                            }
                        }
                    }
            }
        }
        .navigationSplitViewStyle(.balanced)
        .onChange(of: preferredColumn) { _, column in
            // Collapsed back to the list by the system (e.g. a swipe back):
            // drop the detail so it doesn't linger when the window widens.
            if column == .sidebar && isCollapsed && detail != nil {
                detail = nil
                detailPath = []
            }
        }
        .task { await listVM.load() }
        .onChange(of: navigator.pendingNoteID, initial: true) { _, id in
            guard let id else { return }
            navigator.pendingNoteID = nil
            Task { await openFromIntent(noteID: id) }
        }
        .onChange(of: navigator.notesChangedToken) {
            Task { await listVM.refreshLocal() }
        }
        .onChange(of: scenePhase) { _, phase in
            // Intents can create or edit notes while the app is in the
            // background; show those changes when the user comes back.
            if phase == .active { Task { await listVM.refreshLocal() } }
        }
    }

    private func openFromIntent(noteID: String) async {
        guard let note = try? await IosNotesService().getNote(id: noteID) else { return }
        await listVM.refreshLocal()
        show(.note(NoteDraft(
            id: note.id,
            title: note.title,
            content: note.content,
            isNew: false,
            createdAtEpochMs: note.createdAtEpochMs,
            lastEditedAtEpochMs: note.lastEditedAtEpochMs
        ), instance: UUID()))
    }

    private var selectedNoteID: String? {
        if case .note(let draft, _) = detail { return draft.id }
        return nil
    }

    @ViewBuilder
    private var detailRoot: some View {
        switch detail {
        case .note(let draft, let instance):
            NoteDetailView(
                draft: draft,
                showsBackButton: isCollapsed,
                onOpenReader: { title, content in
                    detailPath.append(.reader(title: title, content: content))
                },
                onSaved: {
                    // The list stays visible beside the editor on wide windows.
                    Task { await listVM.refreshLocal() }
                },
                onDone: closeDetail
            )
            .id(instance)
        case .settings:
            SettingsView(
                // Settings can clear local data or sync — closeDetail re-reads
                // the local DB, but doesn't trigger another sync.
                onBack: closeDetail,
                onLoggedOut: onLoggedOut
            )
        case nil:
            ContentUnavailableView(
                "Select a note",
                systemImage: "doc.text",
                description: Text("Pick a note from the list to read or edit it here.")
            )
            .background(AppColor.bg.ignoresSafeArea())
        }
    }

    private func show(_ newDetail: NotesDetail) {
        detailPath = []
        detail = newDetail
        preferredColumn = .detail
    }

    private func closeDetail() {
        detailPath = []
        detail = nil
        preferredColumn = .sidebar
        // Edits are already in the local DB (autosave) — no network
        // round-trip needed to show them.
        Task { await listVM.refreshLocal() }
    }
}
