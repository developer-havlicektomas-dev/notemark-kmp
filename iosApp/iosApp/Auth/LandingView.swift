import SwiftUI

struct LandingView: View {
    let onGetStarted: () -> Void
    let onLogIn: () -> Void

    var body: some View {
        VStack(spacing: 0) {
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
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .padding(.horizontal, 28)

            VStack(spacing: Space.md - 2) {
                PrimaryButton(title: "Get started", action: onGetStarted)
                SecondaryButton(title: "Log in", action: onLogIn)
            }
            .padding(.horizontal, Space.xl)
            .padding(.bottom, 36)
        }
        .background(AppColor.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}
