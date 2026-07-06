package dev.havlicektomas.notemark.core.presentation.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "NoteMark" wordmark with the brand green accent dot. */
@Composable
fun NoteMarkWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 34.sp,
) {
    val dotSize = (fontSize.value * 0.26f).dp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "NoteMark",
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(3.dp))
        Spacer(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
