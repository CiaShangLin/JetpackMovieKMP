## Why

現有 `:shared` 是單一 Gradle module，把 common、model、network、datastore、database、data、domain 七層邏輯全部塞進同一個 `commonMain` source set。依賴方向只能靠人工 code review 把關——目前 `network.provider.DatastoreBaseHostUrlProvider`／`DatastoreLanguageProvider` 已經與 `datastore` package 形成雙向依賴，只是因為同屬一個 module 才沒被 Gradle 擋下來。Room／KSP plugin、iOS framework 匯出、Kover 覆蓋率設定也全部綁在同一份 `shared/build.gradle.kts`，難以針對個別層級做獨立建置最佳化。趁 domain 層剛完成 commonMain 遷移、程式庫邊界還算單純，把既有 package 邊界轉成真正的 Gradle module 邊界，讓依賴方向由 Gradle 強制驗證，而非僅靠約定。

## What Changes

- 新增 7 個 KMP 子模組：`shared/common`、`shared/model`、`shared/network`、`shared/datastore`、`shared/database`、`shared/data`、`shared/domain`，各自承接現有同名 package 的內容，Gradle path 採 `:shared:common`、`:shared:network`…等命名慣例。
- 新增 `shared/app` 子模組，收斂原本放在 `shared` 根目錄的組裝根邏輯：`InitKoin.kt`（統一安裝六個 Koin module 的進入點）、iOS framework 匯出設定（`baseName = "Shared"`、`isStatic = true`）；`androidApp`、iOS bridge 改為依賴 `:shared:app`。
- `shared/` 資料夾本身移除 `build.gradle.kts`，不再是可建置的 Gradle module，純粹作為容納上述 8 個子模組的容器目錄。
- **BREAKING**（僅限建置設定，非執行期行為）：`settings.gradle.kts` 移除 `include(":shared")`，改為 include 8 個新子模組 path；所有原本 `implementation(project(":shared"))` 的地方（`androidApp`、iOS bridge 設定）需改為依賴 `:shared:app`。
- 修正 `network.provider.DatastoreBaseHostUrlProvider`／`DatastoreLanguageProvider` 的雙向依賴：兩個類別搬到 `shared/datastore`，消除 `network → datastore` 這條依賴方向，讓 `network` 模組不再需要知道 `datastore` 的存在。
- 依實際使用範圍重新分配現有 Gradle plugin：`buildconfig`（`TMDB_API_KEY`）與 `key.properties` 讀取邏輯只留在 `shared/network`；`room` + `ksp` 只留在 `shared/database`；`kotlin-serialization` 只保留在實際 import `kotlinx.serialization` 的子模組；`kover` 覆蓋率設定拆到各子模組並依現有 filters/verify rule 重新校準範圍。
- 清理 `shared` 根目錄下已無人使用的 KMP 範本殘留（`Greeting.kt`、`GreetingUtil.kt`）；`Platform.kt` 是否保留與歸屬模組於 design 階段確認。
- 同步更新 8 份既有 openspec spec 對 `shared` module 的位置描述，改為對應的 `shared:xxx` 子模組（見下方 Modified Capabilities）。

## Capabilities

### New Capabilities
- `kmp-shared-module-boundaries`：定義 `shared/` 底下各 KMP 子模組的邊界、命名慣例（`shared:common`／`shared:model`／`shared:network`／`shared:datastore`／`shared:database`／`shared:data`／`shared:domain`／`shared:app`）、允許的模組依賴方向（例如 `network`、`datastore` 皆可依賴 `common`／`model` 但不得反向；`data` 依賴 `network`／`datastore`／`database`；`domain` 依賴 `data`；`app` 依賴全部子模組並作為 `androidApp`／iOS 的唯一進入點），以及 provider 實作應歸屬哪個模組的判斷原則。

### Modified Capabilities
- `common-kernel`：`shared/commonMain` 相關的模組位置描述改為 `shared/common`
- `kmp-dependency-catalog`：「純 KMP 核心與 CMP UI 隔離」需求中的 `core:model`／`core:domain`…等命名改為對應的 `shared:xxx`；Room／KSP plugin 套用位置改為 `shared/database`；ktlintCheck 掃描範圍描述更新為涵蓋所有 `shared:*` 子模組（而非單一 `shared`）
- `ktor-movie-network`：網路層相關描述由 `shared` 改為 `shared/network`
- `kmp-user-preferences-datastore`：`datastoreModule` 所在模組改為 `shared/datastore`，並反映 provider 搬遷後的依賴關係（不再依賴 `network` package）
- `kmp-movie-local-database`：`databaseModule` 所在模組改為 `shared/database`
- `kmp-movie-data-repository`：`dataModule` 所在模組改為 `shared/data`
- `kmp-movie-domain-usecases`：`domainModule` 所在模組改為 `shared/domain`
- `ios-koin-bootstrap`：`initKoin`、iOS framework 匯出所在模組改為 `shared/app`；並同步補上目前 spec 內容遺漏的 `databaseModule`／`dataModule`／`domainModule` 安裝項目（現行 spec 只描述三個 module，已與 `InitKoin.kt` 實際安裝的六個 module 不一致）

## Impact

- 受影響 module：`shared`（拆解為 `shared/common`、`shared/model`、`shared/network`、`shared/datastore`、`shared/database`、`shared/data`、`shared/domain`、`shared/app`）、`androidApp`（依賴目標由 `:shared` 改為 `:shared:app`）、iOS bridge／`iosApp`（framework 依賴來源改變）
- `settings.gradle.kts`：移除 `include(":shared")`，新增 8 個子模組 include
- `gradle/libs.versions.toml`：不需新增外部依賴版本，但既有 alias 的使用範圍依模組重新分配（`room`、`buildconfig`、`kover` 等 plugin 只留在實際用到的子模組 build script）
- 每個子模組需要各自的 `build.gradle.kts`，沿用現有 `shared/build.gradle.kts` 的 KMP target 設定（`iosArm64`、`iosSimulatorArm64`、`androidLibrary`）；除 `shared/app` 外，其餘子模組不需要 iOS framework 匯出設定
- Kover 覆蓋率設定需拆分到各子模組並重新校準 filters／verify rule，無法再用單一 `shared/build.gradle.kts` 的 filters 涵蓋全部範圍
- 現有 8 份 openspec spec 檔案需要 delta 更新模組位置描述（見上方 Modified Capabilities）
