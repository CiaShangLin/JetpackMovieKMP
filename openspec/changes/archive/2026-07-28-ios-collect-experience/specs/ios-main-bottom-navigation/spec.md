## MODIFIED Requirements

### Requirement: 每個 tab 顯示 placeholder text

除收藏 tab 外，尚未實作的搜尋、歷史與設定 tab SHALL 顯示可辨識的 placeholder text。首頁 tab SHALL 呈現既有首頁電影內容；收藏 tab SHALL 呈現 `ios-movie-collection` 定義的可用收藏畫面，而非 placeholder。

#### Scenario: 切換到首頁 tab

- **WHEN** 使用者選取首頁 tab
- **THEN** 畫面 SHALL 顯示既有首頁電影內容

#### Scenario: 切換到收藏 tab

- **WHEN** 使用者選取收藏 tab
- **THEN** 畫面 SHALL 顯示 `FavoritesView` 的收藏電影格線或收藏空狀態

#### Scenario: 切換到搜尋 tab

- **WHEN** 使用者選取搜尋 tab
- **THEN** 畫面 SHALL 顯示搜尋 placeholder text

#### Scenario: 切換到歷史 tab

- **WHEN** 使用者選取歷史 tab
- **THEN** 畫面 SHALL 顯示歷史 placeholder text

#### Scenario: 切換到設定 tab

- **WHEN** 使用者選取設定 tab
- **THEN** 畫面 SHALL 顯示設定 placeholder text
