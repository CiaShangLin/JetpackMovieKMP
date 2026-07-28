## Context

shared 層已透過 Room KMP 持久化 `MovieCollectEntity`，並由 `MovieRepository` 提供收藏清單的 `Flow`、新增與刪除 suspend API。首頁的 `GetHomeMovieListUseCase` 也已合併收藏 id，使重新發出的卡片資料反映 `isCollect`。iOS 端已具備 SKIE，可將 shared 的 `Flow` 消費為 `AsyncSequence`，且 `KoinHelper` 已公開 `getMovieRepository()`；但收藏 tab 仍是 placeholder，首頁卡片的 callback 未接線。

本 change 僅規劃 iOS 的手動實作；Android 收藏功能保持原樣。iOS 的 SwiftUI 畫面必須在主執行緒更新觀察狀態，並在 ViewModel 生命週期結束時停止長期 Flow 觀察。

## Goals / Non-Goals

**Goals:**

- 以既有 shared 資料來源顯示 iOS 收藏電影，並在資料變動時即時更新。
- 讓首頁和收藏頁的 `MovieCardView` 都能加入或移除收藏，並維持愛心狀態一致。
- 將 shared 依賴從組裝根以建構子注入 iOS ViewModel，讓 Swift 邏輯可單元測試。
- 在不改變 Room schema、Repository 契約或 Android 行為的前提下完成串接。

**Non-Goals:**

- 不新增雲端帳號、跨裝置同步、收藏分類、排序／搜尋或批次管理。
- 不實作電影詳情導覽或收藏失敗的持久化重試佇列。
- 不將 iOS UI 移至 Compose Multiplatform，亦不重構 Android `feature:collect`。
- 不新增 shared Domain UseCase；此處沿用 Android 收藏功能既有的 Repository 直接存取模式。

## Decisions

### 1. 以既有 `MovieRepository` 作為 iOS 收藏資料來源

`MainTab`／`FavoritesView` 的組裝根透過 `KoinHelper.getMovieRepository()` 取得單一 shared Repository，再以建構子傳入 `FavoritesViewModel` 與首頁所需的收藏操作物件。ViewModel 以 SKIE 將 `getAllMovieCollect()` 的 `Flow<List<MovieCardResult>>` 迭代為 `AsyncSequence`，每次 emission 轉為 `MovieCardData` 並更新 SwiftUI state；新增／移除以 `insertMovieCollect()`／`deleteMovieCollect()` 執行。

理由：Repository、Room 與 Koin bridge 已存在，能維持 Android 與 iOS 對同一個本機資料庫實例的讀寫語意，且避免為單一 iOS 畫面新增重複 UseCase 或 Presenter。

替代方案：在 `shared/domain` 新增 `GetCollectedMovieListUseCase` 和 `ToggleMovieCollectUseCase`。此方案能讓抽象更整齊，但會偏離既有 Android `CollectViewModel` 直接使用 Repository 的模式，且沒有新增跨平台 business rule，因此不採用。

### 2. 收藏操作集中為可重用的 iOS 方法

建立可由 `FavoritesViewModel` 與首頁持有者共用的收藏切換方法：輸入 `MovieCardData`，依 `movieCardIsCollect` 決定呼叫新增或刪除 API。呼叫期間在非主執行緒等待 shared suspend API，SwiftUI state 只由 shared Flow 後續 emission 更新，不在 UI 端樂觀改寫清單。

理由：單一切換語意可避免首頁和收藏頁對 `isCollect` 的判斷分岔；以資料庫 emission 作為畫面真實來源，也避免寫入失敗時顯示錯誤狀態。

替代方案：各 View 自行判斷與呼叫 Repository，或在點擊時立即改寫本地 UI。前者容易重複與不一致；後者需要 rollback 與錯誤處理，本次不採用。

### 3. 首頁將 callback 接到現有長生命週期擁有者

`HomeContentView` 將 `MovieCardView.onCollectTap` 傳遞至由 `HomeViewModel`（或注入的專責收藏操作物件）提供的方法；不得在每張卡片內取得 Koin 依賴。原有 `HomeMovieListPresenter` 持續接收 `GetHomeMovieListUseCase` 的收藏 id 變化，更新後的 snapshot 讓卡片重新渲染。

理由：保持 `MovieCardView` 為純展示＋callback 元件，符合既有 specification，並避免 SwiftUI 格線中多個卡片各自建立資料觀察。

替代方案：讓 `MovieCardView` 直接使用 `KoinHelper`。這會破壞元件可重用性與測試替換能力，因此不採用。

### 4. 收藏頁採用既有 MovieCard 格線與本機資料空狀態

`FavoritesView` 取代 placeholder，使用 `ScrollView` + `LazyVGrid` 與首頁一致的欄寬／spacing token 顯示電影卡片；清單為空時顯示在地化空狀態。按卡片愛心即取消收藏，當 shared Flow 發出空清單時自動切回空狀態。

理由：和 Android 收藏頁及 iOS 首頁維持一致的資訊密度，並重用既有 `MovieCardView`。

## Risks / Trade-offs

- [SKIE 對 `MovieRepository` 的 suspend／Flow 匯出名稱或迭代型別與預期不同] → 實作前先以現有 `HomeViewModel`／`SplashViewModel` 的消費方式驗證；若無法安全呼叫，才在 `shared/app` 新增最小、具名的 bridge，並新增 Kotlin 測試。
- [首頁 Paging snapshot 未因收藏 id Flow 更新而刷新卡片] → 以首頁加入／移除後的愛心狀態與收藏 tab 同步作為人工驗收；若不會刷新，調整既有 iOS Presenter 的 pages-update 觀察，而非在 View 偽造狀態。
- [快速連點造成寫入順序反轉] → 收藏切換方法在單一 ViewModel 的 Task 中序列化操作，並以資料庫 emission 作為最終畫面狀態。
- [取消最後一筆收藏造成格線迭代中的資料失效] → `ForEach` 使用電影 id 作為穩定 identity，並以最新 emission 整體重建可顯示陣列。
- [Room schema 變更風險] → 本次不變更 entity／DAO／schema，無 migration。

## Migration Plan

1. 保留既有 `FavoritesView` 檔案路徑，以新實作取代 placeholder；不需要資料遷移。
2. 先完成 shared API 可由 iOS 呼叫的驗證，再接上 ViewModel 與首頁 callback，避免 UI 完成後才發現跨語言互通問題。
3. 驗證既有收藏資料在升級後仍可顯示、取消收藏會即時消失、重新開啟 app 後資料仍存在。
4. 若 SwiftUI 實作發生回歸，回退 iOS app 程式碼即可；本次不含 schema 或 API contract 破壞性變更。

## Open Questions

- 收藏寫入失敗時，第一版是否僅以既有錯誤 log 記錄，或需要額外顯示使用者可見的 Toast／alert？建議第一版先沿用資料庫本機操作的無顯性錯誤 UI，僅保留可擴充的錯誤入口。
