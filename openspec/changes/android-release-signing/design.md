## Context

`androidApp/build.gradle.kts` 目前 `buildTypes.release` 只設定 `isMinifyEnabled = false`，沒有 `signingConfig`，`benchmark` build type 則是暫時借用 `signingConfigs.debug`。這代表目前無法產出正式簽章、經過混淆的 release APK/AAB。

開發過程中已產生 keystore 檔案 `JetpackMovieCompose.jsk`（位於專案根目錄），且已被 `git add` 進 staging。專案慣例（`key.properties` 存放 `TMDB_API_KEY`，由 `shared/network/build.gradle.kts` 以 `java.util.Properties` 讀取並透過 `buildconfig` plugin 產生 `BuildConfig` 欄位）已存在敏感設定「不進版控、以 `.properties` 檔案在 build script 內讀取」的先例，本次簽章設定需沿用同一模式，而非另創新機制。

本次所有簽章相關參數（`keyAlias`、`storePassword`、`keyPassword`）的值皆固定為字串 `jetpackmoviekmp`。

## Goals / Non-Goals

**Goals:**
- `androidApp` 的 `release` build type 具備完整 `signingConfig`，可直接用 `./gradlew :androidApp:assembleRelease` 或 `bundleRelease` 產出已簽章的 release 產物。
- 簽章密碼與 alias 不以明文寫在會進版控的 `build.gradle.kts` 或任何 tracked 檔案中。
- release build 啟用 R8（`isMinifyEnabled = true`）與資源縮減（`isShrinkResources = true`），並提供最小可行的 `proguard-rules.pro`，讓 Koin / Ktor / kotlinx.serialization / Room 等框架在混淆後仍可正常運作。
- 修正 `JetpackMovieCompose.jsk` 已被 git 追蹤的問題，回歸「簽章檔不進版控」的專案規範。

**Non-Goals:**
- 不處理 CI/CD（例如 GitHub Actions release workflow）自動化簽章與發布流程，本次僅涵蓋本機 Gradle 設定。
- 不調整 `debug`、`benchmark` build type 既有行為（`benchmark` 仍可維持借用 `debug` 簽章，本次不強制其改用 release 簽章）。
- 不涉及 Play Store / Play App Signing 上架設定。
- 不新增或調整 `gradle/libs.versions.toml` 任何依賴。

## Decisions

### 1. 簽章參數讀取機制：新增 `keystore.properties`，比照 `key.properties` 模式
在 `androidApp/build.gradle.kts` 用 `java.util.Properties()` 讀取 rootProject 的 `keystore.properties`（不存在時給空字串預設值，行為對齊 `shared/network/build.gradle.kts` 讀取 `key.properties` 的寫法），欄位包含：
- `storeFile`（相對於專案根目錄的 keystore 路徑，例如 `JetpackMovieCompose.jsk`）
- `storePassword`
- `keyAlias`
- `keyPassword`

新增 `keystore.properties.example`（進版控）作為範本，值使用佔位字串（不寫入真實密碼）。

**考慮過的替代方案**：直接把密碼寫在 `build.gradle.kts` 明文中 — 否決，違反專案「簽章檔與 secrets 不進版控」規範，且會讓密碼永久留在 git history。改用環境變數（CI 常見做法）— 本次為本機開發設定，且專案已有 `.properties` 檔案先例，優先維持一致性，不引入新機制；未來若接 CI 可再擴充讀取環境變數作為 fallback。

### 2. `signingConfigs.release` 設定位置：直接寫在 `androidApp/build.gradle.kts` 的 `android {}` block
沿用現有單一 module 直接設定的風格（本專案 `androidApp` 沒有自訂 Gradle convention plugin），不額外抽出共用 Gradle script，避免過度設計。

### 3. `buildTypes.release` 同時啟用 `isMinifyEnabled` 與 `isShrinkResources`
兩者一併開啟才能讓 R8 同時做程式碼縮減與未使用資源縮減，符合「release 打包」的一般預期。`proguardFiles` 使用 `getDefaultProguardFile("proguard-android-optimize.txt")` 疊加專案自訂的 `proguard-rules.pro`（沿用 Android Gradle Plugin 標準寫法）。

### 4. `proguard-rules.pro` 涵蓋範圍
本專案技術堆疊為 Kotlin Multiplatform + Koin + Ktor（Android 用 CIO engine）+ kotlinx.serialization + Room（KMP）。這些框架多數已透過各自 artifact 內建的 `consumer-rules.pro` 自動附加 keep 規則（不需專案端重複撰寫），但 R8 對 kotlinx.serialization 的多型/反射存取與 Room 產生的程式碼在特定設定下仍可能需要專案層補強規則。設計原則：
- 優先信任函式庫自帶的 consumer rules，不預先寫入不確定必要的大範圍 `-keep`。
- `proguard-rules.pro` 初始版本先留基礎骨架與必要注解（說明用途），待實際跑過 release build 驗證是否因混淆造成 crash，再依錯誤訊息補上對應 keep 規則（作為 tasks 中的驗證步驟，而非在 design 階段預先猜測所有規則）。

**考慮過的替代方案**：一開始就寫入大量涵蓋所有框架的保守 `-keep class ** { *; }` 規則 — 否決，這會大幅削弱 R8 縮減效果，違背啟用混淆的目的；且 r8-analyzer skill 的原則也建議避免 package-wide 的過寬規則。

### 5. Git 處理：`git restore --staged` 移除追蹤 + `.gitignore` 排除
`JetpackMovieCompose.jsk` 目前是新增（狀態 `A`）尚未 commit，因此 `git restore --staged JetpackMovieCompose.jsk` 即可移除追蹤且不影響實體檔案，不需要 `git rm --cached` 後再處理 history（因為還沒有 commit 記錄，不存在 history 洩漏問題）。`.gitignore` 新增 `*.jks`、`*.jsk` 規則，避免未來又被誤 `git add`。

## Risks / Trade-offs

- [風險] `keystore.properties` 或 keystore 檔案遺失會導致無法再簽出相同 `applicationId` 的 release 更新 → 緩解：由使用者自行妥善備份 keystore 檔案與密碼（超出本次 change 範圍，僅在 tasks 中提醒）。
- [風險] 首次啟用 R8 混淆可能讓現有功能在 release build 出現 runtime crash（reflection、serialization 相關）→ 緩解：tasks 中安排實際建置並手動驗證核心流程（首頁列表、詳情頁、搜尋、收藏、歷史、設定），發現問題再補 `proguard-rules.pro` 規則。
- [風險] `keystore.properties.example` 若不慎填入真實密碼會造成外洩 → 緩解：只使用明顯的佔位字串（如 `CHANGE_ME`），並在 code review 階段確認。
- [trade-off] 不處理 CI 自動化簽章，意味著 release 打包目前仍為手動流程 → 可接受，因為本次範圍限定本機 Gradle 設定，CI 整合留待未來另立 change。
