package dev.havlicektomas.notemark.core.data.networking

/**
 * Host that points at the NoteMark Spring Boot backend on `localhost:8080`.
 * The reachable host differs per platform: the Android emulator sees the host
 * machine as `10.0.2.2`, while the iOS simulator shares the host's `localhost`.
 */
expect val platformBaseUrl: String

object ApiConfig {
    val BASE_URL: String get() = platformBaseUrl
}

fun constructRoute(route: String): String = when {
    route.contains(ApiConfig.BASE_URL) -> route
    route.startsWith("/") -> ApiConfig.BASE_URL + route
    else -> "${ApiConfig.BASE_URL}/$route"
}
