## Context

iOS 端 `HomeContentView.swift`（Home）與 `SearchView.swift`（Search）各自維護一份幾乎相同的 `appendFooter` SwiftUI computed property，依 `HomeMovieListLoadState` / `SearchMovieListLoadState`（皆為 shared 層 `shared/app` iosMain 匯出的 sealed interface：`Idle` / `Loading` / `Error(message)`）渲染 idle（空白）／loading（`ProgressView`）／error（`Button` 重試）三態。兩份型別在 Kotlin 端是獨立定義、結構相同但不共用基底；本次重構決定不觸碰這層（維持 `HomeMovieListPresenter`／`SearchMovieListPresenter` 既有 public API 與 Kotlin 型別），只在 Swift 端新增一層轉接，消除 SwiftUI 端的重複程式碼。

Android 端已有對應前例 `android-paging-load-state-ui`（`core/ui` 共用 Footer Composable），本次設計在概念上比照（行內 Footer、依 LoadState 顯示 loading/error/reset、不遮蔽既有清單內容），但實作方式各自遵循平台慣例（Composable vs SwiftUI View）。

iosApp 既有 `Common/` 目錄已有依元件成一個子資料夾的慣例（`Common/MovieCard/`、`Common/Image/`），本次新增 `Common/Paging/` 沿用此慣例。

## Goals / Non-Goals

**Goals:**
- 新增 `iosApp/iosApp/Common/Paging/AppendLoadState.swift`：一個與 shared 層無關、純 Swift 的 `enum AppendLoadState { case idle, loading, error(message: String) }`，作為共用 Footer View 的輸入型別。
- 新增 `iosApp/iosApp/Common/Paging/PagingAppendFooterView.swift`：接受 `AppendLoadState` 與 `onRetry: () -> Void` 的共用 SwiftUI View，渲染邏輯與現有 `appendFooter` 完全對齊（idle → `EmptyView()`；loading → `ProgressView().padding()`；error → `Button(...) { onRetry() }.padding()`）。
- `HomeContentView.swift` 與 `SearchView.swift` 改為呼叫共用元件，各自只需一段「把 `HomeMovieListLoadState`/`SearchMovieListLoadState` 映射成 `AppendLoadState`」的轉接程式碼。
- 統一 retry 按鈕 localization key 為中性命名（如 `paging_append_retry_button`），兩畫面共用同一個 key；顯示文字（既有中文/英文翻譯內容）維持不變，只改 key 名稱與對應關係。
- 統一 Footer padding：以共用元件內建的單一 padding 值為準（見下方 Decisions），取代 Home 的 `.padding(.horizontal, 16)` 與 Search 的 `.padding(8)`。

**Non-Goals:**
- 不修改 shared 層（Kotlin）的 `HomeMovieListLoadState`／`SearchMovieListLoadState`／`HomeMovieListPresenter`／`SearchMovieListPresenter`，不新增 shared 層共用 sealed interface。
- 不改變 Paging 觸發時機與邏輯（`onAppear { prefetch(index:) }` 與 Paging 3 `prefetchDistance` 機制維持原樣）。
- 不新增「無更多資料」提示——現況 idle 狀態即為 `EmptyView()`，本次重構不新增此前不存在的使用者可見狀態。
- 不涉及 Android（`feature/home`、`feature/search`、`core/ui`）任何變更。
- 不調整 `ErrorView`／`LoadingView`（整頁 refresh 失敗/載入中）的呈現方式，僅限「行內 append Footer」範圍。

## Decisions

- **決策：轉接層放在 Swift 端而非 shared Kotlin 層。**
  理由：`HomeMovieListLoadState`／`SearchMovieListLoadState` 目前分別綁定各自 Presenter 的 `loadStateFlow`／`loadStateStream`，若要在 Kotlin 端抽出共用 sealed interface 需同時調整兩個 Presenter 的對外 API（Swift 消費端型別跟著變動、需要重新驗證 Kotlin/Swift interop 與既有測試），影響範圍與風險超出「單純消除 View 層重複」這個 refactor 的目的。Swift 端轉接層可以在不改動任何既有 public API 的前提下達成同樣的重複消除效果。
  替代方案（不採用）：於 shared 層新增共用 `PagingAppendLoadState` sealed interface 供兩個 Presenter 共用——留待未來若有更多分頁場景（例如新增第三個分頁清單）需要共用時再評估。

