package dev.havlicektomas.notemark.core.presentation.designsystem.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The handful of stroke icons used by the auth screens, lifted from the
 * design's Lucide paths. Built as stroke [ImageVector]s so they stay
 * dependency-free; [androidx.compose.material3.Icon] tints the whole glyph.
 */
object NoteMarkIcons {

    val ArrowLeft: ImageVector by lazy { strokeIcon("M19 12H5M12 19l-7-7 7-7") }

    val Plus: ImageVector by lazy { strokeIcon("M12 5v14M5 12h14") }

    val Close: ImageVector by lazy { strokeIcon("M18 6L6 18M6 6l12 12") }

    /** Lucide "file-text" — the empty-state glyph. */
    val FileText: ImageVector by lazy {
        strokeIcon(
            "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" +
                " M14 2v6h6" +
                " M16 13H8" +
                " M16 17H8" +
                " M10 9H8",
        )
    }

    val Eye: ImageVector by lazy {
        strokeIcon("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z M9 12a3 3 0 1 0 6 0 3 3 0 1 0-6 0")
    }

    val EyeOff: ImageVector by lazy {
        strokeIcon(
            "M3 3l18 18" +
                " M10.6 10.6a3 3 0 0 0 4.2 4.2" +
                " M9.4 5.1A10 10 0 0 1 12 5c6.5 0 10 7 10 7a18 18 0 0 1-3 3.7" +
                " M6.6 6.6A18 18 0 0 0 2 12s3.5 7 10 7a10 10 0 0 0 3.3-.6",
        )
    }

    /** Open-book glyph for the reader-mode toggle. */
    val BookReader: ImageVector by lazy {
        strokeIcon(
            "M2 4h7a3 3 0 0 1 3 3v13a2.5 2.5 0 0 0-2.5-2.5H2z" +
                " M22 4h-7a3 3 0 0 0-3 3v13a2.5 2.5 0 0 1 2.5-2.5H22z",
        )
    }

    /** Lucide "pencil" — the edit-mode toggle. */
    val Pencil: ImageVector by lazy {
        strokeIcon("M12 20h9 M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    }

    /** Lucide "trash-2" — delete action. */
    val Trash: ImageVector by lazy {
        strokeIcon(
            "M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" +
                " M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M10 11v6 M14 11v6",
        )
    }

    /** Lucide "log-out". */
    val LogOut: ImageVector by lazy {
        strokeIcon("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9")
    }

    /** Three vertical dots — the view-mode overflow menu. */
    val MoreVertical: ImageVector by lazy {
        strokeIcon(
            "M12 6.3a1.3 1.3 0 1 0 0-2.6 1.3 1.3 0 0 0 0 2.6z" +
                " M12 13.3a1.3 1.3 0 1 0 0-2.6 1.3 1.3 0 0 0 0 2.6z" +
                " M12 20.3a1.3 1.3 0 1 0 0-2.6 1.3 1.3 0 0 0 0 2.6z",
        )
    }

    /** Slashed wifi glyph for the offline indicators. */
    val WifiOff: ImageVector by lazy {
        strokeIcon("M2 2l20 20 M5 12.5a11 11 0 0 1 6-2.4 M12 20h.01")
    }

    /** Lucide "check" — the saved-status tick. */
    val Check: ImageVector by lazy { strokeIcon("M20 6L9 17l-5-5") }

    /** Circular arrows for the Sync-now row. */
    val Sync: ImageVector by lazy {
        strokeIcon(
            "M3 12a9 9 0 0 1 9-9 9 9 0 0 1 6.7 3 M21 3v6h-6" +
                " M21 12a9 9 0 0 1-9 9 9 9 0 0 1-6.7-3 M3 21v-6h6",
        )
    }

    /** Clock face for the Automatic-sync row. */
    val Clock: ImageVector by lazy {
        strokeIcon("M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M12 7v5l3 2")
    }

    private fun strokeIcon(pathData: String): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).run {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            build()
        }
}
