---
name: indy-build
description: Run a full KMP indy-build and test check for InIndy. Use before committing or after significant changes.
allowed-tools: Bash
---

# KMP Build + Test Check

Run the full verification suite for the InIndy KMP project.

## Steps

Run these commands in order. Stop and report on the first failure.

```bash
# 1. Build shared module for all targets
./gradlew :shared:indy-build

# 2. Run all shared tests
./gradlew :shared:allTests

# 3. Android indy-build check
./gradlew :androidApp:assembleDebug

# 4. Lint
./gradlew lint

# 5. Regenerate SQLDelight in case .sq files changed
./gradlew :shared:generateSqlDelightInterface
```

## On failure
- Show the exact error and file
- Identify whether it's a `commonMain`, `androidMain`, or `iosMain` issue
- Suggest the minimal fix — don't refactor unrelated code
