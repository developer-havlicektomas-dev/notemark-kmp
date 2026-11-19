import SwiftUI
import SharedLogic

/// A single note in the grid: monospaced date, title, and a clamped preview.
struct NoteCard: View {
    let note: IosNote
    /// Outlined when the note is open in the detail column beside the list.
    var isSelected: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: Space.sm) {
            Text(shortDate(epochMs: note.createdAtEpochMs))
                .font(.system(size: 11, design: .monospaced))
                .foregroundStyle(AppColor.fgSubtle)

            Text(note.title)
                .font(.nmHeadline)
                .foregroundStyle(AppColor.fg)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            if !note.content.isEmpty {
                Text(note.content)
                    .font(.nmFootnote)
                    .foregroundStyle(AppColor.fgMuted)
                    .lineLimit(4)
                    .multilineTextAlignment(.leading)
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, minHeight: 140, alignment: .topLeading)
        .padding(Space.lg)
        .background(AppColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: Radius.card, style: .continuous))
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: Radius.card, style: .continuous)
                    .strokeBorder(AppColor.accent, lineWidth: 2)
            }
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Circular avatar showing the user's initials. Tapping it logs out in M2;
/// it becomes the Settings entry point in M3.
struct UserAvatar: View {
    let initials: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(initials)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(AppColor.onAccent)
                .frame(width: 40, height: 40)
                .background(AppColor.accent)
                .clipShape(Circle())
        }
        .buttonStyle(PressScaleStyle(scale: 0.92))
    }
}

/// Short "12 Jun" form for note cards.
func shortDate(epochMs: Int64) -> String {
    let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
    let formatter = DateFormatter()
    formatter.dateFormat = "d MMM"
    return formatter.string(from: date)
}

/// Relative "2 minutes ago" form for the View-mode metadata line.
func relativeTime(epochMs: Int64) -> String {
    let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
    let formatter = RelativeDateTimeFormatter()
    formatter.unitsStyle = .full
    return formatter.localizedString(for: date, relativeTo: Date())
}

/// Relative lowercase form for status lines ("Saved just now", "Last synced
/// 2 minutes ago"): "just now" / "N minutes ago" / "N hours ago", falling back
/// to the short date past a day. Mirrors the shared `toRelativeTime()`.
func relativeStatusTime(epochMs: Int64) -> String {
    let seconds = max(0, Date().timeIntervalSince(Date(timeIntervalSince1970: Double(epochMs) / 1000)))
    if seconds < 60 { return "just now" }
    if seconds < 3600 {
        let minutes = Int(seconds / 60)
        return "\(minutes) minute\(minutes == 1 ? "" : "s") ago"
    }
    if seconds < 86_400 {
        let hours = Int(seconds / 3600)
        return "\(hours) hour\(hours == 1 ? "" : "s") ago"
    }
    return shortDate(epochMs: epochMs)
}

/// Sync-status form per the M4 spec: relative within a day, then a full
/// "12 Jun 2026, 14:05" timestamp. Mirrors the shared `toSyncStatusTime()`.
func syncStatusTime(epochMs: Int64) -> String {
    let date = Date(timeIntervalSince1970: Double(epochMs) / 1000)
    if Date().timeIntervalSince(date) < 86_400 {
        return relativeStatusTime(epochMs: epochMs)
    }
    let formatter = DateFormatter()
    formatter.dateFormat = "d MMM yyyy, HH:mm"
    return formatter.string(from: date)
}

/// At most two uppercase initials from a username (e.g. "Sam Rivera" → "SR").
func userInitials(from username: String) -> String {
    let parts = username.split(whereSeparator: { $0.isWhitespace }).filter { !$0.isEmpty }
    switch parts.count {
    case 0: return ""
    case 1: return String(parts[0].prefix(2)).uppercased()
    default: return (String(parts.first!.prefix(1)) + String(parts.last!.prefix(1))).uppercased()
    }
}
