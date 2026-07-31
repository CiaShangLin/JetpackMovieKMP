## Why

目前 Android App 的首頁、搜尋、收藏與觀看紀錄雖可顯示電影卡片，點擊後卻沒有可到達的電影詳情目的地，既有 shared 層已提供的 detail、演員與推薦資料無法被使用者瀏覽。遷移既有專案的 movie detail feature，可補齊 Android 的主要電影瀏覽流程，同時維持 KMP 專案既有的 Koin、Navigation3 與 shared 資料層架構。

## What Changes

- 新增 Android-only `feature:detail` 模組，提供電影詳情畫面、ViewModel、UI state、Koin module、Navigation3 entry 與本地化字串。
- 顯示電影主資訊、收藏狀態與操作、劇情、主要演員圓形頭像，以及推薦電影清單。
- 將首頁、搜尋、收藏、觀看紀錄與推薦電影卡片的點擊事件導向 typed `MovieDetailKey(movieId)`。
- detail 目的地顯示時隱藏主 Navigation Suite，並支援 Android 系統返回鍵、畫面返回按鈕與推薦電影的多層詳情返回。
- 重用 `core:ui` 的 `MovieActor`、既有 Coil TMDB URL interceptor 與 shared 層的 detail / credits / recommendations UseCase；主 detail 失敗顯示全頁重試，演員或推薦資料失敗則隱藏各自區塊。
- 為空白或載入失敗的演員 `profilePath` 維持既有 placeholder 呈現與圓形版面。

## Capabilities

### New Capabilities

- `android-movie-detail-module`: Android 電影詳情 feature 的畫面、狀態、收藏、演員、推薦電影與 Navigation3 entry 行為。

### Modified Capabilities

- `android-app-entry`: Android App 的 Navigation3 back stack 新增電影詳情目的地，並在 detail 目的地顯示時隱藏主 Navigation Suite。

## Impact

- 受影響模組：`feature:detail`（新增）、`androidApp`、`feature:home`、`feature:search`、`feature:collect`、`feature:history`、`core:ui`。
- 既有 shared 模組 `shared:model`、`shared:data`、`shared:domain`、`shared:network` 僅被重用，不變更其 API、資料庫 schema 或 iOS 實作。
- 不新增外部依賴；沿用 Version Catalog 既有的 Compose、Koin、Navigation3、Paging 與 Coil 依賴。
