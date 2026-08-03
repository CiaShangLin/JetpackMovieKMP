## Why

Android 端的電影詳情頁（主資訊、收藏、演員、推薦電影）已完成並修復導覽 bug，但 iOS 端目前完全沒有詳情頁——首頁、搜尋、收藏、歷史四個列表頁的 `MovieCardView` 點擊 callback（`onMovieTap`）皆存在但未被任何頁面接上動作，使用者點擊電影卡片沒有任何反應。iOS 需要補上這塊核心瀏覽路徑，才能與 Android 功能對齊。

## What Changes

- 新增 iOS 電影詳情頁（`MovieDetailView` + `MovieDetailViewModel` + UiState），比照 Android 版本呈現背景圖、標題／評分／上映日期／片長、簡介、主要演員橫向清單、推薦電影橫向清單，並提供收藏切換；四個資料來源（主 detail、收藏狀態、演員、推薦）各自獨立載入與失敗處理，互不阻塞。
- `KoinHelper` 新增具名 accessor `getMovieRecommendUseCase()`，比照既有 `getMovieDetailUseCase()` 的模式（沿用既有 `ios-koin-bridge` 規範，屬既定擴充行為，非規則變更）。
- 首頁、搜尋、收藏、歷史四個分頁各自以 `NavigationStack` 包裝並定義 `navigationDestination`，將既有 `MovieCardView.onMovieTap` 接上導覽至詳情頁；詳情頁內的推薦電影卡點擊可再推入下一層詳情頁。
- 維持 iOS 平台慣例：切到詳情頁時底部 Tab Bar 保持可見（不比照 Android 隱藏 Navigation Suite 的做法），沿用 SwiftUI `NavigationStack` push 的標準行為。

## Capabilities

### New Capabilities
- `ios-movie-detail`：iOS 電影詳情頁的資料載入、狀態呈現（主內容、演員、推薦電影各自獨立 Loading／Success／Error）、收藏切換與 KoinHelper 依賴取得。

### Modified Capabilities
- `ios-main-bottom-navigation`：四個 tab 頁面新增以 `NavigationStack` 承載電影卡片點擊導覽至詳情頁的行為（前次規格僅定義 tab 切換與 placeholder，尚未定義卡片點擊後的導覽目的地）。

## Impact

- `shared/app`（`KoinHelper.kt`，iosMain）：新增 `getMovieRecommendUseCase()` accessor。
- `iosApp`：新增 `MovieDetail/` 資料夾（`MovieDetailUiState.swift`、`MovieDetailViewModel.swift`、`MovieDetailView.swift`）；修改 `Home/page/HomeContentView.swift`、`Search/SearchView.swift`、`Favorites/FavoritesView.swift`、`History/HistoryView.swift` 接上 `NavigationStack` 與 `navigationDestination`；可能微調 `Main/MainView.swift` 或 `MainTab` 以確認各 tab 的導覽容器歸屬。
- 不涉及 Android（`feature/detail`、`androidApp`）或 `shared/data`、`shared/domain`、`shared/network` 既有邏輯的修改，僅新增 iOS 消費端與一個 KoinHelper accessor。
