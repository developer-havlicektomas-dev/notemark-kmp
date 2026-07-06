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
 * Persists the session into a platform [Settings] instance that is encrypted
 * on Android (EncryptedSharedPreferences). All access is moved off the main
 * thread since the underlying store touches disk.
 */
class EncryptedSessionStorage(
    private val settings: Settings,
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
        } else {
            settings[KEY_ACCESS] = info.tokens.accessToken
            settings[KEY_REFRESH] = info.tokens.refreshToken
            settings[KEY_USERNAME] = info.username
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.Default) {
        clearKeys()
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
