## ADDED Requirements

### Requirement: iOS 歷史頁 SHALL 觀察 shared 觀看紀錄並呈現目前收藏狀態

iOS history tab SHALL 透過既有 `GetHistoryMovieListUseCase` 觀察 shared 本機觀看紀錄，
不得建立重複的 iOS 歷史資料來源或自行合併收藏資料。收到一筆以上資料時，畫面 SHALL
以既有 `MovieCardView` 的自適應格線顯示所有電影，並以電影 id 維持穩定 identity；卡片
收藏狀態 SHALL 反映 UseCase 輸出的 `MovieCardResult.isCollect`。

#### Scenario: 進入歷史 tab 時顯示既有紀錄

- **WHEN** 使用者切換至 history tab，且 `GetHistoryMovieListUseCase` 發出一筆以上電影
- **THEN** 畫面 SHALL 顯示歷史標題、清空操作與每筆對應的 `MovieCardView`

#### Scenario: 收藏狀態異動後同步更新歷史卡片

- **WHEN** `GetHistoryMovieListUseCase` 因 shared 收藏資料變動而發出更新清單
- **THEN** history 頁 SHALL 在不重建 App 或手動刷新下顯示每筆最新的收藏按鈕狀態

### Requirement: iOS 歷史頁 SHALL 顯示在地化空狀態

當 `GetHistoryMovieListUseCase` 發出空清單時，iOS history tab SHALL 顯示專屬且透過
String Catalog 取得的空狀態提示，而非 placeholder、空白格線或錯誤頁。

#### Scenario: 尚無觀看紀錄

- **WHEN** 使用者進入 history tab 且 UseCase 發出空清單
- **THEN** 畫面 SHALL 顯示歷史為空的在地化提示，且不得顯示電影格線或清空操作

#### Scenario: 清空後切換空狀態

- **WHEN** 清空觀看紀錄後 UseCase 發出空清單
- **THEN** 頁面 SHALL 由電影格線切換為歷史空狀態

### Requirement: iOS 歷史頁 SHALL 支援收藏切換且不移除歷史項目

歷史頁的 `MovieCardView` 收藏 callback SHALL 交給注入 `MovieRepository` 的 History
ViewModel 或等效操作物件。當 `movieCardIsCollect` 為 false 時 SHALL 呼叫
`insertMovieCollect()`；為 true 時 SHALL 呼叫 `deleteMovieCollect()`。此操作 SHALL NOT
刪除該筆觀看紀錄，後續畫面狀態以 shared Flow 為準。

#### Scenario: 對未收藏的歷史電影加入收藏

- **WHEN** 使用者點擊一筆 `movieCardIsCollect == false` 歷史電影的收藏按鈕
- **THEN** ViewModel SHALL 呼叫 `insertMovieCollect()` 並保留該電影於歷史清單

#### Scenario: 對已收藏的歷史電影取消收藏

- **WHEN** 使用者點擊一筆 `movieCardIsCollect == true` 歷史電影的收藏按鈕
- **THEN** ViewModel SHALL 呼叫 `deleteMovieCollect()` 並保留該電影於歷史清單

### Requirement: iOS 歷史頁 SHALL 提供清空全部觀看紀錄操作

iOS 歷史頁 SHALL 在有紀錄時提供在地化的清空入口。使用者觸發後，History ViewModel
SHALL 呼叫 `MovieRepository.deleteAllMovieHistory()`；同一個清空尚未完成時 SHALL 忽略
後續清空請求，畫面 SHALL 以 shared Flow 的後續 emission 反映結果。

#### Scenario: 清空全部觀看紀錄

- **WHEN** 使用者點擊歷史頁的清空操作
- **THEN** ViewModel SHALL 呼叫 `deleteAllMovieHistory()` 一次

#### Scenario: 清空進行中再次點擊

- **WHEN** `deleteAllMovieHistory()` 尚未完成且使用者再次觸發清空
- **THEN** ViewModel SHALL 不發出第二次清空呼叫

### Requirement: iOS 歷史頁的依賴與狀態決策 SHALL 可自動化測試

`KoinHelper` SHALL 提供具名方法解析 `GetHistoryMovieListUseCase`；iOS composition root
SHALL 將它與其他 shared 依賴建立為單一 SwiftUI `AppDependencies` environment，使
`MainView`／`MainTab` 不必向每一個 tab 逐一轉送依賴。History ViewModel SHALL 以明確
建構子接收 UseCase 與 Repository，View 與 ViewModel SHALL NOT 直接呼叫 `KoinHelper`。
iOS XCTest SHALL 以 AAA 結構驗證空／成功 UI state、收藏操作決策與清空重複防護。

#### Scenario: KoinHelper 解析歷史 UseCase

- **WHEN** iOS Koin 已初始化後呼叫 `KoinHelper.getHistoryMovieListUseCase()`
- **THEN** SHALL 回傳非 null 的 `GetHistoryMovieListUseCase` 實例

#### Scenario: 新增 feature 依賴不擴增 Main tab 轉送參數

- **WHEN** history 或後續 feature 需要新的 shared 依賴
- **THEN** `IosApp` SHALL 將依賴加入 `AppDependencies` environment，且 `MainView`／`MainTab` SHALL NOT 為每個 feature 增加對應的依賴轉送參數

#### Scenario: ViewModel 狀態與操作測試通過

- **WHEN** 執行 iOS history 的 XCTest
- **THEN** 測試 SHALL 驗證空與成功狀態映射、已收藏與未收藏電影的 repository 呼叫，以及清空進行中的第二次呼叫被忽略
