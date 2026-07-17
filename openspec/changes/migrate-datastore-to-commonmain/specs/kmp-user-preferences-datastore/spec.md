## ADDED Requirements

### Requirement: Shared user preference datastore

`shared/commonMain` MUST provide a user preference data source that exposes `UserData` as a flow and persists configuration, theme mode, and language mode without requiring a separate Gradle datastore module.

#### Scenario: default user data is emitted

- **WHEN** no user preferences have been persisted yet
- **THEN** the data source emits `UserData.getDefault()`

#### Scenario: theme mode is persisted

- **WHEN** `setThemeMode(ThemeMode.DARK)` is called
- **THEN** later `userData` emissions contain `themeMode = ThemeMode.DARK`

#### Scenario: language mode is persisted

- **WHEN** `setLanguageMode(LanguageMode.ENGLISH)` is called
- **THEN** later `userData` emissions contain `languageMode = LanguageMode.ENGLISH`

#### Scenario: configuration is persisted

- **WHEN** `setConfiguration(configuration)` is called
- **THEN** later `userData` emissions contain the same configuration values

### Requirement: Platform-aware datastore creation

The datastore implementation MUST create/open the same logical user preferences store on each supported platform while keeping platform-specific file path logic outside common business logic.

#### Scenario: Android uses application storage

- **WHEN** Android creates the user preferences datastore
- **THEN** the datastore file is located in app-owned storage and does not require callers outside DI to pass raw file paths

#### Scenario: iOS uses a stable app path

- **WHEN** iOS creates the user preferences datastore
- **THEN** the datastore file uses a stable app-owned document/cache path suitable for app restarts

### Requirement: Datastore Koin module

`shared` MUST provide a Koin datastore module that resolves the user preference data source and binds the network `LanguageProvider` to a datastore-backed implementation.

#### Scenario: datastore module resolves user preferences

- **WHEN** Koin starts with the datastore module
- **THEN** `UserPreferenceDataSource` can be resolved

#### Scenario: datastore module resolves language provider

- **WHEN** Koin starts with the datastore module
- **THEN** `LanguageProvider` resolves to the datastore-backed provider

### Requirement: Android button verification

`androidApp` MUST provide a simple button-based verification path that can update language preference and trigger a network request.

#### Scenario: button updates language and calls network

- **WHEN** the user taps the test button
- **THEN** the app persists the selected language and performs a TMDB network call using DI-provided dependencies

#### Scenario: verification result is visible

- **WHEN** the button-triggered network call succeeds or fails
- **THEN** the app displays compact status text indicating the selected language and result or error
