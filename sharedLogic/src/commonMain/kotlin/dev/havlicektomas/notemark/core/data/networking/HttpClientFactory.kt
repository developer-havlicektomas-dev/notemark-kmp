package dev.havlicektomas.notemark.core.data.networking

import dev.havlicektomas.notemark.auth.data.RefreshRequest
import dev.havlicektomas.notemark.auth.data.RefreshResponse
import dev.havlicektomas.notemark.auth.domain.AuthTokens
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun create(
        engine: HttpClientEngine,
        sessionStorage: SessionStorage,
    ): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            // INFO logs method, URL and status only. ALL/BODY/HEADERS would write
            // passwords, bearer/refresh tokens and note contents to the system log.
            level = LogLevel.INFO
        }
        install(Auth) {
            bearer {
                loadTokens {
                    sessionStorage.get()?.tokens?.let {
                        BearerTokens(it.accessToken, it.refreshToken)
                    }
                }
                refreshTokens {
                    val current = sessionStorage.get() ?: return@refreshTokens null
                    val response = client.post(constructRoute("/api/auth/refresh")) {
                        markAsRefreshTokenRequest()
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(current.tokens.refreshToken))
                    }
                    if (response.status.isSuccess()) {
                        val refreshed = response.body<RefreshResponse>()
                        val newTokens = AuthTokens(
                            accessToken = refreshed.accessToken,
                            refreshToken = refreshed.refreshToken,
                        )
                        sessionStorage.set(current.copy(tokens = newTokens))
                        BearerTokens(newTokens.accessToken, newTokens.refreshToken)
                    } else {
                        sessionStorage.clear()
                        null
                    }
                }
            }
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
