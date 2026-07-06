package dev.havlicektomas.notemark.auth.data

import dev.havlicektomas.notemark.auth.domain.AuthInfo
import dev.havlicektomas.notemark.auth.domain.AuthTokens

fun LoginResponse.toAuthInfo(): AuthInfo = AuthInfo(
    username = username,
    tokens = AuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
    ),
)
