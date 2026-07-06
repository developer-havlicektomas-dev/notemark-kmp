package dev.havlicektomas.notemark.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
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
}
