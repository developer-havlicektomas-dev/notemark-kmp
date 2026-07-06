import Foundation
import SharedLogic

enum NoteMode {
    case view
    case edit
}

@MainActor
final class NoteDetailViewModel: ObservableObject {
    @Published var mode: NoteMode
    @Published var title: String {
        didSet { if oldValue != title { onEdited() } }
    }
    @Published var content: String {
        didSet { if oldValue != content { onEdited() } }
    }
    @Published var isSaving = false
    @Published var isDeleting = false
    @Published var isOnline = true
    @Published var errorMessage: String?

    /// Timestamps backing the View-mode metadata line. `lastEditedAt` is bumped
    /// locally after each persist so the "Edited …" label refreshes immediately.
    @Published var createdAtEpochMs: Int64
    @Published var lastEditedAtEpochMs: Int64

    /// When the note on screen was last persisted locally — drives the M4
    /// "Saved just now" / "Offline — saved on this device" status line.
    @Published var lastSavedAtEpochMs: Int64?

    private let draft: NoteDraft

    // What the current edit session started from — the discard target. Autosave
    // persists as the user types, so "discard" reverts to (and re-saves) these.
    private var sessionTitle: String
    private var sessionContent: String
    private var savedDuringSession = false

    private var autosaveTask: Task<Void, Never>?
    /// Suppresses the didSet autosave trigger while the VM itself mutates the
    /// editor (revert on discard, trim on save).
    private var isProgrammaticChange = false

    private let notesService = IosNotesService()
    private let syncService = IosSyncService()
    private var connectivityWatch: IosWatchHandle?

    /// Within the 500–1000ms debounce window the M4 spec asks for.
    private static let autosaveDebounceNs: UInt64 = 800_000_000

    init(draft: NoteDraft) {
        self.draft = draft
        self.title = draft.title
        self.content = draft.content
        self.sessionTitle = draft.title
        self.sessionContent = draft.content
        self.createdAtEpochMs = draft.createdAtEpochMs
        self.lastEditedAtEpochMs = draft.lastEditedAtEpochMs
        self.lastSavedAtEpochMs = draft.isNew ? nil : draft.lastEditedAtEpochMs
        // Existing notes open read-only; new notes go straight into the editor.
        self.mode = draft.isNew ? .edit : .view

        connectivityWatch = syncService.watchIsConnected { [weak self] connected in
            Task { @MainActor in self?.isOnline = connected.boolValue }
        }
    }

    deinit {
        connectivityWatch?.cancel()
        autosaveTask?.cancel()
    }

    var hasChanges: Bool {
        title != sessionTitle || content != sessionContent
    }

    var canSave: Bool {
        !trimmedTitle.isEmpty && (hasChanges || savedDuringSession) && !isSaving
    }

    private var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func startEditing() {
        sessionTitle = title
        sessionContent = content
        savedDuringSession = false
        mode = .edit
    }

    // MARK: - Autosave (M4)

    private func onEdited() {
        guard !isProgrammaticChange, mode == .edit else { return }
        scheduleAutosave()
    }

    /// M4 automatic saving: persist locally 800ms after the user stops typing.
    private func scheduleAutosave() {
        autosaveTask?.cancel()
        guard !trimmedTitle.isEmpty else { return } // a note needs a title to exist
        autosaveTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.autosaveDebounceNs)
            guard !Task.isCancelled else { return }
            await self?.persist()
        }
    }

    /// Writes the current editor content to the repository (local DB + sync queue).
    @discardableResult
    private func persist() async -> Bool {
        guard !trimmedTitle.isEmpty else { return false }
        do {
            if let error = try await notesService.saveNote(
                id: draft.id,
                title: title,
                content: content,
                createdAtEpochMs: createdAtEpochMs
            ) {
                errorMessage = notesMessage(for: error)
                return false
            }
            savedDuringSession = true
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            lastEditedAtEpochMs = now
            lastSavedAtEpochMs = now
            errorMessage = nil
            return true
        } catch {
            errorMessage = "Couldn't save your note. Try again."
            return false
        }
    }

    // MARK: - Actions

    func save(onDone: @escaping () -> Void) {
        guard canSave else { return }
        autosaveTask?.cancel()
        isSaving = true
        Task {
            let saved = await persist()
            isSaving = false
            guard saved else { return }
            if draft.isNew {
                // New notes pop back to the list, which refreshes.
                onDone()
            } else {
                // Keep the user on the note, now read-only with refreshed metadata.
                setProgrammatically { title = trimmedTitle }
                sessionTitle = title
                sessionContent = content
                mode = .view
            }
        }
    }

    /// Decides what back does: leave, drop to read-only, or ask about edits.
    /// An autosaved-but-unchanged new note still asks — leaving would otherwise
    /// silently keep a draft the user may think was thrown away.
    func handleBack(showDiscard: () -> Void, exit: () -> Void) {
        switch mode {
        case .view:
            exit()
        case .edit:
            if hasChanges || (draft.isNew && savedDuringSession) {
                showDiscard()
            } else if draft.isNew {
                exit()
            } else {
                // No edits on an existing note: drop back to read-only.
                autosaveTask?.cancel()
                mode = .view
            }
        }
    }

    func confirmDiscard(exit: @escaping () -> Void) {
        autosaveTask?.cancel()
        if draft.isNew {
            Task {
                // Autosave may have already stored the draft — remove it again.
                if savedDuringSession {
                    _ = try? await notesService.deleteNote(id: draft.id)
                }
                exit()
            }
        } else {
            // Revert the editor to the session snapshot; if autosave already
            // wrote newer content, persist the revert so the stored note
            // matches again.
            setProgrammatically {
                title = sessionTitle
                content = sessionContent
            }
            mode = .view
            if savedDuringSession {
                Task { await persist() }
            }
        }
    }

    func delete(onDone: @escaping () -> Void) {
        guard !draft.isNew, !isDeleting else { return }
        autosaveTask?.cancel()
        isDeleting = true
        errorMessage = nil
        Task {
            defer { isDeleting = false }
            do {
                if let error = try await notesService.deleteNote(id: draft.id) {
                    errorMessage = notesMessage(for: error)
                } else {
                    onDone()
                }
            } catch {
                errorMessage = "Couldn't delete the note. Try again."
            }
        }
    }

    private func setProgrammatically(_ mutate: () -> Void) {
        isProgrammaticChange = true
        mutate()
        isProgrammaticChange = false
    }
}
