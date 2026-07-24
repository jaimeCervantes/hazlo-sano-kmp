---
name: feature-delivery
description: Implement or change Kotlin Multiplatform behavior in a Gradle project by working from a small Gherkin scenario to tests and then code. Use this skill for features, bug fixes, UI changes, Ktor endpoint changes, shared logic changes, and test-backed behavior changes that should be validated with BDD tools. Do not use it for first-time project setup or boilerplate-only tasks; use the scaffold skill for that.
---

# KMP BDD feature delivery

Use this skill for behavior changes. Start from a small scenario, then tests, then implementation.

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
6. Always start with behavior tests, then unit tests, then implementation.
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

Do not skip this framing stage. If context is incomplete, ask concise clarification questions and wait for explicit agreement on feature scope and the first scenario.
For Kotlin and Android, use Cucumber-JVM or Kotest to write step definitions for your `.feature` files.

## Testing rules

- Apply SOLID, clean code, and clean architecture principles and best practices to the tests as well as the implementation.
- Keep test files small and responsibility-focused; extract setup data, fakes, and assertions into dedicated test helpers/builders instead of growing one large test module.
- Preserve test-layer separation: keep scenario orchestration in BDD step files, domain data builders in helper/factory modules, and transport or persistence fakes in dedicated test-double helpers.
- Mirror semantic feature grouping in BDD tests when it improves navigation: prefer paths like `tests/bdd/step_defs/<feature_area>/...` for related specs.
- Apply the testing pyramid:
   1. Unit tests for domain and use-case logic (fast, isolated, with fakes/mocks). Use `core/src/commonTest/`.
   2. Behavior-level tests with Cucumber-JVM or Kotest for the selected scenario (validating the full behavior from the feature file).
   3. Async integration tests only if the behavior genuinely depends on the infra wiring (e.g., database interactions, external APIs).
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
