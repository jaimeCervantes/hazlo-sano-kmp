# KMP Project Instructions

This repository is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop JVM, and a Ktor JVM server. Keep these instructions focused on persistent repository norms. Use the dedicated skills for setup, feature delivery, and reviews.

## Use the right instruction source
- Use `.agents/skills/kmp-project-scaffold/` when the task is project initialization, missing boilerplate, Gradle/module repair, dependency setup, build wiring, platform target setup, or test-environment setup.
- Use `.agents/skills/kmp-feature-delivery/` when the task is a feature, bugfix, Compose UI change, Ktor endpoint change, shared logic change, or any behavior change that should start from a small scenario and tests.
- Use `.agents/skills/review-pr/` when the task is reviewing pull requests, branches, commits, staged diffs, unstaged diffs, or other local code changes.

## Feature alignment gate (mandatory)
- Before implementing any feature or behavior change, ask and capture:
  - `Problem`: real friction being solved.
  - `Savings`: expected savings in time, money, frustration, risk, or operational load.
  - `Why`: connection to the broader goal.
- Propose the smallest valuable first slice and ask for explicit approval.
- Do not write feature code/tests until the user approves that first slice.

## Tooling baseline (Gradle-first)
- Use the checked-in Gradle wrapper as the default command runner.
- On Windows, run Gradle with `.\gradlew.bat`.
- On macOS/Linux, run Gradle with `./gradlew`.
- Keep dependency versions in `gradle/libs.versions.toml`.
- Use type-safe project accessors such as `projects.shared` when wiring project dependencies.
- Do not hardcode new dependency versions directly in module `build.gradle.kts` files when the version catalog can own them.
- Do not introduce another package manager or build system unless the task explicitly requires it.

## Repository structure
- `settings.gradle.kts` declares the included modules: `:composeApp`, `:server`, and `:shared`.
- `gradle/libs.versions.toml` is the source of truth for dependency and plugin versions.
- `composeApp/` contains the Compose Multiplatform UI for Android, iOS, Desktop JVM, JS, and Wasm.
- `composeApp/src/commonMain/` contains UI and presentation code shared across Compose targets.
- `composeApp/src/<target>Main/` contains target-specific UI entry points or integrations only.
- `shared/` contains Kotlin Multiplatform code shared by app and server.
- `shared/src/commonMain/` should hold target-neutral business logic, contracts, models, and utilities.
- `shared/src/<target>Main/` should contain `actual` implementations or platform-specific adapters only when unavoidable.
- `server/` contains the Ktor JVM application and server-only adapters.
- `iosApp/` contains the SwiftUI host application and Xcode project.
- Keep the base package as `energy.s2g.tools` unless a deliberate rename is part of the task.

## Architecture rules
- `shared` must not depend on `composeApp` or `server`.
- `server` may depend on `shared`, but server-only Ktor concerns must stay under `server/`.
- `composeApp` may depend on `shared`, but Compose UI concerns must stay under `composeApp/`.
- Keep reusable business logic out of Composables, Ktor routes, SwiftUI files, and platform entry points.
- Keep `shared/src/commonMain/` free of JVM-only, Android-only, browser-only, and iOS-only APIs.
- Prefer common Kotlin APIs first; use `expect`/`actual` only for real platform differences.
- Keep `expect` declarations small and stable, and place platform-specific implementation details behind them.
- Ktor routes should be thin: parse input, call shared/server application logic, and map results to HTTP responses.
- Composables should be small, state-hoisted where practical, and focused on rendering and interaction.

## Runtime rules
- In Ktor request paths, use suspending/non-blocking APIs when available.
- If blocking I/O is unavoidable, isolate it behind an adapter and dispatch it explicitly instead of running it directly in a route.
- Do not introduce global mutable state for application behavior.
- Keep configuration explicit and avoid burying environment-specific constants in shared business code.

## Coding rules
- Follow the Kotlin style already present in the repository.
- Add explicit return types to public functions and methods.
- Prefer immutable data models and focused functions over large mixed-responsibility classes.
- Use sealed interfaces/classes for closed result or UI state hierarchies when they clarify behavior.
- Add comments only for non-obvious business rules or platform constraints.
- Add or update tests for behavior changes.
- If a feature crosses modules, test the common logic in `shared` first and add module-specific tests only where integration behavior matters.

## Compose rules
- Use Material 3 and existing Compose Multiplatform dependencies unless the task requires a different UI library.
- Keep app screens usable as the first screen; do not build a marketing landing page for tool/app requests.
- Ensure text and controls fit on desktop, mobile, and web viewports without overlap.
- Keep layout dimensions stable for toolbars, controls, grids, and repeated UI elements.
- Avoid business logic in `@Composable` functions; move it to shared logic or presentation state.

## Dependency rules
- Add plugin aliases and library aliases in `gradle/libs.versions.toml`.
- Keep runtime dependencies in the specific module and source set that needs them.
- Do not add server-only dependencies to `shared`.
- Do not add Compose UI dependencies to `shared` unless the module is intentionally becoming UI-aware.
- Avoid adding platform-specific dependencies to `commonMain`.

## Default validation commands
- `.\gradlew.bat test`
- `.\gradlew.bat :shared:check`
- `.\gradlew.bat :composeApp:check`
- `.\gradlew.bat :server:test`
- `.\gradlew.bat :composeApp:assembleDebug`
- `.\gradlew.bat :server:run`

Use the equivalent `./gradlew` commands on macOS/Linux. Some iOS build tasks require macOS/Xcode; do not report Windows iOS task failures as code failures without checking the platform requirement.

## Manual validation reporting (mandatory)
- In the final response for any code, test, or configuration change, include the exact validation commands that were run.
- If validation was not run, say so and provide the exact Gradle commands the user can run from the repository root.
- For changes limited to instructions or skills, include the skill validation command instead of unrelated application build commands.

## Bootstrap context
- Before changing Gradle/module wiring, read:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle/libs.versions.toml`
  - the affected module `build.gradle.kts`
- Before changing shared logic, read:
  - `shared/build.gradle.kts`
  - `shared/src/commonMain/kotlin/energy/s2g/tools/Platform.kt`
  - relevant `shared/src/*Main/` expect/actual files
- Before changing Compose UI, read:
  - `composeApp/build.gradle.kts`
  - `composeApp/src/commonMain/kotlin/energy/s2g/tools/App.kt`
  - relevant target entry points under `composeApp/src/*Main/`
- Before changing the Ktor server, read:
  - `server/build.gradle.kts`
  - `server/src/main/kotlin/energy/s2g/tools/Application.kt`
  - `server/src/test/kotlin/energy/s2g/tools/ApplicationTest.kt`

## Non-negotiable engineering rules
1. SOLID principles, Clean Architecture, and Clean Code have highest priority.
2. Shared common code remains target-neutral.
3. Business behavior belongs in shared/domain/application logic, not UI or HTTP adapters.
4. Dependency versions belong in the Gradle version catalog.
5. Use the Gradle wrapper for project commands.

## PR/change checklist
1. Module boundaries respected.
2. Common code remains multiplatform-safe.
3. Dependencies added to the narrowest correct module/source set.
4. Tests cover changed behavior.
5. Platform compatibility assessed and documented.
6. Validation commands run or clearly reported as not run.
