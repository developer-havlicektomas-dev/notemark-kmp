import SwiftUI

/// A labelled form field matching the `.k-input` kit: 48pt tall, 12pt radius,
/// hairline border that turns green with a soft focus ring. Optional helper
/// text below and an optional password visibility toggle.
struct AuthTextField: View {
    let label: String
    var placeholder: String = ""
    @Binding var text: String
    var helper: String? = nil
    var isSecure: Bool = false
    var keyboard: UIKeyboardType = .default
    var textContentType: UITextContentType? = nil
    var submitLabel: SubmitLabel = .next
    var onSubmit: () -> Void = {}

    @State private var isRevealed = false
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Space.xs + 2) {
            Text(label)
                .font(.nmLabel)
                .foregroundStyle(AppColor.label)

            HStack(spacing: Space.sm) {
                field
                    .focused($isFocused)
                    .font(.nmBody)
                    .foregroundStyle(AppColor.fg)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(keyboard)
                    .textContentType(textContentType)
                    .submitLabel(submitLabel)
                    .onSubmit(onSubmit)

                if isSecure {
                    Button {
                        isRevealed.toggle()
                    } label: {
                        Image(systemName: isRevealed ? "eye.slash" : "eye")
                            .font(.system(size: 18))
                            .foregroundStyle(AppColor.fgSubtle)
                            .frame(width: 28, height: 28)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, Space.lg)
            .frame(height: 48)
            .background(AppColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: Radius.input, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: Radius.input, style: .continuous)
                    .stroke(isFocused ? AppColor.accent : AppColor.border, lineWidth: 1)
            )
            .overlay(
                RoundedRectangle(cornerRadius: Radius.input, style: .continuous)
                    .stroke(AppColor.accent.opacity(isFocused ? 0.18 : 0), lineWidth: 4)
            )
            .animation(.easeOut(duration: 0.12), value: isFocused)

            if let helper {
                Text(helper)
                    .font(.nmFootnote)
                    .foregroundStyle(AppColor.fgSubtle)
            }
        }
    }

    @ViewBuilder
    private var field: some View {
        if isSecure && !isRevealed {
            SecureField(placeholder, text: $text)
        } else {
            TextField(placeholder, text: $text)
        }
    }
}
