## Why

目前 `androidApp` 的 release build type 沒有設定 `signingConfig`，也沒有啟用 R8 混淆（`isMinifyEnabled = false`）。這代表目前無法產出可上架、且具備程式碼保護的 release APK/AAB。同時，開發過程中新增的 keystore 檔案 `JetpackMovieCompose.jsk` 已被 `git add` 進 staging，若直接 commit 會讓簽章金鑰永久留在 git history 中，屬於必須先處理的安全性問題。

## What Changes

- 在 `androidApp/build.gradle.kts` 新增 `signingConfigs.release`，簽章相關敏感參數（storePassword、keyAlias、keyPassword）透過新的 `keystore.properties`（不進版控）讀取，比照現有 `key.properties`（`TMDB_API_KEY`）的做法。
- 新增 `keystore.properties.example` 範本檔（進版控），列出所需欄位名稱與說明性佔位值。
- release build type 的 `signingConfig` 改為指向 `signingConfigs.release`，並啟用 `isMinifyEnabled = true`、`isShrinkResources = true`。
- 新增／補齊 `androidApp/proguard-rules.pro`，為 Koin、Ktor（CIO engine）、kotlinx.serialization、Room（KMP）等框架補上必要的 keep 規則，確保混淆後 release build 仍可正常執行。
- 將已 staged 的 `JetpackMovieCompose.jsk` 從 git 移除追蹤（保留實體檔案），並在 `.gitignore` 新增規則排除 keystore 檔案（`*.jks`、`*.jsk`）。
- **BREAKING**：無（純新增 release 打包能力，不影響現有 debug/benchmark build type 行為）。

## Capabilities

### New Capabilities
- `android-release-build`：`androidApp` release build type 的簽章設定與 R8/ProGuard 混淆設定，涵蓋 `signingConfigs` 讀取機制、`keystore.properties` 範本、`proguard-rules.pro` keep 規則。

### Modified Capabilities
（無：本次為新增 release 打包能力，不涉及既有 spec 的 requirement 變更）

## Impact

- `androidApp`：修改 `build.gradle.kts`（新增 `signingConfigs`、調整 `buildTypes.release`）；新增 `proguard-rules.pro`。
- 專案根目錄：新增 `keystore.properties.example`；修改 `.gitignore`（排除 keystore 檔案與 `keystore.properties`）。
- Git 狀態：`JetpackMovieCompose.jsk` 需從 staging 移除追蹤，不刪除實體檔案。
- 不影響 `shared/*`、`core/*`、`feature/*`、iOS 相關模組；不影響現有 `debug`、`benchmark` build type 設定。
- 不新增或調整 `gradle/libs.versions.toml` 依賴（純 Gradle 設定變更，無新第三方函式庫）。
