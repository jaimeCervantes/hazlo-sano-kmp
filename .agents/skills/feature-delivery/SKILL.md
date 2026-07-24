---
name: feature-delivery
description: Implement or change Kotlin Multiplatform behavior in a Gradle project by working from a small Gherkin scenario to tests and then code. Use this skill for features, bug fixes, UI changes, Ktor endpoint changes, shared logic changes, and test-backed behavior changes that should be validated with BDD tools. Do not use it for first-time project setup or boilerplate-only tasks; use the scaffold skill for that.
---

# KMP BDD feature delivery

Use this skill for behavior changes. Work **outside-in (double-loop)**: when it is worth it for the scenario, first write a failing e2e/integration test that **guides the design**, then implement **bottom-up** — drive out the unit tests (where most coverage lives), then component tests, building until the outer e2e/integration test passes green. Keep a pyramid shape: many unit tests, few e2e.

## Default workflow

> **Cadence is governed by `AGENTS.md` → "Autonomous delivery mode" (the default).** It reduces the
> only approval checkpoints to (1) the alignment gate and (2) the `.feature` + its scenarios. After the
> `.feature` is approved, do NOT ask for validation/authorization for anything else — only interrupt for
> a **very grave** action (destructive/irreversible, a write to shared/prod resources, or a security
> risk). Step 8 and the "Artifact checkpoint gate (Conditional)" section apply ONLY when the user
> explicitly asks for step-by-step mode.

- **ALL NEW CHANGES MUST BE MADE IN A NEW BRANCH AND A PULL REQUEST MUST BE CREATED.** Do not commit directly to the main branch.
0. Alignment gate (mandatory, no exceptions):
   1. Ask the user for:
      - **Problem:** What real friction exists?
      - **Savings:** What do we save? Time, money, frustration, risk, or operational load?
      - **Why:** How does it connect to the bigger goal? Purpose and direction.
   2. Propose the smallest valuable behavior slice based on the request.
   3. Ask for explicit agreement before writing any feature code/tests.
   4. If agreement is missing, stop and wait. Do not scaffold or implement.
1. Ask clarifying questions only if a real blocker prevents implementation. Maximum: three concise questions.
2. Read `AGENTS.md` and the bootstrap files for the affected module.
3. Before any step that will create or modify repository artifacts, tell the user what files or generated artifacts will be affected.
4. Create or update one Gherkin spec in `features/<feature_name>.feature` or under a shallow semantic subfolder when that area already has related specs or is expected to grow.
5. Choose one priority scenario that can be implemented end-to-end in a small slice.
6. Outside-in, double-loop — write the outer test first to guide design, then build coverage from the base:
   1. **Outer loop — write it first, let it fail.** When an end-to-end or integration test is worth it for the scenario, write it up front and leave it red. It drives the design and mirrors the `.feature`'s Given/When/Then; it is not where most coverage lives. UI flows: Compose UI test (`runComposeUiTest`) on the JVM. Device-only behavior (real map rendering, GPS, permission dialogs): instrumented `androidTest` (`connectedDebugAndroidTest`). Infra-dependent behavior: an integration test (e.g. SQLDelight in-memory driver).
   2. **Inner loop — implement bottom-up, unit-first.** Drive the domain/use-case/ViewModel logic with unit tests in `commonTest` (most of the suite), then the component tests for the Composables/adapters (Compose UI test; Robolectric for Android `actual`s on the JVM).
   3. Keep going until the outer e2e/integration test passes green. Distribution stays a pyramid: many unit tests, few e2e.
7. After each step that creates or modifies artifacts, stop and report:
   - the files or generated artifacts created or changed;
   - the purpose of that step;
   - what the user should validate next.
8. Ask for explicit user acceptance before continuing to the next artifact-changing step, **ONLY IF** the user explicitly requested the "Artifact checkpoint gate" in the prompt. Otherwise, proceed autonomously.
9. Implement from stable core logic outward:
   - `core/src/commonMain/` for target-neutral domain logic (models, repository interfaces, use cases).
   - `app/shared/src/commonMain/` for Compose UI, presentation state, ViewModels, data implementations.
   - `app/shared/src/<target>Main/` for platform adapters only when needed.
   - `server/src/main/` for Ktor routes and server-only integrations.
   - `app/androidApp/src/main/`, `app/desktopApp/src/main/`, `app/webApp/src/`, `app/iosApp/` for target entry points and host integration.
10. If new dependencies are needed, add them through `gradle/libs.versions.toml` and the narrowest correct module/source set.
11. Run validations with `./gradlew` commands and report any migration command if SQLDelight/Database models changed.
12. In the final response, include the exact Gradle validation commands the user can run manually from the repository root.
13. Suggest the next scenario instead of forcing an artificial loop.
14. After each slice, append an entry to `docs/features/<feature>-bitacora.md` (append-only). Mandatory for every slice — see the "Bitácora" section.

