import SwiftUI
import SharedLogic

struct NotesListView: View {
    @ObservedObject var viewModel: NotesListViewModel
    let onOpen: (IosNote) -> Void
    let onCreate: () -> Void
    let onOpenSettings: () -> Void

    @State private var noteToDelete: IosNote?

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: Space.md)]

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            AppColor.bg.ignoresSafeArea()

            VStack(spacing: 0) {
                header
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

            if !viewModel.notes.isEmpty {
                createButton
            }
        }
        .toolbar(.hidden, for: .navigationBar)
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

    private var header: some View {
        HStack {
            Text("NoteMark")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(AppColor.fg)
            Spacer()
            UserAvatar(initials: viewModel.initials.isEmpty ? "?" : viewModel.initials) {
                onOpenSettings()
            }
            .accessibilityIdentifier("avatarButton")
        }
        .padding(.horizontal, Space.xl)
        .frame(height: 64)
    }

    private var grid: some View {
        ScrollView {
            LazyVGrid(columns: columns, alignment: .leading, spacing: Space.md) {
                ForEach(viewModel.notes, id: \.id) { note in
                    NoteCard(note: note)
                        .contentShape(Rectangle())
                        .onTapGesture { onOpen(note) }
                        .onLongPressGesture { noteToDelete = note }
                }
            }
            .padding(.horizontal, Space.lg)
            .padding(.top, Space.sm)
            .padding(.bottom, 120)
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

    private var createButton: some View {
        Button(action: onCreate) {
            Image(systemName: "plus")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(AppColor.onAccent)
                .frame(width: 60, height: 60)
                .background(AppColor.accent)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
        }
        .buttonStyle(PressScaleStyle())
        .accessibilityIdentifier("createNoteButton")
        .padding(.trailing, Space.xl)
        .padding(.bottom, Space.xxl)
    }
}
