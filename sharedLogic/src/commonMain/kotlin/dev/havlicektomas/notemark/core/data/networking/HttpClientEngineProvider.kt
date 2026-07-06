package dev.havlicektomas.notemark.core.data.networking

import io.ktor.client.engine.HttpClientEngine

/** Platform default Ktor engine: OkHttp on Android, Darwin on iOS. */
expect fun httpClientEngine(): HttpClientEngine
