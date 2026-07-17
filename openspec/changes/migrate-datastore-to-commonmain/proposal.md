## Why

`JetpackMovieCompose/core/datastore` already owns user preference persistence and provides the language used by network requests, but the KMP project currently has only model types in `shared/commonMain`. The network layer still binds `LanguageProvider` to `DefaultLanguageProvider`, so TMDB requests cannot reflect persisted user language settings.

This change migrates the datastore behavior into `shared/src/commonMain` without creating a new Gradle module. It also wires datastore into Koin so Android can exercise the path end to end with a simple debug button that changes language and then calls the network layer.

## What Changes

- Add a `datastore` package under `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/` for user preferences persistence.
- Migrate the behavior of `UserPreferenceDataSource` from the reference project to KMP:
  - expose `userData: Flow<UserData>`
  - persist `ConfigurationBean`
  - persist `ThemeMode`
  - persist `LanguageMode`
- Use a KMP-compatible DataStore setup in shared code. Do not copy the Android/Hilt module shape from the reference project.
- Add platform-aware datastore creation where platform file paths are required.
- Add a Koin `datastoreModule` that provides the DataStore, `UserPreferenceDataSource`, and datastore-backed `LanguageProvider`.
- Replace the network layer's default language binding with the datastore-backed provider through DI.
- Update Android app startup to install both datastore and network modules.
- Add a simple Android app button to:
  - switch between Traditional Chinese and English
  - trigger a network call
  - show enough response/status text to verify the request path is connected
- Add focused tests for preference mapping, language provider behavior, DI resolution, and network language propagation.

## Capabilities

### New Capabilities

- `kmp-user-preferences-datastore`: shared KMP user preference persistence for configuration, theme, and language.

### Modified Capabilities

- `ktor-movie-network`: `LanguageProvider` must be supplied by datastore-backed user preferences instead of a fixed default provider.

## Impact

- **Affected module**: `shared`, `androidApp`
- **Affected source sets**:
  - `shared/commonMain`: datastore data source, serializer/model mapping, Koin module, language provider implementation
  - `shared/androidMain`: Android datastore file creation when needed
  - `shared/iosMain`: iOS datastore file creation when needed
  - `shared/commonTest`: datastore and DI tests
  - `androidApp`: Koin module startup and temporary button-based verification UI
- **Dependencies**: existing DataStore dependencies are already present in `shared/build.gradle.kts`; implementation should verify whether additional okio/datastore-core platform setup is needed before adding new aliases.

## Non-Goals

- Do not create a standalone `core:datastore` Gradle module.
- Do not migrate Hilt modules or Android-only annotations from the reference project.
- Do not build a production settings screen.
- Do not redesign the network layer beyond replacing the language provider binding.
- Do not introduce secrets or change TMDB API key handling.
