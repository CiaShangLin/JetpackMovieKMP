## Context

目前 `MainUiState`（androidApp）、`HomeUiState`（feature/home）、`MovieDetailUiState` + `DetailSectionState<T>`（feature/detail）各自定義形狀相同的 `Loading/Success/Error` sealed type，且每個 ViewModel 手寫 `AppResult -> XxxUiState` 的 `when` mapping。`shared/common` 已有 `AppResult<T>`／`AppError`（`common-kernel` spec 規範），是資料層的錯誤處理型別，但沒有對應的 UI 層狀態型別，導致每個 feature 自行「翻譯」一次。

`CollectViewModel`／`HistoryViewModel`（`Empty/Success`，無 Loading/Error）、`HomeContentViewModel`／`SearchViewModel`（AndroidX Paging 自身 `LoadState`）、`SettingViewModel`（純 `StateFlow<UserData>`）三類 ViewModel 的狀態語意與 `Loading/Success/Error` 不同構，不在本次調整範圍內。

## Goals / Non-Goals

**Goals:**
- 在 `shared/common` 提供單一 `UiState<T>` 泛型型別，取代 `MainUiState`、`HomeUiState`、`MovieDetailUiState`、`DetailSectionState<T>` 四個獨立定義。
- 提供 `AppResult<T>.toUiState()` extension，消除各 ViewModel 手寫的 `AppResult -> UiState` mapping 樣板。
- 透過 `typealias` 保留 `MainUiState`、`HomeUiState`、`MovieDetailUiState` 原有型別名稱，讓既有呼叫端程式碼（`MainUiState.Loading`、`when (state) { is MovieDetailUiState.Success -> ... }`）與既有 spec 文字（`android-app-entry`、`android-home-module` 提及的型別名稱）不需變動。
- 統一 `Error.throwable` 為非 null，修正 `HomeUiState.Error(throwable: Throwable?)` 目前與其他型別不一致的簽章。

**Non-Goals:**
- 不調整 `CollectUiState`／`HistoryUiState`（`Empty/Success`，資料庫來源、無失敗語意）、`HomeContentViewModel`／`SearchViewModel`（Paging `LoadState`）、`SettingViewModel`（純資料）。
- 不將 `UiState<T>` 匯出給 iOS／Swift：目前 `MainUiState`／`HomeUiState`／`MovieDetailUiState` 皆為 Android-only（`androidApp`、`feature/*`），iOS 端透過 `shared/app` 的獨立 Presenter 層（`HomeMovieListPresenter` 等）與自己的 `*LoadState` 消費 Flow，兩者語意不同，本次不合併，也不需要 SKIE Swift enum 匯出設定。
- 不變更 Repository／UseCase／Koin module 依賴方向；`UiState<T>` 純粹是 ViewModel 層的展示狀態型別，不會被 `shared/data`、`shared/domain` 依賴。

## Decisions

**1. `UiState<T>` 定義為 `shared/common` 的 `sealed interface`，而非 `sealed class`**
現有型別中 `MainUiState`、`MovieDetailUiState`、`DetailSectionState<T>`（3/5）已用 `sealed interface`，且此型別不需要建構子邏輯，`sealed interface` 更輕量、與多數既有寫法一致。`HomeUiState`、`CollectUiState`、`HistoryUiState` 的 `sealed class` 寫法視為統一前的不一致，本次一併修正 `HomeUiState`。

```kotlin
// shared/common/src/commonMain/kotlin/com/shang/jetpackmoviekmp/common/UiState.kt
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val throwable: Throwable) : UiState<Nothing>
}

fun <T> AppResult<T>.toUiState(): UiState<T> = when (this) {
    is AppResult.Success -> UiState.Success(data)
    is AppResult.Failure -> UiState.Error(error)
}
```
`AppError : Exception`（已由 `common-kernel` 規範），故 `UiState.Error(error)` 可直接把 `AppError` 當 `Throwable` 帶入，不需額外轉換。

