## ADDED Requirements

### Requirement: iOS 電影詳情頁 SHALL 顯示主電影資訊並支援失敗重試

`MovieDetailView` SHALL 透過既有 `GetMovieDetailUseCase` 取得指定 `movieId` 的電影詳情，成功時 SHALL 顯示背景圖片、電影名稱、評分、上映日期、片長與劇情簡介。主內容載入中 SHALL 顯示 Loading 畫面；請求失敗時 SHALL 顯示全頁錯誤畫面與重試操作，且 SHALL NOT 因此影響演員或推薦電影區塊各自的狀態。

#### Scenario: 主內容成功載入

- **WHEN** 指定 `movieId` 的電影詳情請求成功
- **THEN** `MovieDetailView` SHALL 顯示該電影的背景圖片、名稱、評分、上映日期、片長與簡介
- **AND** SHALL 保留 `GetMovieDetailUseCase` 既有的觀看紀錄寫入行為

#### Scenario: 主內容請求失敗後重試

- **WHEN** 主內容請求失敗，使用者點擊重試操作
- **THEN** `MovieDetailViewModel` SHALL 取消先前的請求並以相同 `movieId` 重新發起一次

### Requirement: iOS 電影詳情頁 SHALL 支援收藏狀態觀察與切換

`MovieDetailViewModel` SHALL 透過既有 `MovieRepository` 觀察指定 `movieId` 的收藏狀態，並提供收藏／取消收藏操作；操作依 `movieCardIsCollect` 呼叫對應的 `insertMovieCollect()` 或 `deleteMovieCollect()`。

#### Scenario: 收藏未收藏的電影

- **WHEN** 使用者在未收藏的詳情頁點擊收藏按鈕
- **THEN** `MovieDetailViewModel` SHALL 呼叫 `insertMovieCollect()`

#### Scenario: 取消已收藏的電影

- **WHEN** 使用者在已收藏的詳情頁點擊收藏按鈕
- **THEN** `MovieDetailViewModel` SHALL 呼叫 `deleteMovieCollect()`

### Requirement: iOS 電影詳情頁 SHALL 顯示主要演員區塊且獨立於主內容失敗

`MovieDetailView` SHALL 使用既有 credits 資料（`MovieRepository.getMovieActor`）顯示主要演員的橫向清單。演員請求 SHALL 獨立於主內容載入，其失敗 SHALL NOT 阻斷或影響主內容的顯示。

#### Scenario: 演員資料成功載入

- **WHEN** 演員請求成功且包含至少一位演員
- **THEN** `MovieDetailView` SHALL 顯示主要演員的橫向清單

#### Scenario: 演員請求失敗

- **WHEN** 演員請求失敗
- **THEN** `MovieDetailView` SHALL 隱藏演員區塊
- **AND** SHALL NOT 影響主內容或推薦電影區塊的顯示

### Requirement: iOS 電影詳情頁 SHALL 顯示推薦電影區塊且獨立於主內容失敗

`MovieDetailView` SHALL 使用既有 `GetMovieRecommendUseCase` 顯示推薦電影的橫向清單，並沿用既有 `MovieCardView` 的收藏操作與點擊導覽行為。推薦電影請求 SHALL 獨立於主內容載入，其失敗 SHALL NOT 阻斷或影響主內容的顯示。

#### Scenario: 推薦電影成功載入

- **WHEN** 推薦電影請求成功且包含至少一筆電影
- **THEN** `MovieDetailView` SHALL 顯示推薦電影的橫向清單與 `MovieCardView`

#### Scenario: 推薦電影請求失敗

- **WHEN** 推薦電影請求失敗
- **THEN** `MovieDetailView` SHALL 隱藏推薦電影區塊
- **AND** SHALL NOT 影響主內容或演員區塊的顯示

#### Scenario: 點擊推薦電影卡進入下一部詳情頁

- **WHEN** 使用者點擊推薦電影區塊中的一張電影卡
- **THEN** 系統 SHALL 以該電影的 `movieCardId` 推入下一層電影詳情頁

### Requirement: iOS 電影詳情頁 SHALL 直接透過 KoinHelper 取得 shared 依賴

`KoinHelper` SHALL 提供具名方法 `getMovieRecommendUseCase()` 解析 `GetMovieRecommendUseCase`。`MovieDetailView` SHALL 在 `init()` 直接透過 `KoinHelper` 取得 `MovieRepository`、`GetMovieDetailUseCase`、`GetMovieRecommendUseCase` 並建立 `MovieDetailViewModel`；`MainView`／`MainTab` SHALL NOT 為此 feature 新增依賴轉送參數。

#### Scenario: KoinHelper 解析推薦電影 UseCase

- **WHEN** iOS Koin 已初始化後呼叫 `KoinHelper.getMovieRecommendUseCase()`
- **THEN** SHALL 回傳非 null 的 `GetMovieRecommendUseCase` 實例

#### Scenario: MovieDetailView 直接解析依賴

- **WHEN** 使用者導覽進入電影詳情頁
- **THEN** `MovieDetailView` SHALL 能建立出可用的 `MovieDetailViewModel`，且不需要 `MainView`／`MainTab` 傳入任何 shared 依賴參數
