package dev.havlicektomas.notemark.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.havlicektomas.notemark.auth.presentation.landing.LandingRoot
import dev.havlicektomas.notemark.auth.presentation.login.LoginRoot
import dev.havlicektomas.notemark.auth.presentation.register.RegisterRoot
import dev.havlicektomas.notemark.auth.presentation.splash.SplashRoot
import dev.havlicektomas.notemark.notes.presentation.detail.NoteDetailRoot
import dev.havlicektomas.notemark.notes.presentation.list.NotesListRoot
import dev.havlicektomas.notemark.notes.presentation.reader.ReaderRoot
import dev.havlicektomas.notemark.settings.presentation.SettingsRoot

@Composable
fun NavigationRoot(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            SplashRoot(
                onAuthenticated = {
                    navController.navigate(NotesRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onUnauthenticated = {
                    navController.navigate(LandingRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
            )
        }

        composable<LandingRoute> {
            LandingRoot(
                onGetStarted = {
                    navController.navigate(RegisterRoute) { launchSingleTop = true }
                },
                onLogIn = {
                    navController.navigate(LoginRoute) { launchSingleTop = true }
                },
            )
        }

        composable<LoginRoute> {
            LoginRoot(
                onLoggedIn = {
                    navController.navigate(NotesRoute) {
                        // Clear the whole auth flow after a successful login.
                        popUpTo(LandingRoute) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute) { launchSingleTop = true }
                },
                onNavigateBack = { navController.navigateUp() },
            )
        }

        composable<RegisterRoute> {
            RegisterRoot(
                onRegistered = {
                    // Backend issues no tokens on register → send the user to log in.
                    navController.navigate(LoginRoute) {
                        popUpTo(RegisterRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.navigateUp() },
            )
        }

        composable<NotesRoute> {
            NotesListRoot(
                onOpenNote = { id ->
                    navController.navigate(NoteDetailRoute(noteId = id, isNew = false))
                },
                onCreateNote = { id ->
                    navController.navigate(NoteDetailRoute(noteId = id, isNew = true))
                },
                onOpenSettings = {
                    navController.navigate(SettingsRoute) { launchSingleTop = true }
                },
            )
        }

        composable<NoteDetailRoute> {
            NoteDetailRoot(
                onBack = { navController.navigateUp() },
                onOpenReader = { id -> navController.navigate(ReaderRoute(noteId = id)) },
            )
        }

        composable<ReaderRoute> {
            ReaderRoot(
                onBack = { navController.navigateUp() },
            )
        }

        composable<SettingsRoute> {
            SettingsRoot(
                onBack = { navController.navigateUp() },
                onLoggedOut = {
                    // Clear the whole stack and return to the auth flow.
                    navController.navigate(LandingRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
