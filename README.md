# UserHub

A cross-platform user management client built with Kotlin Multiplatform and Compose Multiplatform.
One shared codebase drives both Android and iOS — including 100% of the UI.

## Features

- **User feed** — loads the most recent users from the last page of the GoRest directory, with name
  and email. GoRest has no creation date, so only users added in the app show when they were added:
  "5 minutes ago" and "3 hours ago" within the first day (updated live), then the date.
- **Shimmer loading states** — no spinners anywhere; the list skeleton animates while data loads.
- **Offline fallback** — every fetch, add and delete is written to a local SQLDelight database. When
  the network is unavailable, the feed shows the cached list with a "last updated" note.
- **Add user** — a FAB opens a polished bottom sheet with real-time name and email validation. On a
  successful `201` the new user appears at the top of the feed immediately. Server validation
  errors, such as an email that's already taken, appear on the matching field.
- **Delete with undo** — long-press a user, confirm, and the row animates out with a snackbar
  offering Undo. The delete is only sent to the server once the undo window closes; if it fails, the
  row comes back with an error message.
- **Adaptive layout** — on narrow screens, tapping a user opens their details full-screen; on wide
  screens (700dp and up) the list and details sit side by side. Driven by available width rather
  than device type.
- **Material 3 + dark mode** — the full palette adapts to the system theme.

## Architecture

MVVM with layered modules, split across three Gradle modules so the boundaries are enforced by the
build, not by convention:

```
┌───────────────┐
│  composeApp   │  Compose Multiplatform UI + ViewModels
└───────┬───────┘
        │ depends on
        ▼
┌───────────────┐
│    domain     │  Use cases, time formatting
└───────┬───────┘
        │ depends on
        ▼
┌───────────────┐
│     data      │  Ktor client, SQLDelight cache, repository implementation
└───────────────┘
```

ViewModels depend on use cases, and the UI depends only on ViewModels and immutable state objects.
`composeApp` also depends on `data` directly, for Koin wiring and the `UserDto` model. Dependencies
currently point **outward**, from `domain` to `data`, not inward as in Clean Architecture —
see [Known limitations](#known-limitations).

- **Networking** — Ktor 3.5 with content negotiation and kotlinx.serialization.
- **Persistence** — SQLDelight 2.3, typed queries generated at build time, native drivers per platform.
- **DI** — Koin, initialised once in `MainApplication` on Android and in `MainViewController` on iOS.
- **Concurrency** — structured coroutines throughout; state exposed as `StateFlow` and collected lifecycle-aware.

## Error handling

Only expected failures are caught: network errors (`IOException`), HTTP error responses
(`ResponseException`) and unreadable responses. Cancellation and programming errors are not
swallowed.

- **Loading the feed** — falls back to the cached list; with an empty cache, a "No Internet" screen
  offers a retry.
- **Adding a user** — offline, unauthorised (`401`), field validation (`422`) and other server
  errors each get their own message. Unexpected server errors show the status code, e.g. "Couldn't
  create user (500)".
- **Deleting a user** — on failure the row is restored and a snackbar explains why. A `404` counts
  as success, since the user is already gone.

## Testing

Unit tests across all three modules — 56 tests, run on both Android and the iOS simulator:

| Module       | Covers                                                                                                                                       |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `domain`     | relative time formatting (including hour and day boundaries), all three use cases                                                            |
| `data`       | fetching, caching with the injected clock, offline fallback, empty cache, cancellation, cache updates on add and delete                      |
| `composeApp` | feed ViewModel load, delete and undo (including the deferred delete), selection; add-user errors and one-time delivery; creation-time labels |

Hand-written fakes are used instead of a mocking framework, so the tests stay readable and
multiplatform. Run them with `./gradlew allTests`; `./gradlew test` runs the Android tests only.

## How I used AI tooling

AI assistance was central to hitting this scope in a day, and I treated it as a force multiplier rather than an autopilot:

- **Scaffolding** — I had the assistant generate the module skeletons, Gradle wiring and Compose boilerplate from a written architecture brief, so the structure was decided by me and typed by the tool.
- **Iteration** — feature by feature: I described the behaviour I wanted, reviewed the diff, and pushed back on anything that broke a layer boundary.
- **Tests** — I generated the test suite from the finished implementation and iterated until everything was green, which caught several integration mistakes early.
- **Review** — I finished with an assistant-driven pass over the whole codebase looking for inconsistencies before committing.

The takeaway: the architecture, the module boundaries and the UX decisions are mine; the typing was largely automated.

## Known limitations

### `domain` depends on `data`

`domain` declares `api(project(":data"))`, and `AddUserUseCase` / `DeleteUserUseCase` return Ktor's
`HttpResponse`. As a result:

- Ktor types reach the use cases and the ViewModels, so HTTP error mapping (e.g. GoRest's `422`
  field errors) lives in `AddUserViewModel` instead of the repository.
- `domain` and `composeApp` tests need `ktor-client-mock` to build fake responses, and the ViewModel
  tests wait on real time (`delay(100)`) while the mock engine completes.
- Swapping the backend would touch `domain` and `composeApp`, not only `data`.

Planned fix, in two steps:

1. **Remove Ktor from the repository API.** `createUser` / `deleteUser` return
   `Result<…, DataError>` with a sealed `DataError` (offline, unauthorized, invalid fields, not
   found, server). Error mapping moves into the repository, and the fakes return plain values, so
   tests run on the test scheduler.
2. **Invert the dependency.** Move `UserRepository`, `UsersResult`, `DataError` and a `User` model
   into `domain`; `data` depends on `domain` and maps `UserDto` to `User`. `domain` then has no
   Ktor, SQLDelight or `data` dependency.

This was left out of the current change set to keep the bug fixes reviewable on their own; it
touches about half the Kotlin files.

### Hard-coded API token

The GoRest token is in `ApiConfig.kt` and in the git history. It should be revoked and read from
`local.properties` (e.g. via BuildKonfig) instead.

### Gender is always "male"

GoRest requires a gender when creating a user, and the add-user sheet doesn't ask for one, so every
new user is sent as `"male"`.

### Other

- Code signing is off in the Xcode project (see Build and run).
- On iOS, the detail screen closes with the on-screen back arrow only; the edge-swipe gesture isn't
  supported.
- Deletes made while offline fail and are restored; they aren't queued for later.

## Build and run

Requires JDK 17, Android SDK 36, and Xcode 15+ for iOS.

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Unit tests (all modules, Android + iOS simulator)
./gradlew allTests
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme. A run-script phase
calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, which builds the shared framework
for the selected configuration and destination. Code signing is off, so the app runs on the
simulator; to install on a device, set your own development team locally.

## Tech stack

| | |
| --- | --- |
| Kotlin | 2.3.20 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 8.13.0 |
| Ktor | 3.5.0 |
| SQLDelight | 2.3.2 |
| Koin | 4.1.1 |
| Gradle | 8.14 |
| min / target SDK | 24 / 36 |
