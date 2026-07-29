# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 評估抽出共用的 Paging Append Footer 元件

- 類型: refactor
- 記錄日期: 2026-07-27
- 來源: add-ios-home-movie-list（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

### 背景

`iosApp/iosApp/Home/page/HomeContentView.swift` 的 `appendFooter`（顯示 Paging append
載入中／失敗重試按鈕的清單尾端元件）目前做成私有的 `@ViewBuilder` computed property，
沒有抽成共用元件。

原因：目前只有 Home 這一個消費者，`FavoritesView`／`HistoryView` 都還是空的 placeholder
畫面（`Text("main_favorite_placeholder")` 等），沒有分頁需求；且 `appendFooter` 目前綁定的
型別是 `HomeMovieListLoadState`（Home 專屬的 Kotlin sealed interface，透過 `onEnum(of:)`
橋接），若之後 Favorites／History 也要做分頁，八成會比照現有模式各自有自己的 LoadState
型別，屆時才能看清楚共用介面該怎麼切（例如改用 `isLoading: Bool`／`hasError: Bool`／
`onRetry: () -> Void` 這種跟具體 Kotlin 型別解耦的參數）。

### 後續調整

等之後真的有第二個分頁畫面（例如 Favorites 或 History 導入分頁）時，再回頭評估是否要把
兩邊共通的 append footer 邏輯抽成共用 View，並設計跟 Kotlin sealed interface 解耦的介面。

## 將圖片相對 URL 補 host 邏輯下沉至 Ktor 攔截器

- 類型: refactor
- 記錄日期: 2026-07-28
- 來源: fix-ios-home-card-layout（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

評估在 `shared/network` 的 Ktor client 注入 `DatastoreBaseHostUrlProvider`。攔截器收到沒有 host、
且路徑以圖片副檔名結尾的 URL 時，自動補上圖片 base host。如此可讓 iOS 的 `RemoteImage`
直接使用圖片路徑，不必額外注入 host provider；實作時需明確界定可識別的圖片副檔名、保留已有
host 的完整 URL，並確認不影響 TMDB API 請求或其他非圖片相對路徑。

## 詳細頁遷移後補做歷史頁完整實機驗證

- 類型: feature
- 記錄日期: 2026-07-29
- 來源: add-history-feature（實作中發現）
- 前置依賴: 詳細頁遷移並在進入詳情時寫入觀看歷史
- 狀態: 待處理

目前首頁以 `homeEntry(onMovieClick = {})` 建立，點選電影不會進入詳情，也無法透過正常流程新增觀看紀錄。
待詳細頁遷移完成後，需以實機驗證歷史清單顯示、收藏切換、清空歷史，以及清空後回到空狀態；並完成
`add-history-feature` 的 tasks.md 8.3。
