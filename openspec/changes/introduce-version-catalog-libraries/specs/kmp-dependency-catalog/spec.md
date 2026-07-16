## ADDED Requirements

### Requirement: Catalog library 逐步引入與驗證
專案 MUST 依 `gradle/libs.versions.toml` 的 library alias 逐步引入依賴，且 MUST 優先處理支援 Kotlin Multiplatform 的依賴，再處理 Android-only 依賴；iOS-only 依賴除非為完成 KMP platform wiring 所需，否則 MUST 可略過。

#### Scenario: KMP library 優先引入
- **WHEN** 實作者開始導入尚未使用的 catalog library
- **THEN** 支援 common/KMP source set 的 library MUST 先於 Android-only library 被評估與引入

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
