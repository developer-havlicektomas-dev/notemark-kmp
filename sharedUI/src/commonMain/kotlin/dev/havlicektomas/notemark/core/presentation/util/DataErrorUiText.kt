package dev.havlicektomas.notemark.core.presentation.util

import dev.havlicektomas.notemark.core.domain.util.DataError
import notemark.sharedui.generated.resources.Res
import notemark.sharedui.generated.resources.error_no_internet
import notemark.sharedui.generated.resources.error_request_timeout
import notemark.sharedui.generated.resources.error_serialization
import notemark.sharedui.generated.resources.error_server
import notemark.sharedui.generated.resources.error_too_many_requests
import notemark.sharedui.generated.resources.error_unknown

/** Generic network-error copy. Auth screens override 401/409 with screen-specific messages. */
fun DataError.Network.toUiText(): UiText = UiText.Resource(
    when (this) {
        DataError.Network.NO_INTERNET -> Res.string.error_no_internet
        DataError.Network.REQUEST_TIMEOUT -> Res.string.error_request_timeout
        DataError.Network.TOO_MANY_REQUESTS -> Res.string.error_too_many_requests
        DataError.Network.SERIALIZATION -> Res.string.error_serialization
        DataError.Network.SERVER_ERROR,
        DataError.Network.SERVICE_UNAVAILABLE,
        -> Res.string.error_server
        else -> Res.string.error_unknown
    },
)
