## ADDED Requirements

### Requirement: Android 電影詳情 feature 模組

系統 MUST 提供 Android-only `feature:detail` 模組，並使用專案既有 Version Catalog、Compose、Koin、Navigation3、Paging 與 Coil 依賴。模組 MUST 使用 package 與 namespace `com.shang.jetpackmoviekmp.feature.detail`，且 MUST NOT 引入 Hilt、Dagger 或 classic Navigation Compose。

#### Scenario: detail 模組可被 Android App 建置

- **WHEN** 執行 `./gradlew :androidApp:assembleDebug`
- **THEN** `feature:detail` MUST 成功編譯並由 `androidApp` 以 `implementation(projects.feature.detail)` 使用

#### Scenario: detail 模組使用 Koin ViewModel 注入

- **WHEN** detail 畫面取得 `MovieDetailViewModel`
- **THEN** MUST 使用 `koinViewModel` 與 Koin `viewModel` 定義取得 movieId 參數
- **AND** MUST NOT 使用 `@HiltViewModel`、`@AssistedInject` 或 `hiltViewModel`

### Requirement: 電影詳情主內容與失敗重試

系統 MUST 使用既有 `GetMovieDetailUseCase` 取得指定 movieId 的主 detail，成功時 MUST 顯示背景圖片、電影名稱、評分、上映日期、片長與劇情。主 detail 載入中 MUST 顯示 loading；主 detail 失敗時 MUST 顯示全頁錯誤與 retry 操作。

#### Scenario: 主 detail 成功載入

- **WHEN** 指定 movieId 的 detail 請求成功
- **THEN** 畫面 MUST 顯示該電影的主資訊與返回、收藏操作
- **AND** 系統 MUST 保留 `GetMovieDetailUseCase` 既有的觀看紀錄寫入行為

#### Scenario: 主 detail 請求失敗後重試

- **WHEN** 主 detail 請求失敗且使用者點擊 retry
- **THEN** 系統 MUST 再次請求相同 movieId 的 detail

### Requirement: 收藏狀態與操作

detail 畫面 MUST 觀察指定 movieId 的既有收藏狀態，並提供收藏與取消收藏操作。操作完成後，收藏按鈕及推薦電影卡的收藏狀態 MUST 反映 Repository 的最新資料。

#### Scenario: 收藏未收藏的 detail 電影

- **WHEN** 使用者在未收藏的 detail 電影點擊收藏按鈕
- **THEN** 系統 MUST 將該電影寫入收藏資料

#### Scenario: 取消已收藏的 detail 電影

- **WHEN** 使用者在已收藏的 detail 電影點擊收藏按鈕
- **THEN** 系統 MUST 將該電影從收藏資料移除

### Requirement: 主要演員頭像區塊

detail 畫面 MUST 使用既有 credits 資料顯示主要演員的圓形頭像，且 MUST 重用 `core:ui` 的 `MovieActor` 與既有 TMDB 圖片 URL interceptor。演員區 MUST NOT 顯示演員姓名、角色或人員詳情跳轉。

#### Scenario: 演員資料與圖片可用

- **WHEN** credits 請求成功且演員具有非空 `profilePath`
- **THEN** 系統 MUST 顯示演員的圓形頭像

#### Scenario: 演員沒有可用圖片

- **WHEN** 演員的 `profilePath` 為空白或頭像載入失敗
- **THEN** 系統 MUST 顯示既有演員 placeholder
- **AND** placeholder MUST 保持演員頭像的圓形尺寸與裁切

#### Scenario: credits 請求失敗

- **WHEN** credits 請求失敗
- **THEN** 系統 MUST 隱藏演員區塊
- **AND** MUST NOT 阻斷主 detail 內容

### Requirement: 推薦電影區塊

detail 畫面 MUST 使用既有 `GetMovieRecommendUseCase` 顯示推薦電影卡，並沿用 MovieCard 的收藏操作與點擊回呼。

#### Scenario: 推薦電影資料可用

- **WHEN** recommendations 請求成功且包含電影
- **THEN** 系統 MUST 顯示推薦電影區塊與電影卡

#### Scenario: recommendations 請求失敗

- **WHEN** recommendations 請求失敗
- **THEN** 系統 MUST 隱藏推薦電影區塊
- **AND** MUST NOT 阻斷主 detail 內容

### Requirement: Detail ViewModel 單元測試

`MovieDetailViewModel` MUST 具備遵循 Arrange / Act / Assert 的 JVM 單元測試，涵蓋主 detail 成功、失敗與重試、收藏切換，以及演員與推薦資料的獨立狀態處理。

#### Scenario: 執行 detail 模組單元測試

- **WHEN** 執行 `./gradlew :feature:detail:testDebugUnitTest`
- **THEN** 測試 MUST 驗證上述 ViewModel 行為且成功通過
