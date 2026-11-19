import SwiftUI
import SharedLogic

struct NotesListView: View {
    @ObservedObject var viewModel: NotesListViewModel
    /// The note open in the detail column beside the list (wide windows only).
    var selectedNoteID: String? = nil
    let onOpen: (IosNote) -> Void
    let onCreate: () -> Void
    let onOpenSettings: () -> Void

    @State private var noteToDelete: IosNote?

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: Space.md)]

    var body: some View {
        VStack(spacing: 0) {
            if !viewModel.isOnline {
                OfflineBanner(text: "Offline — changes are saved on this device")
                    .padding(.horizontal, Space.lg)
                    .padding(.bottom, Space.sm)
            }
            if viewModel.notes.isEmpty {
                emptyState
            } else {
                grid
            }
        }
        .background(AppColor.bg.ignoresSafeArea())
        .navigationTitle("NoteMark")
        .toolbar { toolbarContent }
        .confirmationDialog(
            "Delete this note?",
            isPresented: deleteBinding,
            titleVisibility: .visible,
            presenting: noteToDelete
        ) { note in
            Button("Delete", role: .destructive) { viewModel.delete(id: note.id) }
            Button("Cancel", role: .cancel) {}
        } message: { _ in
            Text("This can't be undone.")
        }
    }

    private var deleteBinding: Binding<Bool> {
        Binding(get: { noteToDelete != nil }, set: { if !$0 { noteToDelete = nil } })
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            UserAvatar(initials: viewModel.initials.isEmpty ? "?" : viewModel.initials) {
                onOpenSettings()
            }
            .accessibilityLabel("Settings")
            .accessibilityIdentifier("avatarButton")
        }
        // The avatar is its own filled circle; no glass capsule behind it.
        .sharedBackgroundVisibility(.hidden)

        if !viewModel.notes.isEmpty {
            ToolbarSpacer(.flexible, placement: .bottomBar)
            ToolbarItem(placement: .bottomBar) {
                Button(action: onCreate) {
                    Label("Create note", systemImage: "plus")
                }
                .buttonStyle(.glassProminent)
                .tint(AppColor.accent)
                .accessibilityIdentifier("createNoteButton")
            }
        }
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, alignment: .leading, spacing: Space.md) {
                ForEach(viewModel.notes, id: \.id) { note in
                    NoteCard(note: note, isSelected: note.id == selectedNoteID)
                        .contentShape(Rectangle())
                        .onTapGesture { onOpen(note) }
                        .onLongPressGesture { noteToDelete = note }
                }
            }
            .padding(.horizontal, Space.lg)
            .padding(.top, Space.sm)
            .padding(.bottom, Space.xxl)
        }
    }

    private var emptyState: some View {
        VStack(spacing: Space.lg) {
            Spacer()
            ZStack {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(AppColor.accent.opacity(0.12))
                    .frame(width: 72, height: 72)
                Image(systemName: "doc.text")
                    .font(.system(size: 30))
                    .foregroundStyle(AppColor.accent)
            }
            VStack(spacing: Space.sm) {
                Text("No notes yet")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(AppColor.fg)
                Text("Your notes will show up here. Create your first one to get started.")
                    .font(.nmSubhead)
                    .foregroundStyle(AppColor.fgMuted)
                    .multilineTextAlignment(.center)
            }
            PrimaryButton(title: "New note", action: onCreate)
                .frame(maxWidth: 220)
            Spacer()
            Spacer()
        }
        .padding(.horizontal, Space.xxl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
