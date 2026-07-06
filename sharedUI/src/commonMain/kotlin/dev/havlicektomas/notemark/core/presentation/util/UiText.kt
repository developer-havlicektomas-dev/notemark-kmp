package dev.havlicektomas.notemark.core.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A string that is either already-resolved or backed by a Compose string resource. */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(
        val id: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args.toTypedArray())
    }
}
