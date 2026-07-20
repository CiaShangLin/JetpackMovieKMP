## 1. 建立 module 結構

- [ ] 1.1 在 `settings.gradle.kts` 加入 `include(":core:designsystem")`。
- [ ] 1.2 建立 `core/designsystem/build.gradle.kts`，使用 Android library、Kotlin Android 與 Compose compiler 設定。
- [ ] 1.3 設定 namespace 為 `com.shang.jetpackmoviekmp.core.designsystem`。
- [ ] 1.4 確認 `core/designsystem` 不加入 iOS framework export，也不被任何 `shared:*` module 依賴。

## 2. 遷移 design system source

- [ ] 2.1 從舊專案 `C:\Users\User\AndroidStudioProjects\JetpackMovieCompose\core\designsystem\src/main/kotlin` 遷移 theme 與 component 程式碼。
- [ ] 2.2 將 package 從 `com.shang.designsystem` 調整為 `com.shang.jetpackmoviekmp.core.designsystem`。
- [ ] 2.3 遷移 `src/main/res/drawable-*` 的 loading / error webp 圖片資源。
- [ ] 2.4 保留或建立必要的 `src/main/AndroidManifest.xml`。
- [ ] 2.5 移除舊專案範本測試或無意義 Example test，不遷移無價值測試。

## 3. 串接 Android app

- [ ] 3.1 在 `androidApp/build.gradle.kts` 加入 `implementation(projects.core.designsystem)`。
- [ ] 3.2 確認 Android app 可以引用 design system theme 與 component。
- [ ] 3.3 若舊 component 缺少本專案 version catalog dependency，補齊 `gradle/libs.versions.toml` 與 module dependency。

## 4. 驗證

- [ ] 4.1 執行 `.\gradlew.bat :core:designsystem:compileDebugKotlin`。
- [ ] 4.2 執行 `.\gradlew.bat :androidApp:assembleDebug`。
- [ ] 4.3 檢查 `settings.gradle.kts` 中 module path 使用全小寫 `:core:designsystem`。
- [ ] 4.4 檢查 `shared:*` modules 沒有新增對 `projects.core.designsystem` 的依賴。
