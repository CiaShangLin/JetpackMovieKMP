## 1. 前置準備與盤點

- [ ] 1.1 全域搜尋 `":shared"`、`project(":shared")`、`Shared.framework`、`baseName` 等關鍵字，列出 `androidApp/build.gradle.kts`、iOS Xcode 專案設定／Podfile（或等效設定）中所有需要同步更新的 shared 依賴來源
- [ ] 1.2 全域搜尋 `commonTest`／`androidHostTest`／`iosTest` 底下對 `network.provider.DatastoreBaseHostUrlProvider`、`network.provider.DatastoreLanguageProvider` 的 import，列出需要同步搬遷的測試檔案
- [ ] 1.3 確認 `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` 已啟用（`settings.gradle.kts` 既有設定），規劃各子模組間改用 `projects.shared.xxx` 型別安全存取

## 2. shared:model（無內部依賴，優先遷移）

- [ ] 2.1 建立 `shared/model` 目錄與 `build.gradle.kts`（KMP target：`iosArm64`、`iosSimulatorArm64`、`androidLibrary`，套用 `kotlin-serialization` plugin）
- [ ] 2.2 搬遷 `shared/src/commonMain/kotlin/.../model/*.kt` 全部檔案到 `shared/model/src/commonMain`
- [ ] 2.3 `settings.gradle.kts` 新增 `include(":shared:model")`
- [ ] 2.4 執行 `shared:model` 的 Gradle 編譯，確認無 dependency resolution 或 plugin wiring 錯誤

## 3. shared:common（無內部依賴）

- [ ] 3.1 建立 `shared/common` 目錄與 `build.gradle.kts`（依賴 `projects.shared.model`；套用 `koin-core`、`kotlinx-coroutines-core`、`kotlin-serialization`）
- [ ] 3.2 搬遷 `common/BaseHostUrlProvider.kt`、`common/LanguageProvider.kt`、`common/NetworkException.kt`、`common/di/CommonModule.kt` 到 `shared/common`
- [ ] 3.3 搬遷根目錄 `JsonConfig.kt`（`sharedJson`）到 `shared/common`
- [ ] 3.4 `settings.gradle.kts` 新增 `include(":shared:common")`
- [ ] 3.5 執行 `shared:common` 的 Gradle 編譯與既有 unit test，確認 `commonModule()` 可解析 `CoroutineScope`／`CoroutineDispatcher`

## 4. shared:datastore（含 provider 循環依賴修正）

- [ ] 4.1 建立 `shared/datastore` 目錄與 `build.gradle.kts`（依賴 `projects.shared.common`、`projects.shared.model`；套用 `androidx-datastore`、`androidx-datastore-preferences`、`okio`）
- [ ] 4.2 搬遷 `datastore/UserPreferenceDataSource.kt`、`datastore/UserPreferencesMapper.kt`、`datastore/UserPreferencesDataStoreFactory.kt`（含 android/iosMain actual）、`datastore/di/DatastoreModule.kt` 到 `shared/datastore`
- [ ] 4.3 將 `network/provider/DatastoreBaseHostUrlProvider.kt`、`network/provider/DatastoreLanguageProvider.kt` 搬到 `shared/datastore`（套件路徑改為 `com.shang.jetpackmoviekmp.datastore` 或 `datastore.provider`，與 design.md 決策一致），更新 `DatastoreModule.kt` 內的 import 為模組內部參照
- [ ] 4.4 `settings.gradle.kts` 新增 `include(":shared:datastore")`
- [ ] 4.5 確認 `shared/datastore/build.gradle.kts` 未依賴 `projects.shared.network`
- [ ] 4.6 執行 `shared:datastore` 的 Gradle 編譯與既有 unit test（含 datastore-backed provider 的 `commonTest`）

## 5. shared:network

- [ ] 5.1 建立 `shared/network` 目錄與 `build.gradle.kts`（依賴 `projects.shared.common`、`projects.shared.model`；套用 `buildconfig`、`kotlin-serialization` plugin；`ktor-client-core`／`content-negotiation`／`logging`／`kotlinx-json`；`androidMain` 加 `ktor-client-cio`，`iosMain` 加 `ktor-client-darwin`）
- [ ] 5.2 從根 `build.gradle.kts` 搬遷 `buildConfig { packageName(...); buildConfigField("TMDB_API_KEY", ...) }` 與 `key.properties` 讀取邏輯到 `shared/network/build.gradle.kts`
- [ ] 5.3 搬遷 `network/datasource`、`network/model`、`network/extension`、`network/di/NetworkModule.kt`、`network/provider/DefaultLanguageProvider.kt`、`network/provider/SystemLanguage.kt`（含 android/iosMain actual）到 `shared/network`
- [ ] 5.4 確認 `network/provider` 底下只剩不依賴 datastore 的檔案（`DefaultLanguageProvider`、`SystemLanguage`）
- [ ] 5.5 搬遷對應的 `commonTest`（含 `MovieDataSourceImplTest.kt` 等使用 `BuildConfig.TMDB_API_KEY` 的測試）
- [ ] 5.6 `settings.gradle.kts` 新增 `include(":shared:network")`
- [ ] 5.7 確認 `shared/network/build.gradle.kts` 未依賴 `projects.shared.datastore`
- [ ] 5.8 執行 `shared:network` 的 Gradle 編譯與既有 unit test（含 TMDB base URL、api_key、language provider 相關測試）

## 6. shared:database

