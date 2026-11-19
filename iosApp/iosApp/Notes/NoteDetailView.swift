import SwiftUI
import SharedLogic

struct NoteDetailView: View {
    let draft: NoteDraft
    /// Whether View mode shows a back button. Off when the note sits beside the
    /// list on a wide window, where there is nothing to go back to.
    let showsBackButton: Bool
    let onDone: () -> Void
    let onSaved: () -> Void
    let onOpenReader: (_ title: String, _ content: String) -> Void

    @StateObject private var viewModel: NoteDetailViewModel
    @State private var showDiscardConfirm = false
    @State private var showDeleteConfirm = false

    init(
        draft: NoteDraft,
        showsBackButton: Bool = true,
        onOpenReader: @escaping (_ title: String, _ content: String) -> Void = { _, _ in },
        onSaved: @escaping () -> Void = {},
        onDone: @escaping () -> Void
    ) {
        self.draft = draft
        self.showsBackButton = showsBackButton
        self.onDone = onDone
        self.onSaved = onSaved
        self.onOpenReader = onOpenReader
        _viewModel = StateObject(wrappedValue: NoteDetailViewModel(draft: draft))
    }

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.mode {
            case .edit:
                saveStatusLine
                editor
            case .view:
                reader
            }
        }
        .background(AppColor.bg.ignoresSafeArea())
        .navigationTitle(viewModel.mode == .edit ? "Edit note" : "")
        .navigationBarTitleDisplayMode(.inline)
        // Back goes through the view model so unsaved edits get the discard dialog.
        .navigationBarBackButtonHidden(true)
        .toolbar {
            switch viewModel.mode {
            case .edit: editToolbar
            case .view: viewToolbar
            }
        }
        .task { await viewModel.reloadFromStore() }
        .onChange(of: viewModel.lastSavedAtEpochMs) { _, _ in onSaved() }
        .confirmationDialog("Discard changes?", isPresented: $showDiscardConfirm, titleVisibility: .visible) {
            Button("Discard", role: .destructive) { viewModel.confirmDiscard(exit: onDone) }
            Button("Keep editing", role: .cancel) {}
        } message: {
            Text("Your edits won't be saved.")
        }
        .confirmationDialog("Delete note?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Delete", role: .destructive) { viewModel.delete(onDone: onDone) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This note will be permanently deleted.")
        }
    }

    private func attemptBack() {
        viewModel.handleBack(showDiscard: { showDiscardConfirm = true }, exit: onDone)
    }

    // MARK: - Edit mode

    /// M4 autosave feedback: green "Saved just now" online, amber offline note.
    @ViewBuilder
    private var saveStatusLine: some View {
        if let savedAt = viewModel.lastSavedAtEpochMs {
            HStack(spacing: 6) {
                if viewModel.isOnline {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(AppColor.accent)
                    Text("Saved \(relativeStatusTime(epochMs: savedAt))")
                        .font(.system(size: 12))
                        .foregroundStyle(AppColor.fgMuted)
                } else {
                    Image(systemName: "wifi.slash")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(AppColor.warningFg)
                    Text("Offline — saved on this device")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(AppColor.warningFg)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Space.lg + Space.xs)
            .padding(.bottom, Space.sm)
            .frame(maxWidth: ReadingWidth.max)
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("saveStatusLine")
        }
    }

    private var editor: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.md) {
                TextField("Note title", text: $viewModel.title, axis: .vertical)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(AppColor.fg)
                    .textInputAutocapitalization(.sentences)
                    .accessibilityIdentifier("noteTitleField")

                Divider()
                    .overlay(AppColor.border)

                TextField("Start typing…", text: $viewModel.content, axis: .vertical)
                    .font(.nmBody)
                    .foregroundStyle(AppColor.fg)
                    .frame(minHeight: 220, alignment: .topLeading)
                    .accessibilityIdentifier("noteContentField")

                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.nmFootnote)
                        .foregroundStyle(AppColor.danger)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(.horizontal, Space.lg)
            .padding(.top, Space.md)
            .padding(.bottom, Space.xxl)
            .frame(maxWidth: ReadingWidth.max)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    @ToolbarContentBuilder
    private var editToolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button("Close", systemImage: "xmark", action: attemptBack)
                .accessibilityIdentifier("editCloseButton")
        }
        ToolbarItem(placement: .confirmationAction) {
            Button {
                viewModel.save(onDone: onDone)
            } label: {
                if viewModel.isSaving {
                    ProgressView()
                } else {
                    Text("Save")
                }
            }
            .buttonStyle(.glassProminent)
            .tint(AppColor.accent)
            .disabled(!viewModel.canSave)
        }
    }

    // MARK: - View mode

    private var reader: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(viewModel.title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(AppColor.fg)

                Text("Created \(shortDate(epochMs: viewModel.createdAtEpochMs)) · Edited \(relativeTime(epochMs: viewModel.lastEditedAtEpochMs))")
                    .font(.system(size: 12, design: .monospaced))
                    .foregroundStyle(AppColor.fgMuted)
                    .padding(.top, Space.sm)

                if !viewModel.isOnline {
                    OfflineBanner(text: "Saved on this device")
                        .padding(.top, Space.md)
                }

                if !viewModel.content.isEmpty {
                    Text(viewModel.content)
                        .font(.nmBody)
                        .foregroundStyle(AppColor.fg)
                        .lineSpacing(6)
                        .padding(.top, Space.lg)
                }

                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.nmFootnote)
                        .foregroundStyle(AppColor.danger)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, Space.md)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Space.xxl)
            .padding(.top, Space.sm)
            .padding(.bottom, Space.huge)
            .frame(maxWidth: ReadingWidth.max)
            .frame(maxWidth: .infinity)
        }
    }

    @ToolbarContentBuilder
    private var viewToolbar: some ToolbarContent {
        if showsBackButton {
            ToolbarItem(placement: .topBarLeading) {
                Button("Back", systemImage: "chevron.left", action: attemptBack)
                    .accessibilityIdentifier("detailBackButton")
            }
        }
        ToolbarItemGroup(placement: .topBarTrailing) {
            Button("Reader mode", systemImage: "book") {
                onOpenReader(viewModel.title, viewModel.content)
            }
            .accessibilityIdentifier("readerButton")

            Button("Edit", action: viewModel.startEditing)
        }
        ToolbarSpacer(.fixed, placement: .topBarTrailing)
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Button("Delete", role: .destructive) { showDeleteConfirm = true }
            } label: {
                Label("More", systemImage: "ellipsis")
            }
            .accessibilityIdentifier("noteOptionsButton")
        }
    }
}

/// Caps long-form text at a comfortable line length on wide windows.
enum ReadingWidth {
    static let max: CGFloat = 680
}
