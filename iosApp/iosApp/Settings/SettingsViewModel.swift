import Foundation
import SharedLogic

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var username: String = ""
    @Published var initials: String = ""
    @Published var isOnline = true
    /// Epoch ms of the last completed sync; nil means never synced.
    @Published var lastSyncedAtEpochMs: Int64?
    @Published var syncInterval: SyncInterval = .manualOnly
    @Published var pendingChanges: Int64 = 0
    @Published var isSyncing = false
    @Published var isLoggingOut = false
    /// Transient user-facing notice (sync failures, offline logout block).
    @Published var message: String?

    private let auth = IosAuthService()
    private let notesService = IosNotesService()
    private let syncService = IosSyncService()
    private var watches: [IosWatchHandle] = []

    init() {
        watches.append(syncService.watchIsConnected { [weak self] connected in
            Task { @MainActor in self?.isOnline = connected.boolValue }
        })
        watches.append(syncService.watchLastSyncedAt { [weak self] epochMs in
            Task { @MainActor in self?.lastSyncedAtEpochMs = epochMs?.int64Value }
        })
        watches.append(syncService.watchSyncInterval { [weak self] interval in
            Task { @MainActor in self?.syncInterval = interval }
        })
        watches.append(syncService.watchPendingChanges { [weak self] count in
            Task { @MainActor in self?.pendingChanges = count.int64Value }
        })
    }

    deinit {
        watches.forEach { $0.cancel() }
    }

    var lastSyncedText: String {
        lastSyncedAtEpochMs.map { "Last synced \(syncStatusTime(epochMs: $0))" } ?? "Never synced"
    }

    var logoutDialogMessage: String {
        switch pendingChanges {
        case 0: return "You're all caught up. Log out?"
        case 1: return "You have 1 change that hasn't synced. Sync it before you log out."
        default: return "You have \(pendingChanges) changes that haven't synced. Sync them before you log out."
        }
    }

    func load() async {
        if let name = try? await auth.username() {
            username = name
            initials = userInitials(from: name)
        }
    }

    func syncNow() {
        guard !isSyncing else { return }
        isSyncing = true
        Task {
            defer { isSyncing = false }
            do {
                if let error = try await syncService.syncNow() {
                    message = notesMessage(for: error)
                }
                // Success updates the row via the lastSyncedAt watcher.
            } catch {
                message = "Couldn't reach the server. Check your connection and try again."
            }
        }
    }

    func setInterval(_ interval: SyncInterval) {
        Task {
            try? await syncService.setSyncInterval(interval: interval)
            // The syncInterval watcher confirms the new value in the row.
        }
    }

    /// M4: logging out needs a connection — the queue check and any final sync
    /// both talk to the backend. Returns whether the logout dialog may open.
    func canStartLogout() -> Bool {
        if !isOnline {
            message = "You are offline. Connect to sync or log out."
            return false
        }
        return true
    }

    /// Full sync, then wipe everything: notes, queue, sync metadata, session.
    func syncAndLogout(onLoggedOut: @escaping () -> Void) {
        guard !isLoggingOut else { return }
        isLoggingOut = true
        Task {
            do {
                if let error = try await syncService.syncNow() {
                    // Sync failed — keep the user logged in so nothing is lost.
                    isLoggingOut = false
                    message = notesMessage(for: error)
                    return
                }
                try await notesService.clearAllLocalData()
                try await auth.clearSession()
                onLoggedOut()
            } catch {
                isLoggingOut = false
                message = "Couldn't reach the server. Check your connection and try again."
            }
        }
    }

    /// Clears session + synced notes but preserves the queue for post-login recovery.
    func logoutWithoutSync(onLoggedOut: @escaping () -> Void) {
        guard !isLoggingOut else { return }
        isLoggingOut = true
        Task {
            do {
                try await notesService.clearLocalData()
                try await auth.clearSession()
                onLoggedOut()
            } catch {
                isLoggingOut = false
                message = "Something went wrong. Try again."
            }
        }
    }

    /// Removes synced notes from this device; unsynced rows survive until they upload.
    func clearLocalData() {
        Task {
            try? await notesService.clearLocalData()
        }
    }
}
