## 1. Gradle 依賴設定（shared）

- [x] 1.1 `shared/build.gradle.kts` 的 `commonMain.dependencies` 新增 `implementation(libs.koin.core)`
- [x] 1.2 `shared/build.gradle.kts` 新增 `iosMain.dependencies` 區塊（目前完全缺失），加入 `implementation(libs.ktor.client.darwin)`
- [x] 1.3 確認 `commonTest.dependencies` 已有 `ktor-client-mock`、`koin-test`（catalog 已備妥，僅需確認引用）

## 2. commonMain：HttpClient 與請求設定（network 資料夾）

- [x] 2.1 在 `shared/src/commonMain/kotlin/.../network/` 建立 `HttpClient` 建構程式碼，使用 `defaultRequest { url("https://api.themoviedb.org/3/") }` 設定基底路徑
- [x] 2.2 在 `defaultRequest{}` 內加入 `parameter("api_key", ...)`、`parameter("language", ...)`，language 需每次請求動態取值（新增 `LanguageProvider` 介面 + 暫時性 `DefaultLanguageProvider` 實作〔固定回傳 `zh-TW`，待 DataStore 語言設定模組完成後替換〕，透過 Koin 注入到 `createHttpClient`，`defaultRequest{}` 每次請求都會重新呼叫 `getLanguageCode()`——已用 Ktor 原始碼確認 `DefaultRequest` 是掛在 `requestPipeline.intercept` 上，並非只在建構時執行一次）
- [x] 2.3 加入 `install(ContentNegotiation) { json(...) }`（沿用 commonMain 既有的 kotlinx.serialization Json 設定）
- [x] 2.4 加入 `install(Logging) { level = ... }`，debug/release 行為對應舊專案的 `HttpLoggingInterceptor`（`networkModule` 改為 `fun networkModule(isDebug: Boolean)`：debug 用 `LogLevel.INFO`〔對應舊專案的 `Level.BASIC`〕，release 用 `LogLevel.NONE`；Android 端已在 `JetpackMovieApplication` 接上 `startKoin`，`isDebug` 用執行期的 `applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE` 判斷，避免與 `shared` 模組既有的 `BuildConfig` 撞名；iOS 端刻意未處理，留待後續 change）
- [x] 2.5 確認 `commonMain` 的 `HttpClient` 建構程式碼未 import 任何平台專屬 engine（CIO／Darwin）

## 3. DTO Model

- [x] 3.1 建立 `ConfigurationResponse`、`MovieGenreResponse`、`DiscoverMovieResponse`、`SearchMovieResponse`、`MovieDetailResponse`、`MovieRecommendResponse`、`MovieCastAndCrewResponse`，皆標註 `@Serializable`
- [x] 3.2 對照舊專案 `core/network/model/*.kt` 的欄位形狀，確認命名與巢狀結構一致（逐檔比對過，欄位與 `@SerialName` 皆一致）

## 4. MovieDataSource（統一 API 呼叫與錯誤處理層）

> 與原規劃不同：不拆出獨立的 `MovieApiService`（純 API 呼叫層），改為單一 `MovieDataSource` 直接呼叫 Ktor `HttpClient`，並在同一層做錯誤轉換與 DTO → external model 映射。理由見 `design.md` 決策 3：Ktor 的 `HttpResponse` 本身可被 `ktor-client-mock` 直接替換，兩層拆分沒有帶來額外可測試性。

- [x] 4.1 定義 `MovieDataSource` 介面，涵蓋 7 個端點（configuration、genre/movie/list、discover/movie、search/movie、movie/{id}、movie/{id}/recommendations、movie/{id}/credits）
- [x] 4.2 實作 `MovieDataSourceImpl`，直接呼叫 Ktor `HttpClient`，端點路徑一律使用「不帶開頭斜線」的相對路徑
- [x] 4.3 建立 `NetworkResponse`／`NetworkException`（沿用舊專案命名，暫留在 network scope，不搬到共用層）
- [x] 4.4 實作 `safeApiCall`／`mapData` extension function，對應舊專案 `NetworkExtension.kt` 的行為
- [x] 4.5 每個方法透過 `safeApiCall` 呼叫、`mapData` 轉換成對應 external model（Bean），例外一律轉換成 `NetworkException`，不外洩未捕捉例外

## 5. Koin DI 綁定

