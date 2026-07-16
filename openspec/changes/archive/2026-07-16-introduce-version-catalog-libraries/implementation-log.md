# Implementation Log

## Inventory

### Already used

- `shared`: `kotlin-test`
- `androidApp`: `androidx-activity-compose`, AndroidX Compose BOM, foundation, material3, ui, ui tooling preview, ui tooling

### Planned commonMain order

- `kotlinx-coroutines-core`
- `kotlinx-serialization-json`
- Ktor common group: `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging`
- `koin-core`
- KMP storage/data group: `androidx-datastore`, `androidx-datastore-preferences`, `androidx-paging-common`, `androidx-room-runtime`, `androidx-room-paging`, `androidx-sqlite-bundled`
- Evaluate-only group: `coil-network-ktor3`

### Planned platform source set order

- Android: `ktor-client-cio`
- iOS: `ktor-client-darwin` only if common Ktor setup requires an iOS engine in this change
- Room group: KMP runtime, paging, bundled SQLite, Room plugin, and KSP compiler wiring

### Planned commonTest order

- `kotlinx-coroutines-test`
- `ktor-client-mock`
- `koin-test`

### Planned androidApp order

- Existing Compose BOM/catalog verification
- `androidx-core-ktx`
- `androidx-lifecycle-runtime-ktx`
- `androidx-lifecycle-runtime-compose`
- `androidx-lifecycle-viewmodel-compose`
- Evaluate-only group: Navigation 3, Paging runtime/compose, Coil Compose, WorkManager, AppCompat, instrumentation test aliases

## Validation Results

- `kotlinx-coroutines-core`
  - Added to `shared.commonMain`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- `kotlinx-serialization-json`
  - Added Kotlin serialization plugin to `shared`.
  - Added dependency to `shared.commonMain`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- Ktor common group
  - Added `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-client-logging`, and `ktor-serialization-kotlinx-json` to `shared.commonMain`.
  - Group reason: content negotiation and Kotlinx JSON serialization are configured as a minimal common network stack.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- `koin-core`
  - Added to `shared.commonMain`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- KMP storage/data commonMain group
  - Added `androidx-datastore`, `androidx-datastore-preferences`, `androidx-paging-common`, `androidx-room-runtime`, `androidx-room-paging`, and `androidx-sqlite-bundled` to `shared.commonMain`.
  - Paging3 note: `androidx-paging-common` is the KMP/common dependency; Android UI-specific paging remains in `androidApp`.
  - Room note: added Room and KSP plugins, Room schema directory, and KSP compiler dependencies for Android/iOS targets.
  - Each alias was added and validated incrementally.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed for each alias.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed for each alias.
  - Gradle: `.\gradlew.bat :shared:testAndroidHostTest` -> passed after Room wiring.
  - Note: `androidx-datastore` adds `libdatastore_shared_counter.so` and Room/SQLite adds `libsqliteJni.so`; Gradle reports unstripped native library warnings but builds succeed.
- `coil-network-ktor3`
  - Added to `shared.commonMain`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- `ktor-client-cio`
  - Added to `shared.androidMain`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- commonTest group
  - Added `kotlinx-coroutines-test`, `ktor-client-mock`, and `koin-test` to `shared.commonTest`.
  - Gradle: `.\gradlew.bat :shared:testAndroidHostTest` -> passed after each alias.
  - Android CLI: not rerun for these aliases because they only affect test classpath and do not change the runtime APK.
- Android Compose BOM/catalog baseline
  - Existing AndroidX Compose dependencies already use catalog aliases and `platform(libs.androidx.compose.bom)`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- AndroidX core/lifecycle group
  - Added `androidx-core-ktx`, `androidx-lifecycle-runtime-ktx`, `androidx-lifecycle-runtime-compose`, and `androidx-lifecycle-viewmodel-compose` to `androidApp`.
  - Each alias was added and validated incrementally.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed for each alias.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed for each alias.
- Android Navigation/Paging group
  - Added `androidx-navigation3-runtime`, `androidx-navigation3-ui`, `androidx-paging-runtime`, and `androidx-paging-compose` to `androidApp`.
  - Group reason: Android UI navigation and paging dependencies can be wired without adding product UI.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- `coil-compose`
  - Added to `androidApp`.
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
- Final checks
  - Gradle: `.\gradlew.bat :androidApp:assembleDebug` -> passed.
  - Gradle: `.\gradlew.bat :shared:testAndroidHostTest` -> passed.
  - Android CLI: `android run --type ACTIVITY --activity .MainActivity --apks androidApp\build\outputs\apk\debug\androidApp-debug.apk` -> passed.
  - iOS simulator test was not run because Gradle reports iOS simulator tests are disabled on Windows; Room KSP iOS target wiring is present for later macOS validation.

## Skipped Aliases

- `ktor-client-darwin`
  - Category: iOS-only platform engine.
  - Reason: no common Ktor client factory or iOS engine wiring is introduced in this change; user requested iOS can be ignored unless needed.
  - Attempts: 0.
  - Follow-up: add to `iosMain` when shared network client construction is implemented.
- `androidx-work-runtime-ktx`
  - Category: Android background work.
  - Reason: no background job or scheduling use case exists in this dependency baseline.
  - Attempts: 0.
  - Follow-up: introduce with the first background sync/download task.
- `androidx-appcompat`
  - Category: legacy Android view/appcompat integration.
  - Reason: current app uses `ComponentActivity` with Jetpack Compose and does not need AppCompat APIs.
  - Attempts: 0.
  - Follow-up: introduce only if an AppCompat-only integration is required.
- Android instrumentation aliases: `androidx-test-ext-junit`, `androidx-espresso-core`.
  - Category: Android instrumentation tests.
  - Reason: this change does not add instrumentation tests.
  - Attempts: 0.
  - Follow-up: introduce when adding Android instrumentation test source sets.
