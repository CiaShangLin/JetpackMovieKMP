# AGENTS.md

This file gives coding agents project-specific guidance for `JetpackMovieKMP`.

## Project Overview

- Project type: Kotlin Multiplatform with Android and iOS targets.
- Root project: `JetpackMovieKMP`
- Modules:
  - `androidApp`: Android application entry point.
  - `shared`: shared Compose Multiplatform and Kotlin code.
  - `iosApp`: iOS application entry point.
- Package namespace: `com.shang.jetpackmoviekmp`
- Key versions are managed in `gradle/libs.versions.toml`.

## Reference Project

Agents may reference this local project when looking for established app structure, naming, architecture, release workflow, or implementation ideas:

`C:\Users\User\AndroidStudioProjects\JetpackMovieCompose`

Use it as a reference only. Do not copy code blindly; adapt patterns to this KMP project's module structure and current dependencies.

## Common Commands

Run commands from the repository root.

```bash
# Build Android debug app
./gradlew :androidApp:assembleDebug

# Run shared Android host tests
./gradlew :shared:testAndroidHostTest

# Run shared iOS simulator tests
./gradlew :shared:iosSimulatorArm64Test

# Run broader Gradle verification when available
./gradlew check
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :shared:iosSimulatorArm64Test
.\gradlew.bat check
```

## Development Guidelines

- Prefer existing project patterns before introducing new structure.
- Keep shared business and UI logic in `shared` when it is useful across Android and iOS.
- Keep Android-only behavior in `androidApp` or `shared/src/androidMain`.
- Keep iOS-only behavior in `iosApp` or `shared/src/iosMain`.
- Use Compose Multiplatform conventions already present in `shared/src/commonMain`.
- Keep edits focused on the requested behavior; avoid unrelated refactors.
- Do not commit secrets or machine-specific local configuration.
- Treat `local.properties`, signing files, API keys, and generated build outputs as non-source artifacts unless explicitly requested.

## Fixed Commit Skill

For any user request to commit changes, use the commit skill/workflow by default.

Commit workflow:

1. Inspect current status:

   ```bash
   git status --short
   git diff HEAD
   git branch --show-current
   git log --oneline -10
   ```

2. Stage only files that belong to the requested work.
3. Create a single focused commit with a clear message.
4. Do not include unrelated local changes.
5. Do not push unless the user explicitly asks for push.

When available in the current agent environment, prefer the `ngs-commit` skill for commit preparation and message generation.

## Git Remote

The GitHub remote is:

```bash
origin https://github.com/CiaShangLin/JetpackMovieKMP.git
```

Push only after explicit user approval.
