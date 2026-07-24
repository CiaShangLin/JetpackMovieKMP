## MODIFIED Requirements

### Requirement: commonMain 提供中立的跨層共用型別

`shared/common` 的 `commonMain` MUST 在 `com.shang.jetpackmoviekmp.common` package 提供 `LanguageProvider`、`BaseHostUrlProvider`、`NetworkException`、`AppResult`、`AppError` 型別定義；該 package MUST NOT 依賴 `network` 或 `datastore` package 底下的任何型別，確保依賴方向永遠是消費端（`shared:network`、`shared:datastore`）依賴 `shared:common`，而非反向。

#### Scenario: common package 不依賴 network 或 datastore
- **WHEN** 檢查 `shared:common` 模組底下所有檔案的 import
- **THEN** 不包含任何 `com.shang.jetpackmoviekmp.network.*` 或 `com.shang.jetpackmoviekmp.datastore.*` 型別，且 `shared/common/build.gradle.kts` 不依賴 `projects.shared.network` 或 `projects.shared.datastore`

#### Scenario: LanguageProvider 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.LanguageProvider`
- **THEN** 該型別存在於 `shared:common` 模組，且僅定義 `getLanguageCode(): String`

#### Scenario: BaseHostUrlProvider 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.BaseHostUrlProvider`
- **THEN** 該型別存在於 `shared:common` 模組，且僅定義 `getBaseHostUrl(): String`

#### Scenario: NetworkException 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.NetworkException`
- **THEN** 該型別存在於 `shared:common` 模組，包含 `HttpError`、`ConnectionError`、`TimeoutError`、`ParseError`、`UnknownError` 子型別，且 `Throwable.toNetworkException()` extension 行為與模組化前一致

#### Scenario: AppResult 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.AppResult`
- **THEN** 該型別存在於 `shared:common` 模組，為 `sealed interface AppResult<out T>`，包含 `Success<T>(val data: T)` 與 `Failure(val error: AppError)` 兩個子型別，可被 SKIE 明確匯出成 Swift enum

#### Scenario: AppError 定義位於 common，本次只包含 Network 與 Unknown
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.AppError`
- **THEN** 該型別存在於 `shared:common` 模組，為 `sealed interface AppError`，包含 `Network(val exception: NetworkException)` 與 `Unknown` 兩個子型別；不包含 `Database`／`LocalStorage` 等本次尚無實際呼叫路徑驗證的分類
