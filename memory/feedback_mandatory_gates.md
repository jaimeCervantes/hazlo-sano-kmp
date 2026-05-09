---
name: Mandatory project gates before coding
description: Project has AGENTS.md and .agents/skills/ that must be followed — feature alignment gate, skill selection, version catalog, Gradle wrapper, tests
type: feedback
---

ALWAYS read AGENTS.md at the start of any task in this repo, and follow it strictly.

**Why:** I skipped it on 2026-05-08 and the user caught me — I wrote 11 files without the feature alignment gate (Problem/Savings/Why), without using the correct skill (.agents/skills/kmp-feature-delivery/), without tests, without the version catalog for new dependencies, and without running Gradle validation.

**How to apply:** Before writing any code in this project:
1. Read AGENTS.md
2. Run the feature alignment gate: ask Problem, Savings, Why, propose smallest slice, wait for approval
3. Use the right skill: kmp-project-scaffold for setup, kmp-feature-delivery for features/bugfixes, review-pr for reviews
4. Add dependencies to gradle/libs.versions.toml, not hardcoded
5. Use `.\gradlew.bat test` (Windows) or `./gradlew test` (macOS/Linux)
6. Add tests for behavior changes
7. Run the `simplify` skill after finishing
8. Report validation commands run in the final response
