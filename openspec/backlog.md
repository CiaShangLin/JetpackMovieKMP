# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 評估導入 Proto DataStore 對齊參考專案 storage format
- 類型: feature
- 記錄日期: 2026-07-17
- 來源: migrate-datastore-to-commonmain（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

目前 `shared/commonMain` 的 datastore 是用 Preferences DataStore，因為專案完全沒有設定
protobuf/KMP codegen 工具鏈（`settings.gradle.kts`、`gradle/libs.versions.toml`、
`shared/build.gradle.kts` 均未配置），照 `design.md` 決策 #2 的 fallback 規則走。行為與
參考專案（`JetpackMovieCompose/core/datastore`）等價，但底層 storage format 不同，且原本的
`UserPreferencesSerializer`（綁定 protobuf 生成的 `UserPreferences` message）沒有對應物件可用。

若之後要改用 Proto DataStore 對齊參考專案格式，需要：
1. 建置支援 Kotlin Multiplatform（含 iOS/Kotlin Native target）的 protobuf codegen 工具鏈。
2. 驗證 iOS 編譯可以順利通過（KMP 的 protobuf codegen 對 Native target 支援不算成熟，是主要風險）。
3. 重新設計 `UserPreferenceDataSource` 與對應 serializer，改為讀寫生成的 proto message。
