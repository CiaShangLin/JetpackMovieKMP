## Purpose

定義 Android Release APK 的 CI/CD 自動化打包與發布流程，涵蓋 GitHub Actions workflow 的觸發條件（版本 tag push 或手動 `workflow_dispatch`）、單元測試與打包發布 job 之間的相依關係、簽章憑證與 TMDB API Key 透過 GitHub Secrets 安全注入的方式，以及打包成功後只產出 Release APK（不產出 AAB）並自動建立附帶 changelog 與 APK 附件的 GitHub Release。本 capability 不涉及 iOS 平台的打包發布，也不涉及 Baseline Profile 的自動化生成。

## Requirements

### Requirement: Tag 觸發 Release 打包流程
系統 SHALL 在偵測到符合 `v*.*.*` 格式的 git tag 被 push 到遠端時，自動觸發 GitHub Actions release workflow。

#### Scenario: Push 符合格式的版本 tag
- **WHEN** 使用者執行 `git push origin vX.Y.Z`（例如 `v1.2.0`）
- **THEN** GitHub Actions SHALL 觸發 release workflow，依序執行單元測試 job 與打包發布 job

#### Scenario: Push 不符合格式的 tag
- **WHEN** 使用者 push 一個不符合 `v*.*.*` 格式的 tag（例如 `release-candidate`）
- **THEN** release workflow SHALL 不會被觸發

#### Scenario: 手動觸發
- **WHEN** 使用者在 GitHub Actions 頁面手動執行 `workflow_dispatch`
- **THEN** release workflow SHALL 可以在沒有 push tag 的情況下被手動啟動，用於驗證流程是否正常

### Requirement: 測試先行的 Job 相依關係
系統 SHALL 確保打包與發布 job 只在單元測試 job 成功完成後才會執行。

#### Scenario: 單元測試全數通過
- **WHEN** `test` job 中的單元測試全數執行成功
- **THEN** `build-and-release` job SHALL 開始執行，進行打包與發布

#### Scenario: 單元測試出現失敗
- **WHEN** `test` job 中任一單元測試失敗
- **THEN** `build-and-release` job SHALL 不會執行，workflow SHALL 標示為失敗，且不會產生任何 GitHub Release 或 APK 附件

### Requirement: 簽章與 API Key 透過 Secrets 注入
系統 SHALL 在打包 release APK 前，從 GitHub Secrets 還原簽章 keystore 檔案、簽章密碼與 TMDB API Key，且不得在 workflow 檔案或 repo 中留下任何明文憑證。

#### Scenario: Secrets 設定完整
- **WHEN** `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`、`TMDB_API_KEY` 皆已在 repo 設定
- **THEN** workflow SHALL 將 `KEYSTORE_BASE64` 解碼寫入 `keystore/release.jks`，並以 `storePassword`／`keyAlias`／`keyPassword` 環境變數名稱對應注入密碼與別名，同時產生含 `TMDB_API_KEY` 的 `key.properties`，使 `assembleRelease` 能夠成功套用簽章設定並完成打包

#### Scenario: 缺少簽章相關 Secret
- **WHEN** `KEYSTORE_BASE64` 或其他簽章相關 Secret 未設定或為空值
- **THEN** `build-and-release` job SHALL 在解碼或打包步驟失敗並終止，不會產生未簽章或簽章錯誤的 APK 發布出去

### Requirement: 僅產出 APK 並建立 GitHub Release
系統 SHALL 在打包成功後，只產出 release APK（不產出 AAB），並以觸發的 tag 名稱作為版本資訊建立 GitHub Release，附上依 git 歷史生成的 changelog 與該 APK 檔案。

#### Scenario: 打包並發布成功
- **WHEN** `build-and-release` job 成功執行 `assembleRelease` 並產出 APK
- **THEN** workflow SHALL 以觸發的 tag 作為版本號、比對前一個 tag 之間的 commit 訊息產生 changelog，建立一個對應版本號的 GitHub Release，並將重新命名後的 APK 檔案作為附件上傳，同時額外上傳一份到 Actions Artifacts

#### Scenario: 沒有前一個 tag 可比對
- **WHEN** 觸發的 tag 是這個 repo 第一個符合格式的版本 tag，找不到前一個 tag 可以比對 commit 區間
- **THEN** changelog SHALL 顯示為「初始版本發布」，不會因為找不到前一個 tag 而導致整個 job 失敗
