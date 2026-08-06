## Why

`MainViewModel`、`HomeViewModel`、`MovieDetailViewModel` 各自手寫了形狀幾乎相同的 `Loading/Success/Error` sealed type（`MainUiState`、`HomeUiState`、`MovieDetailUiState`/`DetailSectionState<T>`），且每個 ViewModel 都重複撰寫 `AppResult -> XxxUiState` 的手動 mapping。這些重複樣板讓詳細頁尤其笨重（同時維護 `MovieDetailUiState` 與泛型 `DetailSectionState<T>` 兩個型別），且新增 ViewModel 時容易複製貼上出現不一致（例如 `sealed class` vs `sealed interface`、`Error.throwable` 是否可為 null）。將這個共用形狀抽成 `shared/common` 的單一泛型 `UiState<T>`，可以消除重複定義與重複 mapping，同時讓現有型別名稱以 `typealias` 保留，不影響既有呼叫端與 spec 文字。

## What Changes

- 在 `shared/common` 新增 `sealed interface UiState<out T>`（`Loading`／`Success<T>(data)`／`Error(throwable)`），統一取代 `MainUiState`、`HomeUiState`、`MovieDetailUiState`、`DetailSectionState<T>` 四個各自獨立的 sealed type。
- 在 `shared/common` 新增 `AppResult<T>.toUiState(): UiState<T>` extension，取代各 ViewModel 手寫的 `when (result) { is AppResult.Success -> ...; is AppResult.Failure -> ... }` mapping 樣板。
- `androidApp`、`feature/home`、`feature/detail` 改用 `typealias MainUiState = UiState<ConfigurationBean>`、`typealias HomeUiState = UiState<MovieGenreBean>`、`typealias MovieDetailUiState = UiState<MovieDetailBean>` 保留原有型別名稱與呼叫端寫法（`MainUiState.Loading`、`when (state) { is MovieDetailUiState.Success -> ... }` 等寫法不需變動）。
- `feature/detail` 的 `DetailSectionState<T>` 直接移除，`movieRecommendations`、`movieActors` 兩個 `StateFlow` 改用 `UiState<List<MovieCardResult>>`、`UiState<List<MovieCastAndCrewBean.Cast>>`（或對應 typealias），統一詳細頁的狀態型別。
- 統一 `Error` 攜帶的 `throwable` 為非 null `Throwable`，修正 `HomeUiState.Error(throwable: Throwable?)` 目前與其他型別不一致的 nullable 簽章。
- **不納入本次範圍**：`CollectUiState`、`HistoryUiState`（只有 `Empty/Success`，無 Loading/Error 概念，資料庫來源）、`HomeContentViewModel`／`SearchViewModel`（已使用 AndroidX Paging 自身的 `LoadState`，屬於另一套機制）、`SettingViewModel`（純資料 `StateFlow`，無狀態包裝需求）。這些維持現狀，避免過度調整。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `common-kernel`：`shared/common` 的 `commonMain` 新增 `UiState<T>` 型別與 `AppResult<T>.toUiState()` extension 的存在性與行為規範。

## Impact

- **`shared/common`**（`commonMain`，Android + iOS 共用編譯，但目前僅 Android 端消費）：新增 `UiState.kt`（型別定義）與 `AppResult` 的 `toUiState()` extension，不依賴 `network`／`datastore`，維持 `common-kernel` 既有的依賴方向規則。
- **`androidApp`**：`MainUiState.kt` 改為 `typealias`，`MainViewModel` 的 mapping 邏輯改用 `toUiState()`。`MainActivity` 消費端寫法不變。
- **`feature/home`**：`HomeUiState.kt` 改為 `typealias`，`HomeViewModel` 改用 `toUiState()`；`HomeContentViewModel`、`HomeScreen` 的 Paging `LoadState` 用法不受影響。
- **`feature/detail`**：`MovieDetailUiState.kt` 改為 `typealias`，移除 `DetailSectionState<T>`；`MovieDetailViewModel` 三個 `StateFlow`（`movieDetail`、`movieRecommendations`、`movieActors`）改用 `UiState<T>`／對應 typealias 與 `toUiState()`；`MovieDetailScreen.kt` 的 `when` 分支型別不變，僅需確認 import 來源改為 `shared/common`。
- 不涉及第三方依賴新增或 `gradle/libs.versions.toml` 變更。
- 不影響 iOS：`shared/app` 的 iOS Presenter 層（`HomeMovieListPresenter` 等）使用獨立的 `*LoadState`，與本次 `UiState<T>` 無關，不需要 iOS simulator test 驗證本次變更，但仍需確認 `shared/common` 的 Android host test 通過。
- `feature/collect`、`feature/history`、`feature/setting`、`SearchViewModel`、`HomeContentViewModel` 明確排除在本次變更之外。
