package dev.havlicektomas.notemark.core.presentation.designsystem.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Max width for forms and settings so they don't stretch across tablets and unfolded foldables. */
val FormMaxWidth: Dp = 560.dp

/** Max width for long-form reading/editing (~70–80 characters per line). */
val ReadingMaxWidth: Dp = 720.dp

/**
 * Fills the available width up to [maxWidth] and centers horizontally once the
 * window is wider than that. On phones this behaves like `fillMaxWidth()`.
 */
fun Modifier.centeredMaxWidth(maxWidth: Dp = FormMaxWidth): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
        .fillMaxWidth()