Use this exact prompt template in step 0:
- Problem:
- Savings:
- Why:
- Proposed first slice:
- Do you approve this first slice before implementation? (yes/no)

## Blocker handling

If a blocker appears (permissions, missing tooling, network restrictions, broken wrapper, missing JDK, or unclear scope), explicitly ask whether to continue by resolving the blocker now or stop and leave the task paused.
If the blocker is likely caused by sandbox restrictions or elevated permission needs, request explicit permission to run the required command with escalation and then retry the failed command.

## Artifact checkpoint gate (Conditional)

- **Only apply this if the user explicitly requests it in the prompt.**
- Treat every repository-changing step as a checkpoint.
- Show the concrete artifact list using repository paths whenever possible.
- Ask the user to validate and explicitly accept that checkpoint before continuing.
- Do not batch multiple artifact-changing steps into one approval unless the user explicitly asks for that.
- If a step produces no repository artifact, no checkpoint is required for that step.

## Gherkin rules

Use a compact feature file with business context when it is known or can be inferred safely.

```gherkin
Feature: [Feature Name]

  Context:
  - Problem: [real friction]
  - Savings: [time, money, risk, or frustration saved]
  - Why: [connection to the broader goal]

  As a [role]
  I want to [action]
  So that [benefit]
```

Do not skip this framing stage for genuinely end-to-end scenarios. Skip the `.feature` only for pure unit-level bug fixes with no observable behavior change, and state explicitly why it was skipped. If context is incomplete, ask concise clarification questions and wait for explicit agreement on scope and the first scenario.

The `.feature` file is the authoritative scenario spec. Pair it with an executable behavior test **of the same name** that drives those exact Given/When/Then steps (a Compose UI test for UI flows, an instrumented test for device-only flows). If the repo adopts a Kotlin Gherkin step-runner (Cucumber-JVM, or a Kotest-based runner), wire the `.feature` directly into it instead of duplicating it as prose plus a hand-written test.

## Testing rules

- Apply SOLID, clean code, and clean architecture principles and best practices to the tests as well as the implementation.
- Keep test files small and responsibility-focused; extract setup data, fakes, and assertions into dedicated test helpers/builders instead of growing one large test module.
- Preserve test-layer separation: keep scenario orchestration in BDD step files, domain data builders in helper/factory modules, and transport or persistence fakes in dedicated test-double helpers.
- Mirror semantic feature grouping in BDD tests when it improves navigation: prefer paths like `tests/bdd/step_defs/<feature_area>/...` for related specs.
- **One-time test-tooling bootstrap:** the first time a change needs a test layer the repo lacks, tell the user which tooling is required and get approval **once** — then do not re-ask on later features. Typical KMP tooling: Compose UI test (`org.jetbrains.compose.ui:ui-test` + `runComposeUiTest`) for UI behavior on the JVM; Robolectric plus `withHostTest {}` on the Android target to run Android `actual`s as JVM unit tests; a Gherkin step-runner (Cucumber-JVM or Kotest) to execute `.feature` files; instrumented `androidTest` for device-only behavior.
- Apply the testing pyramid — write the outer e2e/integration test first to guide the design, then implement bottom-up; most tests stay at the base:
   1. **Behavior/e2e** for the selected scenario — Compose UI test (`runComposeUiTest`, JVM) for UI flows; instrumented `androidTest` only for device-bound behavior (map rendering, GPS, permission dialogs).
   2. **Component** — individual Composables via Compose UI test; Android `actual`s via Robolectric on the JVM.
   3. **Unit** for domain, use-case and ViewModel logic (fast, isolated, with fakes/mocks) in `core/src/commonTest/` and `app/shared/src/commonTest/`. Most tests live here.
   4. **Integration** only when the behavior genuinely depends on infra wiring (SQLDelight in-memory driver, external APIs).
- Keep test data minimal and focused on the scenario under implementation.
- Do not overbuild multiple scenarios before the first one is green.

Recommended validation commands:
Choose the narrowest commands that cover the changed behavior:

```bash
./gradlew :core:check
./gradlew :app:shared:check
./gradlew :server:test
./gradlew :app:androidApp:check
./gradlew :app:desktopApp:check
./gradlew :app:webApp:check
./gradlew test
```

When closing the task, always surface the exact validation commands you ran, or the exact manual commands the user should run if validation was skipped or blocked.

## Implementation rules

