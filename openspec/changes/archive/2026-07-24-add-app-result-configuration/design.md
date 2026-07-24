## Context

`add-skie` 導入 SKIE 後，`Flow<T>` 已可在 Swift 端用 `for await` 消費，但 `kotlin.Result<T>` 這種泛型
成功/失敗容器在匯出時會被 type-erase 成 opaque boxed value（`Optional(Success(...))`），Swift 端無法用
`as? ConfigurationBean` 判斷成功。目前唯一的解法是 `shared/app` iosMain 手寫
`IosConfigurationLoader`／`IosConfigurationLoadState`，把 `Result<ConfigurationBean>` 拆成
Swift 看得懂的明確 sealed state（`openspec/backlog.md` 已記錄此發現與草案）。這個 wrapper 只覆蓋
`GetConfigurationUseCase` 一條路徑，其餘 4 個回傳 `Result<T>` 的方法尚未處理，且每加一個 UseCase
就要再手寫一次 wrapper，無法規模化。

`shared/common` 目前已是「中立跨層共用型別」的既定位置（`NetworkException`、`LanguageProvider`、
`BaseHostUrlProvider` 皆在此），且是 `shared:data`、`shared:domain`、`shared:app` 都會依賴的最底層
module，適合放置一個能被 SKIE 明確匯出成 Swift enum 的通用 `AppResult<T>`／`AppError`。

## Goals / Non-Goals

**Goals:**
- 在 `shared/common` 定義 `AppResult<out T>`（`Success`／`Failure`）與 `AppError`，作為未來所有
  UseCase／Repository 逐步遷移的共用契約。
- 把 `GetConfigurationUseCase`（iOS 端實際透過 `KoinHelper` 呼叫的邊界）從 `kotlin.Result<T>`
  遷移到 `AppResult<T>`，驗證型別設計在真實路徑上可行；`MovieRepository.getConfiguration()`
  維持 `Result<T>` 不變，型別轉換只發生在 UseCase 這一層。
- 同步更新 `androidApp` 的 `MainViewModel`／`MainUiState`，確認 Android 端消費 `AppResult` 沒有問題。
- 定義清楚的 iOS 端目標契約（spec delta），讓後續 iOS 端調整有明確依據。

**Non-Goals:**
- 不遷移其餘 4 個回傳 `Result<T>` 的方法／UseCase（`getMovieGenres`、`getMovieDetail`、
  `getMovieRecommendations`、`getMovieActor`）；沿用既有 `kotlin.Result<T>`，留待後續 change。
- 不定義 `AppError.Database`／`AppError.LocalStorage`；本次範圍內完全用不到，等實際遷移會產生這類
  錯誤的 UseCase 時再新增，避免定義沒有任何呼叫路徑驗證的分類。
- 不實作 `shared/app` iosMain（`IosConfigurationLoader` 移除、`KoinHelper.kt` 調整）與 `iosApp`
  Swift 端（`SplashViewModel.swift` 等）的程式碼，這部分由使用者自行處理；本 change 只更新對應的
  spec delta 定義目標契約。
- 不改變 `GetConfigurationUseCase` 既有「API 失敗但本地有快取時靜默 fallback 回傳成功」的行為，
  只換回傳型別。

## Decisions

**沿用既有模式**：`AppResult` 承接的仍是 Repository → UseCase → UI 的既定分層，不改變
Repository／UseCase 職責劃分。型別轉換只發生在「最後輸出會被外部（含 iOS）直接呼叫的邊界」——
也就是 `GetConfigurationUseCase`；`MovieRepository` 是 `shared/data` 內部實作，不會被 iOS
直接呼叫，維持 `kotlin.Result<T>` 不動。屬於既有 MVVM + Repository/UseCase 模式下的型別替換，
非架構偏離。

