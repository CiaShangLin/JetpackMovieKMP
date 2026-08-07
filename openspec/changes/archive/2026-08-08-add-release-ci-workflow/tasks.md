## 1. GitHub Actions Workflow

- [x] 1.1 新增 `.github/workflows/release-apk.yml`，設定觸發條件：push tag `v*.*.*` 與 `workflow_dispatch`
- [x] 1.2 建立 `test` job：checkout → 設定 JDK 17 與 Android SDK → 還原 Gradle 快取 → 用 `secrets.TMDB_API_KEY` 產生 `key.properties` → 執行單元測試（`./gradlew :androidApp:testDebugUnitTest` 或等效指令）→ 上傳測試報告 artifact
- [x] 1.3 建立 `build-and-release` job（`needs: test`）：checkout（`fetch-depth: 0`）→ 設定 JDK/SDK/Gradle 快取 → 產生 `key.properties`
- [x] 1.4 在 `build-and-release` job 中新增「解碼 keystore」步驟：將 `secrets.KEYSTORE_BASE64` base64 decode 後寫入 `keystore/release.jks`（對齊 `androidApp/build.gradle.kts:74` 的讀取路徑，而非參考專案的 `app/release-keystore.jks`）
- [x] 1.5 在打包步驟的 `env:` 區塊，將 `secrets.KEYSTORE_PASSWORD`／`secrets.KEY_ALIAS`／`secrets.KEY_PASSWORD` 對應注入為 `storePassword`／`keyAlias`／`keyPassword`（大小寫需與 `System.getenv()` 讀取邏輯完全一致），執行 `./gradlew :androidApp:assembleRelease`
- [x] 1.6 新增「產生版本資訊與 changelog」步驟：從 `GITHUB_REF` 取得 tag 名稱作為版本號；用 `git describe --tags --abbrev=0 HEAD~1` 找前一個 tag，比對區間產生 changelog（找不到前一個 tag 時輸出「初始版本發布」，不得導致 job 失敗）
- [x] 1.7 新增「重新命名 APK」步驟：將 `androidApp/build/outputs/apk/release/androidApp-release.apk` 重新命名為含版本號的檔名（例如 `JetpackMovieKMP-<version>-release.apk`）
- [x] 1.8 新增「建立 GitHub Release」步驟：用 `softprops/action-gh-release` 以版本號為名稱、changelog 為內容、重新命名後的 APK 為附件建立 Release
- [x] 1.9 新增「上傳 APK 到 Actions Artifacts」步驟，作為 Release 之外的備份存取管道

## 2. 版控與安全性複查

- [x] 2.1 複查 `.gitignore` 已包含 `*.keystore`、`keystore_base64.txt`（討論階段已補上，本任務為 PR 前的最終確認，避免遺漏）
- [x] 2.2 確認 `jetpackmoviekmp.keystore` 未被 git 追蹤（`git status` 應顯示 untracked 或不出現），且不會被本次 change 的 commit 誤帶入

## 3. 文件

- [x] 3.1 在專案文件（README 或 `.github/workflows/` 旁的說明文件）補充「啟用此 workflow 前需在 GitHub repo 設定哪些 Secrets」的步驟：`TMDB_API_KEY`、`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`，並註明 `KEYSTORE_BASE64` 對應的本機檔案應是哪一份 keystore
- [x] 3.2 在同一份文件中註明 Baseline Profile 維持本機手動生成、commit 進 repo 的流程，CI 不會自動生成或更新

## 4. 驗證

- [x] 4.1 於 GitHub Actions 頁面手動觸發一次 `workflow_dispatch`，確認 `test` job 與 `build-and-release` job 都能成功執行並產出 GitHub Release 與 APK 附件 — 改用實際 push tag `v0.0.1` 完整驗證（run 31193726610），`test` job 4m6s 通過、`build-and-release` job 3m52s 通過，成功建立 Release：https://github.com/CiaShangLin/JetpackMovieKMP/releases/tag/v0.0.1，附件 `JetpackMovieKMP-v0.0.1-release.apk`
- [x] 4.2 驗證「測試先行」邊界：暫時讓某個單元測試失敗後觸發一次（或以 code review 方式確認 job 相依設定），確認 `build-and-release` 不會執行、不會產生 Release — 以 code review 方式確認：`.github/workflows/release-apk.yml` 的 `build-and-release` job 設有 `needs: test`，此為 GitHub Actions 平台語意保證，`test` job 失敗時 `build-and-release` 不會被排程執行
- [x] 4.3 執行 `./gradlew ktlintCheck` 確認本次新增檔案未破壞既有格式檢查（本次未變更 Kotlin/Swift 程式碼，僅新增 YAML 與文件）— BUILD SUCCESSFUL
- [x] 4.4 執行 `openspec validate add-release-ci-workflow --type change --strict --no-interactive`，確認本次 change 的所有 artifacts 通過驗證 — Change 'add-release-ci-workflow' is valid
