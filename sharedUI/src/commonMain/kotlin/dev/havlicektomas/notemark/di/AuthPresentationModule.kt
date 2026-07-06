package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.auth.presentation.login.LoginViewModel
import dev.havlicektomas.notemark.auth.presentation.register.RegisterViewModel
import dev.havlicektomas.notemark.auth.presentation.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
