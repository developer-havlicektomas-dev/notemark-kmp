package dev.havlicektomas.notemark.auth.domain

import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult

interface AuthRepository {

    /**
     * Registers a new account. A 409 from the backend (email or username
     * already taken) surfaces as [DataError.Network.CONFLICT].
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
    ): EmptyResult<DataError.Network>

    /**
     * Logs in and, on success, persists the session via [SessionStorage].
     * Invalid credentials surface as [DataError.Network.UNAUTHORIZED].
     */
    suspend fun login(
        email: String,
        password: String,
    ): EmptyResult<DataError.Network>
}
