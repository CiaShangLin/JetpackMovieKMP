## 1. Gradle 依賴設定（shared）

- [ ] 1.1 `shared/build.gradle.kts` 的 `commonMain.dependencies` 新增 `implementation(libs.koin.core)`
- [ ] 1.2 `shared/build.gradle.kts` 新增 `iosMain.dependencies` 區塊（目前完全缺失），加入 `implementation(libs.ktor.client.darwin)`
- [ ] 1.3 確認 `commonTest.dependencies` 已有 `ktor-client-mock`、`koin-test`（catalog 已備妥，僅需確認引用）

## 2. commonMain：HttpClient 與請求設定（network 資料夾）

- [ ] 2.1 在 `shared/src/commonMain/kotlin/.../network/` 建立 `HttpClient` 建構程式碼，使用 `defaultRequest { url("https://api.themoviedb.org/3/") }` 設定基底路徑
- [ ] 2.2 在 `defaultRequest{}` 內加入 `parameter("api_key", ...)`、`parameter("language", ...)`，language 需每次請求動態取值
- [ ] 2.3 加入 `install(ContentNegotiation) { json(...) }`（沿用 commonMain 既有的 kotlinx.serialization Json 設定）
- [ ] 2.4 加入 `install(Logging) { level = ... }`，debug/release 行為對應舊專案的 `HttpLoggingInterceptor`
- [ ] 2.5 確認 `commonMain` 的 `HttpClient` 建構程式碼未 import 任何平台專屬 engine（CIO／Darwin）

## 3. DTO Model

- [ ] 3.1 建立 `ConfigurationResponse`、`MovieGenreResponse`、`DiscoverMovieResponse`、`SearchMovieResponse`、`MovieDetailResponse`、`MovieRecommendResponse`、`MovieCastAndCrewResponse`，皆標註 `@Serializable`
- [ ] 3.2 對照舊專案 `core/network/model/*.kt` 的欄位形狀，確認命名與巢狀結構一致

## 4. MovieApiService（純 API 呼叫層）

- [ ] 4.1 定義 `MovieApiService` 介面，涵蓋 7 個端點（configuration、genre/movie/list、discover/movie、search/movie、movie/{id}、movie/{id}/recommendations、movie/{id}/credits）
- [ ] 4.2 實作 `MovieApiServiceImpl`，端點路徑一律使用「不帶開頭斜線」的相對路徑
- [ ] 4.3 確認每個方法回傳對應 DTO，不做例外捕捉或轉換

## 5. MovieRemoteDataSource（錯誤處理層）

- [ ] 5.1 建立 `NetworkResponse`／`NetworkException`（沿用舊專案命名，暫留在 network scope，不搬到共用層）
- [ ] 5.2 實作 `safeApiCall`／`mapData` extension function，對應舊專案 `NetworkExtension.kt` 的行為
- [ ] 5.3 定義 `MovieRemoteDataSource` 介面與 `MovieRemoteDataSourceImpl`，包裹 `MovieApiService` 並轉換成 external model

## 6. Koin DI 綁定

- [ ] 6.1 建立 `networkModule`（`org.koin.dsl.module`），綁定 `Json`、`HttpClient`、`MovieApiService`、`MovieRemoteDataSource`
- [ ] 6.2 確認介面型別綁定到介面（`single<MovieApiService> { ... }`），消費端不得依賴實作類別

## 7. 平台 Engine 分流

- [ ] 7.1 `androidMain` 提供 CIO engine 給 `HttpClient`（沿用既有 `ktor-client-cio` 依賴）
- [ ] 7.2 `iosMain` 提供 Darwin engine 給 `HttpClient`（對應本次新增的 `ktor-client-darwin` 依賴）
- [ ] 7.3 以 expect/actual 或 Koin 平台模組實作 engine 分流，確認 `commonMain` 不依賴具體 engine 型別

## 8. 測試（commonTest）

- [ ] 8.1 使用 `ktor-client-mock` 驗證至少一個端點的完整請求 URL（含 `/3/` 前綴），涵蓋「開頭斜線覆蓋 base path」的迴歸情境
- [ ] 8.2 驗證每個請求皆帶有 `api_key` 查詢參數；驗證 `language` 參數在兩次請求間隨設定變化而更新
- [ ] 8.3 針對 `MovieRemoteDataSource` 撰寫成功情境測試（DTO 正確映射成 external model）
- [ ] 8.4 針對 `MovieRemoteDataSource` 撰寫例外情境測試（逾時、連線失敗、反序列化失敗皆轉換成對應 `NetworkException`，不拋出未捕捉例外）
- [ ] 8.5 使用 Koin test 驗證 `networkModule` 可完整解析出 `MovieApiService`、`MovieRemoteDataSource`
- [ ] 8.6 確認整體單元測試覆蓋率達 80% 以上（AAA 模式）

## 9. 收尾驗證

- [ ] 9.1 執行 `ktlintCheck` 並修正格式問題
- [ ] 9.2 確認 Android 與 iOS 的主要編譯路徑（metadata/framework compile）成功
- [ ] 9.3 檢查本次未引入 Chucker、`core:common`、`core:network` Gradle module（維持本次刻意排除的範圍）
