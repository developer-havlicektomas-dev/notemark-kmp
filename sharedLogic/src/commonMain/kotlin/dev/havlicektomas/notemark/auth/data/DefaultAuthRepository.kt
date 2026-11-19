package dev.havlicektomas.notemark.auth.data

import dev.havlicektomas.notemark.auth.domain.AuthRepository
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager
import kotlin.coroutines.cancellation.CancellationException

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
    ): EmptyResult<DataError> {
        val response = when (val result = dataSource.login(email = email.trim(), password = password)) {
            is Result.Error -> return result
            is Result.Success -> result.data
        }
        try {
            sessionStorage.set(response.toAuthInfo())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The server accepted the credentials but the secure store didn't (on iOS,
            // e.g. an unsigned build without Keychain access). Don't leave half a session.
            runCatching { sessionStorage.clear() }
            return Result.Error(DataError.Local.SECURE_STORAGE)
        }
        // Post-login recovery: pending sync records left behind by a
        // logout-without-sync upload as soon as connectivity allows.
        syncManager.requestBackgroundSync()
        syncManager.applyStoredSchedule()
        return Result.Success(Unit)
    }
}
