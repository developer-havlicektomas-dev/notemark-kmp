package dev.havlicektomas.notemark.core.data.networking

/** The iOS simulator shares the host machine's loopback, so `localhost` works. */
actual val platformBaseUrl: String = "http://localhost:8080"
