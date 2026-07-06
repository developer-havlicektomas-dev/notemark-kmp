package dev.havlicektomas.notemark.auth.domain

/** Tokens issued by the backend on a successful login. */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/** The authenticated user plus their session tokens. */
data class AuthInfo(
    val username: String,
    val tokens: AuthTokens,
)
