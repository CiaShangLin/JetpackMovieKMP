# ktor-movie-network Specification

## Purpose
TBD - created by archiving change introduce-ktor-network-layer. Update Purpose after archive.
## Requirements
### Requirement: TMDB HttpClient 基底路徑設定
`shared` 的 `commonMain` MUST 透過 Ktor `HttpClient` 的 `defaultRequest` 設定 TMDB API 的基底路徑（`https://api.themoviedb.org/3/`），且各端點呼叫 MUST 使用不帶開頭斜線的相對路徑，讓實際發出的請求 URL 保留 `/3/` 前綴。

#### Scenario: 相對路徑正確解析成完整 URL
- **WHEN** `MovieDataSource` 以相對路徑（例如 `movie/{id}`）呼叫端點
- **THEN** 實際送出的請求 URL 為 `https://api.themoviedb.org/3/movie/{id}`，前綴未被覆蓋或遺漏

#### Scenario: 誤用開頭斜線會被測試攔截
- **WHEN** 開發者不慎以 `/movie/{id}`（開頭帶斜線）呼叫端點
- **THEN** `commonTest` 使用 `ktor-client-mock` 驗證組出的完整 URL 時 MUST 能偵測到 `/3/` 前綴遺失並使測試失敗

### Requirement: Request query parameters 包含 api_key 與 datastore-backed language
`HttpClient` request configuration 必須在每個 TMDB request 附加 `api_key`，並必須從 DI-provided `LanguageProvider` 附加 `language`。在 production DI 中，`LanguageProvider` 必須由 user preferences datastore 提供，而不是固定 default provider。

#### Scenario: 每個 request 都包含 api_key
- **WHEN** `MovieDataSource` 呼叫任一 TMDB endpoint
- **THEN** outgoing request 包含 `api_key` query parameter

#### Scenario: language 來自 datastore-backed provider
- **WHEN** `UserPreferenceDataSource` 持久化 `LanguageMode.ENGLISH`
- **THEN** 後續 network request 包含 `language=en-US`

#### Scenario: language 可跨 request 變更
- **WHEN** persisted language 從 English 變更為 Traditional Chinese
- **THEN** 後續 network requests 使用 `language=zh-TW`，且不需要重新建立 `MovieDataSource`

### Requirement: MovieDataSource 涵蓋既有 TMDB 端點並統一錯誤處理
`commonMain` MUST 提供 `MovieDataSource` 介面與實作，涵蓋舊專案既有的 7 個端點：configuration、genre/movie/list、discover/movie、search/movie、movie/{id}、movie/{id}/recommendations、movie/{id}/credits；每個方法 MUST 透過 `safeApiCall` 呼叫 Ktor `HttpClient`，將例外（逾時、無網路連線、解析失敗等）轉換成結構化的 `NetworkResponse`／`NetworkException` 型別，且不得讓未捕捉的例外往呼叫端外洩；成功時 MUST 透過 `mapData` 將底層 DTO 映射成對應的 external model。不額外拆出純 API 呼叫層（`MovieApiService`）。

#### Scenario: 呼叫成功時回傳轉換後的 external model
- **WHEN** 呼叫 `MovieDataSource.getMovieDetail(id)` 且底層 Ktor 呼叫成功回傳 DTO
- **THEN** 回傳的 `NetworkResponse` 內含轉換後的 `MovieDetailBean`，欄位對應 TMDB API 實際回應的 JSON 結構

#### Scenario: 呼叫發生例外時回傳結構化錯誤
- **WHEN** 底層 Ktor 呼叫拋出逾時、連線失敗或反序列化例外
- **THEN** `MovieDataSource` MUST 攔截該例外並回傳對應分類的 `NetworkException`，不得讓例外繼續往外傳播

### Requirement: Koin 提供 network 相關依賴綁定
`commonMain` MUST 提供一個 Koin `networkModule`，綁定 `HttpClient`、`MovieDataSource`，且介面型別 MUST 綁定到對應介面（而非實作類別），讓消費端只依賴介面。

#### Scenario: networkModule 可被完整解析
- **WHEN** 在 Koin test 環境載入 `networkModule` 並檢查所有 binding
- **THEN** `MovieDataSource` 可被成功解析為非 null 實例

### Requirement: 平台專屬 HttpClientEngine 分流
`androidMain` MUST 使用 `ktor-client-cio` 提供的 CIO engine，`iosMain` MUST 使用 `ktor-client-darwin` 提供的 Darwin engine；`commonMain` 的程式碼不得直接依賴任何具體 engine 型別。

#### Scenario: Android 使用 CIO engine
- **WHEN** 在 Android 平台建立 `HttpClient`
- **THEN** 實際使用的 engine 型別為 CIO

#### Scenario: iOS 使用 Darwin engine
- **WHEN** 在 iOS 平台建立 `HttpClient`
- **THEN** 實際使用的 engine 型別為 Darwin

#### Scenario: commonMain 不指定具體 engine
- **WHEN** 檢查 `commonMain` 中建立 `HttpClient` 的程式碼
- **THEN** 該程式碼不 import 或參照任何平台專屬 engine 類別（如 `CIO`、`Darwin`）

