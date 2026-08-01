## Context

Android 端 `feature:detail` 模組已完成電影詳情頁：主內容（`GetMovieDetailUseCase`）、收藏狀態（`MovieRepository.getMovieCollectEntityById`）、主要演員（`MovieRepository.getMovieActor`）、推薦電影（`GetMovieRecommendUseCase`）四條 `StateFlow` 各自獨立載入，互不阻塞；主內容失敗時整頁顯示 retry，演員／推薦失敗時只隱藏該區塊。導覽方面，Android 曾發生「entry provider 沒接上 `MovieDetailKey` 分支、實際邏輯寫在死代碼函式」的 bug（`fix-movie-detail-navigation`，commit `0135f85`），修法是把分支接進真正被 `NavDisplay` 使用的 `mainEntry()`，並用 `movableContentOf` 避免切換到 detail 容器時把列表頁的 remember 狀態（搜尋輸入框、tab 選擇）清空。

iOS 端目前無任何詳情頁或 push navigation 先例：`MainView.swift` 是純 `TabView`，四個列表頁（Home/Search/Favorites/History）皆用 `MovieCardView(onMovieTap:)` 但沒有任何頁面接上這個 callback。既有 iOS feature（History、即將完成的 Setting）採用一致模式：`@Observable @MainActor` ViewModel、建構子直接注入 shared 型別、透過 `KoinHelper.shared.xxx()` 在 View `init()` 取得依賴（組裝根就是各自的 View，不逐層轉送）、`for await` 消費 SKIE 轉出的 `Flow`／`suspend fun`。`KoinHelper` 已提供 `getMovieDetailUseCase()`、`getMovieRepository()`，尚缺 `getMovieRecommendUseCase()`。

## Goals / Non-Goals

**Goals:**
- iOS 電影詳情頁重現 Android 的四段獨立載入行為（主內容、收藏狀態、演員、推薦電影各自獨立 Loading／Success／Error，互不阻塞）。
- 首頁、搜尋、收藏、歷史四個列表頁的 `MovieCardView.onMovieTap` 皆可導覽進入詳情頁；詳情頁內推薦電影卡點擊可再推入下一層詳情頁（多層堆疊）。
- 沿用既有 iOS feature 的 ViewModel／KoinHelper／SKIE 消費模式，不引入新的狀態管理框架或第三方導覽套件。
- 避免重演 Android 曾踩過的「導覽分支寫在死代碼、未接上實際渲染路徑」問題。

**Non-Goals:**
- 不比照 Android 在進入 detail 時隱藏底部 Navigation Suite——iOS 採用 SwiftUI `NavigationStack` push 的標準行為，維持 Tab Bar 可見，這是刻意的平台慣例差異，不是待補的行為缺口。
- 不新增 iOS UI XCTest（比照既有 `ios-movie-history` 規格的先例，UI 測試不屬於本次 requirement）。
- 不修改 Android（`feature:detail`）或 `shared/data`／`shared/domain`／`shared/network` 既有邏輯，僅新增 iOS 消費端與一個 `KoinHelper` accessor。
- 不處理片長／類型以外的次要 detail 欄位呈現優化（例如預算、原產國、製作公司等 `MovieDetailBean` 欄位），沿用 Android 目前實際顯示的欄位子集（標題、評分、上映日期、片長、簡介、演員、推薦）。

## Decisions

### 1. UiState 建模：沿用 Android 的「主內容 + 獨立區塊狀態」分離模式

比照 `HomeContentViewModel` 既有的 `enum` 狀態寫法，`MovieDetailViewModel` 定義：

```swift
enum MovieDetailUiState {
    case loading
    case success(MovieDetailBean)
    case failure(String)
}

enum DetailSectionState<T> {
    case loading
    case success(T)
    case failure(String)
}
```

四個 `@Observable` 屬性各自獨立：`detailState: MovieDetailUiState`、`isCollected: Bool`、`castState: DetailSectionState<[MovieCastAndCrewBean.Cast]>`、`recommendationState: DetailSectionState<[MovieCardResult]>`。View 端依各自狀態渲染，演員／推薦失敗時只隱藏對應區塊、不影響主內容——與既有 `android-movie-detail-module` spec 規則一致。

**替代方案**：單一大 `UiState` 把四者包在一起。放棄原因：任一區塊變動都會觸發整個 View 重新評估、且失敗語意會被迫耦合（例如推薦失敗不該影響主內容的 Success 判斷），與 Android 既有設計脫節。

### 2. 資料載入：四條獨立 `Task`，非單一 `for await` 循序等待

`start()` 個別為四個資料來源各自建立一個 `Task { for await ... }`（比照 History 用 `.task { await viewModel.loadHistory() }` 觸發，但這裡是四個獨立觸發點），彼此不 `await` 對方完成。主內容與收藏狀態、演員、推薦電影平行送出各自的網路／資料庫請求。

- 主內容（`GetMovieDetailUseCase`）為一次性 `Flow`（單次 emit 後結束），`retryDetail()` 取消先前 `Task` 並重新呼又同一個 `movieId` 建立新 `Task`。
- 收藏狀態（`movieRepository.getMovieCollectEntityById`）為長駐觀察 `Flow`，跟隨 ViewModel 生命週期持續更新，不需要 retry 動作。
- 演員、推薦電影各自的請求失敗時**不提供 retry**，直接依 `DetailSectionState.failure` 隱藏區塊——與 `android-movie-detail-module` spec 的「credits／recommendations 失敗 MUST 隱藏區塊、MUST NOT 阻斷主內容」規則一致（該 spec 並未要求這兩區塊提供 retry）。

