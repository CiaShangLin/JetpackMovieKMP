# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 首頁 feature 層消費 domain UseCase 的架構設計（CoroutineScope 注入方式）
- 類型: feature
- 記錄日期: 2026-07-19
- 來源: migrate-domain-to-commonmain（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

### 背景

`GetHomeMovieListUseCase.invoke(withGenres, scope: CoroutineScope)` 需要呼叫端提供
`CoroutineScope` 給 `cachedIn`。這個專案的 iOS 端（`iosApp`）是原生 SwiftUI，`shared`
模組沒有掛 Compose Multiplatform plugin，所以 Swift 無法直接建立/傳入 `CoroutineScope`。

### 決策方向

1. 不要把這個 scope 改成 app 層級單例注入（比照 `common/di/CommonModule.kt` 現有的
   app-scoped `single<CoroutineScope>`）——那個 scope 是設計給 `DatastoreLanguageProvider`
   這類永不停止的背景 provider 用，若拿來給 `cachedIn` 用，快取會永遠不會釋放（使用者離開
   首頁、切換類型篩選都不會回收，等同記憶體洩漏）。`cachedIn` 的 scope 本質是「畫面該活
   多久」的生命週期問題，不是無狀態資源，不能比照 `ioDispatcher` 那樣全域注入。
2. 建議在 `commonMain` 新增一個 `HomeScreenModel`（純 Kotlin class，不繼承 androidx
   `ViewModel`），自己建立並持有 `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`，
   暴露 `observeMovies(withGenres)` 呼叫 `GetHomeMovieListUseCase.invoke(withGenres, scope)`，
   並提供 `onDestroy()` 取消該 scope。`GetHomeMovieListUseCase` 簽名維持不變。
   - 刻意不讓 `HomeScreenModel` 直接繼承 JetBrains KMP `ViewModel`（`org.jetbrains.androidx.lifecycle`）：
     那套的自動收尾機制是設計給 Compose Multiplatform 的 `ViewModelStoreOwner` 走的，在原生
     SwiftUI（沒有 Compose Multiplatform 承載）能否正確手動觸發收尾沒有把握，改用手動
     `CoroutineScope` + `onDestroy()` 更保證兩平台行為一致。
3. Android 端：用一個真正的 `androidx.lifecycle.ViewModel` 包一層 `HomeScreenModel`，
   `onCleared()` 呼叫 `homeScreenModel.onDestroy()`。透過 Koin 的 `viewModel { HomeViewModel(get()) }`
   DSL 注入（`koin-compose-viewmodel` 已經在 `gradle/libs.versions.toml` 版本目錄裡，但目前
   還沒接到任何模組，之後要記得加進 `androidApp`）。
4. iOS 端：`HomeScreenModel` 走 Koin 解析，但 Swift 沒辦法呼叫 Kotlin 的 reified generic
   `get<T>()`，需要在 commonMain 寫一個明確的橋接物件（比照 Koin 官方 KMP 建議模式）：
   ```kotlin
   object KoinHelper : KoinComponent {
       fun homeScreenModel(): HomeScreenModel = getKoin().get()
   }
   ```
   Swift 端呼叫 `KoinHelper.shared.homeScreenModel()`，並在 SwiftUI 的 `.onDisappear`／包裝的
   `ObservableObject.deinit` 呼叫 `.onDestroy()`。目前專案完全還沒有任何 Koin↔Swift 橋接程式碼
   （`iosApp` 裡搜尋不到 `KoinHelper`／`koin.get<` 之類的東西），這是全新要補的部分。
5. 這整個 iOS 注入的需求**不需要用 `expect`/`actual` 處理**：`KoinHelper` 的邏輯在 Android／iOS
   上完全相同（不是平台實作差異，是 Swift↔Kotlin 語言互通問題），且即使未來 `domain` 拆成獨立
   Gradle module（`core:domain`），這 5 個 UseCase 都是純 commonMain 邏輯，沒有任何平台專屬
   程式碼需要 `expect`/`actual`，模組化與 `expect`/`actual` 是兩個不相干的問題。

### 適用範圍

下一個「首頁 feature 層」或「domain 模組化」相關 change 開始前，可以先翻這則記錄，不需要
重新推導這些取捨。
