package dev.havlicektomas.notemark.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import dev.havlicektomas.notemark.core.data.NwPathConnectivityObserver
import dev.havlicektomas.notemark.core.domain.ConnectivityObserver
import dev.havlicektomas.notemark.db.NoteMarkDatabase
import dev.havlicektomas.notemark.notes.data.sync.IosSyncScheduler
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<SqlDriver> {
        NativeSqliteDriver(NoteMarkDatabase.Schema, "notemark.db")
    }
    single<ConnectivityObserver> { NwPathConnectivityObserver() }
    single<SyncScheduler> { IosSyncScheduler(syncEngine = get()) }
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
    single<Settings>(SecureSettings) {
        val defaults = NSUserDefaults.standardUserDefaults
        // Earlier builds kept the tokens in plain NSUserDefaults; wipe them
        // (users of those builds will need to log in once more).
        defaults.removeObjectForKey("access_token")
        defaults.removeObjectForKey("refresh_token")

        @OptIn(ExperimentalSettingsImplementation::class)
        val keychain = KeychainSettings(service = "dev.havlicektomas.notemark.session")
        // Keychain items survive app deletion, NSUserDefaults don't: on the first
        // launch after a (re)install, drop any session left by a previous install.
        if (!defaults.boolForKey(KEY_INSTALL_MARKER)) {
            keychain.clear()
            defaults.setBool(true, forKey = KEY_INSTALL_MARKER)
        }
        keychain
    }
}

private const val KEY_INSTALL_MARKER = "notemark_install_marker"
