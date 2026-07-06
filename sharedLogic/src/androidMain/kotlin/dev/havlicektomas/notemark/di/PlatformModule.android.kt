package dev.havlicektomas.notemark.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.havlicektomas.notemark.core.data.AndroidConnectivityObserver
import dev.havlicektomas.notemark.core.domain.ConnectivityObserver
import dev.havlicektomas.notemark.db.NoteMarkDatabase
import dev.havlicektomas.notemark.notes.data.sync.WorkManagerSyncScheduler
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SqlDriver> {
        AndroidSqliteDriver(NoteMarkDatabase.Schema, androidContext(), "notemark.db")
    }
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    single<SyncScheduler> { WorkManagerSyncScheduler(androidContext()) }
    single<Settings> {
        val context = androidContext()
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "notemark_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        SharedPreferencesSettings(prefs)
    }
}
