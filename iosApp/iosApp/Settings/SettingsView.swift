import SwiftUI
import SharedLogic

struct SettingsView: View {
    let onBack: () -> Void
    let onLoggedOut: () -> Void

    @StateObject private var viewModel = SettingsViewModel()
    @State private var showLogoutConfirm = false
    @State private var showIntervalPicker = false
    @State private var showClearDataConfirm = false

    private let intervalOptions: [SyncInterval] = [.manualOnly, .minutes15, .minutes30, .hour1]

    var body: some View {
        VStack(spacing: 0) {
            topBar

            ScrollView {
                VStack(alignment: .leading, spacing: Space.md) {
                    if !viewModel.isOnline {
                        OfflineBanner(text: "You are offline. Connect to sync or log out.")
                    }

                    profileCard

                    sectionLabel("Sync")
                        .padding(.top, Space.sm)
                    syncNowRow
                    autoSyncRow

                    sectionLabel("Data")
                        .padding(.top, Space.sm)
                    DestructiveButton(title: "Clear local data", systemImage: "trash") {
                        showClearDataConfirm = true
                    }
                    .accessibilityIdentifier("clearDataButton")

                    DestructiveButton(title: "Log out", systemImage: "rectangle.portrait.and.arrow.right") {
                        if viewModel.canStartLogout() {
                            showLogoutConfirm = true
                        }
                    }
                    .disabled(viewModel.isLoggingOut)
                    .accessibilityIdentifier("logoutButton")
                }
                .padding(.horizontal, Space.lg)
                .padding(.top, Space.xl)
                .padding(.bottom, Space.huge)
            }
        }
        .background(AppColor.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
        .task { await viewModel.load() }
        .confirmationDialog("Log out?", isPresented: $showLogoutConfirm, titleVisibility: .visible) {
            Button("Sync and log out") { viewModel.syncAndLogout(onLoggedOut: onLoggedOut) }
            Button("Log out without syncing", role: .destructive) {
                viewModel.logoutWithoutSync(onLoggedOut: onLoggedOut)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(viewModel.logoutDialogMessage)
        }
        .confirmationDialog("Automatic sync", isPresented: $showIntervalPicker, titleVisibility: .visible) {
            ForEach(intervalOptions, id: \.name) { option in
                Button(intervalLabel(option)) { viewModel.setInterval(option) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Currently: \(intervalLabel(viewModel.syncInterval))")
        }
        .alert("Clear local data?", isPresented: $showClearDataConfirm) {
            Button("Clear", role: .destructive) { viewModel.clearLocalData() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This removes synced notes stored on this device. Notes with unsynced changes are kept until they upload.")
        }
        .alert("NoteMark", isPresented: messageBinding) {
            Button("OK") { viewModel.message = nil }
        } message: {
            Text(viewModel.message ?? "")
        }
    }

    private var messageBinding: Binding<Bool> {
        Binding(
            get: { viewModel.message != nil },
            set: { if !$0 { viewModel.message = nil } }
        )
    }

    // MARK: - Sync rows

    private var syncNowRow: some View {
        Button {
            viewModel.syncNow()
        } label: {
            HStack(spacing: Space.md) {
                Group {
                    if viewModel.isSyncing {
                        ProgressView()
                            .tint(AppColor.accent)
                    } else {
                        Image(systemName: "arrow.triangle.2.circlepath")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundStyle(AppColor.fgMuted)
                    }
                }
                .frame(width: 24, height: 24)

                VStack(alignment: .leading, spacing: 2) {
                    Text(viewModel.isSyncing ? "Syncing…" : "Sync now")
                        .font(.nmBody)
                        .foregroundStyle(AppColor.fg)
                    Text(viewModel.lastSyncedText)
                        .font(.nmFootnote)
                        .foregroundStyle(AppColor.fgMuted)
                        .accessibilityIdentifier("lastSyncedLabel")
                }
                Spacer()
            }
            .padding(.horizontal, Space.md)
            .padding(.vertical, Space.md)
        }
        .buttonStyle(PressScaleStyle())
        .disabled(viewModel.isSyncing)
        .accessibilityIdentifier("syncNowRow")
    }

    private var autoSyncRow: some View {
        Button {
            showIntervalPicker = true
        } label: {
            HStack(spacing: Space.md) {
                Image(systemName: "clock")
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(AppColor.fgMuted)
                    .frame(width: 24, height: 24)
                Text("Automatic sync")
                    .font(.nmBody)
                    .foregroundStyle(AppColor.fg)
                Spacer()
                Text(intervalLabel(viewModel.syncInterval))
                    .font(.nmSubhead)
                    .foregroundStyle(AppColor.fgMuted)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(AppColor.fgSubtle)
            }
            .padding(.horizontal, Space.md)
            .padding(.vertical, Space.md)
        }
        .buttonStyle(PressScaleStyle())
        .accessibilityIdentifier("autoSyncRow")
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.system(size: 13, weight: .semibold))
            .kerning(0.3)
            .foregroundStyle(AppColor.accent)
            .padding(.leading, Space.xs)
    }

    private func intervalLabel(_ interval: SyncInterval) -> String {
        switch interval.name {
        case "MANUAL_ONLY": return "Manual only"
        case "MINUTES_15": return "15 minutes"
        case "MINUTES_30": return "30 minutes"
        default: return "1 hour"
        }
    }

    // MARK: - Chrome

    private var topBar: some View {
        HStack(spacing: Space.xs) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(AppColor.fg)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(PressScaleStyle(scale: 0.92))
            .accessibilityIdentifier("settingsBackButton")

            Text("Settings")
                .font(.nmHeadline)
                .foregroundStyle(AppColor.fg)

            Spacer()
        }
        .padding(.horizontal, Space.sm)
        .frame(height: 56)
    }

    private var profileCard: some View {
        HStack(spacing: Space.md) {
            Text(viewModel.initials.isEmpty ? "?" : viewModel.initials)
                .font(.system(size: 19, weight: .semibold))
                .foregroundStyle(AppColor.onAccent)
                .frame(width: 52, height: 52)
                .background(AppColor.accent)
                .clipShape(Circle())

            Text(viewModel.username)
                .font(.nmHeadline)
                .foregroundStyle(AppColor.fg)

            Spacer()

            if viewModel.isLoggingOut {
                ProgressView()
                    .tint(AppColor.fgMuted)
            }
        }
        .padding(.horizontal, Space.lg)
        .padding(.vertical, Space.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: Radius.card, style: .continuous))
    }
}

/// Full-width bordered destructive action used in Settings.
private struct DestructiveButton: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: Space.sm) {
                Image(systemName: systemImage)
                    .font(.system(size: 17, weight: .medium))
                Text(title)
                    .font(.nmHeadline)
            }
            .foregroundStyle(AppColor.danger)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.button, style: .continuous)
                    .stroke(AppColor.danger, lineWidth: 1)
            )
        }
        .buttonStyle(PressScaleStyle())
    }
}
