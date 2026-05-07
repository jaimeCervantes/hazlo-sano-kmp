---
name: kmp-project-scaffold
description: Initialize or repair a Kotlin Multiplatform repository that uses Gradle Kotlin DSL, version catalogs, Compose Multiplatform, shared common code, Android/iOS/Web/Desktop targets, and a Ktor server. Use this skill when the task is project setup, missing boilerplate, module restructuring, dependency wiring, Gradle wrapper setup, platform target setup, build repair, or test-environment setup. Do not use it for implementing a specific business feature unless the blocker is missing project scaffolding.
---

# KMP project scaffold

Use this skill for setup and repair work, not feature delivery.

## Workflow

1. Decide whether the task is **initialize from scratch** or **repair existing scaffolding**.
2. Read `AGENTS.md` and preserve repository-specific rules.
3. Inspect the current Gradle and module layout before editing:
   - `settings.gradle.kts`
   - `build.gradle.kts`
   - `gradle/libs.versions.toml`
   - `composeApp/build.gradle.kts`
   - `shared/build.gradle.kts`
   - `server/build.gradle.kts`
4. Validate the Gradle wrapper exists before using project commands:
   - Windows: `.\gradlew.bat --version`
   - macOS/Linux: `./gradlew --version`
5. If the wrapper is missing or broken, repair the wrapper before changing project behavior.
6. Create or repair only the structure needed for the requested setup.
7. Keep dependency versions in `gradle/libs.versions.toml`.
8. Wire dependencies through module `build.gradle.kts` files using catalog aliases and source-set-specific dependency blocks.
9. Keep changes minimal; do not rewrite a working generated KMP project just to match a preferred template.
10. Run the narrowest relevant Gradle validation commands and report any skipped validation.

## Expected repository shape

Use this layout unless the existing repository already has a clearly established equivalent:

```text
settings.gradle.kts
build.gradle.kts
gradle/
  libs.versions.toml
composeApp/
  build.gradle.kts
  src/commonMain/
  src/commonTest/
  src/androidMain/
  src/iosMain/
  src/jvmMain/
  src/jsMain/
  src/wasmJsMain/
server/
  build.gradle.kts
  src/main/kotlin/
  src/test/kotlin/
shared/
  build.gradle.kts
  src/commonMain/
  src/commonTest/
  src/androidMain/
  src/iosMain/
  src/jvmMain/
  src/jsMain/
  src/wasmJsMain/
iosApp/
```

## Module responsibilities

- `shared`: multiplatform business logic, target-neutral contracts, data models, utilities, and small `expect` APIs.
- `composeApp`: Compose Multiplatform UI, presentation state, target entry points, and app-specific resources.
- `server`: Ktor JVM application, HTTP routes, server-only configuration, and server adapters.
- `iosApp`: SwiftUI host and Xcode project needed to run the iOS target.

## Dependency management

- Use `gradle/libs.versions.toml` for new dependency and plugin versions.
- Add dependencies only to the module and source set that needs them.
- Keep server-only libraries out of `shared`.
- Keep Compose UI libraries out of `shared` unless the task deliberately changes that module's responsibility.
- Prefer common multiplatform libraries for `commonMain`.
- Do not add Android/JVM/browser/iOS-specific dependencies to `commonMain`.

## Scaffolding rules

- Preserve the package root `energy.s2g.tools` unless the task explicitly includes a rename.
- Keep `settings.gradle.kts` as the module inclusion source of truth.
- Use type-safe project accessors such as `projects.shared` when available.
- Keep platform-specific code in the matching source set.
- Prefer `expect`/`actual` for platform-specific behavior that must be called from common code.
- Keep `expect` APIs small and avoid leaking platform frameworks into common contracts.
- Do not create placeholder modules, folders, or example files that are not needed for the requested setup.
- Do not add CI, Docker, publishing, signing, or release automation unless the task explicitly asks for it.

## Build and validation

Use the narrowest useful command first:

```powershell
.\gradlew.bat :shared:check
.\gradlew.bat :composeApp:check
.\gradlew.bat :server:test
.\gradlew.bat test
```

Use equivalent `./gradlew` commands on macOS/Linux.

Some iOS tasks require macOS and Xcode. If running on Windows or Linux, do not treat unavailable iOS compilation as a scaffolding failure unless the changed code should have been platform-neutral.

## Blocker handling

If a blocker appears, such as missing JDK, broken wrapper, dependency download failure, permissions, or network restrictions, state the blocker explicitly. If it is caused by sandboxing or elevated permission needs, request approval to rerun the required command with escalation and retry after approval.

## Deliverables

When this skill is used, produce the minimum set of files and folders needed for the project to build, test, and accept future feature work cleanly with Gradle-managed dependencies. In the final response, include the exact Gradle commands run or the exact commands the user should run manually.
