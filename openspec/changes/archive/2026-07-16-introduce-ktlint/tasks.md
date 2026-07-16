## 1. root Gradle ktlint 設定

- [x] 1.1 在 root `build.gradle.kts` 建立 ktlint CLI configuration，使用 version catalog alias 注入 dependency
- [x] 1.2 在 root `build.gradle.kts` 註冊 `ktlintCheck` `JavaExec` task，不帶 format flag，掃描 Kotlin source 與 Kotlin Script
- [x] 1.3 在 root `build.gradle.kts` 註冊 `ktlintFormat` `JavaExec` task，帶入 ktlint format flag
- [x] 1.4 在 root `build.gradle.kts` 設定 glob include/exclude，覆蓋 `androidApp`、`shared` 與 Gradle Kotlin DSL，排除 build output
- [x] 1.5 在 root `build.gradle.kts` 將 `check` 依賴 `ktlintCheck`，但不讓 `preBuild` 或 build lifecycle 自動依賴 `ktlintFormat`
- [x] 1.6 在 Android/KMP pre-build lifecycle 接上 `ktlintCheck`，讓 build 前執行格式檢查一次

## 2. version catalog

- [x] 2.1 在 `gradle/libs.versions.toml` 新增 ktlint CLI version
- [x] 2.2 在 `gradle/libs.versions.toml` 新增 ktlint CLI library alias
- [x] 2.3 評估舊專案 `com.pinterest:ktlint:0.49.0` 是否可支援目前 Kotlin 2.4.0；若不可行，在 design 記錄升級版本與原因

## 3. androidApp module

- [x] 3.1 確認 `androidApp/build.gradle.kts` 不需要重複定義 ktlint task
- [x] 3.2 確認 `androidApp` source 會被 root `ktlintCheck` 掃描
- [x] 3.3 確認 Android build 會觸發 `ktlintCheck`，但不會隱式執行 `ktlintFormat`

## 4. shared module

- [x] 4.1 確認 `shared/build.gradle.kts` 不需要重複定義 ktlint task
- [x] 4.2 確認 `shared/src/commonMain`、`shared/src/androidMain`、`shared/src/commonTest` 與 iOS source set 的 Kotlin 檔案會被 root `ktlintCheck` 掃描
- [x] 4.3 確認 KMP Android library 與 iOS target 設定不受 ktlint task 影響

## 5. validation

- [x] 5.1 執行 `gradlew.bat ktlintCheck`
- [x] 5.2 若 `ktlintCheck` 發現既有格式問題，執行 `gradlew.bat ktlintFormat` 後重跑 `gradlew.bat ktlintCheck`
- [x] 5.3 執行 `gradlew.bat check`，確認 `ktlintCheck` 會被納入驗證
- [x] 5.4 如完整 `check` 因非 ktlint 既有問題失敗，至少執行 `gradlew.bat :androidApp:assembleDebug` 與 `gradlew.bat :shared:testAndroidHostTest`，並在實作紀錄中說明未通過項目
