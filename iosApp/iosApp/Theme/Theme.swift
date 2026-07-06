import SwiftUI

// MARK: - Color tokens
// Mirrors specs/Notemark_designs/_ds/.../colors_and_type.css.
// One green accent; everything else a cool neutral scale. Light + dark.

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }

    /// Picks a color based on the active interface style.
    fileprivate static func dynamic(light: Color, dark: Color) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
    }
}

enum AppColor {
    // Accent (brand green) — brighter on dark for AA contrast.
    static let accent = Color.dynamic(light: Color(hex: 0x12B76A), dark: Color(hex: 0x32D583))
    static let accentPress = Color.dynamic(light: Color(hex: 0x027A48), dark: Color(hex: 0x12B76A))
    static let onAccent = Color.dynamic(light: .white, dark: Color(hex: 0x052E1B))

    // Surfaces
    static let bg = Color.dynamic(light: Color(hex: 0xFFFFFF), dark: Color(hex: 0x09090B))
    static let bgSubtle = Color.dynamic(light: Color(hex: 0xF2F2F7), dark: Color(hex: 0x18181B))
    static let surface = Color.dynamic(light: Color(hex: 0xFFFFFF), dark: Color(hex: 0x18181B))

    // Foreground (text + icon)
    static let fg = Color.dynamic(light: Color(hex: 0x09090B), dark: Color(hex: 0xFAFAFA))
    static let fgMuted = Color.dynamic(light: Color(hex: 0x52525B), dark: Color(hex: 0xA1A1AA))
    static let fgSubtle = Color.dynamic(light: Color(hex: 0x71717A), dark: Color(hex: 0x71717A))
    static let label = Color.dynamic(light: Color(hex: 0x3F3F46), dark: Color(hex: 0xD4D4D8))

    // Borders + fills
    static let border = Color.dynamic(light: Color(hex: 0xE4E4E7), dark: Color(hex: 0x27272A))
    static let secondaryFill = Color.dynamic(
        light: Color(hex: 0x787880, alpha: 0.12),
        dark: Color(hex: 0x787880, alpha: 0.24)
    )

    // Semantic
    static let danger = Color.dynamic(light: Color(hex: 0xEF4444), dark: Color(hex: 0xF87171))

    // Warning (offline state, M4) — amber, mirrors Android's WarningContainer.
    static let warningBg = Color.dynamic(light: Color(hex: 0xFFFBEB), dark: Color(hex: 0x2A2109))
    static let warningFg = Color.dynamic(light: Color(hex: 0xB45309), dark: Color(hex: 0xFBBF24))
}

// MARK: - Spacing scale (4px base unit)

enum Space {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 20
    static let xxl: CGFloat = 24
    static let huge: CGFloat = 32
}

// MARK: - Corner radii

enum Radius {
    static let input: CGFloat = 12
    static let button: CGFloat = 14
    static let card: CGFloat = 14
}

// MARK: - Typography
// System font (SF Pro) at the design system's iOS-native scale. The brand uses
// Inter, whose stack falls back to -apple-system on Apple platforms.

extension Font {
    static let nmDisplay = Font.system(size: 34, weight: .bold)
    static let nmTitle = Font.system(size: 28, weight: .bold)
    static let nmBody = Font.system(size: 17)
    static let nmHeadline = Font.system(size: 17, weight: .semibold)
    static let nmSubhead = Font.system(size: 15)
    static let nmLabel = Font.system(size: 13, weight: .semibold)
    static let nmFootnote = Font.system(size: 13)
}
