# CLAUDE.md

Repository norms, architecture rules, skills, and workflow live in **AGENTS.md**.
Always read and follow them for any task in this repository.

## Rules for Terminal and File Edits
- Do NOT chain multiple bash commands using `&&` or `;` with `sed` or other commands to edit code.
- Always use the native `Edit` or `Write` tools to modify code files (`.kt`, `.xml`, etc.).
- Run Gradle test commands and `grep` analysis as separate, unchained shell steps.
- Do NOT redirect output to `/dev/null`.

@AGENTS.md
