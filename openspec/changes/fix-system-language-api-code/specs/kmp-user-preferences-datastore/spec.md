## ADDED Requirements

### Requirement: 語言設定必須同步內容語言與 Android App 文案

使用者在 Android 設定頁變更 `LanguageMode` 後，系統 MUST 以同一個選擇更新 datastore-backed `LanguageProvider` 與 Android application locale。TMDB 請求的 `language` 參數及 Compose／Android resource 文案 MUST 反映相同的有效 Locale。

#### Scenario: 切換為繁體中文

- **WHEN** 使用者將 `LanguageMode` 變更為 `TRADITIONAL_CHINESE`
- **THEN** 後續 TMDB 請求的 `language` 參數為 `zh-TW`
- **AND** Android App 的 `stringResource()` 文案使用繁體中文資源

#### Scenario: 切換為英文

- **WHEN** 使用者將 `LanguageMode` 變更為 `ENGLISH`
- **THEN** 後續 TMDB 請求的 `language` 參數為 `en-US`
- **AND** Android App 的 `stringResource()` 文案使用英文資源

### Requirement: Network module 必須由外部提供語言 provider

`shared:network` 的 `networkModule` MUST 要求呼叫端預先註冊 `LanguageProvider`，且 MUST NOT 提供固定語言的 fallback implementation。正式 App MUST 使用 `shared:datastore` 的 `DatastoreLanguageProvider` 作為此依賴。

#### Scenario: 正式 Koin 組裝使用 datastore-backed provider

- **WHEN** Android 或 iOS 透過 `initKoin()` 啟動
- **THEN** `MovieDataSource` 使用的 `LanguageProvider` 為 `DatastoreLanguageProvider`

#### Scenario: 缺少 language provider 時明確失敗

- **WHEN** 僅安裝 `networkModule` 並要求解析 `MovieDataSource`
- **THEN** Koin 因缺少 `LanguageProvider` 而明確失敗

## MODIFIED Requirements

### Requirement: Datastore Koin module

`shared/datastore` MUST 提供 Koin datastore module，可以 resolve user preference data source，並將 `shared:common` 的 `LanguageProvider`、`BaseHostUrlProvider` 介面綁定到 datastore-backed implementation。這些 datastore-backed provider（`DatastoreLanguageProvider`、`DatastoreBaseHostUrlProvider`）MUST 定義在 `shared:datastore` 模組內，且依賴 `shared:common` 的 `commonModule()` 提供的 `CoroutineScope`，因此 datastore module MUST 搭配 `commonModule` 一起安裝才能完整解析；`shared:datastore` MUST NOT 依賴 `shared:network`。

`DatastoreLanguageProvider` 在 `LanguageMode.SYSTEM_DEFAULT` 時 MUST 取得平台目前 Locale 的語言及可用地區資訊，並提供 TMDB 相容的 BCP 47 語言代碼；例如系統語言為繁體中文（台灣）時，代碼 MUST 為 `zh-TW`。Locale 缺少地區資訊時 MUST 保留可取得的語言代碼；完全無法取得系統語言時 MUST 使用既有 `zh-TW` fallback。

#### Scenario: datastore module 搭配 common module 可解析 user preferences

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** 可以 resolve `UserPreferenceDataSource`

#### Scenario: datastore module 搭配 common module 可解析 language provider

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** `LanguageProvider` resolve 到 `shared:datastore` 模組內的 `DatastoreLanguageProvider`

#### Scenario: 系統繁體中文台灣會保留地區碼

- **WHEN** `LanguageMode.SYSTEM_DEFAULT` 對應的 platform Locale 為繁體中文且地區為台灣
- **THEN** `DatastoreLanguageProvider` 提供 `zh-TW`
- **AND** 透過該 provider 建立的 TMDB 請求 `language` 參數為 `zh-TW`

#### Scenario: 系統 Locale 缺少地區碼

- **WHEN** `LanguageMode.SYSTEM_DEFAULT` 對應的 platform Locale 只提供語言代碼
- **THEN** `DatastoreLanguageProvider` 使用該語言代碼

#### Scenario: 無法取得系統 Locale

- **WHEN** `LanguageMode.SYSTEM_DEFAULT` 且 platform 無法取得系統語言代碼
- **THEN** `DatastoreLanguageProvider` 使用 `zh-TW` 作為 fallback

#### Scenario: datastore module 優先提供安全的 base host url provider

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動，且 configuration 同時包含 HTTP `baseUrl` 與 HTTPS `secureBaseUrl`
- **THEN** `BaseHostUrlProvider` resolve 到 `DatastoreBaseHostUrlProvider`，並回傳 HTTPS `secureBaseUrl`

#### Scenario: secure base host 缺失時升級 fallback host

- **WHEN** configuration 的 `secureBaseUrl` 為空字串且 `baseUrl` 為 HTTP URL
- **THEN** `DatastoreBaseHostUrlProvider` MUST 回傳相同 host 與 path 的 HTTPS URL

#### Scenario: 缺少 common module 時 datastore-backed provider 無法解析

- **WHEN** Koin 只安裝 datastore module、未安裝 `commonModule()`
- **THEN** 解析 datastore-backed `LanguageProvider`／`BaseHostUrlProvider` 會因缺少 `CoroutineScope` binding 而失敗

#### Scenario: shared:datastore 不依賴 shared:network

- **WHEN** 檢查 `shared/datastore/build.gradle.kts` 的 dependencies 區塊
- **THEN** 不包含 `projects.shared.network`，`DatastoreLanguageProvider`／`DatastoreBaseHostUrlProvider` 的實作完全在 `shared:datastore` 模組內部完成，不需要 import 任何 `shared:network` 的型別
