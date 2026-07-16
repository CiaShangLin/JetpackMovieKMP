## 1. Root Build Configuration

- [ ] 1.1 依 design 版本表更新 `gradle/libs.versions.toml`，加入 KSP、Serialization、Ktor、Koin、Room 3、SQLite、Coroutines、DataStore、Paging、Coil、Lifecycle、Navigation 3 與測試 aliases
- [ ] 1.2 從 catalog 排除 Retrofit、OkHttp、Hilt、Gson converter、Retrofit coroutine adapter、舊 Room 2、JVM Protobuf 與未使用的 Sandwich aliases
- [ ] 1.3 在根目錄 `build.gradle.kts` 以 `apply false` 宣告 Serialization、KSP 與 Room 3 plugin aliases，確認 Kotlin/Compose plugins 維持同版
- [ ] 1.4 檢查 alias 命名與版本值，禁止 dynamic/snapshot 版本及 build scripts 重複硬編外部版本
- [ ] 1.5 保留 Compose Multiplatform 與 Compose Compiler aliases，並確認它們未被加入任何純 KMP core dependency bundle

## 2. shared

- [ ] 2.1 檢查 `shared/build.gradle.kts` 的現有依賴均透過 catalog alias 引用，不在第一階段掛入尚未有程式碼使用的 Ktor、Koin、Room 或 DataStore runtime
- [ ] 2.2 依 design 註記後續 source set 映射：KMP libraries 放 commonMain、CIO 放 androidMain、Darwin 放 iosMain、跨平台測試 aliases 放 commonTest
- [ ] 2.3 確認 common source set 不引用 Activity、AppCompat、WorkManager、Chucker、Lottie、Android Material、JUnit/Espresso 或 Android MockK aliases
- [ ] 2.4 記錄 `shared` 為暫時的 optional CMP UI layer，禁止在第一階段新增共用 UI 或把新的 UseCase、Repository 與資料存取邏輯放入其中

## 3. androidApp

- [ ] 3.1 檢查 `androidApp/build.gradle.kts` 的現有 Android app、Activity、debug tooling 與 instrumentation 依賴均使用 catalog alias
- [ ] 3.2 確認 Android-only aliases 沒有透過 bundle 或 shared dependency 污染 common source sets，且未使用套件不預先掛入 runtime
- [ ] 3.3 確認 Android Jetpack Compose aliases 與 optional CMP aliases 可清楚區分，平台 UI 不透過 catalog bundle 反向污染純 KMP core

## 4. Build Verification

- [ ] 4.1 執行 Version Catalog accessor 與 Gradle configuration 驗證，修正 plugin resolution 或 alias 錯誤
- [ ] 4.2 執行現有 Android compile、common tests 與 iOS metadata/framework compile tasks
- [ ] 4.3 檢查 catalog，確認沒有 Retrofit、OkHttp、Hilt、Gson converter、Room 2 runtime/compiler、JVM Protobuf 或 `coil-network-okhttp` aliases
- [ ] 4.4 執行 `ktlintCheck`，並記錄 Kotlin 2.4.0、KSP 2.3.9、Room 3.0.0 與各 KMP 套件的 plugin/catalog resolution 結果
