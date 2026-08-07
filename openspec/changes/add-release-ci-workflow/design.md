## Context

本專案（JetpackMovieKMP）目前沒有任何 `.github/workflows`，Release APK 都是本機手動打包、手動上傳。參考專案 JetpackMovieCompose 已有一套可行的 tag-triggered release workflow，但兩個專案的簽章設定不完全相同：

- JetpackMovieCompose：keystore 解碼到 `app/release-keystore.jks`，用 `assembleProdRelease`（有 `prod` product flavor）。
- JetpackMovieKMP：`androidApp/build.gradle.kts` 讀取 `rootProject.file("keystore/release.jks")`，並用 `hasReleaseKeystore` 判斷檔案是否存在來決定要不要套用 `signingConfigs.release`；簽章密碼相關值透過 `System.getenv("storePassword")`、`System.getenv("keyAlias")`、`System.getenv("keyPassword")` 讀取（無 product flavor，只有單一 `release` build type）。

另外，本專案有獨立的 `:benchmark` 模組（Macrobenchmark）可以生成 Baseline Profile（`androidApp/src/main/baseline-prof.txt`），但沒有掛 `androidx.baselineprofile` Gradle plugin 做自動化。使用者已確認：baseline profile 的生成維持「本機手動生成、commit 進 repo」，CI 不需要負責生成，只需要在打包時使用 repo 內既有的檔案（Gradle 的 release build 本來就會自動讀取 `src/main/baseline-prof.txt`，不需要 workflow 額外處理）。

## Goals / Non-Goals

**Goals:**
- push tag（格式 `v*.*.*`）自動觸發：先跑單元測試，測試通過才打包 release APK。
- 打包完成後自動建立 GitHub Release，帶上版本號、changelog、APK 附件。
- 簽章與 API Key 完全透過 GitHub Secrets 注入，不在 repo 留下任何明文憑證。
- workflow 的檔案路徑與環境變數命名要對齊本專案 `androidApp/build.gradle.kts` 現有邏輯，而不是照抄參考專案的命名習慣。

**Non-Goals:**
- 不在 CI 內生成或更新 Baseline Profile（不新增 emulator/instrumented test job）。
- 不產出 AAB，只出 APK。
- 不涉及 iOS 打包或發布流程。
- 不調整 `androidApp/build.gradle.kts` 既有簽章邏輯本身（workflow 只是配合現有邏輯提供對應輸入）。
- 不引入 product flavor（本專案目前只有單一 `release` build type，不需要像參考專案的 `assembleProdRelease`）。

## Decisions

### 1. Keystore 落地路徑對齊現有 Gradle 邏輯，而非沿用參考專案路徑
`androidApp/build.gradle.kts:74` 寫死讀取 `rootProject.file("keystore/release.jks")`。CI 解碼 `KEYSTORE_BASE64` 後必須寫到這個路徑，而不是參考專案的 `app/release-keystore.jks`。
- 替代方案：改 `build.gradle.kts` 去配合 CI 慣用路徑——**捨棄**，因為這屬於無關重構，且會影響本機開發者既有的簽章檔案放置習慣。

### 2. 環境變數命名沿用現有的 `storePassword`／`keyAlias`／`keyPassword`
`build.gradle.kts` 用 `System.getenv()` 讀取這三個大小寫敏感的變數名稱。Workflow 的 `env:` 區塊必須完全比對這三個名稱，Secrets 本身仍可用慣用的全大寫命名（`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`），在 `env:` 對應時做轉換即可，不需要改 secrets 命名去配合 gradle，也不需要改 gradle 去配合慣用命名。
- 替代方案：把 `build.gradle.kts` 的 `System.getenv()` 改成大寫命名以貼近業界慣例——**捨棄**，超出本次 change 範圍（修改範圍應聚焦在新增 CI，而非調整既有簽章程式碼）。

