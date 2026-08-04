## ADDED Requirements

### Requirement: 語言改變時電影詳情內容 MUST 以新語言重新查詢

`MovieDetailViewModel` 的 `movieDetail`、`movieRecommendations`、`movieActors` 資料流 MUST 觀察 `UserDataRepository.userData` 的 `languageMode`（去除連續重複值），當 `languageMode` 改變時 MUST 以新語言重新查詢對應內容，行為 MUST 與既有手動 retry 機制（`retryTrigger`）共用同一套重新查詢流程。

#### Scenario: 語言改變時電影詳情重新以新語言查詢

- **WHEN** `MovieDetailViewModel` 存續期間，`userData.languageMode` 從一個值改變為另一個不同的值
- **THEN** `movieDetail` MUST 重新以新語言呼叫詳情查詢

#### Scenario: 語言改變時推薦電影與演員清單重新以新語言查詢

- **WHEN** `MovieDetailViewModel` 存續期間，`userData.languageMode` 從一個值改變為另一個不同的值
- **THEN** `movieRecommendations` MUST 重新以新語言呼叫推薦電影查詢
- **AND** `movieActors` MUST 重新以新語言呼叫演員清單查詢

#### Scenario: 語言未改變時不影響既有手動 retry 行為

- **WHEN** 使用者手動觸發 `retryMovieDetail()`，且 `languageMode` 未改變
- **THEN** `movieDetail` MUST 依既有邏輯以目前語言重新查詢，行為與本次變更前一致
