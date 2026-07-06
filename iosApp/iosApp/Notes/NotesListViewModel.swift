import Foundation
import SharedLogic

@MainActor
final class NotesListViewModel: ObservableObject {
    @Published var notes: [IosNote] = []
    @Published var initials: String = ""
    @Published var isLoading: Bool = false
    @Published var isOnline: Bool = true
    @Published var errorMessage: String?

    private let notesService = IosNotesService()
    private let auth = IosAuthService()
    private let syncService = IosSyncService()
    private var connectivityWatch: IosWatchHandle?

    init() {
        connectivityWatch = syncService.watchIsConnected { [weak self] connected in
            Task { @MainActor in self?.isOnline = connected.boolValue }
        }
    }

    deinit {
        connectivityWatch?.cancel()
    }

    /// Refreshes the avatar initials and the notes list; runs a full sync.
    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            if let name = try await auth.username() {
                initials = userInitials(from: name)
            }
            let result = try await notesService.getNotes()
            notes = result.notes
            // NO_INTERNET is expected offline — the banner already covers it.
            errorMessage = result.error.flatMap {
                $0.name == "NO_INTERNET" ? nil : notesMessage(for: $0)
            }
        } catch {
            errorMessage = "Couldn't reach the server. Check your connection and try again."
        }
    }

    /// Re-reads the local database only — enough after in-app edits, and it
    /// won't resurrect notes the user just cleared in Settings.
    func refreshLocal() async {
        if let local = try? await notesService.getLocalNotes() {
            notes = local
        }
    }

    func delete(id: String) {
        Task {
            do {
                if let error = try await notesService.deleteNote(id: id) {
                    errorMessage = notesMessage(for: error)
                }
            } catch {
                errorMessage = "Couldn't reach the server. Check your connection and try again."
            }
            await refreshLocal()
        }
    }
}