- [ ] 6.1 建立 `shared/database` 目錄與 `build.gradle.kts`（依賴 `projects.shared.model`；套用 `room`、`ksp` plugin；`androidx-room-runtime`／`room-paging`／`sqlite-bundled`；為 `androidLibrary`、`iosArm64`、`iosSimulatorArm64` 設定 Room KSP compiler）
- [ ] 6.2 搬遷 `database/AppDatabase.kt`（含 android/iosMain actual：`AppDatabase.android.kt`、`AppDatabase.ios.kt`）、`database/dao`、`database/entity`、`database/di/DatabaseModule.kt` 到 `shared/database`
- [ ] 6.3 搬遷 `database` 相關的 `commonTest`／`androidHostTest`／`iosTest`（含 `TestDatabaseBuilder` 系列）
- [ ] 6.4 搬遷 `room { schemaDirectory(...) }` 設定與 `schemas/` 目錄到 `shared/database`，確認受版控 schema directory 生效
- [ ] 6.5 `settings.gradle.kts` 新增 `include(":shared:database")`
- [ ] 6.6 執行 `shared:database` 的 Gradle 編譯與既有 unit test

## 7. shared:data

- [ ] 7.1 建立 `shared/data` 目錄與 `build.gradle.kts`（依賴 `projects.shared.common`、`projects.shared.model`、`projects.shared.network`、`projects.shared.datastore`、`projects.shared.database`；套用 `androidx-paging-common`、`coil-network-ktor3`）
- [ ] 7.2 搬遷 `data/repository`、`data/model`、`data/paging`、`data/di/DataModule.kt` 到 `shared/data`
- [ ] 7.3 搬遷對應的 `commonTest`
- [ ] 7.4 `settings.gradle.kts` 新增 `include(":shared:data")`
- [ ] 7.5 執行 `shared:data` 的 Gradle 編譯與既有 unit test（含 `MovieRepository`／`UserDataRepository` 相關測試）

## 8. shared:domain

- [ ] 8.1 建立 `shared/domain` 目錄與 `build.gradle.kts`（依賴 `projects.shared.common`、`projects.shared.model`、`projects.shared.data`）
- [ ] 8.2 搬遷 `domain/usecase`、`domain/di/DomainModule.kt` 到 `shared/domain`
- [ ] 8.3 搬遷對應的 `commonTest`（5 個 UseCase 的測試）
- [ ] 8.4 `settings.gradle.kts` 新增 `include(":shared:domain")`
- [ ] 8.5 執行 `shared:domain` 的 Gradle 編譯與既有 unit test

## 9. shared:app（組裝根）

- [ ] 9.1 建立 `shared/app` 目錄與 `build.gradle.kts`（依賴其餘全部 7 個 `shared:*` 子模組；套用 iOS framework 匯出設定 `baseName = "Shared"`、`isStatic = true`、`androidLibrary` namespace）
- [ ] 9.2 搬遷 `InitKoin.kt`、`InitKoinIos.kt` 到 `shared/app`
- [ ] 9.3 確認 `Greeting.kt`、`GreetingUtil.kt`、`Platform.kt`（含 android/iosMain actual）在專案其他地方無任何引用後刪除
- [ ] 9.4 `settings.gradle.kts` 移除 `include(":shared")`，新增 `include(":shared:app")`
- [ ] 9.5 更新 `androidApp/build.gradle.kts` 的 shared 依賴為 `projects.shared.app`
- [ ] 9.6 依 1.1 盤點結果更新 iOS 專案設定，確認 iOS framework 依賴來源改為 `shared/app`
- [ ] 9.7 執行 `shared:app` 的 Gradle 編譯，確認 iOS framework 可正常匯出

## 10. Kover 覆蓋率重新校準

- [ ] 10.1 依原 `shared/build.gradle.kts` 的 filters 清單（`network.di`／`network.datasource`／`network.extension`／`network.provider`、`datastore`／`datastore.di`、`database`／`database.di`／`database.entity`、`common.di`、`data.repository`／`data.paging`／`data.model`／`data.di`、`domain.usecase`／`domain.di`），對應拆分到 `shared:network`、`shared:datastore`、`shared:database`、`shared:common`、`shared:data`、`shared:domain` 各自的 `kover { reports { filters { ... } } }`
- [ ] 10.2 依原 excludes 清單（`database.dao`、`DatabaseBuilder_androidKt`、`AppDatabase_Impl`、`AppDatabaseConstructor`）套用到 `shared:database`
- [ ] 10.3 各子模組的 `verify { rule(...) { minBound(80) } }` 重新設定，確認每個子模組各自維持最低 80% 覆蓋率門檻
- [ ] 10.4 執行各子模組的 `koverVerify`，確認覆蓋率門檻仍然通過

## 11. 清理與收尾

- [ ] 11.1 確認 `shared/` 根目錄已無 `build.gradle.kts`、無殘留 Kotlin 原始碼，只剩 8 個子模組目錄
- [ ] 11.2 執行 `gradlew.bat ktlintCheck`，確認涵蓋所有新子模組且通過
- [ ] 11.3 執行 Android debug compile／assemble，並使用 `android-cli` 安裝啟動確認 app 可正常開啟
- [ ] 11.4 執行 iOS metadata／framework compile，確認 iOS 端可正常建置
- [ ] 11.5 執行全專案 `commonTest`／`androidHostTest`／`iosTest`，確認所有既有測試通過
- [ ] 11.6 檢查 openspec `specs/` delta 是否與最終實作一致（模組路徑、provider 位置、`initKoin` 安裝的 6 個 module），如有落差同步修正 delta spec