- [x] 5.1 建立 `networkModule`（`org.koin.dsl.module`），綁定 `HttpClient`、`MovieDataSource`（`Json` 只在 `HttpClient` 建構時內部使用，不需獨立綁定）
- [x] 5.2 確認介面型別綁定到介面（`single<MovieDataSource> { ... }`），消費端不得依賴實作類別

## 6. 平台 Engine 分流

- [x] 6.1 `androidMain` 提供 CIO engine 給 `HttpClient`（沿用既有 `ktor-client-cio` 依賴，`commonMain` 未指定 engine，交由 Ktor 自動偵測）
- [x] 6.2 `iosMain` 提供 Darwin engine 給 `HttpClient`（對應本次新增的 `ktor-client-darwin` 依賴；Ktor 在 Kotlin/Native 上透過 `engines.firstOrNull()` 自動偵測，不需要 expect/actual 或明確指定 `HttpClient(Darwin)`）
- [x] 6.3 以 expect/actual 或 Koin 平台模組實作 engine 分流，確認 `commonMain` 不依賴具體 engine 型別（本次採用「per-platform 依賴 + Ktor 自動偵測」，不需要額外的 expect/actual 程式碼）

## 7. 測試（commonTest）

- [x] 7.1 使用 `ktor-client-mock` 驗證至少一個端點的完整請求 URL（含 `/3/` 前綴），涵蓋「開頭斜線覆蓋 base path」的迴歸情境（`MovieDataSourceImplTest` 驗證 `getConfiguration` 完整 URL，另有專門的 `leading_slash_path_drops_the_3_base_path_prefix_regression_guard` 測試；`MovieDataSourceImplEndpointsTest` 驗證其餘 6 個端點的 URL 前綴）
- [x] 7.2 驗證每個請求皆帶有 `api_key` 查詢參數；驗證 `language` 參數在兩次請求間隨設定變化而更新（`getConfiguration_request_includes_api_key_query_parameter`、`language_parameter_reflects_current_value_across_requests`；這段邏輯是所有端點共用的 `configureMovieClient`，測一次即可涵蓋全部端點）
- [x] 7.3 針對 `MovieDataSource` 撰寫成功情境測試（DTO 正確映射成 external model）（7 個端點全部涵蓋：`MovieDataSourceImplTest` 測 `getConfiguration`，`MovieDataSourceImplEndpointsTest` 測其餘 6 個）
- [x] 7.4 針對 `MovieDataSource` 撰寫例外情境測試（逾時、連線失敗、反序列化失敗皆轉換成對應 `NetworkException`，不拋出未捕捉例外）（HttpError／ParseError／TimeoutError／ConnectionError 四種情境皆有測試，透過 `getConfiguration` 驗證，因為 `safeApiCall`／`toNetworkException` 是所有端點共用的同一段程式碼）
- [x] 7.5 使用 Koin test 驗證 `networkModule` 可完整解析出 `MovieDataSource`（`NetworkModuleTest`，用 `KoinTest` + `by inject()` 驗證非 null）
- [x] 7.6 確認整體單元測試覆蓋率達 80% 以上（AAA 模式）——新增 `kover`（0.9.8）catalog alias + plugin，只套在 `shared/build.gradle.kts`。`koverVerify` 對本次新增的業務邏輯套件（`network.di`／`network.datasource`／`network.extension`／`network.provider`）強制 ≥80% line coverage，實測皆為 100%（共 18 個測試案例）；`network.model`（DTO 樣板）與 `shared` 其他既有程式碼不在此次 gate 範圍內，理由見 `design.md` 新增的 risk 條目

## 8. 收尾驗證

- [x] 8.1 執行 `ktlintCheck` 並修正格式問題
- [ ] 8.2 確認 Android 與 iOS 的主要編譯路徑（metadata/framework compile）成功（`compileAndroidMain`、`compileCommonMainKotlinMetadata`、`compileIosMainKotlinMetadata`、`compileKotlinIosSimulatorArm64`〔真正的 Kotlin/Native klib compile，非僅 metadata〕皆已驗證成功；最後產生 `.framework` 的 link 步驟需要 macOS + Xcode 工具鏈，目前 Windows 開發環境無法驗證，待有 macOS 環境時補驗）
- [x] 8.3 檢查本次未引入 Chucker、`core:common`、`core:network` Gradle module（維持本次刻意排除的範圍）
