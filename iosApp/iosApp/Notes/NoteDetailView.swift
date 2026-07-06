import SwiftUI
import SharedLogic

struct NoteDetailView: View {
    let draft: NoteDraft
    let onDone: () -> Void
    let onOpenReader: (_ title: String, _ content: String) -> Void

    @StateObject private var viewModel: NoteDetailViewModel
    @State private var showDiscardConfirm = false
    @State private var showDeleteConfirm = false

    init(
        draft: NoteDraft,
        onOpenReader: @escaping (_ title: String, _ content: String) -> Void = { _, _ in },
        onDone: @escaping () -> Void
    ) {
        self.draft = draft
        self.onDone = onDone
        self.onOpenReader = onOpenReader
        _viewModel = StateObject(wrappedValue: NoteDetailViewModel(draft: draft))
    }

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.mode {
            case .edit:
                editTopBar
                saveStatusLine
                editor
            case .view:
                viewTopBar
                reader
            }
        }
        .background(AppColor.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
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
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private var editTopBar: some View {
        HStack(spacing: Space.sm) {
            Button(action: attemptBack) {
                Image(systemName: "xmark")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(AppColor.fg)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(PressScaleStyle(scale: 0.92))
            .accessibilityIdentifier("editCloseButton")

            Text("Edit note")
                .font(.nmHeadline)
                .foregroundStyle(AppColor.fg)
                .frame(maxWidth: .infinity)

            Button {
                viewModel.save(onDone: onDone)
            } label: {
                Group {
                    if viewModel.isSaving {
                        ProgressView().tint(AppColor.onAccent)
                    } else {
                        Text("Save").font(.nmLabel)
                    }
                }
                .foregroundStyle(AppColor.onAccent)
                .frame(height: 36)
                .padding(.horizontal, Space.lg)
                .background(AppColor.accent)
                .clipShape(Capsule())
                .opacity(viewModel.canSave ? 1 : 0.5)
            }
            .buttonStyle(PressScaleStyle())
            .disabled(!viewModel.canSave)
        }
        .padding(.horizontal, Space.md)
        .frame(height: 56)
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
        }
    }

    private var viewTopBar: some View {
        HStack(spacing: Space.xs) {
            Button(action: attemptBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(AppColor.fg)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(PressScaleStyle(scale: 0.92))
            .accessibilityIdentifier("detailBackButton")

            Spacer()

            Button {
                onOpenReader(viewModel.title, viewModel.content)
            } label: {
                Image(systemName: "book")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(AppColor.fg)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(PressScaleStyle(scale: 0.92))
            .accessibilityIdentifier("readerButton")

            Button(action: viewModel.startEditing) {
                Text("Edit")
                    .font(.nmHeadline)
                    .foregroundStyle(AppColor.accent)
                    .padding(.horizontal, Space.sm)
                    .frame(height: 40)
            }
            .buttonStyle(PressScaleStyle(scale: 0.92))

            Menu {
                Button("Delete", role: .destructive) { showDeleteConfirm = true }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(AppColor.fg)
                    .frame(width: 40, height: 40)
            }
            .accessibilityIdentifier("noteOptionsButton")
        }
        .padding(.horizontal, Space.sm)
        .frame(height: 56)
    }
}
