## Context

`GetMovieDetailUseCase`、`GetMovieRecommendUseCase`、`MovieRepository.getMovieActor` 目前都回傳 `Flow<kotlin.Result<T>>`。專案已有可被 SKIE 明確匯出成 Swift enum 的 `AppResult`（`shared/common`），並已在兩處落地：

- `GetConfigurationUseCase`：**UseCase 層轉換**——`MovieRepository.getConfiguration()` 仍回傳 `Result`，UseCase 內部用 `transform { result.fold(...) }` 轉成 `AppResult` 對外。
- `MovieRepository.getMovieGenres()`：**Repository 層直接回傳 `AppResult`**——因為這支方法沒有 UseCase 包裝層，iOS 端（`HomeViewModel`）直接呼叫 Repository。

本次三個目標方法中，`GetMovieDetailUseCase`／`GetMovieRecommendUseCase` 有 UseCase 包裝層，`getMovieActor` 沒有（`MovieDetailViewModel` 直接呼叫 `movieRepository.getMovieActor(...)`）。兩種既有慣例剛好各自對應。

Android 端唯一呼叫方是 `MovieDetailViewModel`，三處都使用 `Result.fold(onSuccess, onFailure)` 消費，改動後需要同步改寫。iOS 端目前完全沒有程式碼呼叫這三個方法，本次屬於「讓 iOS 能開始使用」的前置修復，沒有既有 iOS 呼叫端需要相容。

## Goals / Non-Goals

**Goals:**
- `GetMovieDetailUseCase`、`GetMovieRecommendUseCase`、`MovieRepository.getMovieActor` 三者的公開回傳型別改為 `AppResult`，可被 SKIE 匯出成 Swift enum
- 成功／失敗判斷邏輯與既有行為（例如 `GetMovieDetailUseCase` 成功時寫入瀏覽紀錄、`GetMovieRecommendUseCase` 標記收藏狀態）完全不變，只調整外層型別包裝
- 同步更新 Android 端 `MovieDetailViewModel` 與所有受影響測試（含 fake 實作），維持既有測試涵蓋率規則

**Non-Goals:**
- 不新增或修改 `AppResult`／`AppError` 本身的定義
- 不建立新的 iOS `DetailViewModel`／畫面（iOS 尚無 detail 功能，本次只做 shared 層修復，不涉及 iOS UI）
- 不處理其餘尚未改用 `AppResult` 的 `MovieRepository` 方法（例如 `getConfiguration`、`getMovieListPager` 等），維持現狀

## Decisions

### 1. `GetMovieDetailUseCase`／`GetMovieRecommendUseCase` 沿用 `GetConfigurationUseCase` 的 UseCase 層轉換模式

`MovieRepository.getMovieDetail(movieId)`／`getMovieRecommendations(movieId)` 維持回傳 `Result`（不動 Repository 這兩支方法），UseCase 內部用 `transform { result.fold(onSuccess = { emit(AppResult.Success(it)) }, onFailure = { emit(AppResult.Failure(it.toAppError())) }) }` 轉換。

**理由**：這兩個 UseCase 本身已有非 trivial 的邏輯（`GetMovieDetailUseCase` 寫入瀏覽紀錄、`GetMovieRecommendUseCase` 合併收藏狀態），維持 Repository 回傳 `Result`、只在 UseCase 邊界轉換，符合專案既有「UseCase 是 iOS 呼叫邊界」的慣例，改動範圍最小。

**替代方案**：讓 `MovieRepository.getMovieDetail`／`getMovieRecommendations` 直接回傳 `AppResult`。放棄原因：這兩支方法目前只被對應 UseCase 呼叫，若 Repository 直接改，UseCase 內部組裝邏輯（`combine`、`onEach`、`fold`）都要跟著搬移或重寫轉換邏輯，改動面更大且偏離既有慣例（`getConfiguration` 也是留在 Repository 回傳 `Result`，由 UseCase 轉換）。

### 2. `getMovieActor` 沿用 `getMovieGenres` 的 Repository 層直接轉換模式

`MovieRepository.getMovieActor(id)` 介面簽名與 `MovieRepositoryImpl` 實作直接改為回傳 `Flow<AppResult<MovieCastAndCrewBean>>`，不新增 UseCase 包裝層。

**理由**：`getMovieActor` 目前沒有 UseCase 層，`MovieDetailViewModel` 直接呼叫 Repository，與 `getMovieGenres`（`HomeViewModel` 直接呼叫）情境一致，比照既有慣例處理最一致。若為此新增一個 UseCase 只為了做型別轉換，屬於超出本次修復範圍的架構變動。

### 3. 不變更 `shared/data` 對 `shared/common` 的依賴宣告方式

`shared/data/build.gradle.kts` 目前對 `shared.common` 用 `implementation(...)`，但 `getMovieGenres` 已經在公開介面暴露 `AppResult`（現況已如此，非本次新增問題）。本次 `getMovieActor` 改動會再多暴露一次同樣的型別。

**理由**：本次目標是修復三個方法的 iOS 互通性，不擴大範圍去調整既有的依賴宣告策略；目前所有實際依賴 `MovieRepository` 的上層模組（`shared/domain`、`shared/app`）都已用 `api(projects.shared.common)`，實務上不會編譯失敗。是否改為 `api` 屬於獨立的依賴衛生議題，留待未來需要時再處理，不在本次 tasks 範圍內。

## Risks / Trade-offs

- **[Risk]** 三個方法皆為 **BREAKING** 變更，Android 端 `MovieDetailViewModel` 與多個模組的測試 fake 實作需要同步修改，遺漏任何一處都會造成編譯失敗 → **Mitigation**：tasks.md 以檔案清單方式列出所有已知呼叫端與 fake 實作（含 `feature/home`、`feature/collect`、`feature/search`、`feature/history`、`androidApp`、`shared/app` 的 commonTest／iosTest），逐一檢查編譯錯誤訊息確保無遺漏
- **[Risk]** `shared/data` 對 `common` 使用 `implementation` 而非 `api`，理論上屬於依賴傳遞的脆弱設定 → **Mitigation**：本次不修改，僅在 proposal／design 記錄待確認事項；若未來新增不直接依賴 `shared.common` 的上層模組導致編譯失敗，再另開 change 處理
- **無資料庫 schema 變更**，不涉及 Room migration

## Open Questions

（無）
