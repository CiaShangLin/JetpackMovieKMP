## Context

`shared` 模組目前是空殼（只有 `Greeting.kt`／`Platform.kt`），`gradle/libs.versions.toml` 已經備妥 Ktor 3.5.1、Koin 4.2.2 的 catalog aliases，但都還沒被實際使用。舊專案 `JetpackMovieCompose` 的 `core/network` 模組已用 Retrofit + OkHttp + Hilt 完整串接 TMDB API（configuration、genre、discover、search、movie detail、recommendations、credits 共 7 個端點），這次要把同樣的串接邏輯用 Ktor + Koin 在 `shared/commonMain` 重新實作一次。

本 change 只處理「資料夾內的程式碼」（`shared/src/commonMain/kotlin/.../network/`），不建立獨立的 `core:network` Gradle module；模組化留待下一個 change，等這次的程式碼邏輯驗證過、測試通過後再處理 Gradle 層級的切分。

## Goals / Non-Goals

**Goals:**
- 在 `commonMain` 建立可運作、可測試的 Ktor 網路層，涵蓋 TMDB 既有的 7 個端點
- 錯誤處理行為與命名盡量貼近舊專案的 `safeApiCall`／`NetworkResponse`／`NetworkException`，降低轉換過程的認知負擔
- 補齊 iOS 端目前完全缺失的 Ktor engine 依賴（`ktor-client-darwin`），讓 iOS 也能實際發出網路請求
- 導入 Koin 作為本專案第一次的 DI 綁定，範圍僅限 network 相關型別

**Non-Goals:**
- 不建立 `core:network` Gradle module（下一個 change 處理）
- 不建立 `core:common`；錯誤型別暫留在 network scope，不重新命名、不抽出共用層
- 不處理 Repository、UseCase 或任何 Compose UI 層的串接
- 不導入 Chucker 或其他 Android-only network inspector（沿用 `kmp-dependency-catalog` design.md 既有決定）
- 不解決 TMDB API Key 的跨平台（含 iOS）安全注入問題（見 Open Questions）

## Decisions

**遵循的既有模式**：本次只落地 Data Source 層，尚未建立 Repository／Use Case，因此嚴格來說還沒有完整的 Repository 模式；但型別介面（`MovieApiService`、`MovieRemoteDataSource`）刻意設計成未來能被 Repository 包一層而不需修改，符合 `kmp-dependency-catalog` spec 對 core 分層（model/domain/data/network）的既有方向。

**1. Base URL 用 `defaultRequest { url(...) }` 取代 Retrofit 的 `baseUrl`**
`HttpClient` 建構時以 `defaultRequest { url("https://api.themoviedb.org/3/") }` 設定基底路徑，各端點呼叫一律使用「不帶開頭 `/`」的相對路徑（例如 `client.get("movie/$id")`）。
- 考慮過的替代方案：Ktor `Resources` plugin（`@Resource` 宣告式端點，寫法更接近 Retrofit 介面）。不採用，因為目前只有 7 個端點，多引入一個 plugin 的維護成本大於好處（YAGNI）；未來端點數量明顯增加時可重新評估。

**2. `api_key`／`language` 用 `defaultRequest{}` 內的 `parameter()` 取代 OkHttp Interceptor**
`defaultRequest{}` 這個 block 會在每次請求時重新執行，因此動態語言代碼（`languageProvider.getLanguageCode()`）的行為與舊專案的 `LanguageInterceptor` 等價，不需要額外的攔截器類別。`HttpLoggingInterceptor` 則對應 Ktor 內建的 `Logging` plugin。
- 考慮過的替代方案：用 Ktor 的 `createClientPlugin` 自訂 plugin 模擬 Interceptor 行為。不採用，目前需求（附加固定/動態查詢參數）用 `defaultRequest` 就能滿足，自訂 plugin 是不必要的複雜度。

