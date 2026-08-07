## Why

iOS 端 `HomeContentView.swift` 與 `SearchView.swift` 各自手刻了一份幾乎逐行相同的 Paging append Footer UI（idle/loading/error 三態渲染與 retry 觸發），造成重複程式碼與未來修改需雙邊同步的風險；Android 端已透過 `android-paging-load-state-ui` capability 完成同類抽取，iOS 端應比照建立共用元件以消除這份重複。

## What Changes

- 在 `iosApp/iosApp/Common/Paging/` 新增共用的 `AppendLoadState`（idle/loading/error 轉接層 enum）與 `PagingAppendFooterView`（共用 SwiftUI Footer 元件），僅存在於 Swift 端，不變動 shared 層既有的 `HomeMovieListLoadState` / `SearchMovieListLoadState` Kotlin 型別。
- `HomeContentView.swift` 改為將既有 `HomeMovieListLoadState` 映射為 `AppendLoadState` 後交給共用元件渲染，移除本地 `appendFooter` computed property。
- `SearchView.swift` 改為將既有 `SearchMovieListLoadState` 映射為 `AppendLoadState` 後交給共用元件渲染，移除本地 `appendFooter` computed property。
- 統一 Footer 的 retry 按鈕 localization key：新增中性命名的 key（如 `paging_append_retry_button`），取代 Search 端原本借用 Home 專屬 `home_retry_button` 的用法；顯示文字不變。
- 統一 Footer 的 padding 數值（原本 Home 為 `.padding(.horizontal, 16)`、Search 為 `.padding(8)` 四邊），改為共用元件內建的單一 padding 規則，兩畫面視覺間距將略為調整並趨於一致。
- 本次為 `refactor`：不新增分頁功能、不改變 loading/error/retry 的觸發時機與判斷邏輯，僅整合重複的渲染程式碼與上述兩項既有的小型不一致。

## Capabilities

### New Capabilities

- `ios-paging-load-state-ui`：定義 iOS 端 `Common/Paging` 共用的 Append LoadState 轉接型別與 Footer Composable View，供任何 iOS 分頁清單畫面重用行內 Loading／Error／Retry 呈現邏輯。

### Modified Capabilities

（無：`ios-home-movie-list` 與 `ios-movie-search` 既有規格描述的是「載入中/失敗/重試」等行為層級需求，其呈現改由共用元件負責，但對外可觀察的觸發時機與互動行為不變，不涉及既有 Requirement 文字修改。）

## Impact

- `iosApp/iosApp/Common/Paging/AppendLoadState.swift`（新增）：共用轉接 enum。
- `iosApp/iosApp/Common/Paging/PagingAppendFooterView.swift`（新增）：共用 Footer SwiftUI View。
- `iosApp/iosApp/Home/page/HomeContentView.swift`（修改）：移除本地 `appendFooter`，改用共用元件並映射 `HomeMovieListLoadState → AppendLoadState`。
- `iosApp/iosApp/Search/SearchView.swift`（修改）：移除本地 `appendFooter`，改用共用元件並映射 `SearchMovieListLoadState → AppendLoadState`。
- `iosApp/iosApp/Localizable.xcstrings`（修改）：新增中性 retry 按鈕 key，兩畫面改用新 key。
- 不影響 Android（`feature/home`、`feature/search`、`core/ui`）與 shared 模組（`shared/app`、`shared/domain` 等）；`HomeMovieListPresenter`、`SearchMovieListPresenter` 及其 Kotlin LoadState 型別維持不變。
- 明確不在本次範圍：shared 層 Kotlin `HomeMovieListLoadState`/`SearchMovieListLoadState` 的共用化重構、Android 端任何變更、iOS 分頁觸發邏輯（`prefetchDistance`／`onAppear`）的調整。
