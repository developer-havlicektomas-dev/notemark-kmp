package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.auth.data.DefaultAuthRepository
import dev.havlicektomas.notemark.auth.data.EncryptedSessionStorage
import dev.havlicektomas.notemark.auth.data.KtorAuthDataSource
import dev.havlicektomas.notemark.auth.domain.AuthRepository
import dev.havlicektomas.notemark.auth.domain.SessionEvents
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.auth.domain.UserDataValidator
import dev.havlicektomas.notemark.core.data.networking.HttpClientFactory
import dev.havlicektomas.notemark.core.data.networking.httpClientEngine
import dev.havlicektomas.notemark.db.NoteMarkDatabase
import dev.havlicektomas.notemark.notes.data.KtorNotesDataSource
import dev.havlicektomas.notemark.notes.data.OfflineFirstNotesRepository
import dev.havlicektomas.notemark.notes.data.local.SqlDelightNotesDataSource
import dev.havlicektomas.notemark.notes.data.sync.DefaultSyncManager
import dev.havlicektomas.notemark.notes.data.sync.NotesSyncEngine
import dev.havlicektomas.notemark.notes.data.sync.SqlDelightSyncQueueDataSource
import dev.havlicektomas.notemark.notes.data.sync.SyncMetadataStore
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import dev.havlicektomas.notemark.notes.domain.commands.NoteCommands
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDataModule = module {
    single { httpClientEngine() }
    single { SessionEvents() }
    single { HttpClientFactory.create(engine = get(), sessionStorage = get(), sessionEvents = get()) }
    single<SessionStorage> {
        EncryptedSessionStorage(
            settings = get(SecureSettings),
            // Resolved lazily at call time (the HttpClient itself depends on
            // SessionStorage). Without this, Ktor keeps serving the previous
            // user's cached bearer tokens after logout / login as someone else.
            onIdentityChanged = {
                get<HttpClient>().authProvider<BearerAuthProvider>()?.clearToken()
            },
        )
    }
}

val authDataModule = module {
    singleOf(::KtorAuthDataSource)
    singleOf(::DefaultAuthRepository) bind AuthRepository::class
    singleOf(::UserDataValidator)
}

val notesDataModule = module {
    single { NoteMarkDatabase(driver = get()) }
    singleOf(::KtorNotesDataSource)
    singleOf(::SqlDelightNotesDataSource)
    singleOf(::SqlDelightSyncQueueDataSource)
    singleOf(::SyncMetadataStore)
    singleOf(::NotesSyncEngine)
    singleOf(::DefaultSyncManager) bind SyncManager::class
    singleOf(::OfflineFirstNotesRepository) bind NotesRepository::class
    single { NoteCommands(notesRepository = get(), sessionStorage = get()) }
}
