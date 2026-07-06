import SwiftUI

/// The NoteMark wordmark: "NoteMark" with a green accent dot, per the design.
struct Wordmark: View {
    var size: CGFloat = 34
    var dotSize: CGFloat = 9

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 3) {
            Text("NoteMark")
                .font(.system(size: size, weight: .bold))
                .foregroundStyle(AppColor.fg)
                .tracking(-0.5)
            Circle()
                .fill(AppColor.accent)
                .frame(width: dotSize, height: dotSize)
        }
    }
}
