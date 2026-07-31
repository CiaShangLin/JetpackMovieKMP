## 1. Root Gradle 設定

- [x] 1.1 在 `settings.gradle.kts` 註冊 `:feature:detail`，並建立 Android library 模組的 Gradle、manifest 與資源目錄結構

## 2. core:ui 演員頭像元件

- [x] 2.1 調整 `MovieActor`，讓空白 `profilePath` 與圖片載入失敗均使用既有 placeholder，並保留原卡片的圓形裁切、尺寸與邊框
- [x] 2.2 為演員頭像的空白路徑與 error placeholder 呈現補上可驗證的 UI / 元件測試

## 3. feature:detail 模組

- [x] 3.1 建立 `MovieDetailUiState` 與 Koin `detailModule()`，以 `MovieRepository`、`GetMovieDetailUseCase`、`GetMovieRecommendUseCase` 及 movieId 參數建立 `MovieDetailViewModel`
- [x] 3.2 實作主 detail、收藏狀態與收藏切換：成功寫入既有觀看紀錄，主請求失敗顯示全頁 retry
- [x] 3.3 實作 detail Compose 畫面：背景、電影資訊、返回與收藏按鈕、劇情、主要演員純頭像和推薦電影清單
- [x] 3.4 實作演員與推薦的獨立 loading / success / failure 呈現；失敗時隱藏對應區塊且不影響主 detail
- [x] 3.5 建立 detail 的三語系字串資源，並新增 `MovieDetailKey` 與 Navigation3 entry，將返回與推薦電影點擊事件以 callback 對外暴露
- [x] 3.6 為 `MovieDetailViewModel` 撰寫 AAA JVM 單元測試，覆蓋主 detail 成功、失敗、retry、收藏切換、演員與推薦的成功 / 失敗狀態
- [x] 3.7 為 detail 畫面的關鍵 UI 狀態與演員 placeholder 補上 Compose UI / 元件測試

## 4. androidApp 導航整合

- [x] 4.1 在 `androidApp` 加入 `feature:detail` 依賴，並於 `JetpackMovieApplication` 載入 `detailModule()`
- [x] 4.2 在 `MainActivity` 的 Navigation3 entry provider 註冊 detail entry，將 Home、Search、Collect、History 與推薦電影點擊事件加入對應 `MovieDetailKey`
- [x] 4.3 依目前 back stack key 分流主框架：detail 顯示時隱藏 `JMNavigationSuiteScaffold`，其他 root destination 保持既有主導航
- [x] 4.4 為 Navigation3 整合補上單元測試，驗證各入口開啟 detail、巢狀推薦 detail 返回，以及返回 root 後恢復主導航

## 5. 驗證

- [x] 5.1 執行 `./gradlew :feature:detail:testDebugUnitTest` 與受影響 Android App 單元測試
- [x] 5.2 執行 `./gradlew ktlintCheck` 與 `./gradlew :androidApp:assembleDebug`
