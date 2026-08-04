## ADDED Requirements

### Requirement: 語言改變時首頁電影列表 MUST 以新語言重新查詢

`HomeContentViewModel` 的電影列表 Paging 資料流 MUST 觀察 `UserDataRepository.userData` 的 `languageMode`（去除連續重複值），當 `languageMode` 改變時 MUST 以新語言重新建立 Pager 並重新查詢電影列表，取代原本「僅在 ViewModel 建構時查詢一次並鎖定快取」的行為。

#### Scenario: 語言改變時電影列表重新以新語言查詢

- **WHEN** `HomeContentViewModel` 存續期間，`userData.languageMode` 從一個值改變為另一個不同的值
- **THEN** 電影列表的 `Pager` MUST 被重新建立，並以新語言重新呼叫電影列表查詢
- **AND** 前一個語言對應的查詢 MUST 被取消，不與新查詢並存

#### Scenario: 語言未改變時不重複查詢

- **WHEN** `userData` 發出新值，但 `languageMode` 與前一次相同（例如僅 `themeMode` 改變）
- **THEN** 電影列表 MUST NOT 重新建立 Pager 或重新查詢
