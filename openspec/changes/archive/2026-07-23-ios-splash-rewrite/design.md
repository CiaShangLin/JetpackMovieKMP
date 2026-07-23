## Context

`iosApp` 目前是原生 SwiftUI 專案，尚未有可運作的 Splash 畫面。`iosApp/iosApp/Splash/` 目錄下三個檔案現況：

- `SplashUiState.swift`：已定義 `enum SplashUiState { case loading; case success(data: ConfigurationBean); case failure(error: String) }`。
- `SplashView.swift`、`SplashViewModel.swift`：僅有檔頭註解，無任何內容。

`androidApp` 沒有對應的 Splash 畫面可參考，本次是全新設計，沒有既有 Android 實作可比照。

跨平台已具備的基礎設施：

- **`GetConfigurationUseCase`**（`shared/domain`）：`operator fun invoke(): Flow<Result<ConfigurationBean>>`。內部已處理「成功寫入快取」「失敗時退回本地快取」「兩者皆無才回傳錯誤」的邏輯，Splash 端不需重複實作快取退回。
- **`KoinHelper`**（`shared/app` iosMain，`ios-koin-bridge` capability）：具名 `KoinComponent` object，目前曝露 `userDataRepository()`、`getMovieDetailUseCase()`。慣例是消費端在組裝根（App 進入點或畫面組裝處）呼叫 `KoinHelper.shared.xxx()` 取得實例，再以建構子參數往下傳給 ViewModel；且只能在 `doInitKoinIos(isDebug:)` 呼叫之後使用。
- **SKIE**（`ios-skie-interop` capability）：`Flow<Result<T>>` 在 Swift 端可用 `for await` 迭代，但實機除錯確認 `kotlin.Result<T>` 會在 Swift 端呈現為 opaque boxed value（例如 `Optional(Success(ConfigurationBean(...)))`），不能直接用 `as? ConfigurationBean` 取出成功值。

**特殊協作前提**：使用者對 iOS/SwiftUI 不熟，本次 Swift 實作由使用者親自撰寫，Claude 僅逐步討論設計決策、review 程式碼、輔助除錯。因此本文件刻意把「SwiftUI 進場動畫效果」「View／ViewModel 生命週期收尾方式」「呼叫 KoinHelper 的時機」等需要教學與逐步確認的細節列為 Open Questions；經過討論後的決議已整理於下方 Decisions，僅剩實作階段才會浮現的細節仍列在 Open Questions。

**既有 iOS 程式碼現況（`iosApp/iosApp/Main/`）**：討論過程中發現既有的 `MainView.swift`／`MainViewModel.swift`（目前也還是雛型）提供了兩個可直接沿用的前例：
- `MainView` 直接在 View 屬性初始化處呼叫 `KoinHelper.shared.userDataRepository()` 建立 ViewModel，而非在 `iOSApp.swift` 的 `init()`。
- `MainViewModel` 目前是 `@MainActor final class MainViewModel: ObservableObject`，但 `MainView` 卻用 `@State private var mainViewModel = ...` 宣告它——`@State` 通常要搭配 `@Observable` class 才能正確追蹤變化，搭配一般 `ObservableObject` 現況很可能是不正確的組合（目前因為 `MainViewModel` 完全是空殼、沒有任何 `@Published` 屬性，所以還看不出實際問題）。此為既有程式碼的潛在問題，不在本次 Splash change 的修改範圍內，僅記錄於此供後續參考。

## Goals / Non-Goals

**Goals:**
- Splash 顯示時播放進場動畫（Logo 淡入 + 輕微縮放，見 Decisions）。
- Splash 啟動時透過 `KoinHelper.shared.getConfigurationLoader()` 取得 iOS 友善的 configuration loader，呼叫一次 configuration 拿取，並將結果映射到 `SplashUiState`（`loading` → `success` 或 `failure`）。
- 依循既有 `ios-koin-bridge` 慣例新增 `KoinHelper` accessor，不繞過具名 accessor 模式直接暴露 reified generic。
- 在 Kotlin iOS bridge 端將 `Result<ConfigurationBean>` 轉成明確的 `IosConfigurationLoadState`，避免 Swift 直接拆 Kotlin `Result`。

**Non-Goals:**
- 不處理主題（theme）切換。
- 不處理語言（language）切換。
- 不新增 Android 端對應的 Splash 畫面。
- 不全面解決 `openspec/backlog.md` 已記錄的「Swift 友善 Flow Result 匯出模型」架構債——本次只針對 configuration loading 加一層薄 wrapper。
- 不由 Claude 直接產出完整 Swift 實作程式碼——這是本次協作模式的明確排除項，而非技術範圍排除。

