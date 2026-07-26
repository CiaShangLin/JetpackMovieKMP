## Why

iOS 端目前只有 `MainView` 底部導覽與五個 placeholder 分頁（`ios-main-bottom-navigation`），首頁 tab 顯示的是純文字 placeholder，尚未有任何電影瀏覽功能。Android 端（`feature/home`）已有完整的 Tab + Pager + 電影卡片列表 + Loading/Error 畫面實作可供對照，現在需要在 iOS 建立對等的首頁體驗，作為 iOS 電影瀏覽功能的第一個實質畫面，並讓其中的電影卡片元件可被後續的收藏、搜尋、歷史等頁面重用。

## What Changes

- 新增 iOS 首頁畫面：頂部 Tab（分類）+ `TabView(.page)` 樣式的橫向分頁（對應 Android `JMScrollableTabRow` + `HorizontalPager`），每個分類分頁各自顯示對應的電影清單
- 新增可跨頁面重用的 iOS 電影卡片元件（SwiftUI View），比照 Android `core/ui` 的 `MovieCard` 呈現海報、標題、上映日期、評分、收藏按鈕
- 將電影卡片 UI 資料模型（目前的 `core/ui.MovieCardData`）搬到 `shared/model`，讓 Android 與 iOS 共用同一份型別定義，取代「兩平台各自維護一份欄位相同的 wrapper model」
- 新增 Loading 畫面（Lottie 動畫，比照 Android `core/ui` 的 `LoadingScreen` 所使用的 `loading.json`）與 API 失敗畫面（含重試按鈕，比照 Android `ErrorScreen`）
- 新增 iOS 端 `HomeViewModel`／`HomeUiState`，第一階段以假資料驅動 UI 開發與驗證，之後串接 shared 端既有 API（`MovieRepository.getMovieGenres()`、`GetHomeMovieListUseCase`）
- 新增 Lottie iOS 依賴（`lottie-ios`，透過 Swift Package Manager，本專案 iOS 端目前無 CocoaPods）
- **BREAKING（範圍內可接受的既有行為變更）**：首頁 tab 不再顯示 `main_home_placeholder` 文字，改為顯示上述實際畫面

## Capabilities

### New Capabilities
- `ios-home-screen`：iOS 首頁的 Tab + Pager 導覽、`HomeViewModel`/`HomeUiState` 狀態管理（Loading／Success／Error）、假資料到真實 API 的串接方式
- `ios-movie-card`：iOS 端可跨頁面重用的電影卡片 SwiftUI 元件，涵蓋顯示欄位、收藏按鈕互動、元件放置位置
- `kmp-movie-card-ui-model`：電影卡片 UI 資料模型改為存放於 `shared/model`，供 Android `core/ui` 與 iOS 共用同一份型別，取代兩平台各自定義的重複 wrapper

### Modified Capabilities
- `ios-main-bottom-navigation`：首頁 tab 的內容需求從「顯示 placeholder text」改為「顯示 `ios-home-screen` 提供的實際首頁畫面」

## Impact

- **受影響模組（iOS，Xcode 專案原生程式碼，非 Gradle 模組）**：
  - `iosApp/iosApp/Home/`（新增 `HomeView`、`HomeViewModel`、`HomeUiState`、假資料、Tab/Pager 畫面、Loading/Error 畫面）
  - 新增共用元件目錄（例如 `iosApp/iosApp/Common/MovieCard/`）承載可重用的電影卡片 View 與資料模型轉換
  - `iosApp/iosApp/Main/MainTab.swift`：`HomeView()` 內容從 placeholder 換成實際首頁
  - `iosApp/iosApp/Localizable.xcstrings`：新增首頁相關文案 key，移除或保留 `main_home_placeholder`（視是否仍有其他 tab 使用而定）
  - `iosApp/iosApp.xcodeproj/project.pbxproj`：新增 `lottie-ios` Swift Package 依賴與 Loading 動畫資源（`.json`）加入 bundle
- **受影響模組（shared，Gradle KMP 模組）**：
  - `shared/app`（`KoinHelper.kt`）：新增 iOS 端可呼叫的 accessor（電影分類、電影清單相關 UseCase）
  - `shared/domain`：新增 UseCase（包裝 `AppResult` 的電影分類查詢、單頁快照電影清單查詢）
  - `shared/model`：新增電影卡片 UI 資料模型（從 `core/ui.MovieCardData` 搬移過來），供雙平台共用
  - 若涉及新增 Gradle 依賴：對照 `gradle/libs.versions.toml`（本專案以 Version Catalog 管理版本，無 buildSrc）
- **受影響模組（Android，因資料模型搬移）**：
  - `core/ui`：移除本地的 `MovieCardData.kt`，改為依賴 `shared/model` 新位置的型別；`MovieCard.kt`、`MovieCardData.kt` 的既有 `asMovieCardResult()`/`asMovieCardData()` 轉換函式隨型別搬移調整 import
  - `feature/home`：`HomeContentViewModel.kt`、`HomeScreen.kt` 中對 `MovieCardData` 的 import 路徑需一併更新
  - 此搬移不改變欄位內容與既有轉換邏輯，僅改變型別所在模組，`core/ui` 依賴 `shared/model` 本就是既有架構允許的方向（`android-ui-module` spec 既有規則）
- **不受影響**：Android 端 UI 呈現行為、`feature/home` 對外可觀察行為維持不變
