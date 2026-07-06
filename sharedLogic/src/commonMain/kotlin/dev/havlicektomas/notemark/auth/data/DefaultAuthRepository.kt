package dev.havlicektomas.notemark.auth.data

import dev.havlicektomas.notemark.auth.domain.AuthRepository
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.asEmptyResult
import dev.havlicektomas.notemark.core.domain.util.onSuccess
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager

class DefaultAuthRepository(
    private val dataSource: KtorAuthDataSource,
    private val sessionStorage: SessionStorage,
    private val syncManager: SyncManager,
) : AuthRepository {

    override suspend fun register(
        username: String,
        email: String,
        password: String,
    ): EmptyResult<DataError.Network> = dataSource.register(
        username = username.trim(),
        email = email.trim(),
        password = password,
    )

    override suspend fun login(
        email: String,
        password: String,
    ): EmptyResult<DataError.Network> = dataSource
        .login(email = email.trim(), password = password)
        .onSuccess { response ->
            sessionStorage.set(response.toAuthInfo())
            // Post-login recovery: pending sync records left behind by a
            // logout-without-sync upload as soon as connectivity allows.
            syncManager.requestBackgroundSync()
            syncManager.applyStoredSchedule()
        }
        .asEmptyResult()
}
