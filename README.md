This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

## Validation

Run lint checks, type checks, and tests across all modules:

- on macOS/Linux
  ```shell
  ./gradlew check
  ```
- on Windows
  ```shell
  .\gradlew.bat check
  ```

To validate a single module only:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:check
  ./gradlew :shared:check
  ./gradlew :server:check
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:check
  .\gradlew.bat :shared:check
  .\gradlew.bat :server:check
  ```

## Testing

Run all tests across every module:

- on macOS/Linux
  ```shell
  ./gradlew allTests
  ```
- on Windows
  ```shell
  .\gradlew.bat allTests
  ```

Run tests for a specific module or platform:

- on macOS/Linux
  ```shell
  # All composeApp tests (JVM, JS, Wasm, Android unit tests)
  ./gradlew :composeApp:allTests

  # Shared module tests
  ./gradlew :shared:allTests

  # Server tests
  ./gradlew :server:test

  # Single-platform tests
  ./gradlew :composeApp:jvmTest
  ./gradlew :composeApp:jsTest
  ./gradlew :composeApp:wasmJsTest
  ./gradlew :composeApp:testDebugUnitTest
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:allTests
  .\gradlew.bat :shared:allTests
  .\gradlew.bat :server:test
  .\gradlew.bat :composeApp:jvmTest
  .\gradlew.bat :composeApp:jsTest
  .\gradlew.bat :composeApp:wasmJsTest
  .\gradlew.bat :composeApp:testDebugUnitTest
  ```

## Running (Debug / Development)

### Android

  ```shell
  ./gradlew :composeApp:assembleDebug
  ```

Install the APK from `composeApp/build/outputs/apk/debug/` onto a connected device or emulator.

To build and install in one step (device/emulator required):

  ```shell
  ./gradlew :composeApp:installDebug
  ```

### Desktop (JVM)

  ```shell
  ./gradlew :composeApp:run
  ```

### Server

- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```

The server starts in development mode with hot reload. Set the `development` project property to `false` to disable:

- on macOS/Linux
  ```shell
  ./gradlew :server:run -Pdevelopment=false
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run "-Pdevelopment=false"
  ```

### Web (Wasm) — modern browsers

  ```shell
  ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
  ```

### Web (JS) — legacy browser support

  ```shell
  ./gradlew :composeApp:jsBrowserDevelopmentRun
  ```

### iOS

Open the [/iosApp](./iosApp) directory in Xcode, select a simulator or device, and run from there.

## Building (Release / Production)

### Android

  ```shell
  ./gradlew :composeApp:assembleRelease
  ```

The signed release APK is written to `composeApp/build/outputs/apk/release/`.

### Desktop (JVM) — native installers

```shell
./gradlew :composeApp:createDistributable
```

Generates native installers (DMG on macOS, MSI on Windows, DEB on Linux) under `composeApp/build/compose/binaries/`.

### Web

  ```shell
  # Wasm (production-optimized)
  ./gradlew :composeApp:wasmJsBrowserProductionRun

  # JS (production-optimized)
  ./gradlew :composeApp:jsBrowserProductionRun
  ```


To produce distributable web assets without launching a browser:

  ```shell
  ./gradlew :composeApp:wasmJsBrowserDistribution
  ./gradlew :composeApp:jsBrowserDistribution
  ```

Output lands in `composeApp/build/dist/wasmJs/productionExecutable/` and `composeApp/build/dist/js/productionExecutable/`.

### Server — distributable JAR

  ```shell
  ./gradlew :server:build
  ```

The fat JAR is written to `server/build/libs/`.

### iOS

Archive via Xcode: open [/iosApp](./iosApp) in Xcode, select **Product → Archive**.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).