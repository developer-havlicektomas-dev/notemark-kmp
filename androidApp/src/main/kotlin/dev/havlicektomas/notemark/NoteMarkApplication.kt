package dev.havlicektomas.notemark

import android.app.Application
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.di.authDataModule
import dev.havlicektomas.notemark.di.authPresentationModule
import dev.havlicektomas.notemark.di.coreDataModule
import dev.havlicektomas.notemark.di.notesDataModule
import dev.havlicektomas.notemark.di.notesPresentationModule
import dev.havlicektomas.notemark.di.platformModule
import dev.havlicektomas.notemark.di.settingsPresentationModule
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class NoteMarkApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidLogger()
            androidContext(this@NoteMarkApplication)
            modules(
                platformModule,
                coreDataModule,
                authDataModule,
                authPresentationModule,
                notesDataModule,
                notesPresentationModule,
                settingsPresentationModule,
            )
        }.koin

        // M4: when a session exists, catch up on pending changes and re-assert
        // the periodic sync schedule as soon as the app starts.
        applicationScope.launch {
            if (koin.get<SessionStorage>().get() != null) {
                val syncManager = koin.get<SyncManager>()
                syncManager.requestBackgroundSync()
                syncManager.applyStoredSchedule()
            }
        }
    }
}
