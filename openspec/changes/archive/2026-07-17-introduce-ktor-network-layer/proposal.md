## Why

`shared` 模組目前只有 `Greeting.kt`／`Platform.kt` 的骨架程式碼，尚未有任何實際網路串接。舊專案（`JetpackMovieCompose`）已用 Retrofit + OkHttp + Hilt 串接 TMDB API，但 `kmp-dependency-catalog` spec 已明確排除 Retrofit/OkHttp/Hilt，改採 Ktor + Koin。這次要把舊專案既有的電影資料串接邏輯，用 Ktor 在 `commonMain` 重新實作一次，讓 Android/iOS 能共用同一套網路層，並為之後把 network 邏輯切成獨立 `core:network` module 打基礎。

## What Changes

- 在 `shared/src/commonMain/kotlin` 新增 `network` 套件，設定 Ktor `HttpClient`：以 `defaultRequest { url(...) }` 取代 Retrofit 的 `baseUrl`，並在同一區塊帶入 `api_key`／`language` 預設查詢參數（取代舊專案的 `ApiKeyInterceptor`／`LanguageInterceptor`），搭配 `ContentNegotiation`（kotlinx.serialization）與 `Logging` plugin（取代 `HttpLoggingInterceptor`）
- 新增 `MovieDataSource`（+ Impl）：對應舊專案 `MovieApiService` 的 7 個端點（configuration、genre/movie/list、discover/movie、search/movie、movie/{id}、movie/{id}/recommendations、movie/{id}/credits），直接呼叫 Ktor `HttpClient`，並在同一層用 `safeApiCall`／`mapData` 做錯誤轉換與 DTO → external model 映射（行為與命名維持與舊專案的 `safeApiCall`／`NetworkResponse`／`NetworkException` 相近，這次不重新命名、不搬到共用層）；不額外拆出純 API 呼叫層（`MovieApiService`），因為 Ktor 的 `HttpResponse` 本身可直接被 `ktor-client-mock` 測試替身注入，兩層拆分沒有帶來額外可測試性（詳見 design.md 決策 3）
- 新增對應 TMDB response 的 DTO model，改用 `kotlinx.serialization` 的 `@Serializable`（取代舊專案的 Gson）
- 導入 Koin：撰寫 `networkModule`，以 `single<Interface> { Impl(get()) }` 的形式取代 Hilt 的 `NetworkModule`／`DataSourceModule`
- 補上 `shared/build.gradle.kts` 的 `iosMain.dependencies`（目前完全空缺），加入 `ktor-client-darwin`；`HttpClientEngine` 依平台分流（Android 用既有的 CIO，iOS 用新增的 Darwin），`commonMain` 不依賴具體 engine
- **BREAKING**：無（`shared` 目前無對外使用者，androidApp/iosApp 尚未消費這些型別）

**不在本次範圍**（留待後續 change）：
- 不會把 `network` 相關程式碼切成獨立的 `core:network` Gradle module，先留在 `shared/commonMain/network` 資料夾
- 不會抽出 `core:common`；`NetworkResponse`／`NetworkException` 暫留在 network scope 內，不重新命名
- 不會處理 Chucker 或其他 Android-only network inspector（維持 `kmp-dependency-catalog` design.md 的既有決定）
- 不會建立 Repository / UseCase / Compose UI 層，僅止於 DataSource 層

## Capabilities

### New Capabilities
- `ktor-movie-network`：`shared/commonMain` 中透過 Ktor 呼叫 TMDB API 的網路層能力，涵蓋 HttpClient 設定、單一 MovieDataSource 層、錯誤處理與 Koin DI 綁定

### Modified Capabilities

（無。`kmp-dependency-catalog` 的「Ktor 網路 catalog 契約」需求本來就已涵蓋 Ktor aliases 的存在，這次只是實際使用這些依賴，並未改變該 spec 的 requirement。）

## Impact

- **受影響 module**：`shared`（新增 `commonMain`/`androidMain`/`iosMain` 程式碼與依賴設定）。`androidApp`／`iosApp` 本次不受影響（尚未消費新程式碼）。
- **依賴異動**（`gradle/libs.versions.toml`，本專案使用 Version Catalog，不涉及 buildSrc）：
  - `commonMain` 已有 `ktor-client-core`／`ktor-client-content-negotiation`／`ktor-client-logging`／`ktor-serialization-kotlinx-json`，需新增使用 `koin-core`（alias 已存在，尚未在 `shared` 引用）
  - `androidMain` 已有 `ktor-client-cio`，無需新增
  - `iosMain` 目前無任何依賴宣告，需新增 `implementation(libs.ktor.client.darwin)`（alias 已存在於 catalog）
- **測試**：新增 `commonTest` 對 `MovieDataSource` 的單元測試，使用既有的 `ktor-client-mock` alias，採 AAA 模式，目標覆蓋率 ≥ 80%
