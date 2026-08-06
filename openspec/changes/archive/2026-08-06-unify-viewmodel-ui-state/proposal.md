## Why

`MainViewModel`、`HomeViewModel`、`MovieDetailViewModel` 各自手寫了形狀幾乎相同的 `Loading/Success/Error` sealed type（`MainUiState`、`HomeUiState`、`MovieDetailUiState`/`DetailSectionState<T>`），且每個 ViewModel 都重複撰寫 `AppResult -> XxxUiState` 的手動 mapping。這些重複樣板讓詳細頁尤其笨重（同時維護 `MovieDetailUiState` 與泛型 `DetailSectionState<T>` 兩個型別），且新增 ViewModel 時容易複製貼上出現不一致（例如 `sealed class` vs `sealed interface`、`Error.throwable` 是否可為 null）。將這個共用形狀抽成 `shared/common` 的單一泛型 `UiState<T>`，可以消除重複定義與重複 mapping。

## What Changes

- 在 `shared/common` 新增 `sealed interface UiState<out T>`（`Loading`／`Success<T>(data)`／`Error(throwable)`），統一取代 `MainUiState`、`HomeUiState`、`MovieDetailUiState`、`DetailSectionState<T>` 四個各自獨立的 sealed type。
- 在 `shared/common` 新增 `AppResult<T>.toUiState(): UiState<T>` extension，取代各 ViewModel 手寫的 `when (result) { is AppResult.Success -> ...; is AppResult.Failure -> ... }` mapping 樣板。
- `androidApp`、`feature/home`、`feature/detail` 移除 `MainUiState`、`HomeUiState`、`MovieDetailUiState` 三個獨立型別定義，直接改用 `UiState<ConfigurationBean>`、`UiState<MovieGenreBean>`、`UiState<MovieDetailBean>`；呼叫端全面改寫為 `UiState.Loading`／`UiState.Success`／`is UiState.Error`（**BREAKING**：`MainUiState.Loading`、`HomeUiState`、`MovieDetailUiState` 型別名稱不再存在，`android-app-entry`、`android-home-module` 兩份 spec 中引用這些型別名稱的文字需同步修改）。
- `feature/detail` 的 `DetailSectionState<T>` 直接移除，`movieRecommendations`、`movieActors` 兩個 `StateFlow` 改用 `UiState<List<MovieCardResult>>`、`UiState<List<MovieCastAndCrewBean.Cast>>`，統一詳細頁的狀態型別。
- 統一 `Error` 攜帶的 `throwable` 為非 null `Throwable`，修正 `HomeUiState.Error(throwable: Throwable?)` 目前與其他型別不一致的 nullable 簽章。
- **不納入本次範圍**：`CollectUiState`、`HistoryUiState`（只有 `Empty/Success`，無 Loading/Error 概念，資料庫來源）、`HomeContentViewModel`／`SearchViewModel`（已使用 AndroidX Paging 自身的 `LoadState`，屬於另一套機制）、`SettingViewModel`（純資料 `StateFlow`，無狀態包裝需求）。這些維持現狀，避免過度調整。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `common-kernel`：`shared/common` 的 `commonMain` 新增 `UiState<T>` 型別與 `AppResult<T>.toUiState()` extension 的存在性與行為規範。
- `android-app-entry`：`MainViewModel.configuration` 的型別由 `MainUiState` 改為 `UiState<ConfigurationBean>`，spec 中引用 `MainUiState.Loading` 等文字需改寫。
- `android-home-module`：`HomeViewModel.movieGenres` 的型別由 `HomeUiState` 改為 `UiState<MovieGenreBean>`，spec 中列舉 `HomeUiState` 型別存在性的文字需改寫。

## Impact

- **`shared/common`**（`commonMain`，Android + iOS 共用編譯，但目前僅 Android 端消費）：新增 `UiState.kt`（型別定義）與 `AppResult` 的 `toUiState()` extension，不依賴 `network`／`datastore`，維持 `common-kernel` 既有的依賴方向規則。
- **`androidApp`**：移除 `MainUiState.kt`，`MainViewModel.configuration` 改為 `StateFlow<UiState<ConfigurationBean>>`、mapping 邏輯改用 `toUiState()`；`MainActivity` 消費端（`MainScreen`、splash 條件判斷）改寫為 `UiState.Loading`／`Success`／`Error`。
- **`feature/home`**：移除 `HomeUiState.kt`，`HomeViewModel.movieGenres` 改為 `StateFlow<UiState<MovieGenreBean>>`、mapping 邏輯改用 `toUiState()`；`HomeScreen.kt` 消費端改寫為 `UiState.Loading`／`Success`／`Error`（`Success.data` 取代原本的 `Success.movieGenres` 欄位名）；`HomeContentViewModel`、`HomeScreen` 中依賴 Paging `LoadState` 的部分不受影響。
- **`feature/detail`**：移除 `MovieDetailUiState.kt` 與 `DetailSectionState<T>`；`MovieDetailViewModel` 三個 `StateFlow`（`movieDetail`、`movieRecommendations`、`movieActors`）改用 `UiState<T>` 與 `toUiState()`（`movieActors` 因需從 `MovieCastAndCrewBean` 取出 `cast` 欄位，保留手寫 `when` mapping）；`MovieDetailScreen.kt` 的 `when` 分支與欄位存取（`Success.movie` → `Success.data`）同步改寫。
- 不涉及第三方依賴新增或 `gradle/libs.versions.toml` 變更。
- 不影響 iOS：`shared/app` 的 iOS Presenter 層（`HomeMovieListPresenter` 等）使用獨立的 `*LoadState`，與本次 `UiState<T>` 無關，不需要 iOS simulator test 驗證本次變更，但仍需確認 `shared/common` 的 Android host test 通過。
- `feature/collect`、`feature/history`、`feature/setting`、`SearchViewModel`、`HomeContentViewModel` 明確排除在本次變更之外。
