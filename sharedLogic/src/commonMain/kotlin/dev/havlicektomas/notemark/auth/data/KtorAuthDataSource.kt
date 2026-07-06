package dev.havlicektomas.notemark.auth.data

import dev.havlicektomas.notemark.core.data.networking.postEmptyResult
import dev.havlicektomas.notemark.core.data.networking.postResult
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.Result
import io.ktor.client.HttpClient

/** Talks to the NoteMark auth endpoints. Single source → data source. */
class KtorAuthDataSource(
    private val httpClient: HttpClient,
) {
    suspend fun register(
        username: String,
        email: String,
        password: String,
    ): EmptyResult<DataError.Network> = httpClient.postEmptyResult(
        route = "/api/auth/register",
        body = RegisterRequest(
            username = username,
            email = email,
            password = password,
        ),
    )

    suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResponse, DataError.Network> = httpClient.postResult(
        route = "/api/auth/login",
        body = LoginRequest(email = email, password = password),
    )
}
