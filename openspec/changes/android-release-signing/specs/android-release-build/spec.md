## ADDED Requirements

### Requirement: `androidApp` release build type MUST 具備完整簽章設定

`androidApp/build.gradle.kts` 的 `buildTypes.release` MUST 設定 `signingConfig`，指向由 `keystore.properties` 讀取的 `signingConfigs.release`（包含 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`）。

#### Scenario: 具備 `keystore.properties` 時可成功組裝 release 產物

- **WHEN** 根目錄存在填妥實際值的 `keystore.properties`，且執行 `./gradlew :androidApp:assembleRelease`
- **THEN** 建置 MUST 成功完成
- **AND** 產出的 APK MUST 已使用 `keystore.properties` 指定的 keystore 完成簽章（可用 `apksigner verify` 或等效方式驗證）

#### Scenario: 缺少 `keystore.properties` 時建置失敗訊息明確

- **WHEN** 根目錄不存在 `keystore.properties`，且執行 `./gradlew :androidApp:assembleRelease`
- **THEN** 建置 MUST 失敗或明確提示缺少簽章設定，MUST NOT 產出未簽章或使用空白密碼簽章的產物

### Requirement: 簽章敏感參數 MUST NOT 進版控

簽章相關的 `storePassword`、`keyAlias`、`keyPassword` 與實際 keystore 檔案 MUST 不進版控；專案 MUST 提供 `keystore.properties.example` 範本檔（進版控，僅含欄位名稱與佔位值）供開發者複製為本機 `keystore.properties`。

#### Scenario: `.gitignore` 排除 keystore 相關檔案

- **WHEN** 檢查根目錄 `.gitignore`
- **THEN** MUST 包含排除 `keystore.properties` 與 keystore 副檔名（例如 `*.jks`、`*.jsk`）的規則

#### Scenario: 範本檔案不含真實密碼

- **WHEN** 檢查 `keystore.properties.example` 內容
- **THEN** 各欄位值 MUST 為明顯的佔位字串，MUST NOT 為可用於實際簽章的真實密碼或 alias

### Requirement: release build MUST 啟用 R8 混淆與資源縮減

`androidApp` 的 `buildTypes.release` MUST 設定 `isMinifyEnabled = true` 與 `isShrinkResources = true`，並套用專案自訂的 `proguard-rules.pro` 疊加 Android Gradle Plugin 預設優化規則檔。

#### Scenario: release 產物完成程式碼與資源縮減

- **WHEN** 執行 `./gradlew :androidApp:assembleRelease`
- **THEN** 產出的 release APK MUST 經過 R8 處理（`isMinifyEnabled` 生效）
- **AND** 未使用的資源 MUST 被移除（`isShrinkResources` 生效）

### Requirement: 混淆後 release build MUST 維持既有核心功能可正常運作

R8 混淆規則 MUST 涵蓋 Koin、Ktor（CIO engine）、kotlinx.serialization、Room 等框架在 release 模式下所需的 keep 規則，混淆後的 release build 中，首頁列表、詳情頁、搜尋、收藏、歷史、設定等既有功能 MUST 維持正常運作，不得因缺少 keep 規則而發生 `ClassNotFoundException`、序列化失敗或 Koin 注入失敗等 runtime 例外。

#### Scenario: 安裝並操作混淆後的 release APK

- **WHEN** 在實體裝置或模擬器安裝 `./gradlew :androidApp:assembleRelease` 產出的 APK 並依序操作首頁列表、詳情頁、搜尋、收藏、歷史、設定
- **THEN** 各流程 MUST 正常渲染與運作，MUST NOT 因混淆造成閃退或功能性例外

### Requirement: 已誤入 git staging 的 keystore 檔案 MUST 移除追蹤

專案根目錄的 `JetpackMovieCompose.jsk` 檔案 MUST 從 git 追蹤中移除（保留本機實體檔案），使其回歸「簽章檔不進版控」的專案規範。

#### Scenario: keystore 檔案不再出現於 git 追蹤狀態

- **WHEN** 執行 `git status`
- **THEN** `JetpackMovieCompose.jsk` MUST NOT 出現在 staged 或 tracked 檔案列表中
- **AND** 該檔案 MUST 仍存在於本機檔案系統中