1. **`AppResult` 用 `sealed interface`，不用 `sealed class`**
   - 選擇：`sealed interface AppResult<out T> { data class Success<T>(val data: T); data class
     Failure(val error: AppError) : AppResult<Nothing> }`
   - 理由：專案既有的狀態型別（`MainUiState`、`HomeUiState`、`IosConfigurationLoadState`）均為
     `sealed interface`／`sealed class` 三態或兩態設計，`sealed interface` 讓 `Success`／`Failure`
     可各自是輕量 `data class`，不需要共用建構子邏輯；也是 SKIE 匯出 Swift enum 時的既有慣例
     （`IosConfigurationLoadState` 已驗證過這個 pattern 可行）。
   - 備選方案：延用 `kotlin.Result<T>` 但在 `shared/app` iosMain 持續手寫個別 wrapper——被否決，
     因為這正是本次要解決的規模化問題本身。

2. **`AppError` 本次只定義 `Network`／`Unknown` 兩種**
   - 選擇：`sealed class AppError : Exception { data class Network(val exception: NetworkException) :
     AppError(cause = exception); data object Unknown : AppError() }`（見決策 8：為何是
     `sealed class AppError : Exception` 而非單純 `sealed interface`）
   - 理由：`getConfiguration()` 的失敗來源目前 100% 是 `MovieRepositoryImpl` 對 `NetworkResponse.error`
     的處理（見 `shared/data` 現況），本來就已經是 `NetworkException`；`Database`／`LocalStorage`
     分類在這條路徑上沒有任何實際案例可驗證，先定義只會是憑空猜測的介面設計，且 `sealed class`
     之後要擴充分類是純新增（不影響既有 `when` 分支的既有 case，只會讓既有未窮舉的 `when` 編譯期報錯
     提示要處理新 case），擴充成本低，不需要現在一次到位。
   - 備選方案：backlog 草稿的四分類（`Network`／`Database`／`LocalStorage`／`Unknown`）——與使用者
     確認後改為先只定義用得到的兩類，其餘留到後續遷移對應 UseCase 時再加。

3. **`NetworkException` 不變、`AppError.Network` 包裝它，不取代它**
   - 理由：`NetworkException` 職責維持「network request 流程中的錯誤」，`AppError` 是更高一層的
     「跨 UseCase 通用錯誤模型」，兩者職責不同、不合併。`shared/network` 的 `safeApiCall` 與
     `MovieRepositoryImpl` 既有的 `NetworkException` 分類邏輯完全不變。

4. **`AppResult` 放在 `shared/common`，沿用 `common-kernel` 既有能力範圍，不開新 module**
   - 理由：`shared/common` 已是 `NetworkException` 等中立型別的既定位置，且是
     `shared:data`／`shared:domain`／`shared:app` 共同依賴的最底層 module；`AppResult`／`AppError`
     的定位與既有型別一致，沒有理由另開新 module 增加依賴圖複雜度。

5. **`GetConfigurationUseCase` 的快取 fallback 邏輯維持原樣，只換型別**
   - 理由：這是既有已上線行為，非本次要調整的範圍；只把 `result.fold(...)` 改寫成對 `AppResult`
     的 `when` 分支等價邏輯，降低本次改動的風險與 review 負擔。

6. **範圍邊界：`shared/app` iosMain 與 `iosApp` Swift 不在本次實作任務內**
   - 理由：依使用者明確分工——iOS 端（含 `shared/app` iosMain 的 Kotlin 程式碼）由使用者自行處理，
     Android／共用（`shared/common`、`shared/domain`、`androidApp`）由本次 change 實作。spec delta
     （`ios-koin-bridge`、`ios-splash-screen`）仍會同步更新，作為使用者對齊實作的依據，但程式碼改動
     不列入 `tasks.md`。

