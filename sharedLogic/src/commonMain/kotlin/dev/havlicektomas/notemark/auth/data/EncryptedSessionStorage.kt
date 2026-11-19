package dev.havlicektomas.notemark.auth.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dev.havlicektomas.notemark.auth.domain.AuthInfo
import dev.havlicektomas.notemark.auth.domain.AuthTokens
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists the session into a secure platform [Settings] instance
 * (EncryptedSharedPreferences on Android, Keychain on iOS). All access is moved
 * off the main thread since the underlying store touches disk.
 *
 * [onIdentityChanged] runs when the session is cleared or a different user is
 * stored (not on a token refresh for the same user), so in-memory token caches
 * such as Ktor's bearer provider can be dropped.
 */
class EncryptedSessionStorage(
    private val settings: Settings,
    private val onIdentityChanged: () -> Unit = {},
) : SessionStorage {

    override suspend fun get(): AuthInfo? = withContext(Dispatchers.Default) {
        val access = settings.getStringOrNull(KEY_ACCESS)
        val refresh = settings.getStringOrNull(KEY_REFRESH)
        val username = settings.getStringOrNull(KEY_USERNAME)
        if (access != null && refresh != null && username != null) {
            AuthInfo(
                username = username,
                tokens = AuthTokens(accessToken = access, refreshToken = refresh),
            )
        } else {
            null
        }
    }

    override suspend fun set(info: AuthInfo?): Unit = withContext(Dispatchers.Default) {
        if (info == null) {
            clearKeys()
            onIdentityChanged()
        } else {
            val previousUser = settings.getStringOrNull(KEY_USERNAME)
            settings[KEY_ACCESS] = info.tokens.accessToken
            settings[KEY_REFRESH] = info.tokens.refreshToken
            settings[KEY_USERNAME] = info.username
            if (previousUser != info.username) onIdentityChanged()
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.Default) {
        clearKeys()
        onIdentityChanged()
    }

    private fun clearKeys() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
        settings.remove(KEY_USERNAME)
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USERNAME = "username"
    }
}
