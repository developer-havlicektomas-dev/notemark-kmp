import SwiftUI

/// Amber offline notice used on the notes list, note view, and settings (M4).
struct OfflineBanner: View {
    let text: String

    var body: some View {
        HStack(spacing: Space.sm) {
            Image(systemName: "wifi.slash")
                .font(.system(size: 13, weight: .medium))
            Text(text)
                .font(.system(size: 13, weight: .medium))
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(AppColor.warningFg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(AppColor.warningBg)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .accessibilityIdentifier("offlineBanner")
    }
}
