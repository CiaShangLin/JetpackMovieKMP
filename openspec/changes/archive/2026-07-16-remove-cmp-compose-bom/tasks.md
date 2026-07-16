## 1. Root Build Configuration

- [x] 1.1 更新 `gradle/libs.versions.toml`，新增或整理 AndroidX Compose BOM 與 AndroidX Compose aliases，並移除 `compose-multiplatform` version。
- [x] 1.2 移除 `org.jetbrains.compose` plugin alias 與所有 `org.jetbrains.compose.*` library aliases。
- [x] 1.3 保留 `compose-compiler` plugin alias，並確認它只服務 Android Jetpack Compose，不被視為 CMP plugin。
- [x] 1.4 更新 root `build.gradle.kts`，移除 `libs.plugins.compose.multiplatform` apply false。

## 2. androidApp

- [x] 2.1 從 `androidApp/build.gradle.kts` 移除 `libs.plugins.compose.multiplatform` plugin。
- [x] 2.2 將 Android Compose dependencies 改為 AndroidX Compose aliases，並加入 `implementation(platform(libs.androidx.compose.bom))`。
- [x] 2.3 確認 `androidApp` 的 Compose tooling preview/tooling 使用 AndroidX artifacts，debug tooling 維持在 debug configuration。
- [x] 2.4 若現有 shared `App()` 仍需 Android 顯示，將必要 UI 搬到 `androidApp` 或建立最小 Android Compose entrypoint，避免依賴 shared CMP UI。
- [x] 2.5 確認 `androidApp` 可成功編譯並保留可啟動的最小 Android UI entrypoint，不新增 Compose UI 測試檔。

## 3. shared and iOS Bridge

- [x] 3.1 從 `shared/build.gradle.kts` 移除 `libs.plugins.compose.multiplatform`、CMP dependencies、CMP resources 設定與 `androidRuntimeClasspath(libs.compose.ui.tooling)`。
- [x] 3.2 移除或改寫 `shared/src/commonMain` 中依賴 `@Composable`、Compose resources、Painter、Modifier 或 Compose UI 的檔案。
- [x] 3.3 移除 `ComposeUIViewController` entrypoint，改為提供 SwiftUI 可消費的 shared facade、model/state API，或刪除未使用的 CMP bridge。
- [x] 3.4 確認 `shared` 公開 API 只暴露 Kotlin model、Flow/StateFlow、suspend API 或平台可橋接 facade。

## 4. Verification

- [x] 4.1 以搜尋確認沒有 `org.jetbrains.compose`、`compose-multiplatform`、`ComposeUIViewController` 或 `org.jetbrains.compose.resources` 殘留。
- [x] 4.2 執行 Android compile 與 shared KMP metadata/framework compile，確認 plugin 與 dependency resolution 正常。
- [x] 4.3 安裝並啟動 `androidApp`，確認移除 CMP 後 app 可在裝置或模擬器開啟。
- [x] 4.4 執行既有 unit tests，必要時補上 shared facade 或 Android entrypoint 測試，維持 AAA 模式與最低 80% 覆蓋率要求。
- [x] 4.5 專案未提供 `ktlintCheck` task，改以 `git diff --check` 與 `:androidApp:lintDebug` 確認格式與靜態檢查通過。
