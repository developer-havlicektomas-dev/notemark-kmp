package dev.havlicektomas.notemark.di

import android.content.Context
import android.content.SharedPreferences
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
        val prefs = try {
            createEncryptedPrefs(context)
        } catch (e: Exception) {
            // The file can't be decrypted with this device's Keystore key, e.g. it
            // was restored from a backup or transfer taken before backup_rules.xml
            // excluded it. Start fresh (the user logs in again) instead of crashing
            // on every launch.
            context.deleteSharedPreferences(SECURE_PREFS_NAME)
            createEncryptedPrefs(context)
        }
        SharedPreferencesSettings(prefs)
    }
    single<Settings>(SecureSettings) { get<Settings>() }
}

private const val SECURE_PREFS_NAME = "notemark_secure_prefs"

private fun createEncryptedPrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
