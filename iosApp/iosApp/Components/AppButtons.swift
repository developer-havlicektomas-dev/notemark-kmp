import SwiftUI

/// Primary action as prominent Liquid Glass tinted with the accent. Shows a
/// spinner and disables itself while busy.
struct PrimaryButton: View {
    let title: String
    var isLoading: Bool = false
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                if isLoading {
                    ProgressView()
                } else {
                    Text(title)
                        .font(.nmHeadline)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.glassProminent)
        .tint(AppColor.accent)
        .controlSize(.extraLarge)
        .disabled(!isEnabled || isLoading)
    }
}

/// Neutral secondary action on regular Liquid Glass ("Log in" on landing).
struct SecondaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.nmHeadline)
                .foregroundStyle(AppColor.fg)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.glass)
        .controlSize(.extraLarge)
    }
}

/// Max width for forms and settings on wide windows (unfolded iPhone Duo, iPad).
enum FormWidth {
    static let max: CGFloat = 420
}

/// Scales to 0.97 on press — the design system's tactile press feedback.
struct PressScaleStyle: ButtonStyle {
    var scale: CGFloat = 0.97
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
