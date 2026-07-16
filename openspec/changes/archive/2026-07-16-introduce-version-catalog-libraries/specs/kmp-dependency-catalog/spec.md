## ADDED Requirements

### Requirement: Catalog library 逐步引入與驗證
專案 MUST 依 `gradle/libs.versions.toml` 的 library alias 逐步引入依賴，且 MUST 優先處理支援 Kotlin Multiplatform 的依賴，再處理 Android-only 依賴；iOS-only 依賴除非為完成 KMP platform wiring 所需，否則 MUST 可略過。

#### Scenario: KMP library 優先引入
- **WHEN** 實作者開始導入尚未使用的 catalog library
- **THEN** 支援 common/KMP source set 的 library MUST 先於 Android-only library 被評估與引入

#### Scenario: Paging3 common 依賴納入 shared
- **WHEN** catalog 包含 Paging3 common 與 Android UI aliases
- **THEN** `androidx-paging-common` MUST 被加入 `shared.commonMain`，且 Android UI paging aliases MUST 只加入 `androidApp` 或 Android UI source set

#### Scenario: Room KMP wiring 納入 shared
- **WHEN** catalog 包含 Room runtime、compiler、paging、SQLite bundled、Room plugin 與 KSP plugin aliases
- **THEN** `shared` MUST 套用 Room 與 KSP plugin，加入 Room KMP runtime/paging/SQLite dependencies，並為 Android 與 iOS KSP target 設定 Room compiler

#### Scenario: Room schema directory 受版控
- **WHEN** Room plugin 被套用到 `shared`
- **THEN** `shared` MUST 設定受版控 schema directory，且本 change 不需要新增 database schema、DAO 或 migration

#### Scenario: Android-only library 限制在 Android 範圍
- **WHEN** catalog library 只支援 Android 或 Android instrumentation
- **THEN** 該 library MUST 只被加入 `androidApp`、`androidMain`、`androidUnitTest` 或 Android instrumentation source set，不得加入 `commonMain`

#### Scenario: 每次引入後執行 Gradle 驗證
- **WHEN** 一個 library 或最小不可拆分 library group 已加入 build script
- **THEN** 實作者 MUST 立即執行對應 Gradle 編譯或測試任務，並確認沒有 dependency resolution、source set 或 plugin wiring 錯誤

#### Scenario: Gradle 成功後啟動 Android app
- **WHEN** 該輪 Gradle 驗證成功
- **THEN** 實作者 MUST 使用 `android-cli` 安裝並啟動 Android app，確認 app 可開啟到目前 entrypoint

#### Scenario: 單一 library 最多修復三次
- **WHEN** 某個 library 引入後的 Gradle 或 Android smoke test 失敗
- **THEN** 實作者 MUST 最多嘗試三次修復；若仍失敗，MUST 記錄失敗原因並跳過該 library，繼續評估下一個 library

#### Scenario: 跳過項目保留紀錄
- **WHEN** library 因不支援目前 KMP target、需要未規劃功能、環境阻塞或三次修復失敗而未引入
- **THEN** tasks 或實作紀錄 MUST 列出 library alias、跳過原因與後續建議