- When a working reference implementation exists, treat it as a source of correct APIs and behavior, not as the quality bar. Do not copy it verbatim: re-express it applying Clean Code, Clean Architecture, SOLID, and current Android/KMP best practices, and place it in the correct module (`core` domain, `app/shared` presentation/UI, platform source sets for platform code).
- Covering the changed code with tests is mandatory. Unit-test the domain/use-case/ViewModel logic; when UI or platform-only pieces cannot be host-tested, say so explicitly and note the instrumented test needed.
- Follow repository conventions before introducing new abstractions.
- Keep `core/src/commonMain/` free of platform-only APIs, Compose, and data-layer dependencies.
- Keep `app/shared/src/commonMain/` free of platform-only APIs.
- Keep business logic in `core/`, not in Composables, Ktor routes, or platform entry points.
- Write only the code needed to satisfy the selected scenario cleanly.
- Keep files small and responsibility-focused; extract collaborators before a file becomes a mixed implementation unit.
- Do not use lazy imports or hidden dependency wiring to avoid module boundary problems; keep imports explicit.
- Avoid adding dependencies unless they remove meaningful complexity or are required by the scenario.
- If a feature area grows too many sibling files (or grows into mixed orchestration, mapping, persistence, and presentation responsibilities), reorganize into shallow responsibility-based packages and split into focused modules.

## Coding Standards (STRICT)

You already know Clean Code, Clean Architecture, and SOLID principles. Apply them ruthlessly. Additionally, enforce these specific rules for Kotlin:

- **Typing:** EVERY public function and method MUST have explicit return types.
- **Docstrings:** Use KDoc ONLY for complex business logic. Do not write obvious documentation for simple getters/setters.
- **Error Handling:** Never return generic errors. Use sealed classes or sealed interfaces to model specific domain errors and handle them exhaustively.
- **Naming:** Variables and functions `MUST` be in `camelCase`. Classes and interfaces `MUST` be in `PascalCase`.
- **Refactoring and Clean Architecture:** If a file (especially a use case or viewmodel) grows too long or takes on multiple responsibilities, proactively propose and execute a refactoring to split it into a cohesive package with smaller, single-responsibility modules.

## Bitácora (mandatory, per slice)

After each slice, append an entry to `docs/features/<feature>-bitacora.md` (append-only — never rewrite past entries). Each entry records:

- **Objective:** the scenario/slice delivered.
- **Decisions + rationale:** notable design/architecture choices and why.
- **Files touched:** grouped by area/module.
- **Key commands:** the Gradle/`adb` commands run.
- **Validation results:** with numbers (tests passed, build result, device check).
- **Deviations:** anything done differently from the plan.
- **Follow-ups:** known gaps or next candidates.

Every entry MUST end with:

- **Recap:** one paragraph on the current state.
- **Próximos pasos (opciones):** concrete next choices, plus anything pending on the user.

## Migration rule

If SQLDelight `.sq` files or other database models under data layers change, report the exact steps and Gradle commands needed to generate or apply migrations (e.g., `./gradlew generateSqlDelightInterface` or any specific plugin task used in the project).

## Module placement

- Put reusable models, rules, validators, and use-case logic in `core/src/commonMain/`.
- Put Compose UI, ViewModels, presentation state, data implementations, and UI resources in `app/shared/src/commonMain/`.
- Put Android, JVM, browser, or iOS-specific platform implementations in the matching source set under `app/shared/src/`.
- Put Ktor routing, request/response mapping, server configuration, and server adapters in `server/`.
- Put thin platform entry points (main functions, activities, HTML) in `app/<platform>/`.
- Put SwiftUI host changes and Xcode project changes in `app/iosApp/` only when needed to run or integrate iOS.

## Dependency rules

- Add dependency aliases in `gradle/libs.versions.toml`.
- Add runtime dependencies to the module and source set that actually uses them.
- Do not add server-only dependencies to `shared` or `core`.
- Do not add Compose dependencies to `core`.
- Do not add SQLDelight or data-layer dependencies to `core`.
- Prefer multiplatform libraries for `commonMain`.
- Keep plugin changes at the module level unless the root build already centralizes that plugin.

## What to avoid

- Do not stop after writing Gherkin if the task can be implemented now.
- Do not ask the user for a "small scenario" if you can derive one from the request.
- Do not add unnecessary discussion beyond the mandatory artifact checkpoints and approval gates.
- Do not let platform layers leak into core layers.
- Do not jump into coding before problem framing and feature-scope approval.
- Do not start implementation without explicit user approval from the alignment gate.
- Do not create a new module for a small behavior change unless module separation is the actual goal.
- Do not put platform-specific APIs into `commonMain`.
- Do not put HTTP or UI adapters into `core`.
- Do not add broad dependencies to solve narrow problems.
