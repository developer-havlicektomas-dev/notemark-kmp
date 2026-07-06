import SwiftUI

/// Filled green primary action. Shows a spinner and disables itself while busy,
/// dims when disabled. 50pt tall, 14pt radius — matches the `.k-btn.primary` kit.
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
                        .tint(AppColor.onAccent)
                } else {
                    Text(title)
                        .font(.nmHeadline)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .foregroundStyle(AppColor.onAccent)
            .background(AppColor.accent)
            .clipShape(RoundedRectangle(cornerRadius: Radius.button, style: .continuous))
            .opacity(isEnabled && !isLoading ? 1 : 0.5)
        }
        .buttonStyle(PressScaleStyle())
        .disabled(!isEnabled || isLoading)
    }
}

/// Neutral secondary action used on the landing screen ("Log in").
struct SecondaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.nmHeadline)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .foregroundStyle(AppColor.fg)
                .background(AppColor.secondaryFill)
                .clipShape(RoundedRectangle(cornerRadius: Radius.button, style: .continuous))
        }
        .buttonStyle(PressScaleStyle())
    }
}

/// iOS-style leading "‹ Back" navigation control in the accent color.
struct BackButton: View {
    var title: String = "Back"
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 1) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 19, weight: .semibold))
                Text(title)
                    .font(.nmBody)
            }
            .foregroundStyle(AppColor.accent)
            .padding(.vertical, Space.xs)
            .padding(.horizontal, Space.sm)
        }
        .buttonStyle(PressScaleStyle(scale: 0.92))
    }
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
