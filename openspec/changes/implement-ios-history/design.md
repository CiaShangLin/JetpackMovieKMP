## Context

Android `feature:history` 已透過 `GetHistoryMovieListUseCase` 將觀看紀錄與收藏 id
合併，並提供清單、空狀態、切換收藏與清空功能。iOS 的 `HistoryView` 目前只顯示
`main_history_placeholder`；但 shared database、repository 與 domain UseCase 均已支援
iOS，且 SKIE 已將 Kotlin `Flow` 匯出為可供 Swift `for await` 消費的序列。

iOS 現有 `FavoritesView`／`FavoritesViewModel` 是最接近的 presentation 模式：由
`IosApp` composition root 取得 shared 依賴、建構子傳遞至 SwiftUI View、`@MainActor`
`@Observable` ViewModel 監聽 Flow，再以 `MovieCardView` 建立自適應格線。本變更只補足
iOS presentation 與 Koin bridge，不更動既有跨平台資料契約。

## Goals / Non-Goals

**Goals:**

- 使 iOS history tab 的行為與 Android history feature 對齊：反應式清單、空狀態、收藏切換與清空全部。
- 維持收藏狀態由 shared `GetHistoryMovieListUseCase` 單一來源計算，避免 iOS 重複合併資料。
- 重用既有 SwiftUI 格線、`MovieCardView`、design token 與 String Catalog 慣例。
- 讓可單獨測試的狀態轉換及命令決策位於 Swift ViewModel／小型 action 型別，而非 View。

**Non-Goals:**

- 不新增、排序或遷移歷史資料庫 schema，也不改變歷史資料的寫入時機。
- 不實作電影詳情導航、單筆刪除、搜尋、確認對話框、loading/error 畫面或 Android UI 重構。
- 不新增 Swift Package 或第三方依賴。

## Decisions

### 1. 透過 KoinHelper 匯出 GetHistoryMovieListUseCase，並由 AppDependencies environment 注入

`shared/app` 的 `KoinHelper` 新增具名 `getHistoryMovieListUseCase()` accessor；`IosApp`
在完成 `doInitKoinIos` 後取得它與其他 shared 依賴，建立單一 `AppDependencies`，再以
SwiftUI environment 注入畫面樹。各 feature View 從 environment 讀取其所需依賴，並以
明確建構子參數建立自己的 ViewModel；View 與 ViewModel 都不直接存取全域 Koin。

目前 `MainTab.content(movieRepository:)` 會將 Repository 傳入所有 tab，即使大多數 tab
不使用它。改為 environment 後，`MainView` 僅持有 `AppDependencies`，不會因新增 feature
依賴而增加 `MainView`、`MainTab` 或每個 tab 的函式參數；依賴清單仍集中於真正的
composition root `IosApp`，這是可預期且可檢視的組裝責任。

不直接在 iOS 用 `MovieRepository.getAllMovieHistory()`：該 Flow 未合併收藏狀態，會使
history 卡片在其他頁調整收藏後無法正確同步。也不建立 iOS 專屬 wrapper UseCase，因為
shared `GetHistoryMovieListUseCase` 已是跨平台的完整 domain 契約。

### 2. 以 @MainActor @Observable ViewModel 消費 shared Flow 並映射 UI state

建立 `HistoryViewModel`，持有 `GetHistoryMovieListUseCase` 與 `MovieRepository`；
`loadHistory()` 以 `for await` 持續消費 UseCase 的輸出，在主執行緒將空陣列轉成 `.empty`，
其餘轉成 `.success(data:)`。`HistoryView` 以 `.task` 啟動一次監聽，取消 task 即停止迴圈。

此做法沿用 Favorites 的 Observation/MVVM 模式，而不是在 View 直接處理非同步資料與寫入。
ViewModel 同時處理收藏切換與清空，讓 SwiftUI 僅發出使用者意圖。

### 3. 歷史頁重用 MovieCardView 與 Favorites 的自適應格線

成功狀態以 `ScrollView` + `LazyVGrid` 顯示 `MovieCardResult`，`ForEach` 以電影 id 維持
identity，並將 `asMovieCardData()` 交給 `MovieCardView`。空狀態使用系統 `clock.arrow.circlepath`
圖示與專屬在地化文字。清單標題與「清空」按鈕放在格線前方；按鈕呼叫 ViewModel 的
`clearHistory()`，不在畫面中手動移除項目，等待 shared Flow 的資料更新作為唯一真實狀態。

這可維持 `ios-movie-card` 的互動與尺寸契約；不複製 Android 的 Compose 元件或 Android
drawable。清空行為比照 Android：直接執行、沒有確認對話框。

### 4. 寫入操作重用收藏決策，並避免重複觸發

歷史 ViewModel 使用既有 `MovieCollectAction(data:)` 決定呼叫 `insertMovieCollect()` 或
`deleteMovieCollect()`，以與首頁、收藏頁一致的錯誤處理方式執行。收藏切換與清空各自
以 in-flight guard 避免使用者連續點擊產生重複寫入；失敗時保留 Flow 所反映的目前 UI
狀態並記錄診斷訊息。

替代方案是將所有 repository 呼叫直接寫在 View 或立即樂觀更新 `uiState`。前者破壞現有
可測試的 MVVM 邊界，後者可能與資料庫失敗或 Flow 排程不同步，因此不採用。

### 5. 更新 String Catalog 與導覽 delta spec

新增 history 空狀態、標題、清空等顯示文案的 `en`／`zh-Hant` 翻譯；移除不再被使用的
`main_history_placeholder` key。`MainTab` 既有 `.history` case 保留，只改為將所注入的
UseCase 傳給可用的 `HistoryView`。因此需修改底部導覽規格中「history 為 placeholder」的
既有 requirement。

## Risks / Trade-offs

- [SwiftUI environment 使 feature View 隱含依賴 AppDependencies] → environment 只負責組裝
  邊界；History ViewModel 保持以 UseCase 與 Repository 的明確 initializer 接收依賴，單元測試
  直接建立 ViewModel 或其可測試的 state/action 型別，不需測試 Main 或 SwiftUI View。
- [SKIE 匯出的 UseCase 呼叫或 Flow 命名與預期不同] → 先以目前 Favorites 的 `for await`
  用法確認產生的 Swift API；必要時依 framework 實際匯出名稱調整，不建立重複 bridge。
- [持續監聽因 tab 生命週期重建而有多個訂閱] → 監聽僅在 `HistoryView.task` 內啟動，並讓
  Swift concurrency 在 View 消失時取消；不要在 `init` 啟動 detached Task。
- [使用者快速重複點擊造成重複寫入] → 以 ViewModel 內的操作中旗標防護，完成或失敗後重設。
- [清空動作失敗時使用者缺乏明確回饋] → 第一版沿用既有 Favorites 的診斷輸出與 Flow 驅動
  顯示；後續若需要可另提案增加可見錯誤 UI。

## Migration Plan

1. 先新增 Koin bridge accessor 與其 shared iOS 測試，確保 Swift 可解析 UseCase。
2. 新增 `AppDependencies` environment，將目前由 `MainView`／`MainTab` 轉送的 Repository
   改為在 feature 端讀取所需依賴。
3. 新增 Swift history ViewModel、UI state、View、String Catalog 文案與 XCTest。
4. 將 history tab 改接新頁面，執行 iOS build／tests 與 Swift style tasks。
4. 此變更不包含資料遷移；若需回退，只需還原 iOS presentation 與 accessor，既有歷史資料不受影響。

## Open Questions

- 無。第一版明確比照 Android 採直接清空，不增加確認對話框。
