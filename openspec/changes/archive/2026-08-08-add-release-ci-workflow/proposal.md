## Why

目前 JetpackMovieKMP 沒有任何 GitHub Actions workflow，每次要發布 Android Release APK 都得在本機手動打包、手動建立 GitHub Release、手動上傳檔案，容易出錯且無法保證每次發布前都先跑過單元測試。參考專案 JetpackMovieCompose 已有成熟的 tag-triggered release workflow，本次要把同樣的自動化流程帶進本專案，並針對本專案的簽章設定差異（keystore 路徑、環境變數命名）與現有的 Baseline Profile 手動流程做對應調整。

## What Changes

- 新增 `.github/workflows/release-apk.yml`：push tag（格式 `v*.*.*`）或手動 `workflow_dispatch` 觸發。
- Workflow 分兩個 job，第二個 job 依賴第一個 job 成功：
  - `test`：checkout → 設定 JDK 17、Android SDK → 還原 Gradle 快取 → 用 `secrets.TMDB_API_KEY` 產生 `key.properties` → 執行 `./gradlew :androidApp:testDebugUnitTest`（或等效單元測試 task）→ 上傳測試報告。
  - `build-and-release`（`needs: test`）：checkout（`fetch-depth: 0` 供 changelog 使用）→ 設定 JDK/SDK/Gradle 快取 → 產生 `key.properties` → 用 `secrets.KEYSTORE_BASE64` 解碼並寫入 `keystore/release.jks`（對齊 `androidApp/build.gradle.kts` 的 `rootProject.file("keystore/release.jks")` 讀取路徑，**不是**參考專案的 `app/release-keystore.jks`）→ 以 `storePassword`／`keyAlias`／`keyPassword` 三個環境變數名稱（對應 `System.getenv()` 讀取邏輯，大小寫需完全一致）注入 `secrets.KEYSTORE_PASSWORD`／`secrets.KEY_ALIAS`／`secrets.KEY_PASSWORD` → 執行 `./gradlew :androidApp:assembleRelease` → 用 tag 名稱產生版本號與 changelog（沿用 `git log` 區間比對前一個 tag 的方式）→ 重新命名 APK → 用 `softprops/action-gh-release` 建立 GitHub Release 並上傳 APK → 額外上傳 APK 到 Actions Artifacts。
- 只打包並發布 APK，不產出 AAB。
- Baseline Profile（`androidApp/src/main/baseline-prof.txt`）維持現行「本機手動用 `:benchmark` 模組生成後 commit 進 repo」的流程，CI 不新增 emulator/instrumented test job，也不在 workflow 中重新生成 profile。
- 隨附文件更新：在專案文件中補充「首次啟用此 workflow 前，需要在 GitHub repo 設定哪些 Secrets」的操作說明（`TMDB_API_KEY`、`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`），Secrets 本身由使用者手動在 GitHub UI 設定，不在本次程式碼變更範圍內。

## Capabilities

### New Capabilities
- `release-ci`：定義 tag 觸發的 Android Release 打包與發布行為——觸發條件、job 執行順序與相依關係、簽章與 API Key 注入方式、打包輸出（僅 APK）、GitHub Release 產出內容（版本號、changelog、附件）。

### Modified Capabilities
（無現有 spec 的需求異動，本次為全新的 CI 能力，不涉及既有 `openspec/specs/` 底下任何 capability 的行為調整。）

## Impact

- 新增檔案：`.github/workflows/release-apk.yml`（純 CI 設定，不屬於任何 Gradle module，不影響 Kotlin/Swift 原始碼）。
- 讀取但不修改：`androidApp/build.gradle.kts` 的簽章邏輯（`signingConfigs`、`hasReleaseKeystore` 判斷）與 `shared/network` 的 `BuildConfig.TMDB_API_KEY` 來源設定——workflow 只是照現有邏輯提供對應的檔案與環境變數，不調整這兩處程式碼本身。
- 不涉及：`shared/*`、`core/*`、`feature/*`、`iosApp` 的任何原始碼；不涉及 iOS 平台的打包或發布流程（本次僅處理 Android release）。
- 不涉及：`:benchmark` 模組與 `androidx.baselineprofile` 自動化——Baseline Profile 生成流程維持現狀，不在本次 change 範圍內调整或自動化。
- 文件影響：可能需要在 README 或既有文件補充 Secrets 設定步驟（待 tasks 階段確認落腳位置）。
- `.gitignore` 已於討論過程中先行補上 `*.keystore`、`keystore_base64.txt`（避免簽章檔誤入版控），本次 change 的 tasks 會將這項既有修正一併納入，不需要重複調整。
