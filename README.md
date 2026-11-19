# NoteMark

NoteMark is an offline-first note-taking app built with **Kotlin Multiplatform (KMP)**,
targeting **Android** and **iOS**. Business and data logic are shared across platforms in
Kotlin, while the UI is Compose Multiplatform on Android and native SwiftUI on iOS.

## Features

- **Auth & onboarding** — splash, landing, login and register, backed by an encrypted
  local session (JWT access + refresh tokens).
- **Notes CRUD** — adaptive grid list, editable note detail with borderless title/body,
  create/edit/delete.
- **Reading experience** — a dedicated View mode (with created/edited metadata) and a
  full-screen Reader mode with auto-hiding controls.
- **Settings** — profile, manual "Sync now", configurable automatic-sync interval, and a
  logout flow that accounts for pending unsynced changes.
- **Offline-first sync** — a local SQLite database is the source of truth. Edits are saved
  locally and queued; a sync engine reconciles with the backend using last-writer-wins.
  Autosave persists changes as you type. Android syncs via WorkManager; iOS uses a
  foreground timer.

## Architecture

The project is organized as three package-layered Gradle modules:

| Module         | Responsibility                                                                                     |
|----------------|----------------------------------------------------------------------------------------------------|
| `:sharedLogic` | Domain + data layer (Ktor networking, Koin DI, SQLDelight, multiplatform-settings). Compose-free; has Android + iOS actuals. |
| `:sharedUI`    | Compose Multiplatform screens, MVI ViewModels, design system, and type-safe navigation.            |
| `:androidApp`  | Android entry point — `NoteMarkApplication` (`startKoin`) and `MainActivity`, consuming `:sharedUI`. |
| `iosApp/`      | iOS entry point — a **native SwiftUI** app that reuses `:sharedLogic` through Kotlin/Native bridges. |

> **Note on the iOS UI:** although `:sharedUI` is Compose Multiplatform, the iOS app uses
> its own native SwiftUI screens and talks to shared Kotlin code via `KoinComponent`-based
> bridge facades exported in the `SharedLogic` framework. Only the Android app renders the
> Compose UI.

The app follows an MVI presentation pattern (State / Action / Event / ViewModel), a
repository-based data layer, and a generic typed `Result` wrapper for error handling.

## Requirements

- **JDK 17+** (this project builds with a modern JDK; Gradle uses a JVM 11 target).
- **Android SDK** — compile SDK 37, target SDK 36, min SDK 29.
- **Xcode** (for iOS) with an iOS 18.2+ simulator or device (the project's deployment target).
- The **NoteMark backend** running locally (see below) for anything auth- or sync-related.

## Running the backend locally

The mobile app talks to a companion Spring Boot backend, **notemark-api**, on
`http://localhost:8080`. It stores everything (users, tokens, notes) **in memory** — data
is lost on restart, and no database setup is required.

```bash
cd ../notemark-api   # sibling of this repo
./gradlew bootRun
```

The server listens on `0.0.0.0:8080`. Key endpoints:

| Method | Path                     | Auth   | Notes                                             |
|--------|--------------------------|--------|---------------------------------------------------|
| POST   | `/api/auth/register`     | public | 201 / 400 (validation) / 409 (duplicate)          |
| POST   | `/api/auth/login`        | public | returns `accessToken`, `refreshToken`, `username` |
| POST   | `/api/auth/refresh`      | public | rotates and returns new tokens                    |
| POST   | `/api/notes`             | Bearer | create (client sends `id` + timestamps)           |
| PUT    | `/api/notes`             | Bearer | update (upsert by `id`)                           |
| GET    | `/api/notes?page=0`      | Bearer | `page=-1` returns all; response `{ notes, total }`|
| DELETE | `/api/notes/{id}`        | Bearer | delete                                            |

The server uses plain HTTP (no TLS). The clients are already configured to reach it: the
base URL is defined in `sharedLogic/.../core/data/networking/ApiConfig.kt` via an
`expect/actual platformBaseUrl`:

- **Android emulator** → `http://10.0.2.2:8080` (host loopback as seen from the emulator)
- **iOS simulator** → `http://localhost:8080` (shares the host's localhost)

Start the backend before launching either app so login/register and sync work.

## Running on Android

```bash
# Point ANDROID_HOME at your SDK if it isn't already exported
export ANDROID_HOME=$HOME/Library/Android/sdk

# Build the debug APK
./gradlew :androidApp:assembleDebug
```

Then install and launch on a running emulator or device:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n dev.havlicektomas.notemark/.MainActivity
```

Alternatively, open the project in **Android Studio** and run the `androidApp`
configuration from the run widget.

## Running on iOS

The iOS app is a standard Xcode project that links the `SharedLogic` Kotlin framework.

1. Make sure the backend is running (see above).
2. Open the Xcode project:

   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. Select the **NoteMarkTests** scheme (the only shared one; it builds the app) and an iOS simulator, then Run (⌘R). Gradle builds the
   `SharedLogic` framework automatically as part of the build.

To build from the command line for a simulator:

```bash
cd iosApp
xcodebuild -scheme NoteMarkTests -sdk iphonesimulator \
  -destination "id=<simulator-udid>" \
  -configuration Debug -derivedDataPath build build-for-testing
```

Don't add `CODE_SIGNING_ALLOWED=NO`: the session tokens are stored in the Keychain, which an
unsigned app can't use, so login fails with "Couldn't reach the server". The default ad-hoc
simulator signing is enough.

## Libraries used

Versions are managed centrally in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

**Language & UI**
- **Kotlin Multiplatform** — shared Android/iOS code.
- **Compose Multiplatform** + **Material 3** — declarative UI (Android app + `:sharedUI`).
- **SwiftUI** — native iOS UI in `iosApp/`.
- **Jetpack Lifecycle (ViewModel / runtime Compose)** — MVI ViewModels.

**Networking & serialization**
- **Ktor Client** — multiplatform HTTP client (OkHttp engine on Android, Darwin on iOS),
  with content negotiation, logging, and bearer auth plugins.
- **kotlinx.serialization** — JSON serialization.
- **kotlinx.datetime** — multiplatform `Instant` for note timestamps.
- **kotlinx.coroutines** — async and reactive flows.

**Persistence & offline-first**
- **SQLDelight** — typed local SQLite database (Android + native drivers); the offline
  source of truth and sync queue.
- **Multiplatform Settings** — key/value storage for session and sync metadata
  (**AndroidX Security Crypto** / EncryptedSharedPreferences on Android).
- **AndroidX WorkManager** — background sync scheduling on Android.

**Dependency injection & navigation**
- **Koin** — DI across `core`, `android`, `compose`, and `compose-viewmodel`.
- **Navigation Compose** — type-safe, `@Serializable` route navigation (Android).

**Testing**
- **kotlin-test** / **kotlinx-coroutines-test** — configured for shared unit tests (no Kotlin
  tests exist yet).
- **XCUITest** — interactive iOS UI tests in `iosApp/iosAppUITests/` (scheme `NoteMarkTests`).

## Project layout

```
NoteMark/
├── androidApp/     # Android application (Compose UI)
├── sharedLogic/    # Shared domain + data (Ktor, Koin, SQLDelight) — Android + iOS actuals
├── sharedUI/       # Compose Multiplatform screens, MVI, design system, navigation
├── iosApp/         # Native SwiftUI app + XCUITests, links the SharedLogic framework
└── gradle/         # Version catalog (libs.versions.toml)
```