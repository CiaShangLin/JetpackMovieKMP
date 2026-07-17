## ADDED Requirements

### Requirement: TMDB HttpClient 基底路徑設定
`shared` 的 `commonMain` MUST 透過 Ktor `HttpClient` 的 `defaultRequest` 設定 TMDB API 的基底路徑（`https://api.themoviedb.org/3/`），且各端點呼叫 MUST 使用不帶開頭斜線的相對路徑，讓實際發出的請求 URL 保留 `/3/` 前綴。

#### Scenario: 相對路徑正確解析成完整 URL
- **WHEN** `MovieApiService` 以相對路徑（例如 `movie/{id}`）呼叫端點
- **THEN** 實際送出的請求 URL 為 `https://api.themoviedb.org/3/movie/{id}`，前綴未被覆蓋或遺漏

#### Scenario: 誤用開頭斜線會被測試攔截
- **WHEN** 開發者不慎以 `/movie/{id}`（開頭帶斜線）呼叫端點
- **THEN** `commonTest` 使用 `ktor-client-mock` 驗證組出的完整 URL 時 MUST 能偵測到 `/3/` 前綴遺失並使測試失敗

### Requirement: 請求自動附加 api_key 與 language 查詢參數
`HttpClient` 的 `defaultRequest` 區塊 MUST 為每一次請求附加 `api_key` 查詢參數，並附加反映當下語言設定的 `language` 查詢參數；`language` 的值 MUST 在每次請求時重新取得，不得於 `HttpClient` 建構當下就固定寫死。

#### Scenario: 每個請求皆附加 api_key
- **WHEN** `MovieApiService` 呼叫任一 TMDB 端點
- **THEN** 實際送出的請求 MUST 帶有 `api_key` 查詢參數

#### Scenario: language 參數反映當下設定
- **WHEN** 語言設定在兩次請求之間發生變化
- **THEN** 第二次請求的 `language` 查詢參數值 MUST 反映變化後的新值，而非沿用第一次請求的舊值

### Requirement: MovieApiService 涵蓋既有 TMDB 端點
`commonMain` MUST 提供 `MovieApiService` 介面與實作，涵蓋舊專案既有的 7 個端點：configuration、genre/movie/list、discover/movie、search/movie、movie/{id}、movie/{id}/recommendations、movie/{id}/credits；每個方法 MUST 回傳對應的 `@Serializable` DTO model，且不進行例外轉換（例外轉換屬於 `MovieRemoteDataSource` 的職責）。

#### Scenario: 呼叫端點回傳對應 DTO
- **WHEN** 呼叫 `MovieApiService.getMovieDetail(id)`
- **THEN** 回傳型別為 `MovieDetailResponse`，欄位對應 TMDB API 實際回應的 JSON 結構

### Requirement: MovieRemoteDataSource 統一錯誤處理
`commonMain` MUST 提供 `MovieRemoteDataSource` 介面與實作，包裹 `MovieApiService` 的呼叫，將例外（逾時、無網路連線、解析失敗等）轉換成結構化的 `NetworkResponse`／`NetworkException` 型別，且不得讓未捕捉的例外往呼叫端外洩；成功時 MUST 將 `MovieApiService` 回傳的 DTO 映射成對應的 external model。

#### Scenario: 呼叫成功時回傳轉換後的 external model
- **WHEN** `MovieApiService` 的底層呼叫成功回傳 DTO
- **THEN** `MovieRemoteDataSource` MUST 回傳包含轉換後 external model 的成功結果

#### Scenario: 呼叫發生例外時回傳結構化錯誤
- **WHEN** 底層 Ktor 呼叫拋出逾時、連線失敗或反序列化例外
- **THEN** `MovieRemoteDataSource` MUST 攔截該例外並回傳對應分類的 `NetworkException`，不得讓例外繼續往外傳播

### Requirement: Koin 提供 network 相關依賴綁定
`commonMain` MUST 提供一個 Koin `networkModule`，綁定 `Json`、`HttpClient`、`MovieApiService`、`MovieRemoteDataSource`，且介面型別 MUST 綁定到對應介面（而非實作類別），讓消費端只依賴介面。

#### Scenario: networkModule 可被完整解析
- **WHEN** 在 Koin test 環境載入 `networkModule` 並檢查所有 binding
- **THEN** `MovieApiService` 與 `MovieRemoteDataSource` 皆可被成功解析為非 null 實例

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
