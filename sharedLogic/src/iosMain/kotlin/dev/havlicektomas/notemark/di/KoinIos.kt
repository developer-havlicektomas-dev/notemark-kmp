package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.auth.domain.AuthRepository
import dev.havlicektomas.notemark.auth.domain.PasswordValidationState
import dev.havlicektomas.notemark.auth.domain.SessionEvents
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import dev.havlicektomas.notemark.auth.domain.UserDataValidator
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

/**
 * Entry point the SwiftUI app calls once at launch to wire up the shared
 * domain + data layer. Mirrors the Android `startKoin` in NoteMarkApplication,
 * minus the Compose presentation module (iOS has its own SwiftUI screens).
 */
fun initKoinIos() {
    startKoin {
        modules(
            platformModule,
            coreDataModule,
            authDataModule,
            notesDataModule,
        )
    }
}

/**
 * Swift-friendly facade over the auth domain. Hides the generic `Result` type
 * from Swift by returning a nullable [DataError.Network] — `null` means success,
 * a non-null value is the error the caller maps to a message.
 */
class IosAuthService : KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val validator: UserDataValidator by inject()
    private val sessionStorage: SessionStorage by inject()
    private val sessionEvents: SessionEvents by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Fires [onExpired] (on the main thread) when the backend rejects the refresh
     * token and the session has been cleared; the app should return to auth.
     */
    fun watchSessionExpired(onExpired: () -> Unit): IosWatchHandle =
        IosWatchHandle(sessionEvents.expired.onEach { onExpired() }.launchIn(scope))

    @Throws(Exception::class)
    suspend fun login(email: String, password: String): DataError.Network? =
        when (val result = authRepository.login(email, password)) {
            is Result.Success -> null
            is Result.Error -> result.error
        }

    @Throws(Exception::class)
    suspend fun register(
        username: String,
        email: String,
        password: String,
    ): DataError.Network? =
        when (val result = authRepository.register(username, email, password)) {
            is Result.Success -> null
            is Result.Error -> result.error
        }

    @Throws(Exception::class)
    suspend fun hasSession(): Boolean = sessionStorage.get() != null

    /** The signed-in user's username, used by the notes screen for the avatar initials. */
    @Throws(Exception::class)
    suspend fun username(): String? = sessionStorage.get()?.username

    @Throws(Exception::class)
    suspend fun clearSession() = sessionStorage.clear()

    fun isValidEmail(email: String): Boolean = validator.isValidEmail(email)

    fun isValidUsername(username: String): Boolean = validator.isValidUsername(username)

    fun validatePassword(password: String): PasswordValidationState =
        validator.validatePassword(password)
}
