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

**遵循的既有模式**：本次只落地 Data Source 層，尚未建立 Repository／Use Case，因此嚴格來說還沒有完整的 Repository 模式；但 `MovieDataSource` 介面刻意設計成未來能被 Repository 包一層而不需修改，符合 `kmp-dependency-catalog` spec 對 core 分層（model/domain/data/network）的既有方向。

**1. Base URL 用 `defaultRequest { url(...) }` 取代 Retrofit 的 `baseUrl`**
`HttpClient` 建構時以 `defaultRequest { url("https://api.themoviedb.org/3/") }` 設定基底路徑，各端點呼叫一律使用「不帶開頭 `/`」的相對路徑（例如 `client.get("movie/$id")`）。
- 考慮過的替代方案：Ktor `Resources` plugin（`@Resource` 宣告式端點，寫法更接近 Retrofit 介面）。不採用，因為目前只有 7 個端點，多引入一個 plugin 的維護成本大於好處（YAGNI）；未來端點數量明顯增加時可重新評估。

**2. `api_key`／`language` 用 `defaultRequest{}` 內的 `parameter()` 取代 OkHttp Interceptor**
`defaultRequest{}` 這個 block 會在每次請求時重新執行，因此動態語言代碼（`languageProvider.getLanguageCode()`）的行為與舊專案的 `LanguageInterceptor` 等價，不需要額外的攔截器類別。`HttpLoggingInterceptor` 則對應 Ktor 內建的 `Logging` plugin。
- 考慮過的替代方案：用 Ktor 的 `createClientPlugin` 自訂 plugin 模擬 Interceptor 行為。不採用，目前需求（附加固定/動態查詢參數）用 `defaultRequest` 就能滿足，自訂 plugin 是不必要的複雜度。

**3. 單一 `MovieDataSource` 層，不額外拆出 `MovieApiService`**
`MovieDataSource`（+ Impl）直接呼叫 Ktor `HttpClient`，並在同一層用 `safeApiCall`／`mapData` 把例外轉換成 `NetworkResponse`／`NetworkException`、把 DTO 映射成 external model。錯誤處理的命名與行為維持與舊專案一致（`safeApiCall`／`NetworkResponse`／`NetworkException`），不改名為 `AppResult`／`AppError`，也不搬到尚未存在的 `core:common`；但不再區分「純 API 呼叫」與「錯誤處理」兩層。
- 原規劃是 ApiService → RemoteDataSource 兩層，理由是讓純 API 呼叫與錯誤轉換邏輯分開、利於個別測試。實作後改採單層：Ktor 的 `HttpResponse` 本身就是可被 `ktor-client-mock` 直接替換的型別，測試 `MovieDataSource` 時一樣能在不觸發真實網路請求的情況下驗證請求 URL／參數與錯誤轉換行為，兩層拆分在這裡沒有帶來額外可測試性，反而讓一個端點的邏輯要跨兩個檔案讀。
- 現在就把錯誤型別抽到 `core:common`——仍然不採用，理由不變：目前只有 network 一個消費者，等 `core:database` 這類第二個消費者出現時再決定是否搬遷（技術債已在 proposal 中明確記錄）。
- 若之後真的出現需要繞過錯誤轉換、直接拿原始 DTO 的消費者（目前沒有這種需求），再評估要不要拆回兩層。

**4. DI 改用 Koin，以 `single<Interface> { Impl(get()) }` 取代 Hilt `@Binds`/`@Provides`**
新增 `networkModule`（`org.koin.dsl.module`），提供 `HttpClient`、`MovieDataSource` 兩個 binding。原規劃另外提到的 `Json`／`MovieApiService`／`MovieRemoteDataSource` 三個 binding，`Json` 因為只在 `HttpClient` 建構時內部使用、沒有其他消費者需要單獨注入而不曝露成 binding，`MovieApiService`／`MovieRemoteDataSource` 則因為決策 3 的單層化而不存在。這是本專案第一次實際使用 Koin，範圍僅限本次新增的型別，不涉及既有程式碼。

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
  → **Mitigation**：原規劃是本次只建立 `networkModule` 本身，暫不接上 `startKoin`。實作 debug/release log level 分流（見下一條）時發現這兩件事分不開——`Logging` plugin 的 level 要吃到 debug/release 差異，就必須有人在啟動時把這個布林值傳進 `networkModule`，因此範圍調整為：**Android 端**已在 `JetpackMovieApplication.onCreate()` 接上 `startKoin`；**iOS 端**（`iosApp/iosApp/iOSApp.swift`）刻意保持不動，尚未接上，留待後續 change 決定。

