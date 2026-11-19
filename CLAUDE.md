# CLAUDE.md — notemark-kmp

Offline-first note-taking app. Kotlin Multiplatform for Android + iOS.
Companion backend: `../notemark-api` (Spring Boot, in-memory, `http://localhost:8080`).

## Modules

| Module | What lives there |
|---|---|
| `:sharedLogic` | Domain + data: Ktor, Koin, SQLDelight, multiplatform-settings. **Compose-free.** Has `androidMain` / `iosMain` actuals. Builds the static `SharedLogic` framework for iOS. |
| `:sharedUI` | Compose Multiplatform screens, MVI ViewModels, design system, type-safe navigation. **Used only by Android.** |
| `:androidApp` | `NoteMarkApplication` (`startKoin`, kicks off sync) + `MainActivity`. |
| `iosApp/` | **Native SwiftUI** app. Does NOT use `:sharedUI`. Talks to Kotlin through facades in `sharedLogic/src/iosMain/.../di/` (`KoinIos.kt` → `initKoinIos()`, `IosAuthService`; `IosNotesService`; `IosSyncService`). |

Feature packages follow `<feature>/{domain,data,presentation}` under `dev.havlicektomas.notemark`
(features: `auth`, `notes`, `settings`, plus `core`).

**A feature change usually needs both UIs:** Compose in `sharedUI` *and* SwiftUI in `iosApp/iosApp/`
(+ a facade method in `iosMain` if Swift needs new shared functionality).

## Build & run

Requires JDK 17+, Android SDK (compileSdk 37, targetSdk 37, min 29), Xcode. Start the backend first:
`cd ../notemark-api && ./gradlew bootRun`.

```bash
./gradlew :androidApp:assembleDebug
./gradlew :sharedLogic:allTests          # no Kotlin tests exist yet
open iosApp/iosApp.xcodeproj              # only shared scheme: NoteMarkTests (builds app + UI tests)
```

- Xcode's "Compile Kotlin Framework" build phase runs `./gradlew :sharedLogic:embedAndSignAppleFrameworkForXcode`.
- iOS deployment target is **27.0** (Liquid Glass and `safeAreaBar` used without availability checks).
- iOS UI: use system toolbars (`.toolbar`/`ToolbarItem`), not hand-drawn bars: they get Liquid Glass, and on
  iPhone Duo the system moves them into a vertical bar on the leading/trailing edge (a persistent horizontal
  safe-area inset). Never pad by hand to clear bars; use `safeAreaBar` / `safeAreaInset`, and keep
  `ignoresSafeArea()` to background fills. Buttons use `.glass` / `.glassProminent` (`AppButtons.swift`).
- iOS notes are a `NavigationSplitView` (`NotesFlowView`): compact (iPhone, folded Duo) collapses to one stack
  driven by `preferredCompactColumn`; regular (unfolded Duo, iPad) shows list + detail. Detail = note or
  Settings; Reader is pushed in the detail column. Check layout changes on the iPhone Duo simulator closed,
  half-open and open.
- UI tests need a fresh user per run: `TEST_RUNNER_NM_EMAIL` / `_PASSWORD` / `_USERNAME`. The "Save Password?"
  prompt can appear late and eat a tap (flaky). `testMilestone4Journey` fails on iOS 27 at "interval picker
  should open": `confirmationDialog` is a popover there, not `app.sheets[...]` (predates the iOS 27 work).
- Never build for the simulator with `CODE_SIGNING_ALLOWED=NO`: the session lives in the Keychain,
  an unsigned app can't access it, and login then fails with a misleading "Couldn't reach the server".
- Base URL is `expect/actual platformBaseUrl` in `core/data/networking/ApiConfig*.kt`:
  Android emulator `http://10.0.2.2:8080`, iOS simulator `http://localhost:8080`.
  Android allows cleartext via `usesCleartextTraffic="true"`.
- Versions live in `gradle/libs.versions.toml` (Gradle 9.9.1, Kotlin 2.4.21, CMP 1.12.2, AGP 9.5.1,
  Ktor 3.6.1, Koin 4.2.2, SQLDelight 2.4.1, kotlinx-datetime 0.8.0). JVM target 11.
  JetBrains lifecycle/navigation/material3 versions must move in step with CMP.
- Use `kotlin.time.Instant` / `kotlin.time.Clock` (kotlinx-datetime 0.8 removed its own);
  kotlinx-datetime is still used for `TimeZone` / `toLocalDateTime`.
- `androidx.security:security-crypto` is deprecated (end of life); its warnings are expected until it's replaced. Configuration cache + build cache are on.

## Conventions

**Errors** — `core/domain/util`: `Result<D, E : Error>` (`Success` / `Error`), `EmptyResult<E>`,
helpers `map`, `onSuccess`, `onFailure`, `asEmptyResult`. Errors are `DataError.Network` / `DataError.Local`
enums. Don't throw across layers; return `Result`. Map to UI strings via `DataErrorUiText.kt` / `UiText`.

