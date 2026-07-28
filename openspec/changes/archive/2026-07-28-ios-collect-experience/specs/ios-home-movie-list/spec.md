## MODIFIED Requirements

### Requirement: HomeContentView 依 Genre 顯示分頁電影清單

iOS `HomeContentView`（SwiftUI）SHALL 依目前選定的 Genre 顯示對應 Presenter 已呈現的電影清單，並提供下拉刷新與捲動時的分頁載入互動。每張電影卡片的收藏 callback SHALL 交由注入的收藏操作處理，且 shared 收藏資料變動後卡片的愛心狀態 SHALL 與最新 `isCollect` 一致。

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

#### Scenario: 首頁卡片切換收藏

- **WHEN** 使用者點擊首頁任一電影卡片的收藏按鈕
- **THEN** `HomeContentView` SHALL 將該卡片資料交給收藏操作，且 shared 收藏資料更新後該卡片與收藏 tab SHALL 顯示一致結果