## Decisions

### 1. ViewModel 資料層透過 `IosConfigurationLoader` 使用 `GetConfigurationUseCase`，不繞道直接呼叫 Repository

沿用專案既有分層慣例（`network/database/datastore → data → domain → UI`），Splash 端透過 `IosConfigurationLoader` 使用 `domain` 層的 `GetConfigurationUseCase`，不直接注入 `MovieRepository`／`UserDataRepository`。這與 CLAUDE.md「優先沿用既有模式」的規範一致，也讓快取退回邏輯集中在 domain 層，SplashViewModel 保持單純。

**替代方案考慮**：讓 SplashViewModel 直接持有 Repository 並自行組裝快取退回邏輯——會重複 `GetConfigurationUseCase` 已寫好的邏輯，且違反既有分層慣例，不採用。

### 2. `KoinHelper` 新增具名 accessor，而非讓 SplashViewModel 自行呼叫 Koin reified generic

比照既有 `userDataRepository()`、`getMovieDetailUseCase()` 慣例，新增：

```kotlin
fun getConfigurationUseCase(): GetConfigurationUseCase = getKoin().get()
fun getConfigurationLoader(): IosConfigurationLoader = IosConfigurationLoader(getKoin().get())
```

`getConfigurationUseCase()` 保留為通用 accessor；`getConfigurationLoader()` 是本次 Splash 使用的 Swift-friendly wrapper。取得實例的時機與位置為 `SplashView` 屬性初始化處（見 Decision 4），遵循「組裝根取得實例、以建構子參數傳給 ViewModel」的既有慣例，不讓 ViewModel 內部直接呼叫 `KoinHelper.shared.xxx()`。

### 3. `SplashViewModel` 採用 `@Observable` macro，不沿用 `MainViewModel` 的 `ObservableObject` 風格

`SplashViewModel` SHALL 用 iOS 17+ 的 `@Observable` macro（Observation framework），而非
`ObservableObject` + `@Published`：

```swift
@Observable
@MainActor
final class SplashViewModel {
    private(set) var uiState: SplashUiState = .loading
    ...
}
```

`iosApp` 的 deployment target 為 18.2，`@Observable` 可安全使用。選擇原因：
1. `@Observable` 语法更簡單（不需手動加 `@Published`），對不熟 Swift 的使用者更容易上手。
2. `@Observable` class 搭配 `@State` 宣告在 View 端才是正確組合；`MainView` 現況用
   `@State` 宣告 `ObservableObject` 的 `MainViewModel`，兩者搭配可能不正確（見上方
   Context 的既有程式碼觀察），本次不重蹈覆轍。
3. SwiftUI 對 `@Observable` 的重繪追蹤更精準（只有畫面實際讀取且變化的屬性才觸發重繪）。

**替代方案考慮**：沿用 `ObservableObject` 讓風格與 `MainViewModel` 一致——優點是專案內
風格統一，但代價是繼承既有（很可能不正確的）`@State`／`ObservableObject` 搭配問題，且
語法對新手更繁瑣（需手動管理 `@Published`）。經與使用者討論後選擇 `@Observable`；
`MainViewModel` 未來是否統一改用 `@Observable` 留給獨立的重構 change 處理，不在本次
範圍內。

### 4. KoinHelper 呼叫時機：沿用 `MainView` 既有慣例，在 View 屬性初始化處取得實例

比照 `MainView` 現況（`@State private var mainViewModel = MainViewModel(userDataRepository: KoinHelper.shared.userDataRepository())`），`SplashView` SHALL 在自己的屬性初始化處呼叫
`KoinHelper.shared.getConfigurationLoader()`，建立並持有 `SplashViewModel`：

```swift
struct SplashView: View {
    @State private var viewModel = SplashViewModel(
        configurationLoader: KoinHelper.shared.getConfigurationLoader()
    )
    ...
}
```

這符合 `ios-koin-bridge` 規格「組裝根取得實例、以建構子參數傳遞」的慣例——這裡的
「組裝根」就是 `SplashView` 本身的宣告處，而非 `iOSApp.swift` 的 `init()`。選擇跟隨
既有 `MainView` 前例，維持專案內呼叫慣例一致，不引入第二種取得實例的方式。

