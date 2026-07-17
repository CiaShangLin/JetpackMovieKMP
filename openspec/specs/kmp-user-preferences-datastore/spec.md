# kmp-user-preferences-datastore Specification

## Purpose
TBD - created by archiving change migrate-datastore-to-commonmain. Update Purpose after archive.
## Requirements
### Requirement: Shared 使用者偏好設定 datastore

`shared/commonMain` 必須提供 user preference data source，以 flow 暴露 `UserData`，並持久化 configuration、theme mode、language mode；此能力不得要求建立獨立 Gradle datastore module。

#### Scenario: 預設 user data 會被發出

- **WHEN** 尚未持久化任何 user preferences
- **THEN** data source 發出 `UserData.getDefault()`

#### Scenario: theme mode 會被持久化

- **WHEN** 呼叫 `setThemeMode(ThemeMode.DARK)`
- **THEN** 後續 `userData` emission 包含 `themeMode = ThemeMode.DARK`

#### Scenario: language mode 會被持久化

- **WHEN** 呼叫 `setLanguageMode(LanguageMode.ENGLISH)`
- **THEN** 後續 `userData` emission 包含 `languageMode = LanguageMode.ENGLISH`

#### Scenario: configuration 會被持久化

- **WHEN** 呼叫 `setConfiguration(configuration)`
- **THEN** 後續 `userData` emission 包含相同 configuration values

### Requirement: 平台感知 datastore 建立方式

datastore implementation 必須在每個支援平台建立/開啟同一個邏輯 user preferences store，並將 platform-specific file path logic 留在 common business logic 之外。

#### Scenario: Android 使用 application storage

- **WHEN** Android 建立 user preferences datastore
- **THEN** datastore file 位於 app-owned storage，且 DI 外部 caller 不需要傳入 raw file paths

#### Scenario: iOS 使用穩定 app path

- **WHEN** iOS 建立 user preferences datastore
- **THEN** datastore file 使用適合 app restart 的穩定 app-owned document/cache path

### Requirement: Datastore Koin module

`shared` 必須提供 Koin datastore module，可以 resolve user preference data source，並將 network `LanguageProvider` 綁定到 datastore-backed implementation。

#### Scenario: datastore module 可解析 user preferences

- **WHEN** Koin 使用 datastore module 啟動
- **THEN** 可以 resolve `UserPreferenceDataSource`

#### Scenario: datastore module 可解析 language provider

- **WHEN** Koin 使用 datastore module 啟動
- **THEN** `LanguageProvider` resolve 到 datastore-backed provider

### Requirement: Android button 驗證

`androidApp` 必須提供簡易 button-based verification path，可以更新 language preference 並觸發 network request。

#### Scenario: button 更新語言並呼叫 network

- **WHEN** 使用者點擊測試 button
- **THEN** app 持久化選取語言，並透過 DI-provided dependencies 執行 TMDB network call

#### Scenario: 驗證結果可見

- **WHEN** button-triggered network call 成功或失敗
- **THEN** app 顯示精簡 status text，指出選取語言與 result 或 error