**替代方案**：用單一 `async let` 群組等待全部完成再一次性更新畫面。放棄原因：會讓任一區塊的慢請求拖住其他區塊的顯示時機，違背「各自獨立、互不阻塞」的既有行為。

### 3. 收藏切換：沿用 `MovieCollectAction` 既有模式

`toggleCollect(_ data: MovieCardResult)` 依 `movieCardIsCollect` 呼叫 `movieRepository.insertMovieCollect()` 或 `deleteMovieCollect()`，與 History／Home 既有寫法一致，不另外設計新的收藏抽象。

### 4. KoinHelper 擴充：新增 `getMovieRecommendUseCase()`

比照既有 `getMovieDetailUseCase()` 寫法在 `KoinHelper.kt`（iosMain）新增一行具名 accessor。這是 `ios-koin-bridge` spec 既定允許的擴充行為（規格原文：「並隨消費端需求持續新增對應的具名 accessor」），不視為該 spec 的需求變更，因此本次不建立 `ios-koin-bridge` 的 delta spec。

### 5. 導覽：每個 tab 各自的 `NavigationStack` + `navigationDestination(for: Int32.self)`

四個列表頁（Home/Search/Favorites/History）的根內容各自包一層 `NavigationStack`，並掛上 `.navigationDestination(for: Int32.self) { movieId in MovieDetailView(movieId: movieId) }`；`MovieCardView(onMovieTap:)` 改為 `{ movie in path.append(movie.movieCardId) }`（`movieCardId` 型別為 `Int32`，四處必須一致，否則 destination 無法解析）。詳情頁內推薦電影卡的點擊直接對同一個 `NavigationPath` 再 `append` 一次，形成多層 push；返回沿用系統返回手勢／按鈕，一次只 pop 一層。

`MovieDetailView` 在 `init()` 依 iOS 既有慣例透過 `KoinHelper.shared` 取得 `MovieRepository`、`GetMovieDetailUseCase`、`GetMovieRecommendUseCase` 建立 `MovieDetailViewModel`，不由 `MainView`／`MainTab` 逐層轉送依賴（比照 `ios-movie-history` spec 的既有規則）。

**替代方案 A**：比照 Android 用單一全域導覽容器（例如 App 層級唯一 `NavigationStack` 包住整個 `TabView`），並在進入 detail 時隱藏 Tab Bar。放棄原因：SwiftUI 對「TabView 外包一層 NavigationStack」的支援下，會讓所有 tab 共用同一個 push 堆疊，不符合 iOS 使用者對「detail 是屬於目前這個 tab」的預期，且 Tab Bar 隱藏需要額外的 `toolbar(.hidden, for: .tabBar)` 手動管理，增加與本次目標無關的複雜度。
**替代方案 B**：用 `.sheet`／`.fullScreenCover` 呈現 detail。放棄原因：與 Android 的「detail 是同層級可返回堆疊」語意不符，且推薦電影再點擊進入下一部 detail 時，sheet 疊 sheet 的 UX 較差，`NavigationStack` push 更自然支援多層堆疊與返回。

**避免重演 Android 導覽 bug 的作法**：`navigationDestination` 直接掛在每個 tab 實際渲染的根 View 上（`HomeContentView`／`SearchView`／`FavoritesView`／`HistoryView` 本身，而非另外包一層未被使用的 wrapper），逐一在四個 tab 手動點擊驗證會進入 detail，而非只驗證其中一個 tab 就假設其餘一致。

## Risks / Trade-offs

- **[Risk]** 四個 tab 各自宣告 `.navigationDestination(for: Int32.self)`，若其中一處誤用其他型別（例如包一層自訂 struct）會導致該 tab 點擊卡片沒有反應且不易發現。
  → **Mitigation**：統一約定「一律用 `Int32`（`movieCardId` 原生型別）作為 path element」，並在 tasks 中把四個 tab 的接線拆成各自獨立步驟，逐一手動驗證。
- **[Risk]** 詳情頁四條獨立 `Task` 若未在 View 消失時取消，可能造成不必要的網路請求或記憶體殘留（例如使用者快速返回）。
  → **Mitigation**：比照既有 `@Observable` ViewModel 生命週期綁定 View（`MovieDetailView` 持有 `@State private var viewModel`），View 消失時隨 `@State` 釋放自然取消底層 `Task`；`retryDetail()` 呼叫前先 cancel 前一個 `Task` 避免重複並行請求。
- **[Risk]** `KoinHelper` 新增 accessor 若忘記在 Koin 尚未初始化前呼叫（例如 Preview 或測試情境），會 crash。
  → **Mitigation**：沿用既有 `MovieDetailView` 只在 `iosApp` 正式執行環境（Koin 已於 App 啟動時初始化）建立 ViewModel 的既有慣例，不在 SwiftUI `#Preview` 中直接建構真實 ViewModel。
- **[Trade-off]** 不比照 Android 隱藏 Tab Bar，行為與 Android 版本不完全一致。
  → 已在 Non-Goals 明確定義為刻意的平台慣例差異，而非遺漏。

## Open Questions

- 演員／推薦電影區塊目前依 Android spec 不提供 retry，若後續使用者回饋需要，屬於下一次獨立變更，不在本次範圍內先行設計。