- **決策：`AppendLoadState` 用 Swift `enum` 定義，映射邏輯以各 View 的 computed property 或小型 mapper function 呈現，不引入額外的 protocol 抽象。**
  理由：目前只有兩個呼叫端（Home、Search），且雙方的來源型別（`HomeMovieListLoadState`／`SearchMovieListLoadState`）都是三態 `Idle/Loading/Error(message)` 結構一致，直接用 `switch onEnum(of:)` 轉成 `AppendLoadState` 即可，不需要額外的 protocol 或泛型化來換取「尚未出現」的擴充性。

- **決策：Footer padding 統一採用 Search 現有的 `.padding(8)`（四邊 8pt）作為共用元件內建值，而非 Home 的 `.padding(.horizontal, 16)`。**
  理由：四邊 padding 對 loading/error 狀態的視覺留白更一致（尤其 error 狀態的重試按鈕需要垂直方向留白，Home 原本的水平限定 padding 在垂直方向留白不足，一致性較差）；改動後 Home 底部 Footer 的水平留白會從 16pt 降為 8pt，視覺上更貼近清單其餘內容的水平邊界。此為使用者已確認可接受的小幅視覺調整。
  替代方案（不採用）：沿用 Home 的 16pt 水平 padding、Search 改為配合——會導致 Search 原本的垂直留白消失，錯誤重試按鈕與清單最後一項距離過近。

- **決策：localization key 新增 `paging_append_retry_button`，兩畫面改用新 key，原 `home_retry_button` 予以淘汰（若無其他呼叫端使用則移除，若仍被其他畫面引用則保留但不再用於 Search）。**
  理由：`home_retry_button` 名稱隱含「僅限首頁」語意，但實際上被 Search 沿用，屬於命名技術債；抽出共用元件的同時一併命名為中性 key 可避免未來閱讀程式碼時的誤解。

## Risks / Trade-offs

- [風險] 統一 padding 屬於使用者可觀察的視覺變動（Home 底部 Footer 水平留白從 16pt 變 8pt）→ 緩解：已於 proposal 階段與使用者確認為可接受的變動範圍，且僅限 append Footer 區域，不影響清單本身既有的 Grid padding。
- [風險] 若 `home_retry_button` 這個 localization key 目前仍被其他非 Paging 相關畫面引用，直接重新命名／移除可能造成遺漏 → 緩解：實作階段（tasks）需先於 `Localizable.xcstrings` 與程式碼庫搜尋 `home_retry_button` 的所有引用點，確認僅 Home／Search 兩處使用後才進行 key 重新命名，否則改為新增獨立 key 並保留原 key。
- [風險] `onEnum(of:)`（SKIE 產生的 Kotlin sealed interface exhaustive switch 輔助）在兩個來源型別（`HomeMovieListLoadState`／`SearchMovieListLoadState`）的映射程式碼仍會各自出現一次 switch-case → 這是決策「不引入 shared 層共用型別」下無法完全避免的殘留重複，但範圍已從「整個 Footer View + 版面」縮小到「一段短短的型別映射」，符合本次 refactor 的風險/效益取捨。

## Migration Plan

不涉及資料遷移或執行環境變更，純程式碼重構：
1. 新增 `Common/Paging/AppendLoadState.swift` 與 `Common/Paging/PagingAppendFooterView.swift`。
2. 新增 `paging_append_retry_button` localization key。
3. `HomeContentView.swift` 改用共用元件，移除本地 `appendFooter`。
4. `SearchView.swift` 改用共用元件，移除本地 `appendFooter`。
5. 於 iOS Simulator 手動驗證 Home、Search 的 loading／error／retry 三態與下拉刷新後 Footer 是否維持原有觸發邏輯，並確認新的 padding 視覺可接受。
6. 執行 `iosFormat`／`iosFormatCheck`／`iosLint`（依 CLAUDE.md 慣例）。

無需 rollback 機制（純 UI 層重構，可直接以還原 commit 方式回退）。

## Open Questions

- 無。
