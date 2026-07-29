## Context

`JetpackMovieCompose`（舊專案）的 `feature/history` 使用 Hilt 注入、classic Navigation Compose 與 Material2 遺留寫法，並直接在 ViewModel 內組合 `MovieRepository` 與 `GetHistoryMovieListUseCase`。`JetpackMovieKMP`（新專案）已透過遷移 `feature/collect` 建立了一套 Android feature module 的標準寫法（Koin、Navigation3、Material3、`sealed class` UI state），且 shared 層（`shared/database`／`shared/data`／`shared/domain`）早已具備 history 所需的 entity、DAO、Repository 方法與 `GetHistoryMovieListUseCase`，皆有測試覆蓋。本次設計的核心是「用新專案的 UI 層慣例，重寫舊版 history 畫面邏輯」，而非新增 shared 層能力。

## Goals / Non-Goals

**Goals:**
- 新增 `feature:history` 模組，畫面行為（顯示歷史清單、切換收藏、清空歷史、空狀態）與舊版一致
- UI 層架構（DI、導覽、狀態管理、元件）與 `feature:collect` 保持一致，降低後續維護成本
- 將 history 頁接回 `androidApp` 既有、已預留的 `MainNavItem.HISTORY` 底部導覽項目

**Non-Goals:**
- 不變更 shared 層任何程式碼或資料庫 schema（`MovieHistoryEntity`／`MovieHistoryDao`／`MovieRepository`／`GetHistoryMovieListUseCase` 皆維持現狀）
- 不處理 iOS 對應實作
- 不調整 history 清單的排序、分頁或快取策略（沿用 `GetHistoryMovieListUseCase` 現有行為）

## Decisions

### 1. 沿用 Repository + Use Case 混合模式，不做成純 Use Case 或純 Repository

`CollectViewModel` 只依賴 `MovieRepository`（因為收藏列表不需要額外合併邏輯）；但 history 列表需要「合併目前收藏狀態」，這個邏輯已經封裝在 `GetHistoryMovieListUseCase` 並有測試覆蓋。因此 `HistoryViewModel` 建構子注入 `GetHistoryMovieListUseCase`（取清單）與 `MovieRepository`（切換收藏、清空歷史）兩者，而非強行統一成單一依賴來源。這與專案既有慣例一致：能重用 Use Case 封裝的合併邏輯就重用，單純 CRUD 才直接呼叫 Repository。

**替代方案**：把「清空歷史」「切換收藏」也包成新的 Use Case。因為這兩個操作是單一 Repository 方法呼叫、無額外業務邏輯需要封裝／測試複用，新增 Use Case 只是多一層轉呼叫，故不採用。

### 2. UI State 採用 `sealed class HistoryUiState`（Empty／Success），比照 `CollectUiState`

舊版直接把 `List<MovieCardResult>` 用 `stateIn(initialValue = emptyList())`，畫面用 `if (list.isEmpty())` 判斷空狀態。新專案的 `feature:collect` 已建立 `sealed class` UI state 慣例（`Empty`／`Success(list)`），語意更明確且與其他已遷移模組一致，本次沿用同樣模式。

### 3. 「切換收藏」邏輯原樣保留，只換底層呼叫的 API

舊版 `toggleMovieCollectStatus`：依 `data.movieCardIsCollect` 決定呼叫 `insertMovieCollect()` 或 `deleteMovieCollect()`。這段業務邏輯（history 頁的收藏按鈕是「切換」，不是像 collect 頁那樣單向「移除」）維持不變，只是改成呼叫 `shared/data` 現有的 `MovieRepository.insertMovieCollect()`／`deleteMovieCollect()`（方法簽章與舊版相容）。

### 4. `MainNavItem.HISTORY` 由「註解佔位」改為正式項目

`androidApp` 的 `MainNavItem.kt` 已預留註解掉的 `HISTORY` entry（含 icon、字串資源 id），是先前遷移時刻意保留的擴充點。本次直接取消註解並指向 `feature:history` 新增的 `HistoryKey`，不更動其餘既有導覽項目（`HOME`／`COLLECT`）的實作方式，維持 `MainActivity` entryProvider 以 `when` 分支對應各 `NavKey` 的既有模式。

## Risks / Trade-offs

- [風險] history 頁的「切換收藏」與 collect 頁的「移除收藏」共用同一批 Repository 方法（`insertMovieCollect`／`deleteMovieCollect`），若未來兩頁的收藏語意分歧，需要同時檢視兩個 ViewModel → 目前行為與舊版一致，且已有既有 `MovieRepositoryImplTest` 覆蓋底層方法，風險可控，暫不額外抽象化
- [風險] 空狀態圖從 `icon_empty.webp`（點陣圖）換成比照 collect 的 vector drawable → 需確認新繪製的 vector 在各密度下視覺效果與舊版一致，會在實作階段人工比對
- 不涉及資料庫 schema 變更，無 Room migration 需求