**3. 保留 ApiService → RemoteDataSource 兩層，錯誤處理沿用舊命名**
`MovieApiService`（+ Impl）只負責發出請求、回傳原始 DTO；`MovieRemoteDataSource`（+ Impl）包一層 `safeApiCall`／`mapData`，把例外轉換成 `NetworkResponse`／`NetworkException` 並映射成 external model。命名維持與舊專案一致，不改名為 `AppResult`／`AppError`，也不搬到尚未存在的 `core:common`。
- 考慮過的替代方案：(a) 直接合併成一層，`RemoteDataSource` 內部直接呼叫 Ktor——不採用，會讓純 API 呼叫與錯誤轉換邏輯耦合，不利個別測試；(b) 現在就把錯誤型別抽到 `core:common`——不採用，目前只有 network 一個消費者，屬於預先設計還用不到的抽象，等 `core:database` 這類第二個消費者出現時再決定是否搬遷（技術債已在 proposal 中明確記錄）。

**4. DI 改用 Koin，以 `single<Interface> { Impl(get()) }` 取代 Hilt `@Binds`/`@Provides`**
新增 `networkModule`（`org.koin.dsl.module`），提供 `Json`、`HttpClient`、`MovieApiService`、`MovieRemoteDataSource` 四個 binding。這是本專案第一次實際使用 Koin，範圍僅限本次新增的型別，不涉及既有程式碼。

**5. iOS engine 缺口：新增 `iosMain.dependencies { implementation(libs.ktor.client.darwin) }`**
目前 `shared/build.gradle.kts` 完全沒有 `iosMain.dependencies` 區塊。`HttpClientEngine` 依平台分流：`androidMain` 沿用既有的 `ktor-client-cio`，`iosMain` 新增 `ktor-client-darwin`；`commonMain` 的 `HttpClient` 建構程式碼不得指定具體 engine 型別，遵循 `kmp-dependency-catalog` spec 既有規則。

## Risks / Trade-offs

- **[Risk]** `defaultRequest` 的「端點路徑開頭多打一個 `/` 會覆蓋掉 base path」是容易誤踩的坑，一旦踩到會讓所有請求打到錯誤路徑但不一定立即報錯（可能得到 404 才發現）。
  → **Mitigation**：commonTest 用 `ktor-client-mock` 針對至少一個端點驗證實際組出的完整 URL（含 `/3/` 前綴），而不是只驗證回傳的 DTO 內容。

- **[Risk]** `NetworkResponse`／`NetworkException` 暫留在 network scope、未抽到共用層，若之後 `core:database` 也需要一致的錯誤語言，屆時需要一次搬遷＋改名，屬於刻意接受的技術債。
  → **Mitigation**：已在 proposal 的「不在本次範圍」明確記錄，非本次疏漏；下一個涉及 core:database 的 change 需重新評估。

- **[Risk]** TMDB API Key 目前只知道「不進版控」，但舊專案的 `BuildConfig` 讀 `key.properties` 是 Android-only 機制，iOS target 沒有對應方案。
  → **Mitigation**：列為 Open Question，本次先用一個可替換的暫時性 provider（例如寫死在僅本機的設定檔或環境變數，明確標記為暫時）頂著，不阻塞 network 層邏輯的開發與測試；正式的跨平台金鑰注入留待後續 change。

- **[Risk]** 這是本專案第一次導入 Koin，尚無任何啟動（`startKoin`）邏輯或既有 module 可參考。
  → **Mitigation**：本次只建立 `networkModule` 本身與其單元測試（用 Koin test 工具直接檢查 module 定義是否可解析），暫不接上 `startKoin` 的應用程式進入點，避免範圍擴大到 androidApp/iosApp 的啟動流程。

**資料庫 schema**：本次不涉及 Room／資料庫變更，無 migration 需求。

## Migration Plan

本次是全新功能，`shared` 模組目前沒有任何消費者（`androidApp`／`iosApp` 都還沒使用這些型別），因此不需要正式的部署／回滾流程。若需要撤回，直接移除新增的 `network` 套件與對應的 `gradle/libs.versions.toml` 使用（`koin-core`、`ktor-client-darwin`）即可，不影響任何既有功能。

## Open Questions

- TMDB API Key 要如何在 KMP（含 iOS）安全地跨平台注入？本次先用暫時性方案頂著，需要獨立 change 討論。
- `NetworkResponse`／`NetworkException` 未來要不要搬到 `core:common`、要不要改名？等第二個消費者（例如 `core:database`）出現時再議。
- `core:network` 何時、以何種順序從資料夾切成獨立 Gradle module？下一個 change 的主題，屆時需重新確認 `shared` 是否收斂為純 iOS framework 匯出口。
