import SwiftUI

struct LandingView: View {
    let onGetStarted: () -> Void
    let onLogIn: () -> Void

    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        Group {
            if horizontalSizeClass == .regular {
                // Wide window: copy on the leading side, actions on the trailing side.
                HStack(alignment: .center, spacing: Space.huge) {
                    copy
                        .frame(maxWidth: 520, alignment: .leading)
                    Spacer(minLength: 0)
                    actions
                        .frame(maxWidth: FormWidth.max - 60)
                }
                .padding(.horizontal, 48)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                VStack(spacing: 0) {
                    copy
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                        .padding(.horizontal, 28)
                    actions
                        .padding(.horizontal, Space.xl)
                        .padding(.bottom, 36)
                }
            }
        }
        .background(AppColor.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }

    private var copy: some View {
        VStack(alignment: .leading, spacing: Space.lg) {
            Wordmark(size: 17, dotSize: 6)
                .padding(.bottom, Space.xs)
            Text("Your notes, everywhere you are.")
                .font(.nmDisplay)
                .foregroundStyle(AppColor.fg)
                .lineSpacing(2)
                .fixedSize(horizontal: false, vertical: true)
            Text("Write freely. NoteMark keeps everything on your device and in sync — even offline.")
                .font(.nmBody)
                .foregroundStyle(AppColor.fgMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var actions: some View {
        VStack(spacing: Space.md - 2) {
            PrimaryButton(title: "Get started", action: onGetStarted)
            SecondaryButton(title: "Log in", action: onLogIn)
        }
    }
}
