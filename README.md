# Hazlo Sano — KMP

Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), and a Ktor JVM server.

## Project structure

```
Root project 'kmp'
+--- :app
|    +--- :app:androidApp     Android entry point
|    +--- :app:desktopApp     Desktop JVM entry point
|    +--- :app:shared         Compose UI, data implementations, platform adapters
|    \--- :app:webApp         Web (JS) entry point
+--- :core                    Domain models, repository interfaces, use cases
\--- :server                  Ktor server application
```

- **[app/](app)** — Thin platform entry-point modules (Android, Desktop, Web, iOS) + shared client code.
- **[app/shared/](app/shared/src/commonMain/kotlin/com/hazlosano/kmp)** — Compose Multiplatform UI, presentation state, data layer implementations, platform adapters. Client-only. Depends on `:core`.
- **[core/](core/src/commonMain/kotlin/com/hazlosano/kmp/domain)** — Target-neutral domain logic: models, repository interfaces, use cases. Shared between client and server. No Compose, no SQLDelight, no platform APIs.
- **[server/](server/src/main/kotlin/com/hazlosano/kmp)** — Ktor JVM server.

## Clean Architecture alignment

```
app/shared/              ← Presentation + Data (Compose UI, ViewModels, repository impls, SQLDelight)
  ↑ depends on
core/                    ← Domain (entities, use cases, repository interfaces)
  ↑ depends on
(nothing)                ← Pure Kotlin + coroutines + datetime
```

```
server/                  ← Ktor HTTP adapter
  ↑ depends on
core/                    ← Domain (shared with client)
  +
app/shared/              ← For Greeting/Platform utilities
```

## Dependency rules

- `:core` has no project dependencies. Pure domain logic.
- `:app:shared` depends on `:core`.
- `:server` depends on `:core` and `:app:shared`.
- All app entry-point modules (`:app:androidApp`, `:app:desktopApp`, `:app:webApp`) depend on `:app:shared`.

## Validation

Run lint checks, type checks, and tests across all modules:

```shell
./gradlew check          # macOS/Linux
.\gradlew.bat check      # Windows
```

To validate a single module:

```shell
./gradlew :core:check
./gradlew :app:shared:check
./gradlew :server:check
./gradlew :app:androidApp:check
./gradlew :app:desktopApp:check
./gradlew :app:webApp:check
```

## Testing

Run all tests across every module:

```shell
./gradlew allTests
```

Run tests for a specific module or platform:

```shell
# Shared module (JVM, JS, Android unit tests)
./gradlew :app:shared:allTests

# Core module tests
./gradlew :core:allTests

# Server tests
./gradlew :server:test

# Single-platform tests
./gradlew :app:shared:jvmTest
./gradlew :app:shared:jsTest
./gradlew :app:androidApp:testDebugUnitTest
```

## Running (development)

### Android

```shell
./gradlew :app:androidApp:assembleDebug
```

Install the APK from `app/androidApp/build/outputs/apk/debug/` onto a connected device or emulator.

To build and install in one step (device/emulator required):

```shell
./gradlew :app:androidApp:installDebug
```

### Desktop (JVM)

```shell
./gradlew :app:desktopApp:run
```

### Server

```shell
./gradlew :server:run
```

The server starts in development mode with hot reload. Disable development mode:

```shell
.\gradlew.bat :server:run "-Pdevelopment=false"
```

### Web (JS) — browser

```shell
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

### iOS

Open the [`app/iosApp`](app/iosApp) directory in Xcode, select a simulator or device, and run from there.

## Building (production)

### Android

```shell
./gradlew :app:androidApp:assembleRelease
```

The signed release APK is written to `app/androidApp/build/outputs/apk/release/`.

### Desktop (JVM) — native installers

```shell
./gradlew :app:desktopApp:createDistributable
```

Generates native installers (DMG on macOS, MSI on Windows, DEB on Linux) under `app/desktopApp/build/compose/binaries/`.

### Web

```shell
# Development run
./gradlew :app:webApp:jsBrowserDevelopmentRun

# Production-optimized run
./gradlew :app:webApp:jsBrowserProductionRun
```

To produce distributable web assets without launching a browser:

```shell
./gradlew :app:webApp:jsBrowserDistribution
```

Output lands in `app/webApp/build/dist/js/productionExecutable/`.

### Server — distributable JAR

```shell
./gradlew :server:build
```

The fat JAR is written to `server/build/libs/`.

### iOS

Archive via Xcode: open [`app/iosApp`](app/iosApp) in Xcode, select **Product → Archive**.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/).

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
