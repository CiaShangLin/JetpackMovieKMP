## ADDED Requirements

### Requirement: iOS 電影清單 Presenter 沿用 Paging 3 驅動分頁

iOS 端 SHALL 提供一個 Presenter（`shared/app` `iosMain`），依 Genre 建立實例，內部建立並持有一個 `CoroutineScope`，沿用既有 `GetHomeMovieListUseCase` 取得 `Flow<PagingData<MovieCardResult>>`，並透過內部包裝的 `PagingDataPresenter<MovieCardResult>` 子類別收集該 Flow，對外暴露 `get(index)`、`retry()`、`refresh()`、`loadStateFlow`、`onPagesUpdatedFlow`、`clear()`。

#### Scenario: 建立 Presenter 後開始載入第一頁

- **WHEN** 透過 Genre 建立 Presenter 實例
- **THEN** Presenter 內部立即以既有 `GetHomeMovieListUseCase(withGenres, internalScope)` 開始收集分頁資料，`loadStateFlow` 依序反映 refresh 的載入中→完成（或失敗）狀態

#### Scenario: 存取項目觸發下一頁載入

- **WHEN** 呼叫 `get(index)` 存取一個接近目前已載入清單尾端的 index
- **THEN** Paging 3 依內建的 `prefetchDistance` 判斷並觸發下一頁載入（append），`loadStateFlow` 反映 append 的載入中狀態，完成後 `onPagesUpdatedFlow` 發出更新訊號

#### Scenario: 呼叫 refresh 建立新一代分頁資料

- **WHEN** 呼叫 `refresh()`
- **THEN** Presenter 建立新一代 `PagingSource`，重新從第一頁開始載入，`loadStateFlow` 反映 refresh 的載入中→完成（或失敗）狀態，已呈現清單於載入完成後更新為最新結果

#### Scenario: 呼叫 retry 重試失敗的載入

- **WHEN** `loadStateFlow` 反映某次載入（refresh 或 append）為失敗狀態，呼叫 `retry()`
- **THEN** Presenter 重新嘗試同一次失敗的載入請求，不建立新一代 `PagingSource`，成功後 `loadStateFlow` 反映為完成

#### Scenario: 呼叫 clear 後取消內部協程

- **WHEN** 呼叫 `clear()`
- **THEN** Presenter 內部持有的 `CoroutineScope` 被取消，後續不再有任何 `loadStateFlow`／`onPagesUpdatedFlow` emission

### Requirement: HomeContentView 依 Genre 顯示分頁電影清單

iOS `HomeContentView`（SwiftUI）SHALL 依目前選定的 Genre 顯示對應 Presenter 已呈現的電影清單，並提供下拉刷新與捲動時的分頁載入互動。

#### Scenario: 選定 Genre 顯示對應電影卡片清單

- **WHEN** 使用者切換到某個 Genre 分頁
- **THEN** 畫面顯示該 Genre 對應 Presenter 目前已呈現的電影清單，每筆資料以既有 `MovieCardView` 呈現

#### Scenario: 下拉刷新觸發重新載入

- **WHEN** 使用者在電影清單畫面執行下拉刷新手勢
- **THEN** 畫面呼叫對應 Presenter 的 `refresh()`，並在重新整理指示器上反映載入狀態

#### Scenario: 渲染項目時觸發分頁載入

- **WHEN** 畫面依序渲染清單中的每一列
- **THEN** 每一列渲染時呼叫對應 Presenter 的 `get(index)` 取得資料，藉此讓 Paging 3 依 `prefetchDistance` 自動判斷並觸發下一頁載入，畫面不需要另外偵測「捲到底」

#### Scenario: 載入失敗時顯示錯誤畫面與重試

- **WHEN** Presenter 的 `loadStateFlow` 反映 refresh 失敗，且目前尚無任何已載入的電影資料
- **THEN** 畫面顯示既有 `ErrorView` 與重試按鈕，點擊重試呼叫 `retry()`（若目前無任何資料則呼叫 `refresh()`）
