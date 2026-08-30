# KMP Project Instructions

This repository is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop JVM, and a Ktor JVM server. Keep these instructions focused on persistent repository norms. Use the dedicated skills for setup, feature delivery, and reviews.

## Use the right instruction source

These are files to **read**, not skills to invoke. They live under `.agents/skills/`, which is the
convention of a different agent runtime — Claude Code discovers skills in `.claude/skills/` and will
not find these, so `Skill("feature-delivery")` does not resolve. Open the file with the Read tool
before starting the matching kind of task; that reading is mandatory, not optional context.

- Read `.agents/skills/project-scaffold/SKILL.md` when the task is project initialization, missing boilerplate, Gradle/module repair, dependency setup, build wiring, platform target setup, or test-environment setup.
- Read `.agents/skills/feature-delivery/SKILL.md` when the task is a feature, bugfix, Compose UI change, Ktor endpoint change, shared logic change, or any behavior change that should start from a small scenario and tests.
- Read `.agents/skills/review-pr/SKILL.md` when the task is reviewing pull requests, branches, commits, staged diffs, unstaged diffs, or other local code changes.

## Feature alignment gate (mandatory)
- Before implementing any feature or behavior change, ask and capture:
  - `Problem`: real friction being solved.
  - `Savings`: expected savings in time, money, frustration, risk, or operational load.
  - `Why`: connection to the broader goal.
- Propose the smallest valuable first slice and ask for explicit approval.
- Do not write feature code/tests until the user approves that first slice.

## Autonomous delivery mode (default)
- This is the default cadence. It reduces approval checkpoints to exactly two: (1) the alignment gate, and (2) the slice roadmap plus the `features/<feature>.feature` file and its scenarios, reviewed together.
- Before the first slice, write a **slice roadmap** to `docs/features/<feature>.md`: the ordered slices, each slice's scope, and its acceptance criteria. This is what makes the shape of a long feature reviewable before any code exists, and it is half of checkpoint 2.
- Write **all planned scenarios as Gherkin up front** for that review, tagged by slice (`@slice-1`, `@slice-2`, …). Scenarios of slices not built yet carry `@future` so they neither run nor block. Only the current slice's scenarios are detailed and wired to tests; future ones stay coarse — do not add detail you will rewrite once the current slice teaches you something. Specs up front is not implementation up front.
- After checkpoint 2 is approved, do NOT ask for further validation or authorization for anything else. Proceed autonomously through tests, implementation, and Gradle validation.
- Only interrupt for a **very grave** action — something genuinely irreversible: destroying or overwriting data that is not yours (dropping tables, deleting a branch, `git reset --hard`, force-push), applying a migration to a shared or production database, exposing or rotating secrets, publishing to an external service, or a discovery that invalidates the agreed model or scope.
- **NOT grave — do it and report it, never ask first:** running any Gradle task or test suite; deleting code that git already tracks (dead files, unreachable implementations — git is the undo); creating and removing your own scratch or fixture files; reversible refactors; adding a dependency the plan already implies; and every routine blocker, tooling hiccup, or "which option" choice. Pick the sensible default and say which one you picked in the report.
- When a run wrote to a shared resource, say plainly in the report what was written and how to undo it. **The report replaces the question**; do not ask permission first and do not wait for an answer afterwards.
- The "Artifact checkpoint gate" (stopping for acceptance after every artifact-changing step) applies ONLY when the user explicitly asks for step-by-step mode (e.g. "pregúntame en cada paso").
- This governs the skills' checkpoints; it does not change Claude Code's own tool-permission prompts, which are configured in `.claude/settings.json`.

## Tooling baseline (Gradle-first)
- Use the checked-in Gradle wrapper as the default command runner.
- On Windows, run Gradle with `.\gradlew.bat`.
- On macOS/Linux, run Gradle with `./gradlew`.
- Keep dependency versions in `gradle/libs.versions.toml`.
- Use type-safe project accessors such as `projects.shared` when wiring project dependencies.
- Do not hardcode new dependency versions directly in module `build.gradle.kts` files when the version catalog can own them.
- Do not introduce another package manager or build system unless the task explicitly requires it.