7. **型別轉換只發生在最終呼叫邊界，`MovieRepository` 不動**
   - 選擇：`MovieRepository.getConfiguration()`／`MovieRepositoryImpl` 維持
     `Flow<Result<ConfigurationBean>>`；`GetConfigurationUseCase` 內部用 `result.fold(...)`
     消化 Repository 的 `Result`，轉換成 `AppResult` 再往外 emit。
   - 理由：`MovieRepository` 是 `shared/data` 內部實作，只被 `shared/domain` 的 UseCase 呼叫，
     不會被 iOS 或 `androidApp` 直接依賴；真正需要「明確可被 SKIE 匯出成 Swift enum」型別的邊界，
     是 UseCase 對外（含 iOS `KoinHelper`）曝露的那一層。在不需要改動的內部介面上引入新型別，
     只會擴大改動範圍與 review 負擔，不會帶來實際效益。
   - 備選方案：原規劃連同 `MovieRepository` 一併改為回傳 `AppResult`——經檢視後撤回，因為
     `MovieRepository` 從未被 UseCase 以外的任何呼叫端直接使用。

8. **`AppError` 繼承 `kotlin.Exception`，而非單純 `sealed interface`**
   - 選擇：`sealed class AppError(message: String? = null, cause: Throwable? = null) :
     Exception(message, cause)`；`Network` 建構時把 `cause` 設為攜帶的 `NetworkException`。
   - 理由：`androidApp` 的 `MainUiState.Error(val throwable: Throwable)` 需要一個 `Throwable`；
     若 `AppError` 只是 `sealed interface`，`MainViewModel` 就要多寫一層
     `AppError.toThrowable()` 轉換（`Unknown` case 還得憑空捏造一個代表性例外）。讓 `AppError`
     本身就是 `Throwable`，`MainViewModel` 可以直接 `MainUiState.Error(result.error)`，少一層
     轉換、也少一個「捏造例外」的尷尬 case。
   - **這不會啟用 Kotlin/Native 的例外跨界拋出機制**：`AppError` 一律透過 `AppResult.Failure`
     當作一般資料值經由 `Flow` 傳遞給呼叫端，程式碼裡完全沒有 `throw AppError(...)`；
     `@Throws`／跨界 `NSError` 轉換只在「函式真的 throw」時才會觸發，這裡不涉及，所以不會有
     iOS 端未預期 crash 的風險。
   - **iOS 端待驗證事項**：`AppError` 繼承 `Throwable` 後，Kotlin/Native 產生的 Obj-C header
     會讓它額外可能被橋接成 Swift `Error`-conforming 型別；SKIE 官方文件（sealed class → Swift
     enum 那頁）並未特別討論「sealed 例外階層」這個組合，理論上應該跟一般 sealed class 一樣被
     攤平成 enum，但沒有文件明確保證。這件事留給使用者實作 iOS 端時實際驗證 SKIE 產生的 Swift
     型別是否符合預期（若有出入，設計仍可退回「`sealed interface` + 呼叫端各自轉換」的版本）。
   - 備選方案：維持 `sealed interface AppError`，`MainViewModel` 自行寫
     `AppError.toThrowable()` 轉換——可行但多一層樣板碼，且 `Unknown` case 沒有真正的
     underlying exception 可用，只能塞一個佔位例外，語意上不如「`AppError` 本身就是例外」乾淨。

## Risks / Trade-offs

- **[Risk]** `shared/domain` 的 UseCase 層此後會同時存在 `AppResult<T>`（`GetConfigurationUseCase`）
  與 `kotlin.Result<T>`（其餘 4 個 UseCase）兩種對外回傳型別，呼叫端需要分別處理，容易混淆；
  `MovieRepository` 本身則完全不受影響，維持單一 `Result<T>` 型別，不會有這個問題。
  → **Mitigation**：在 `GetConfigurationUseCase` KDoc 明確註記「這是對外邊界，內部把 Repository
  的 `Result` 轉為 `AppResult`」；本次不強行一次遷移全部 UseCase 以控制改動範圍，待評估後續
  change 排程時全部遷移完成後即消除此不一致。

- **[Risk]** `androidApp` 的 `MainViewModel`／`MainUiState` 屬於 breaking change，若遺漏調整會導致
  `androidApp` 編譯失敗。
  → **Mitigation**：`shared/domain` 改動後立即執行 `:androidApp:assembleDebug` 驗證編譯通過，
  作為 tasks.md 的驗收項之一。

