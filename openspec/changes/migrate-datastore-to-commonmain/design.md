## Context

The reference Android project stores user preferences with Proto DataStore, Hilt modules, and generated protobuf classes. This KMP project is not modularized the same way and uses Koin in `shared/commonMain`. It already has `UserData`, `ThemeMode`, `LanguageMode`, `ConfigurationBean`, and a network `LanguageProvider` interface.

Because this migration targets `commonMain`, Android-specific pieces such as `Context.dataStoreFile`, Hilt `@Module`, and JVM-only serializer assumptions must be replaced with KMP-compatible equivalents.

## Goals / Non-Goals

**Goals:**

- Keep user preference logic in `shared/commonMain`.
- Preserve the reference behavior for configuration, theme, and language preferences.
- Make `LanguageProvider.getLanguageCode()` return the latest datastore-derived language synchronously for the network layer.
- Provide DI through Koin modules.
- Prove Android wiring with a small button-driven test surface.

**Non-Goals:**

- A full settings UI.
- A separate datastore Gradle module.
- A repository/use-case layer for preferences.
- Changing TMDB endpoint behavior besides the `language` query parameter source.

## Decisions

### 1. Keep datastore in `shared/commonMain`

Datastore code will live under `com.shang.jetpackmoviekmp.datastore`. This matches the user's constraint that no new module is needed now and avoids forcing early module boundaries before more shared app architecture exists.

### 2. Prefer KMP-safe persistence over copying generated Android proto setup blindly

The reference project uses `user_preferences.proto` and generated JVM/Android classes. For this KMP migration, implementation must choose a buildable KMP DataStore representation. If protobuf generation is already configured for KMP, keep the proto schema equivalent. If it is not, use Preferences DataStore with explicit keys for:

- configuration JSON or equivalent serialized value
- theme mode
- language mode

The acceptance point is behavior parity and KMP compilation, not preserving the exact generated class names.

### 3. Use Koin for DI

Add a `datastoreModule` and install it before or alongside `networkModule`. The network module should not bind `DefaultLanguageProvider` when a datastore-backed provider is available. Preferred shape:

```kotlin
modules(
    datastoreModule(...),
    networkModule(isDebug),
)
```

`networkModule` should keep depending on `LanguageProvider`; the binding source changes to datastore.

### 4. Keep `LanguageProvider` synchronous by caching datastore flow values

Ktor's `defaultRequest` currently reads `languageProvider.getLanguageCode()` synchronously. The datastore-backed implementation should collect `UserPreferenceDataSource.userData` in an application-level scope and update a volatile/cache-backed language code. Default value should remain `zh-TW` until preferences emit.

Mapping:

- `LanguageMode.ENGLISH` -> `en-US`
- `LanguageMode.TRADITIONAL_CHINESE` -> `zh-TW`
- `LanguageMode.SYSTEM_DEFAULT` -> platform/system language when available; otherwise `zh-TW`

### 5. Use a temporary Android button for verification

The Android app currently calls `getConfiguration()` on launch. Replace or extend that debug surface with a simple button that changes language and calls the network layer. This should be clearly simple test UI, not a production settings screen.

## Risks / Trade-offs

- **DataStore format risk**: Proto DataStore may require extra KMP protobuf setup. If that setup is too large for this change, Preferences DataStore is acceptable as long as behavior is equivalent and tests cover mapping.
- **Synchronous language risk**: Cached language can briefly use the default before datastore emits. This is acceptable for startup; tests should verify changes propagate after collection.
- **DI ordering risk**: If `networkModule` still binds `DefaultLanguageProvider`, datastore binding may be shadowed or overridden. Tests should assert Koin resolves the datastore-backed provider.
- **Android-only verification risk**: The button proves Android wiring only. Shared tests must cover common datastore behavior so iOS remains protected at compile/test level.

## Migration Plan

1. Add datastore data source and mapping in `shared/commonMain`.
2. Add platform datastore creation APIs where file paths are platform-specific.
3. Add Koin `datastoreModule` and update `JetpackMovieApplication`.
4. Replace network language provider binding with datastore-backed binding.
5. Add button verification in `androidApp`.
6. Add tests and run shared/Android verification.