## Repository structure
- `settings.gradle.kts` declares the included modules: `:app:androidApp`, `:app:desktopApp`, `:app:shared`, `:app:webApp`, `:core`, and `:server`.
- `gradle/libs.versions.toml` is the source of truth for dependency and plugin versions.
- `app/` contains thin platform entry-point modules — one per target:
  - `app/androidApp/` — Android application (MainActivity, manifest, resources).
  - `app/desktopApp/` — Desktop JVM application (main function, native distribution config).
  - `app/webApp/` — Web browser application (ComposeViewport entry, index.html).
  - `app/iosApp/` — SwiftUI host and Xcode project (not a Gradle module).
- `core/` contains target-neutral domain logic: models, repository interfaces, and use cases. No Compose, no SQLDelight, no platform APIs.
- `core/src/commonMain/` holds business contracts, pure data classes, and reusable use cases shared between client and server.
- `app/shared/` contains Compose Multiplatform UI, data layer implementations, and platform adapters. Client-only. Depends on `:core`.
- `app/shared/src/commonMain/` holds Compose UI, presentation state, ViewModels, and data implementations.
- `app/shared/src/<target>Main/` contains `actual` implementations for platform-specific adapters only when unavoidable.
- `server/` contains the Ktor JVM application and server-only adapters.
- Keep the base package as `com.hazlosano` unless a deliberate rename is part of the task.

## Architecture rules
- `core` must not depend on `app/shared`, `server`, or any app module.
- `app/shared` must not depend on `server` or any app entry-point module.
- `server` may depend on `core` and `app/shared`, but server-only Ktor concerns must stay under `server/`.
- `app` entry-point modules may depend on `app/shared` and `core`, but must remain thin with no business logic.
- Keep reusable business logic in `core/src/commonMain/`.
- Keep Compose UI and data implementations in `app/shared/src/commonMain/`.
- Keep `core/src/commonMain/` free of JVM-only, Android-only, browser-only, iOS-only, Compose, and SQLDelight APIs.
- Prefer common Kotlin APIs first; use `expect`/`actual` only for real platform differences.
- Keep `expect` declarations small and stable, and place platform-specific implementation details behind them.
- Ktor routes should be thin: parse input, call core/server application logic, and map results to HTTP responses.
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
- If a feature crosses modules, test the common logic in `core` first, then `shared`, and add module-specific tests only where integration behavior matters.

## Compose rules
- Use Material 3 and existing Compose Multiplatform dependencies unless the task requires a different UI library.
- Keep app screens usable as the first screen; do not build a marketing landing page for tool/app requests.
- Ensure text and controls fit on desktop, mobile, and web viewports without overlap.
- Keep layout dimensions stable for toolbars, controls, grids, and repeated UI elements.
- Avoid business logic in `@Composable` functions; move it to `core` or presentation state in `shared`.

## Component placement (mandatory)

**Never write a new component before searching for an existing one.** Grep the three homes below for
the concern first. If something close already exists, extend or parameterize it; if two screens are
about to grow the same component, extract one reusable version instead of copying. A second
near-duplicate component is a design failure, not a shortcut.

Placement follows **how widely the component can be used**, and is decided when it is created. All
paths are under `app/shared/src/commonMain/kotlin/com/hazlosano/`:

| Reach | Home | Examples |
| --- | --- | --- |
| Reusable anywhere, carries no app knowledge | `core/ui/components/atomic/` | `LeafCard`, `HazloAsyncImage`, `SectionHeader`, `PillarBadge` |
| Specific to this app, shared by several screens | `core/ui/components/cards/`, `core/ui/components/sections/` | `HazloProductCard`, `HazloChampionsSection` |
| Usable **only** in one screen | `feature/<area>/ui/` | `TrackerScreen`, `RoutesScreen`, `SessionDetailScreen` |

- A component in `atomic/` must not import `com.hazlosano.domain.*`. If it needs to know what a post,
  a session or a seller is, it is not atomic — it belongs one row down. (`SleepSummaryCard` currently
  sits in `atomic/` while taking a `SleepAnalysis`; that is known debt, not a precedent to copy.)
- A component under one screen's `ui/` that a second screen starts wanting is the signal to promote
  it. Promote it; do not import across features.