**Presentation (Compose, MVI)** — per screen: `XState` data class, `XAction` sealed interface,
`XEvent` sealed interface, `XViewModel` exposing `state: StateFlow` and `events` (from a `Channel`),
single `onAction(action)` entry point. Collect events with `ObserveAsEvents`. Each screen file has `XRoot` (gets ViewModel via `koinViewModel()`, collects events) and a
stateless `XScreen(state, onAction)` plus `@Preview`s.
Register ViewModels with `viewModelOf(::X)` in `sharedUI/.../di/*PresentationModule.kt`.

**Navigation** — `@Serializable` route objects/classes in `navigation/Routes.kt`, graph in `NavigationRoot.kt`.

**DI** — Koin. Data modules in `sharedLogic/.../di/SharedLogicModules.kt` (`coreDataModule`,
`authDataModule`, `notesDataModule`) + `platformModule` (expect/actual). New modules must be added to
**both** `NoteMarkApplication.startKoin` and `initKoinIos()` (iOS omits presentation modules).

**iOS facades** — hide Kotlin generics/`Flow`/`Instant` from Swift: return nullable `DataError.Network`
(`null` = success), epoch-millis timestamps (`IosNote`), snapshot calls instead of flows.
Swift ViewModels are `@MainActor ObservableObject`s calling these facades with `async/await`.

## Offline-first sync (read before touching notes data)

- SQLDelight DB (`sharedLogic/src/commonMain/sqldelight/.../db/`) is the source of truth:
  `noteEntity` (scoped by `username`) and `syncQueueEntity` (operation metadata only — payload is read
  from `noteEntity` at sync time).
- `OfflineFirstNotesRepository` writes locally + enqueues; `NotesSyncEngine.sync()` (mutex-guarded)
  uploads the queue, downloads all notes, merges with **last-writer-wins on `lastEditedAt`**. A failed
  step aborts; remaining queue items retry later.
- Client generates note UUIDs and timestamps; the server trusts them.
- Scheduling via `SyncScheduler`: Android `WorkManagerSyncScheduler` + `SyncWorker`; iOS `IosSyncScheduler`
  is an in-process foreground timer (no BGTaskScheduler) that must swallow exceptions — an uncaught
  coroutine exception terminates the iOS process.
- Logout without sync keeps rows still referenced by the queue (`deleteSyncedForUser`).

## Gotchas

- Session (JWT access + refresh tokens) stored via `EncryptedSessionStorage` in the `Settings` qualified
  `SecureSettings` (EncryptedSharedPreferences on Android, Keychain on iOS); the unqualified `Settings`
  is for non-secret data. Ktor bearer auth handles refresh in `HttpClientFactory`; Ktor caches tokens in
  memory, so the session storage calls `clearToken()` whenever the user changes or logs out.
- Keep Ktor `Logging` at `INFO` or lower — higher levels log passwords, tokens and note bodies.
- Sync queue: one record per note; re-enqueuing bumps `enqueuedAt`, and the engine removes a record
  only if `enqueuedAt` is unchanged (`removeIfUnchanged`), so edits made during an upload aren't lost.
  Deletes always enqueue a DELETE (never "cancel out" a CREATE).
- App icon = the wordmark in miniature ("N" + green dot on `#09090B`). Android: vector adaptive-icon
  layers in `androidApp/src/main/res/drawable/ic_launcher_*.xml` (incl. monochrome). iOS: PNGs generated by
  `swift iosApp/scripts/render_app_icon.swift iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`.
  Change the geometry in both places together.
- Android build from the CLI needs `ANDROID_HOME=$HOME/Library/Android/sdk` (no `local.properties`).
- targetSdk 37 blocks local-network addresses (incl. emulator `10.0.2.2`) without the runtime permission
  `ACCESS_LOCAL_NETWORK`, declared only in `androidApp/src/debug/AndroidManifest.xml`; `MainActivity` asks for
  it on launch when declared. Denied → every request fails as "No internet connection". To skip the dialog
  (e.g. automated testing): `adb shell pm grant dev.havlicektomas.notemark android.permission.ACCESS_LOCAL_NETWORK`.
- Notes list + detail are one destination (`NotesRoute` → `notes/presentation/NotesListDetailRoot.kt`,
  material3-adaptive `ListDetailPaneScaffold`); there is no detail route. The detail ViewModel gets
  `noteId`/`isNew` via `SavedStateHandle` default args built there. Reader and Settings stay full-screen routes.
- Backend is in-memory — restarting it wipes users; the app may hold a session for a user that no
  longer exists.
- Sending header `Debug: true` makes the backend issue 30 s access tokens (useful for testing refresh).