- **[Risk]** `shared/app` iosMain（`IosConfigurationLoader`）與 `iosApp`（`SplashViewModel.swift`）
  在本次 change 合併後，因 `GetConfigurationUseCase` 回傳型別已變更，會編譯失敗，直到使用者完成
  對應調整為止；這段期間 iOS build 會處於壞掉狀態。
  → **Mitigation**：proposal／design／spec delta 明確定義目標契約，讓使用者可以直接對照實作；
  建議與使用者確認 merge 時機（例如同一個 PR 內一併調整，或先開 feature branch 等 iOS 端跟上再合併），
  避免主幹長時間處於編譯失敗狀態。

- **[Risk]** 之後要新增 `AppError.Database`／`AppError.LocalStorage` 時，所有既有對 `AppError` 做
  exhaustive `when` 的呼叫端都要重新檢查。
  → **Mitigation**：這是 `sealed class` 的設計目的所在——編譯器會在新增 case 時，對所有未窮舉
  的 `when` 主動報錯，不會是靜默的執行期問題。

- **[Risk]** `AppError` 繼承 `Exception` 後，SKIE 對「sealed 例外階層 → Swift enum」這個組合的
  匯出行為沒有官方文件明確保證（見決策 8）；若 SKIE 有未預期的特殊處理，iOS 端拿到的型別可能不是
  預期的 enum 形狀。
  → **Mitigation**：這件事本來就在使用者後續實作 iOS 端的驗證範圍內；若驗證後發現有問題，可以
  退回「`AppError` 維持 `sealed interface`，`MainViewModel` 自行轉換」的版本，改動範圍很小。

## Migration Plan

1. `shared/common`：新增 `AppResult`／`AppError`／`Throwable.toAppError()`，補單元測試（成功/失敗
   建構、`sealed interface` 窮舉行為、`toAppError()` 兩種分類）。
2. `shared/domain`：`GetConfigurationUseCase` 改為回傳 `Flow<AppResult<ConfigurationBean>>`，
   內部用 `result.fold(...)` 消化 `MovieRepository.getConfiguration()`（維持
   `Flow<Result<ConfigurationBean>>` 不變）的結果，fallback 邏輯改寫為對應 `AppResult` 的
   emit；更新既有測試。
3. `androidApp`：`MainViewModel` 改為消費 `AppResult`（`MainUiState.Error` 欄位型別維持
   `Throwable`；因 `AppError` 本身即為 `Throwable`，`result.error` 可直接傳入，不需要額外轉換）；
   執行 `:androidApp:assembleDebug` 驗證編譯與啟動皆正常（Loading／Error／Success 三態行為不變）。
4. `shared/app`：`AppDiagnosticsTest` 的預期字串同步更新（`AppDiagnostics` 本身邏輯不變）。
5. 更新 `common-kernel`、`kmp-movie-domain-usecases`、`ios-koin-bridge`、`ios-splash-screen`
   四份 spec delta（`kmp-movie-data-repository` 不受影響，不列入本次 spec delta）。
6. 使用者接手 `shared/app` iosMain（`IosConfigurationLoader` 移除、`KoinHelper.kt` 調整）與
   `iosApp` Swift 端（`SplashViewModel.swift` 等）的對應調整。

**Rollback**：本次改動範圍限定在 `getConfiguration` 一條路徑，且是型別簽名層級的變更，不涉及
資料庫 schema 或持久化格式；若需回退，直接 revert 對應 commit 即可，不影響其餘 4 個既有
`Result<T>` 方法與其他既有功能。

## Open Questions

- 其餘 4 個回傳 `Result<T>` 的方法／UseCase 何時排入遷移，尚未排程，需另開 change 或回填
  `openspec/backlog.md`。
- 是否要在 `AppResult.Success` 加入「資料來源」metadata（例如 configuration 是來自 API 還是快取
  fallback）——本次討論已決定不做，維持現有「呼叫端看不到 fallback 曾發生」的行為，但列為未來若有
  UI 需要顯示「離線資料」提示時的候選調整方向。
