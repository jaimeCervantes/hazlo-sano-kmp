---
name: kmp-feature-delivery
description: Implement or change Kotlin Multiplatform behavior in a Gradle project with Compose Multiplatform, shared common code, platform source sets, and a Ktor server. Use this skill for features, bug fixes, UI changes, Ktor endpoint changes, shared logic changes, platform-specific behavior, and test-backed behavior changes. Do not use it for first-time project setup or boilerplate-only tasks; use the scaffold skill for that.
---

# KMP feature delivery

Use this skill for behavior changes. Start from a small approved scenario, then tests, then implementation.

## Default workflow

0. Alignment gate (mandatory, no exceptions):
   1. Ask the user for:
      - **Problem:** What real friction exists?
      - **Savings:** What do we save? Time, money, frustration, risk, or operational load?
      - **Why:** How does it connect to the bigger goal?
   2. Propose the smallest valuable behavior slice based on the request.
   3. Ask for explicit agreement before writing any feature code/tests.
   4. If agreement is missing, stop and wait. Do not scaffold or implement.
1. Read `AGENTS.md` and the bootstrap files for the affected module.
2. Ask clarifying questions only if a real blocker prevents implementation. Maximum: three concise questions.
3. Write or update the smallest useful test first.
4. Implement from stable shared logic outward:
   - `shared/src/commonMain/` for target-neutral business logic.
   - `shared/src/<target>Main/` for platform adapters only when needed.
   - `server/src/main/` for Ktor routes and server-only integrations.
   - `composeApp/src/commonMain/` for Compose UI and presentation state.
   - `composeApp/src/<target>Main/` or `iosApp/` for target entry points and host integration.
5. Add dependencies through `gradle/libs.versions.toml` and the narrowest correct module/source set.
6. Run relevant Gradle validation commands.
7. In the final response, include the exact commands run or the exact commands the user should run manually.
8. Suggest the next scenario only when it naturally follows from the delivered slice.

Use this exact prompt template in step 0:

- Problem:
- Savings:
- Why:
- Proposed first slice:
- Do you approve this first slice before implementation? (yes/no)

## Scenario and testing rules

- Start with one scenario that can be implemented end-to-end in a small slice.
- Prefer unit tests in `shared/src/commonTest/` for shared business logic.
- Use `server/src/test/` and Ktor test host for server route behavior.
- Use `composeApp/src/commonTest/` for presentation logic and pure UI-related behavior where practical.
- Add target-specific tests only when the behavior genuinely depends on the target.
- Keep test data minimal and focused on the scenario.
- Do not overbuild multiple scenarios before the first one is green.

## Module placement

- Put reusable models, rules, validators, use-case logic, and target-neutral services in `shared/src/commonMain/`.
- Put Android, JVM, browser, Wasm, or iOS-specific implementations in the matching source set.
- Put Ktor routing, request/response mapping, server configuration, and server adapters in `server/`.
- Put Composable screens, UI state, app navigation, and UI resources in `composeApp/`.
- Put SwiftUI host changes and Xcode project changes in `iosApp/` only when needed to run or integrate iOS.

## Implementation rules

- Follow repository conventions before introducing new abstractions.
- Keep `shared/src/commonMain/` free of platform-only APIs.
- Keep business logic out of Composables, Ktor routes, and platform entry points.
- Keep Ktor routes thin and delegate behavior to focused functions/classes.
- Keep Composables small and state-hoisted where practical.
- Prefer immutable state and sealed result/state types when they make behavior clearer.
- Add explicit return types to public functions and methods.
- Do not use lazy imports or hidden dependency wiring to avoid module boundary problems.
- Avoid adding dependencies unless they remove meaningful complexity or are required by the scenario.
- If a file grows into mixed orchestration, mapping, persistence, and presentation responsibilities, split it into focused modules.

## Dependency rules

- Add dependency aliases in `gradle/libs.versions.toml`.
- Add runtime dependencies to the module and source set that actually uses them.
- Do not add server-only dependencies to `shared`.
- Do not add Compose dependencies to `shared` unless the module is intentionally changing into a UI-aware module.
- Prefer multiplatform libraries for `commonMain`.
- Keep plugin changes at the module level unless the root build already centralizes that plugin.

## Validation commands

Choose the narrowest commands that cover the changed behavior:

```powershell
.\gradlew.bat :shared:check
.\gradlew.bat :composeApp:check
.\gradlew.bat :server:test
.\gradlew.bat test
```

Use equivalent `./gradlew` commands on macOS/Linux.

For Android smoke validation when UI or Android wiring changes:

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

For server smoke validation when Ktor runtime behavior changes:

```powershell
.\gradlew.bat :server:run
```

Some iOS tasks require macOS and Xcode. Report platform limitations explicitly when validation cannot run in the current environment.

## Blocker handling

If a blocker appears, such as missing JDK, broken wrapper, dependency download failure, network restrictions, permissions, or unclear scope, state the blocker explicitly. If the blocker is likely caused by sandboxing or elevated permission needs, request approval to rerun the required command with escalation and retry after approval.

## What to avoid

- Do not jump into coding before problem framing and feature-scope approval.
- Do not create a new module for a small behavior change unless module separation is the actual goal.
- Do not put platform-specific APIs into `commonMain`.
- Do not put HTTP or UI adapters into shared business logic.
- Do not add broad dependencies to solve narrow problems.
- Do not stop after writing tests if the implementation can be completed in the current turn.