### 3. Baseline Profile 不進 CI 自動化
Macrobenchmark 生成 profile 需要 emulator/實機跑 instrumented test，在 GitHub-hosted runner 上要另外設定 `reactivecircus/android-emulator-runner` 之類的 action，會顯著拉長 CI 時間（emulator 啟動+測試通常 5–10 分鐘起），且 emulator 上量出來的 profile 準確度不如實機。使用者已確認採用「本機生成、commit 進 repo」的模式，這也是 Google Now in Android 範例採用的做法：profile 不需要每次 release 都重新生成，只有程式碼有重大導航/路徑變化時才需要手動更新。
- 替代方案：CI 內用 emulator 跑 `:benchmark` 自動生成——**保留為 Open Question**，若未來需要「每次 release 都保證 profile 是最新」的保證，可以另開 change 評估，不在本次範圍。

### 4. 兩個 job（`test` → `build-and-release`）而非單一 job
沿用參考專案的兩階段設計：`build-and-release` 用 `needs: test` 相依，確保測試沒過就不會打包、不會建立 Release。若合併成單一 job，測試失敗時仍可能因為 step 順序疏漏而誤打包；分成兩個 job 讓「測試」與「打包發布」的成敗邊界更明確，且測試報告可以獨立上傳、獨立在 Actions 頁面檢視。

### 5. 只打包 APK，不產出 AAB
現階段沒有上架 Google Play 的需求（`versionCode`/`versionName` 目前是寫死的 `1`/`"1.0"`，尚未對接自動版本號機制），APK 足以滿足直接發布/側載安裝的需求。若未來要上 Play Console，AAB 與版本號自動化需另開 change 討論。

## Risks / Trade-offs

- **[Risk]** `versionCode`/`versionName` 目前寫死在 `androidApp/build.gradle.kts`，不會隨 tag 自動更新，多次發版後 APK 內部版本號不會遞增。
  → **Mitigation**：本次 change 不處理版本號自動化（超出範圍），但在 tasks 中記錄這個限制，GitHub Release 的版本號仍會正確反映 tag（因為是從 `GITHUB_REF` 取得，不是從 APK 內部版本號），使用者可自行決定是否另開 change 處理。
- **[Risk]** Keystore 是使用者現成的 `jetpackmoviekmp.keystore`，若 Secrets 設定錯誤（密碼、alias 打錯），會在 `build-and-release` job 才失敗，且錯誤訊息可能不夠直觀。
  → **Mitigation**：tasks 中提供明確的 Secrets 設定步驟與對照表，並建議使用者先在本機用同一組密碼驗證 keystore 可正常簽章，再設定進 GitHub Secrets。
- **[Risk]** Baseline Profile 若忘記在本機重新生成、忘記 commit，release 版會用到過舊的 profile（雖然不會導致 build 失敗，只會讓效能優化不完全）。
  → **Mitigation**：在文件中註明「重大導航路徑變更後記得重新生成 baseline-prof.txt」，屬於流程提醒而非 CI 強制檢查（強制檢查需要 emulator，已在 Non-Goals 排除）。
- **[Trade-off]** 兩個 job 序列執行（`needs: test`）會讓整體 CI 時間比合併成一個 job 略長（Gradle daemon/快取無法完全跨 job 共用，兩個 job 各自要還原一次快取）。
  → 可接受：正確性（測試沒過絕不發版）優先於少量的時間成本。

## Migration Plan

1. 新增 `.github/workflows/release-apk.yml`（不影響任何現有程式碼路徑，純新增檔案）。
2. 使用者於 GitHub repo 手動設定 5 個 Secrets（`TMDB_API_KEY`、`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`）。
3. 先用 `workflow_dispatch` 手動觸發一次驗證整條流程（不需要真的 push tag），確認 test job、build-and-release job 都能成功跑完並產出 Release。
4. 驗證通過後，之後的正式發版改用 `git tag vX.Y.Z && git push origin vX.Y.Z` 觸發。
5. 沒有回滾機制需求——這是新增 CI 檔案，移除或停用只需刪除/停用該 workflow 檔案，不影響任何已發布的 Release 或程式碼。

## Open Questions

- 是否要在未來另開 change，把 `versionCode`/`versionName` 改成從 tag 自動計算（例如用 `versionName` 對應 tag、`versionCode` 用 commit count 或時間戳）？本次不處理，先維持現狀。
- 是否要在未來評估導入 `androidx.baselineprofile` Gradle plugin 搭配 CI emulator 自動化生成 Baseline Profile？本次維持手動流程，待有明確需求再另開 change。