**2. 以 `typealias` 保留 feature 端型別名稱，而非直接改用 `UiState<ConfigurationBean>` 內聯**
考慮過直接讓 `MainViewModel.configuration: StateFlow<UiState<ConfigurationBean>>` 內聯泛型，但這會：
(a) 破壞 `android-app-entry`、`android-home-module` spec 中明確寫出的型別名稱（`MainUiState.Loading`、`HomeUiState`），需要同步修改兩份 spec；
(b) 降低呼叫端可讀性（`is UiState.Success<ConfigurationBean>` 不如 `is MainUiState.Success` 直觀）。
改用 `typealias MainUiState = UiState<ConfigurationBean>` 後，Kotlin 允許透過別名存取巢狀 classifier（`MainUiState.Loading`、`MainUiState.Success`、`is MainUiState.Error` 皆可直接編譯），現有 `when` exhaustive 分支與 spec 文字都不需異動，同時仍消除了重複的 sealed type 定義與 mapping 邏輯。

**3. `DetailSectionState<T>` 直接刪除，不保留 typealias**
`DetailSectionState<T>` 本身就是泛型、且只在 `feature/detail` 內部使用（`movieRecommendations`、`movieActors` 兩處），沒有像 `MainUiState`/`HomeUiState` 一樣被 spec 具名引用（`android-movie-detail-module` spec 只描述行為，未提及型別名稱），因此直接改用 `UiState<List<MovieCardResult>>`／`UiState<List<MovieCastAndCrewBean.Cast>>`，不新增額外 typealias，避免多一層無意義的別名。

**4. `Error.throwable` 統一非 null**
`HomeUiState.Error(throwable: Throwable?)` 目前允許 null，但實際 mapping 來源 `AppResult.Failure.error: AppError`（非 null）從未產生 null throwable，nullable 純屬歷史不一致。統一為非 null 後，`HomeViewModel` 呼叫端若有 `throwable?.let { ... }` 之類的 null 檢查需一併簡化（改為直接使用）。

## Risks / Trade-offs

- **[Risk] typealias 對巢狀 classifier 的存取雖然是合法 Kotlin 語法，但部分工具（如某些 IDE 重構、KDoc 產生器）對 typealias 巢狀存取的支援不如原生型別完整** → Mitigation：僅在 `MainUiState`、`HomeUiState`、`MovieDetailUiState` 三處使用，範圍小；`./gradlew :androidApp:assembleDebug`、`./gradlew ktlintCheck` 與各 feature module 既有 unit test 可在編譯期立即驗證 typealias 是否正確展開。
- **[Risk] `HomeUiState.Error(throwable: Throwable?)` 改為非 null 是簽章變更，若有呼叫端依賴 null 分支（例如顯示不同文案）會編譯錯誤或行為改變** → Mitigation：修改前先搜尋 `feature/home` 內所有 `HomeUiState.Error` 建構與消費點，逐一確認並同步調整；`feature:home` 既有 unit test 需全數通過。
- **[Trade-off] `shared/common` 新增的 `UiState<T>` 目前只被 Android 端使用，iOS 端（`shared/app` commonMain 之外的 iosMain）不會消費它** → 屬於刻意的 Non-Goal（見上），因為 iOS 已有語意不同的 `*LoadState` 機制；`shared/common` 是 KMP 模組但型別本身不含平台相依程式碼，放在 commonMain 不影響 iOS 建置或 SKIE 匯出範圍（新型別未在任何 iOS 匯出邊界的 public API 中被引用）。

## Migration Plan

1. 在 `shared/common` 新增 `UiState.kt`（型別 + `toUiState()` extension）與對應 Android host test，確保 `common-kernel` Kover 80% 門檻不因新檔案而下降。
2. 逐一調整 `MainViewModel`／`MainUiState`（androidApp）→ 執行 `feature` 對應的既有 unit test，確認行為不變。
3. 調整 `HomeViewModel`／`HomeUiState`（feature/home），同步修正 `Error.throwable` 非 null 簽章與所有消費點。
4. 調整 `MovieDetailViewModel`／`MovieDetailUiState`，移除 `DetailSectionState<T>`，`movieRecommendations`／`movieActors` 改用 `UiState<T>`。
5. 每個模組調整後個別執行 `./gradlew :<module>:testAndroidHostTest`（或既有 Android JVM test task），最後執行 `./gradlew :androidApp:assembleDebug` 與 `ktlintCheck` 做整體驗證。
6. 無需 Room migration、無資料庫 schema 變更；無需 iOS simulator test（本次不觸碰 iosMain／expect-actual／Swift interop 邊界）。

## Open Questions

（無待決問題；範圍已於討論階段與使用者確認。）
