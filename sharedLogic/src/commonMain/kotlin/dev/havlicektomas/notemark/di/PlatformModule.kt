package dev.havlicektomas.notemark.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named

/**
 * Per-platform bindings. Two [com.russhwolf.settings.Settings] are provided: the
 * default one (sync metadata, preferences) and one qualified with
 * [SecureSettings] for the session tokens (EncryptedSharedPreferences on
 * Android, Keychain on iOS).
 */
expect val platformModule: Module

val SecureSettings = named("secureSettings")
