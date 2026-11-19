import SwiftUI

/// Distraction-free reading. Tapping anywhere toggles a floating control bar that
/// also auto-hides a few seconds after it appears. Mirrors the Android ReaderScreen.
struct ReaderView: View {
    let title: String
    let content: String
    let onBack: () -> Void

    @State private var controlsVisible = true

    private let autoHideSeconds: UInt64 = 3

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Space.xl) {
                Text(title)
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(AppColor.fg)

                if !content.isEmpty {
                    Text(content)
                        .font(.system(size: 18))
                        .foregroundStyle(AppColor.fg)
                        .lineSpacing(8)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Space.xl)
            .padding(.top, Space.lg)
            .padding(.bottom, Space.huge)
            .frame(maxWidth: ReadingWidth.max)
            .frame(maxWidth: .infinity)
        }
        .background(AppColor.bg.ignoresSafeArea())
        // The controls reserve their own space at the top instead of covering
        // the text, and stay inside the safe area on every edge.
        .safeAreaBar(edge: .top) {
            if controlsVisible {
                controlsBar
                    .padding(.horizontal, Space.md)
                    .transition(.opacity)
            }
        }
        .overlay(alignment: .bottom) {
            if !controlsVisible {
                Text("Tap anywhere to show controls")
                    .font(.nmFootnote)
                    .foregroundStyle(AppColor.fgMuted)
                    .padding(.bottom, Space.xl)
                    .transition(.opacity)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) { controlsVisible.toggle() }
        }
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
        // Auto-hide: each time the controls become visible, start a fresh timer.
        .task(id: controlsVisible) {
            guard controlsVisible else { return }
            try? await Task.sleep(nanoseconds: autoHideSeconds * 1_000_000_000)
            if !Task.isCancelled {
                withAnimation(.easeInOut(duration: 0.2)) { controlsVisible = false }
            }
        }
    }

    private var controlsBar: some View {
        HStack(spacing: Space.sm) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(AppColor.fg)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(PressScaleStyle(scale: 0.92))
            .accessibilityIdentifier("readerBackButton")

            Spacer()

            Text("READER")
                .font(.system(size: 12, weight: .semibold))
                .kerning(0.5)
                .foregroundStyle(AppColor.fgMuted)

            Spacer()

            // Typography control is visual-only in M3.
            Text("Aa")
                .font(.system(size: 15, weight: .bold, design: .monospaced))
                .foregroundStyle(AppColor.fgMuted)
                .frame(width: 40, height: 40)
        }
        .padding(.horizontal, Space.sm)
        .frame(height: 48)
        .glassEffect(.regular.interactive(), in: Capsule())
        .padding(.top, Space.sm)
    }
}
