## Context

`feature/search` 目前直接向 `MovieRepository` 取得搜尋 Pager，搜尋結果未與收藏 id 資料流合併；畫面也未將 `MovieCard` 的收藏事件交給 ViewModel。因此收藏資料庫已異動時，既有搜尋卡片不會更新收藏圖示。使用者已先接上部分電影點擊導覽與收藏切換程式碼，仍需依專案既有 domain UseCase 與收藏狀態標記模式完成整合。

## Goals / Non-Goals

**Goals:**

- 搜尋結果的電影卡可開啟既有電影詳情導覽，並可切換收藏狀態。
- 搜尋 Pager 與 `getCollectedMovieIds()` 合併，讓已載入結果隨收藏資料流更新 `isCollect`。
- 遵循既有 MVVM、Repository、UseCase 與 Koin module 分層，並以單元測試保護搜尋與收藏狀態行為。

**Non-Goals:**

- 不調整 TMDB 搜尋 API、資料庫 schema 或 Room migration。
- 不改變收藏頁、首頁、歷史頁或 iOS 端的收藏操作。
- 不新增外部依賴，也不變更搜尋關鍵字 debounce、Paging 載入或錯誤處理策略。

## Decisions

### 以 `GetSearchMovieListUseCase` 封裝收藏狀態合併

在 `shared/domain` 新增 UseCase，取得 Repository 搜尋 Pager 後，以 `combine` 合併收藏 id 資料流，並對每筆 `PagingData` 更新 `isCollect`。這與 `GetHomeMovieListUseCase` 的既有模式一致，使 feature 不必了解收藏 id 的資料來源。

替代方案是在 `SearchViewModel` 直接合併兩個 Repository flow；不採用，因為會讓 Android feature 跨越 domain 分層，且與首頁既有架構不一致。

### 收藏寫入保留於 SearchViewModel

`SearchViewModel` 接收 `MovieCardData` 的收藏點擊，依目前狀態呼叫既有 Repository 的新增或刪除收藏方法。資料庫資料流變動後，UseCase 自然重新標記 Pager，畫面透過 `collectAsLazyPagingItems()` 更新。

替代方案是點擊後直接修改畫面的局部 state；不採用，因為它會和資料庫實際狀態脫鉤，且無法同步其他畫面。

### 導覽事件由上層注入

`SearchScreen` 與 `searchEntry` 接收 `onMovieClick`，由 Android App 依既有 Navigation3 的詳情導覽模式提供。收藏事件只在 feature 內處理，避免讓導覽層承擔資料寫入職責。

## Risks / Trade-offs

- [收藏 id emission 時重新映射 PagingData 可能增加處理量] → 收藏清單規模通常有限，並轉為 `Set` 以降低每筆查詢成本；沿用首頁已驗證的模式。
- [已存在的局部實作可能與 UseCase 注入不一致] → 實作前先對照 Koin、測試 fake 與既有首頁模式，並補齊編譯與 ViewModel 測試。
- [收藏寫入失敗時卡片未立即切換] → 以資料庫實際 emission 為唯一狀態來源，避免顯示未持久化的樂觀結果。
