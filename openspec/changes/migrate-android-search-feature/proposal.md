## Why

目前 Android App 的底部導覽尚未提供搜尋入口，使用者無法透過既有的 TMDB 搜尋資料能力尋找電影。來源專案已具備成熟的 Android 搜尋體驗，而 KMP 專案的 shared/data 已支援搜尋 Paging，因此可在不擴大共用層或 iOS 範圍的前提下完成 Android 功能遷移。

## What Changes

- 新增 Android-only `feature/search` 模組，提供電影關鍵字輸入、送出搜尋、分頁結果、初始提示、首次載入與錯誤重試 UI。
- 將來源專案的 Hilt 與舊 Navigation Compose 實作，適配為本專案既有的 Koin、Navigation3、Compose Material 3、`core/ui` 與 `core/designsystem` 模式。
- 在 `androidApp` 建立 Search Navigation3 entry，並啟用底部導覽的搜尋項目。
- 沿用 `shared:data` 的 `MovieRepository.getMovieSearchPager`，不修改共用資料層、TMDB API、資料庫或 iOS App。

## Capabilities

### New Capabilities

- `android-search-module`: Android 使用者可從底部導覽進入搜尋頁，依關鍵字瀏覽可分頁的電影搜尋結果，並在載入失敗時重試。

### Modified Capabilities

- 無。

## Impact

- 受影響 module：新增 `feature/search`；修改 `androidApp`（Gradle 依賴、`MainNavItem`、Navigation3 entry）；重用 `core/designsystem`、`core/ui`、`shared:data`、`shared:model`、`shared:common`。
- 不新增第三方依賴，不修改 `gradle/libs.versions.toml`、shared 模組、iOS target、TMDB API 或 Room schema。
