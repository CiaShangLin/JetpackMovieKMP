## Context

目前 `MainUiState`（androidApp）、`HomeUiState`（feature/home）、`MovieDetailUiState` + `DetailSectionState<T>`（feature/detail）各自定義形狀相同的 `Loading/Success/Error` sealed type，且每個 ViewModel 手寫 `AppResult -> XxxUiState` 的 `when` mapping。`shared/common` 已有 `AppResult<T>`／`AppError`（`common-kernel` spec 規範），是資料層的錯誤處理型別，但沒有對應的 UI 層狀態型別，導致每個 feature 自行「翻譯」一次。

`CollectViewModel`／`HistoryViewModel`（`Empty/Success`，無 Loading/Error）、`HomeContentViewModel`／`SearchViewModel`（AndroidX Paging 自身 `LoadState`）、`SettingViewModel`（純 `StateFlow<UserData>`）三類 ViewModel 的狀態語意與 `Loading/Success/Error` 不同構，不在本次調整範圍內。

## Goals / Non-Goals

**Goals:**
- 在 `shared/common` 提供單一 `UiState<T>` 泛型型別，取代 `MainUiState`、`HomeUiState`、`MovieDetailUiState`、`DetailSectionState<T>` 四個獨立定義。
- 提供 `AppResult<T>.toUiState()` extension，消除各 ViewModel 手寫的 `AppResult -> UiState` mapping 樣板。
- `androidApp`、`feature/home`、`feature/detail` 呼叫端全面改用 `UiState<T>`（`UiState.Loading`、`UiState.Success`、`is UiState.Error`），不再保留 `MainUiState`、`HomeUiState`、`MovieDetailUiState` 這些 feature-local 型別名稱；`android-app-entry`、`android-home-module` 兩份 spec 中引用這些型別名稱的文字同步改寫（見本次 change 的 `specs/android-app-entry/spec.md`、`specs/android-home-module/spec.md`）。
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

**2. 呼叫端直接內聯使用 `UiState<ConfigurationBean>`，不透過 `typealias` 保留 feature 端型別名稱**
原本考慮用 `typealias MainUiState = UiState<ConfigurationBean>` 保留既有型別名稱與呼叫端寫法（`MainUiState.Loading`），但實作時發現這個假設在 Kotlin 不成立：**typealias 無法讓呼叫端透過別名存取被別名型別的巢狀 classifier**——`typealias MainUiState = UiState<ConfigurationBean>` 定義後，`MainUiState.Loading` 會編譯失敗（`Unresolved reference 'Loading'`），因為 `Loading`／`Success`／`Error` 是定義在 `UiState` 上的巢狀型別，Kotlin 的 typealias 只取代型別參照本身，不會把巢狀 classifier 的查找路徑也轉發到別名上。

因此改為直接讓 `MainViewModel.configuration: StateFlow<UiState<ConfigurationBean>>` 內聯泛型，呼叫端一律改寫為 `UiState.Loading`／`UiState.Success`／`is UiState.Error`。這會：
(a) 是 **BREAKING** 變更：`MainUiState`、`HomeUiState`、`MovieDetailUiState` 型別名稱不再存在，`android-app-entry`、`android-home-module` spec 中明確寫出這些型別名稱的文字需要同步修改（已在本次 change 的 `specs/android-app-entry/spec.md`、`specs/android-home-module/spec.md` 處理）；
(b) 換得型別系統的單一事實來源（`shared/common.UiState<T>`），不需要為每個 feature 額外維護一個 typealias 宣告檔案。

**3. `DetailSectionState<T>` 直接刪除**
`DetailSectionState<T>` 本身就是泛型、且只在 `feature/detail` 內部使用（`movieRecommendations`、`movieActors` 兩處），沒有像 `MainUiState`/`HomeUiState` 一樣被 spec 具名引用（`android-movie-detail-module` spec 只描述行為，未提及型別名稱），因此直接改用 `UiState<List<MovieCardResult>>`／`UiState<List<MovieCastAndCrewBean.Cast>>`，與決策 2 的做法一致。

**4. `Error.throwable` 統一非 null**
`HomeUiState.Error(throwable: Throwable?)` 目前允許 null，但實際 mapping 來源 `AppResult.Failure.error: AppError`（非 null）從未產生 null throwable，nullable 純屬歷史不一致。統一為非 null 後，`HomeViewModel` 呼叫端若有 `throwable?.let { ... }` 之類的 null 檢查需一併簡化（改為直接使用）。

## Risks / Trade-offs

- **[Risk] 移除 `MainUiState`／`HomeUiState`／`MovieDetailUiState` 型別名稱是 BREAKING 變更，任何未同步更新的呼叫端會編譯失敗** → Mitigation：透過 `./gradlew :androidApp:assembleDebug`、各 feature module 既有 unit test 在編譯期立即攔截；`android-app-entry`、`android-home-module` 兩份 spec 中引用舊型別名稱的文字已同步改寫為 `UiState<T>`。
- **[Risk] `HomeUiState.Error(throwable: Throwable?)` 改為非 null 是簽章變更，若有呼叫端依賴 null 分支（例如顯示不同文案）會編譯錯誤或行為改變** → Mitigation：修改前先搜尋 `feature/home` 內所有 `HomeUiState.Error` 建構與消費點，逐一確認並同步調整；`feature:home` 既有 unit test 需全數通過。
- **[Risk] `UiState.Success<T>` 的欄位統一命名為 `data`，與原本各 feature 各自取的欄位名（`MainUiState.Success.data`、`HomeUiState.Success.movieGenres`、`MovieDetailUiState.Success.movie`）不一致，消費端需同步改寫欄位存取** → Mitigation：`HomeScreen.kt`（`state.movieGenres` → `state.data`）、`MovieDetailScreen.kt`（`state.movie` → `state.data`）已同步修改，並由既有 UI 邏輯測試與編譯期檢查覆蓋。
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
