package dev.havlicektomas.notemark.di

import org.koin.core.module.Module

/** Per-platform bindings — notably the encrypted [com.russhwolf.settings.Settings]. */
expect val platformModule: Module
