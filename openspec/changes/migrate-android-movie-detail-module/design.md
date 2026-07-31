## Context

shared 層已提供 `MovieRepository`、`GetMovieDetailUseCase`、`GetMovieRecommendUseCase` 與 `MovieCastAndCrewBean`，並由 `GetMovieDetailUseCase` 在 detail 成功後寫入觀看紀錄。Android 既有 `feature:home`、`feature:search`、`feature:collect` 與 `feature:history` 都已透過 callback 暴露電影卡點擊事件，但 `MainActivity` 目前傳入空 callback，因此沒有 detail 目的地。

Android App 使用 Koin、Compose、Navigation3 typed `NavKey` 與 `NavBackStack`。`core:ui` 已提供可經 Coil `HostInterceptor` 將 TMDB 相對路徑轉成完整 URL 的 `MovieActor`，並包含演員 placeholder 資源。

本變更只遷移 Android UI；shared API、資料庫 schema、iOS Swift UI 與人員詳情不在範圍內。

## Goals / Non-Goals

**Goals:**

- 建立遵循既有 feature 模式的 Android `feature:detail` 模組。
- 將所有既有電影卡與 detail 內推薦卡導向可堆疊的 `MovieDetailKey(movieId)`。
- 顯示參考專案的電影 detail 內容與收藏操作，並保留 shared 層的觀看紀錄寫入。
- detail 顯示時隱藏主 Navigation Suite，保留明確的返回行為。
- 演員使用既有圓形頭像與 placeholder；演員或推薦 API 失敗不阻斷主 detail。

**Non-Goals:**

- 不建立 iOS detail presenter、SwiftUI 畫面或 iOS 導航。
- 不建立演員／工作人員的人員詳情頁，也不顯示演員姓名與角色。
- 不變更 TMDB API、shared Repository / UseCase contract、Room schema 或圖片 CDN 設定。
- 不在本次統一重構既有 feature 的 `onMovieClick` callback 型別。

## Decisions

### 以 Android-only feature 模組承載 UI，維持既有 MVVM 與 Koin 模式

`feature:detail` 會比照現有 feature 模組建立 `MovieDetailScreen`、`MovieDetailViewModel`、`MovieDetailUiState`、`DetailNavigation` 與 `detailModule()`。ViewModel 使用 Koin `viewModel { params -> }` 接收 `movieId`，並以 `koinViewModel` 的穩定 key 取得實例。

採用此方案是為了延續目前 Android feature 的模組邊界與 DI 方式；不沿用參考專案的 Hilt assisted injection。將 UI 放入 shared Compose Multiplatform 的替代方案會同時擴大 iOS 範圍，與本次只處理 Android 的決策不符。

### 以 `MovieDetailKey(movieId)` 加入 Navigation3 back stack

新增可序列化的 typed `MovieDetailKey`，由 `androidApp` 將 Home 的 `Int` 或其他 feature 的 `MovieCardData.movieCardId` 轉為 key 後加入 `NavBackStack`。detail entry 的返回 callback 與 `NavDisplay.onBack` 都只移除最後一個 key；推薦電影也用相同方式加入新 key。

此方案保留來源頁及巢狀 detail 的返回順序，不需要在 feature 之間相依，也不需要引入 classic Navigation Compose。統一修改所有 feature callback 型別是可行替代方案，但屬於與本功能無關的重構，因此不採用。

### detail 目的地直接顯示 `NavDisplay`，不包主 Navigation Suite

當 back stack 最後一個 key 為 `MovieDetailKey` 時，`MainActivity` 直接顯示同一個 `NavDisplay`；其他 root destination 維持 `JMNavigationSuiteScaffold`。這能在手機、平板與大螢幕都隱藏 bottom bar、rail 或 drawer，同時保留現有主導覽行為。

保留 Navigation Suite 並僅取消選取狀態的替代方案會佔用 detail 內容空間，且與 detail 的返回操作重疊，因此不採用。

### 主 detail、推薦與演員採獨立狀態並分級處理錯誤

主 detail 使用可 retry 的 Loading / Success / Error state，因為它決定整頁能否顯示。收藏狀態觀察本機資料；推薦與演員各自載入，成功時顯示區塊，失敗時直接隱藏，載入中只顯示既有 loading 呈現。收藏切換沿用 Repository 的 insert / delete 操作。

這能讓 detail 在非關鍵附屬 API 暫時失敗時仍可閱讀。對每個附屬區塊加入錯誤與重試 UI 是可行替代方案，但已明確不在此次 UX 範圍。

### 重用 `MovieActor`，並正確處理空白圖片路徑

演員區只傳入非空的 `profilePath`；空白路徑或 Coil 載入失敗時必須顯示 `MovieActor` 既有 placeholder，且保持卡片指定的圓形尺寸與裁切。`HostInterceptor` 會處理非空 TMDB 相對路徑，因此不新增 URL resolver 或圖片下載依賴。

另建演員專用圖片元件能提供更細的樣式控制，但會重複既有 core UI 能力，故不採用。

## Risks / Trade-offs

- [主框架需依 key 分流，可能影響 root 導覽排列] → 保持既有 root destination 的 scaffold 分支不變，並以 Navigation3 entry / UI 測試驗證 detail 與返回流程。
- [detail 遭重複快速點擊時形成相同 movieId 的多層 key] → 初版保留一般 back stack 語意；不做去重以免改變使用者返回預期。
- [空白 profilePath 可能被 interceptor 組成無效 URL] → UI 在傳給 `MovieActor` 前辨識空值並直接走 placeholder。
- [推薦或演員失敗不顯示錯誤] → 這是確認過的體驗取捨；主 detail 仍提供全頁 retry。
- [Room migration] → 不涉及資料庫 entity 或 schema，無須新增 migration。

## Migration Plan

1. 新增 `feature:detail` 與 Android App 的 module / Koin / Navigation3 整合。
2. 將各既有 feature 的電影點擊 callback 接到 `MovieDetailKey`。
3. 執行 detail 與受影響導航的單元測試、`ktlintCheck` 與 Android debug build。
4. 若需要回退，可移除新的 detail key entry 與 `feature:detail` 依賴；shared 層與資料庫不受影響。

## Open Questions

無。本次範圍、導航顯示方式、演員呈現與附屬區塊錯誤策略均已確認。
