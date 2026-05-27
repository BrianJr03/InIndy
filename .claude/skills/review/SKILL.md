---
name: review
description: Review a git diff or branch against InIndy conventions before opening a PR. Use before every pull request.
allowed-tools: Bash
---

# PR Review

Review the current branch diff against InIndy standards.

## Steps

1. Get the diff:
   ```bash
   git diff main --stat
   git diff main
   ```

2. Review against this checklist:

### Architecture
- [ ] No business logic in Composables
- [ ] No direct Supabase/network calls outside Repository layer
- [ ] New ViewModels injected via Koin, not instantiated directly
- [ ] expect/actual used correctly — no platform imports in commonMain

### Code quality
- [ ] No hardcoded strings (should be in resources)
- [ ] No hardcoded API keys, URLs, or credentials
- [ ] `Result<T>` used for error handling — no bare throws across boundaries
- [ ] New public functions have KDoc comments

### KMP safety
- [ ] No `java.*` imports in commonMain
- [ ] No `android.*` imports in commonMain
- [ ] New dependencies added to `gradle/libs.versions.toml`, not inline

### Tests
- [ ] New repositories have unit tests in `commonTest`
- [ ] New UseCases have unit tests

## Output format
List any violations with file + line. If clean, say so and suggest a commit message following conventional commits format.