- **[Risk]** debug/release 的 `HttpLoggingInterceptor` 行為（舊專案用 `BuildConfig.DEBUG` 判斷）在 KMP 沒有直接對應物：Android 有 `BuildConfig.DEBUG`，iOS 沒有等價機制；且 `androidApp` 若啟用 AGP 原生 `buildFeatures.buildConfig` 生成自己的 `BuildConfig`，會因為 namespace 跟 `shared` 模組的 `com.github.gmazzo.buildconfig` plugin 產生的 `com.shang.jetpackmoviekmp.BuildConfig`（`packageName("com.shang.jetpackmoviekmp")`）撞到同一個 fully-qualified class name。
  → **Mitigation**：`networkModule` 改成 `fun networkModule(isDebug: Boolean)`，`isDebug` 由呼叫端決定怎麼取得。Android 端改用執行期的 `applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE`（讀 AGP 依 build type 自動設定的 manifest `debuggable` 屬性），完全不需要啟用 AGP 的 buildConfig，避免撞名。iOS 端若之後要做，對應機制是 Swift 的 `#if DEBUG`（這個專案的 `iosApp.xcodeproj` 已經有 `SWIFT_ACTIVE_COMPILATION_CONDITIONS = "DEBUG $(inherited)"`），但本次不處理。

- **[Risk]** 專案原本沒有任何程式碼覆蓋率工具，task 7.6 要求「單元測試覆蓋率 ≥80%」但沒有數字可以驗證；而 `shared` 模組整體／`network.model` 套件量測出來都低於 80%，因為 Kotlin `@Serializable` DTO 每個欄位都會生成 `equals()`/`hashCode()`/`toString()`/`copy()`/`componentN()`，這些樣板方法在我們的程式碼裡從未被呼叫（DTO 只用來反序列化後立刻映射成 Bean）。
  → **Mitigation**：新增 `kover`（`org.jetbrains.kotlinx.kover` 0.9.8）catalog alias + plugin，只套用在 `shared/build.gradle.kts`。Kover 0.9.8 的 verify rule 沒有獨立的 per-rule filters——`filters{}` 是掛在 `reports{}` 底下、對 log/HTML/XML/verify 全部生效的全域設定（查證過原始碼 `KoverReportsConfig`／`KoverVerifyRule` 介面，前者才有 `filters`，後者只有 `groupBy`/`bound`/`minBound`/`maxBound`）。因此把 `reports { filters { includes { packages(...) } } }` 收斂成只涵蓋這次 change 實際新增的業務邏輯（`network.di`／`network.datasource`／`network.extension`／`network.provider`），不含 `network.model`（DTO 樣板）與 `shared` 模組其他既有、與本次無關的程式碼（`Platform`、`ThemeMode`、`UserData` 等）。收斂後這四個套件的 line coverage 皆為 100%，`koverVerify` 通過 80% 門檻。代價是 `koverLog`／`koverHtmlReport` 現在只反映這四個套件，不是整個 `shared` 模組——如果未來要幫其他套件（如 `network.model`）也建立覆蓋率門檻，需要另外評估要不要放寬 filters 範圍或改用 `excludes` 排除純樣板類別。

**資料庫 schema**：本次不涉及 Room／資料庫變更，無 migration 需求。

## Migration Plan

本次是全新功能，`shared` 模組目前沒有任何消費者（`androidApp`／`iosApp` 都還沒使用這些型別），因此不需要正式的部署／回滾流程。若需要撤回，直接移除新增的 `network` 套件與對應的 `gradle/libs.versions.toml` 使用（`koin-core`、`ktor-client-darwin`）即可，不影響任何既有功能。

## Open Questions

- TMDB API Key 要如何在 KMP（含 iOS）安全地跨平台注入？本次先用暫時性方案頂著，需要獨立 change 討論。
- `NetworkResponse`／`NetworkException` 未來要不要搬到 `core:common`、要不要改名？等第二個消費者（例如 `core:database`）出現時再議。
- `core:network` 何時、以何種順序從資料夾切成獨立 Gradle module？下一個 change 的主題，屆時需重新確認 `shared` 是否收斂為純 iOS framework 匯出口。