### 5. App 進入點加入 Splash → Main 的 root 狀態切換

`iOSApp.swift` 目前直接顯示 `MainView`，本次 SHALL 加入一個 root 層級的狀態切換
（例如以 `@State private var isSplashFinished = false` 或一個簡單的 root enum），
在 `SplashViewModel` 回報 configuration 拿取成功後，切換顯示 `MainView`。

**替代方案考慮**：用 `NavigationStack` 做畫面導轉——對「啟動時的一次性畫面替換」而言
過重，且 Splash 完成後不應該讓使用者「返回」到 Splash，簡單的 root 狀態切換更符合語意，
故不採用 NavigationStack。

### 6. Configuration 拿取失敗時提供重試按鈕

`SplashUiState.failure(error:)` 對應的畫面 SHALL 顯示錯誤文案與一個重試按鈕，點擊後
重新呼叫 `IosConfigurationLoader`（重置 `uiState` 為 `.loading` 後再次觸發拿取流程）。
此情境理論上只會發生在「首次啟動且離線」（因為 domain 層已處理快取退回，只有本地
完全沒有快取時才會回傳 `Result.failure`），提供重試按鈕讓使用者離線恢復後不需要
重新啟動整個 App。

### 7. 非同步呼叫的 concurrency context

`SplashViewModel` 標記為 `@MainActor`（沿用 `MainViewModel` 既有慣例），呼叫
`IosConfigurationLoader` 的 `for await` 迴圈由 `SplashView` 的 `.task { }` modifier
啟動（而非 `onAppear` 手動建立 `Task { }`），讓 SwiftUI 自動管理該非同步工作的生命週期
（View 消失時自動取消）。這是實作階段的技術細節，非產品決策，Claude 會在使用者實作時
提供具體程式碼建議。

### 8. Flow 結果處理改用 iOS-friendly state wrapper

實測顯示 `GetConfigurationUseCase` 回傳的 `Flow<Result<ConfigurationBean>>` 在 Swift 端迭代時會取得 `Optional<AnyObject>`，內容為 `Success(ConfigurationBean(...))`，不是 `ConfigurationBean` 本體。因此本次在 `shared/app` iosMain 新增 `IosConfigurationLoader`，由 Kotlin 端先把 `Result` 拆成明確狀態：

```kotlin
sealed interface IosConfigurationLoadState {
    data class Success(val data: ConfigurationBean) : IosConfigurationLoadState
    data class Failure(val message: String) : IosConfigurationLoadState
}
```

Swift 端只需判斷 `IosConfigurationLoadStateSuccess`／`IosConfigurationLoadStateFailure`，不直接拆 Kotlin `Result`。

**替代方案考慮**：全面設計通用 Swift 友善的 sealed result wrapper（呼應 backlog 的「評估 Swift 友善的 Flow Result 匯出模型」項目）——這是較大的架構改動，範圍超過補完 Splash 畫面。本次採局部 wrapper 解決已確認的 configuration loading 問題。

## Risks / Trade-offs

- **[Risk] Swift 端直接拆 Kotlin `Result` 會拿到 opaque boxed value** → Mitigation：本次新增 `IosConfigurationLoader`，在 Kotlin 端先轉成 `IosConfigurationLoadState.Success/Failure` 後再匯出給 Swift。
- **[Risk] 使用者不熟 SwiftUI，進場動畫與生命週期實作可能需要多輪來回討論** → Mitigation：這是本次協作模式的預期成本，design.md 與 tasks.md 刻意把相關細節留白／標記為待討論項，避免 Claude 一次幫使用者做完決策後才發現不符合預期。
- **[Risk] `KoinHelper` 呼叫時機若選在非組裝根位置（例如 ViewModel 內部）** → Mitigation：實作前先與使用者確認呼叫位置，並在 review 階段檢查是否違反既有慣例。

## Open Questions（實作階段才會浮現的細節，待使用者動手實作時逐一討論）

1. 動畫確切時長／曲線（例如 0.6 秒 easeOut）留待實作時邊看邊調整，非本文件需先定案的項目。
2. 錯誤文案的具體措辭（例如「載入失敗，請檢查網路連線」）留待實作時討論，不影響本次架構決策。
3. `SplashUiState.failure(error: String)` 的 `error` 內容由 `IosConfigurationLoader` 從 Kotlin failure message 轉出；若未來要統一所有 iOS Flow Result 橋接，可另開 change 處理通用 wrapper。
