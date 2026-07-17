# ios-koin-bootstrap Specification

## Purpose
TBD - created by archiving change migrate-common-providers-to-commonmain. Update Purpose after archive.

## Requirements
### Requirement: shared 提供跨平台共用的 Koin 啟動進入點

`shared/commonMain` MUST 提供一個 `initKoin(dataStore, isDebug, appDeclaration)` 函式，內部呼叫 `startKoin`，並安裝 `commonModule()`、`datastoreModule(dataStore)`、`networkModule(isDebug, provideDefaultLanguageProvider = false)`；`androidApp`、`iosApp` MUST 都透過這個進入點啟動 Koin，不得各自重複組裝 module 清單。

#### Scenario: initKoin 安裝三個必要 module
- **WHEN** 呼叫 `initKoin(dataStore, isDebug = true) {}`
- **THEN** Koin 啟動後可以 resolve `CoroutineScope`、`UserPreferenceDataSource`、`MovieDataSource`、datastore-backed `LanguageProvider`、datastore-backed `BaseHostUrlProvider`

#### Scenario: androidApp 透過 initKoin 啟動並帶入 androidContext
- **WHEN** `JetpackMovieApplication.onCreate()` 執行
- **THEN** 呼叫 `initKoin(...)` 並透過 `appDeclaration` 帶入 `androidContext(this)`，不再直接呼叫 `startKoin { ... }` 組裝 module

### Requirement: iOS app 啟動時初始化 Koin

`iosApp` MUST 在 app 啟動流程（`iOSApp` 的 `init`）呼叫 shared 匯出的 `initKoin(...)`，讓 `commonModule`、`datastoreModule`、`networkModule` 在 iOS 上可被解析，不再維持目前完全未初始化 Koin 的狀態。

#### Scenario: iOS app 啟動後 Koin 可解析 shared 依賴
- **WHEN** `iOSApp` 完成初始化
- **THEN** shared 的 `MovieDataSource`、`LanguageProvider`、`BaseHostUrlProvider` 皆可透過 Koin 成功解析，不拋出 `NoBeanDefFoundException` 等未安裝例外
