---
name: review-pr
description: Review pull requests, local branches, commit ranges, staged diffs, unstaged diffs, and code changes in this Kotlin Multiplatform Gradle repository with a bug-finding code-review stance. Use when Codex is asked for a PR review, pull request review, code review, branch review, merge request review, commit range review, or local diff review.
---

# Review PR

Use this skill to review code changes. Stay read-only by default: do not edit files, push commits, update PR metadata, approve/reject pull requests, or publish remote comments unless the user explicitly asks for that separate action.

## Workflow

1. Read `AGENTS.md` first and apply repository-specific rules before inspecting the diff.
2. Identify the review target:
   - Pull request URL or PR ID.
   - Current branch compared with its base branch.
   - Explicit commit range.
   - Staged or unstaged local diff.
3. Collect the minimum context needed to review accurately:
   - Changed file list and diff stat.
   - Full diff for changed behavior.
   - Nearby code in changed files.
   - Relevant Gradle files, tests, source-set declarations, routes, entry points, and platform adapters.
4. Review from the highest-risk behavior outward. Focus on correctness, regressions, missing tests, module boundary violations, platform compatibility, blocking I/O in server paths, dependency scope, configuration, and backwards compatibility.
5. Return findings first, ordered by severity. Keep summaries secondary.

## Remote PRs

- Derive organization, project, repository, source branch, and target branch from the PR URL and `git remote -v` when possible.
- If Azure Repos is used and `az` is available/authenticated, use Azure CLI to collect metadata.
- If GitHub is used and `gh` is available/authenticated, use GitHub CLI to collect metadata.
- If remote tooling is missing, unauthenticated, or cannot fetch the PR, degrade to Git-local review.
- Ask for a branch, commit range, or pasted diff only if local context is insufficient.

## Git-local review

Use local Git context for branches, commit ranges, staged changes, unstaged changes, or fallback from remote PR tooling.

Typical context commands:

```powershell
git status --short
git branch --show-current
git diff --stat
git diff --name-only
git diff
git diff --staged
```

For branch reviews, compare against the intended base branch. If the base is unclear, infer it from the PR target or remote tracking branch; ask only when multiple plausible bases would materially change the review.

## Repository review rules

- Enforce module boundaries from `AGENTS.md`.
- Confirm `core/src/commonMain/` remains target-neutral and free of platform-only, Compose, and data-layer APIs.
- Confirm `shared` does not depend on server or any app module.
- Confirm `core` does not depend on `shared` or `server`.
- Confirm server-only Ktor code stays in `server/`.
- Confirm Compose UI and data implementations stay in `app/shared/`.
- Confirm thin platform entry points stay in `app/<platform>/`.
- Verify dependencies are added to `gradle/libs.versions.toml` and the narrowest correct module/source set.
- Check that Ktor route changes avoid direct blocking I/O in request paths.
- Check that Compose changes do not bury business behavior in Composables.
- Check that domain logic is placed in `core/`, not in `app/shared/`.
- Confirm behavior changes include focused tests in the relevant module/source set.
- Treat missing tests as a finding when changed behavior can regress.
- For iOS-specific changes, distinguish real code issues from Windows/Linux inability to run Xcode tasks.

## Output format

Use this structure:

```markdown
Findings
- [Severity] path/to/file.kt:123 - Explain the bug or risk, why it matters, and what should change.

Open Questions
- Ask only questions that block a confident review.

Summary
- Briefly summarize what was reviewed and any residual risk.
```

If no issues are found, say that clearly and mention any remaining test gaps or context limits.