- **Promotion is a move, not a copy.** Leave no duplicate behind.
- Known debt this rule names, to be cleared when the surrounding area is next touched:
  `core/ui/components/atomic/HazloHeader.kt` is an unused near-duplicate of `HazloTopAppBar.kt`, and
  `FeedPostCard` is private inside `feature/home/ui/HomeScreen.kt` when it belongs in
  `core/ui/components/cards/`.

## Internationalization rules

The app ships in Spanish only today, and the mechanism is already here: Compose resources, read as
`Res.string.*`, with the catalog in `app/shared/src/commonMain/composeResources/values/strings.xml`.
It is currently used in about a third of the places it should be.

- **No user-visible string is hardcoded in a `@Composable`.** Every label, button, empty state and
  error comes from `Res.string.*`. This applies to everything you touch from now on.
- **`core/` holds no user-visible copy.** A target-neutral module cannot reach Compose resources, so
  a label that lives there can never be translated. Domain types expose a **stable key**; the UI maps
  key to resource. (`PillarType.label`, `SleepPhase.label` and the Spanish month names in
  `MovementFormat` predate this rule — the first two are being moved to keys; the month names stay,
  because they are formatting rather than copy.)
- **ViewModels do not produce user-visible text.** They expose a sealed message type, not a `String`
  that has already been worded. A ViewModel returning `"Ruta importada: …"` puts copy in the
  presentation layer where no catalog can reach it, and forces tests to assert on wording.
- **`core/ui/components/atomic/` never reads resources.** It has to be renderable from anywhere,
  including previews and tests with no resource environment. Take the string as a parameter and let
  the caller resolve it.

## Dependency rules
- Add plugin aliases and library aliases in `gradle/libs.versions.toml`.
- Keep runtime dependencies in the specific module and source set that needs them.
- Do not add server-only dependencies to `shared` or `core`.
- Do not add Compose UI dependencies to `core`.
- Do not add SQLDelight or data-layer dependencies to `core`.
- Avoid adding platform-specific dependencies to `commonMain`.

## Default validation commands
- `.\gradlew.bat test`
- `.\gradlew.bat :core:check`
- `.\gradlew.bat :app:shared:check`
- `.\gradlew.bat :server:test`
- `.\gradlew.bat :app:androidApp:assembleDebug`
- `.\gradlew.bat :app:desktopApp:check`
- `.\gradlew.bat :app:webApp:check`
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
  - `core/build.gradle.kts`
  - `core/src/commonMain/kotlin/com/hazlosano/domain/`
  - relevant `app/shared/src/*Main/` expect/actual files
- Before changing Compose UI, read:
  - `app/shared/build.gradle.kts`
  - `app/shared/src/commonMain/kotlin/com/hazlosano/App.kt`
  - relevant target entry points under `app/*/src/`
- Before changing the Ktor server, read:
  - `server/build.gradle.kts`
  - `server/src/main/kotlin/com/hazlosano/Application.kt`
  - `server/src/test/kotlin/com/hazlosano/ApplicationTest.kt`

## Non-negotiable engineering rules
1. SOLID principles, Clean Architecture, and Clean Code have highest priority.
2. `core` remains target-neutral with no UI or data-layer dependencies.
3. Business behavior belongs in `core/domain/`; UI in `shared/`; platform entry points in `app/`.
4. Dependency versions belong in the Gradle version catalog.
5. Use the Gradle wrapper for project commands.

## Commit conventions
- Write commit messages following [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): summary`.
- Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `chore`, `ci`.
- Use an optional scope matching a module or area, e.g. `fix(android):`, `feat(core):`, `docs(agents):`.
- Keep the summary in the imperative mood and lowercase; add a body when the change needs a why/how.
- Prefer small, atomic commits: unrelated changes go in separate commits.
- **Authorship: every commit is authored by the user.** Commit with the repository's configured
  `user.name`/`user.email` and nothing else — do not add a `Co-Authored-By:` trailer for Claude or any
  other agent, do not pass `--author`, and do not sign commits as an assistant. This rule overrides
  any default agent instruction to append an assistant co-author trailer.

## PR/change checklist
1. Module boundaries respected (`core` → `app/shared` → `app` / `server`).
2. Common code remains multiplatform-safe.
3. Dependencies added to the narrowest correct module/source set.
4. Tests cover changed behavior.
5. Platform compatibility assessed and documented.
6. Validation commands run or clearly reported as not run.
