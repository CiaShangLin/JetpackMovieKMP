## 1. Module setup

- [x] 1.1 在 `settings.gradle.kts` 加入 `include(":core:ui")`。
- [x] 1.2 建立 `core/ui/build.gradle.kts`，使用 Android library、Kotlin Android 與 Compose compiler 設定。
- [x] 1.3 設定 namespace 為 `com.shang.jetpackmoviekmp.core.ui`。
- [x] 1.4 確認 `core/ui` 不加入 iOS framework export，也不被任何 `shared:*` module 依賴。

## 2. Dependencies

- [x] 2.1 讓 `:core:ui` 依賴 `projects.core.designsystem`。
- [x] 2.2 讓 `:core:ui` 依賴 `projects.shared.model` 與 `projects.shared.common`。
- [x] 2.3 在 `gradle/libs.versions.toml` 補齊缺少的 `lottie-compose` 與 `androidx-compose-material-icons-extended` alias。
- [x] 2.4 加入 Compose、Paging Compose、Coil、Lottie、Material Icons Extended 與必要 Koin 依賴。
- [x] 2.5 不新增 Hilt / Dagger 依賴。

## 3. Source migration

- [x] 3.1 從舊專案 `C:\Users\User\AndroidStudioProjects\JetpackMovieCompose\core\ui\src\main\kotlin` 遷移 UI source。
- [x] 3.2 將 package 從 `com.shang.ui` 調整為 `com.shang.jetpackmoviekmp.core.ui`。
- [x] 3.3 將 `com.shang.model` import 調整為 `com.shang.jetpackmoviekmp.model`。
- [x] 3.4 將 `com.shang.common` import 調整為 `com.shang.jetpackmoviekmp.common`。
- [x] 3.5 將 `com.shang.designsystem` import 調整為 `com.shang.jetpackmoviekmp.core.designsystem`。
- [x] 3.6 將 `R` reference 調整為新 module namespace。

## 4. Resource migration

- [x] 4.1 遷移 `src/main/res/raw/loading.json`。
- [x] 4.2 遷移 `src/main/res/drawable-*/icon_actor_placeholder.webp`。
- [x] 4.3 遷移 `src/main/res/values*` 字串資源。
- [x] 4.4 檢查舊 `androidTest` / `test` example tests 不直接帶入，除非改寫為本專案有意義的測試。

## 5. DI migration

- [x] 5.1 移除舊 `UiModule` 的 Hilt annotations 與 `javax.inject` constructor pattern。
- [x] 5.2 以 Koin module 或 app composition wiring 提供 `HostInterceptor` 與 Coil `ImageLoader`。
- [x] 5.3 確認 Android app 初始化會載入 `core:ui` 所需 DI module，且不影響 iOS Koin bootstrap。

## 6. Integration

- [x] 6.1 視 Android app 整合需要，在 `androidApp/build.gradle.kts` 加入 `implementation(projects.core.ui)`。
- [x] 6.2 確認 `androidApp` 或後續 Android feature modules 可使用 `MovieCard`、`MovieActor`、`LoadingScreen`、`ErrorScreen`。
- [x] 6.3 確認 `shared:*` modules 沒有新增 `projects.core.ui` 或 `project(":core:ui")` 依賴。

## 7. Verification

- [x] 7.1 執行 `.\gradlew.bat :core:ui:compileDebugKotlin`。
- [x] 7.2 執行 `.\gradlew.bat :androidApp:assembleDebug`。
- [x] 7.3 執行 `.\gradlew.bat ktlintCheck` 或專案可用的等效格式檢查。
- [x] 7.4 檢查 `settings.gradle.kts` 中 module path 使用全小寫 `:core:ui`。
- [x] 7.5 檢查 `shared:*` modules 沒有反向依賴 Android-only `core:ui`。
