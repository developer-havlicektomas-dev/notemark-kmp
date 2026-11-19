package dev.havlicektomas.notemark.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LandingRoute

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object NotesRoute

@Serializable
data class ReaderRoute(
    val noteId: String,
)

@Serializable
object SettingsRoute
