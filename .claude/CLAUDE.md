# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 語言規範

- 一律使用繁體中文（zh-TW）回覆使用者；程式碼、指令、檔名、API 名稱、commit message 可保留原文。
- 程式碼註解與文件也使用繁體中文撰寫。

## 專案概覽

Kotlin Multiplatform 電影瀏覽 App（TMDB API），目標平台為 Android 與 iOS。共用邏輯以 Compose/Kotlin 放在 `shared/*`，Android UI 在 `androidApp` + `core/*`，iOS 入口在 `iosApp`（Xcode 專案，消費名為 `Shared` 的靜態 framework，由 `shared/app` 產出）。

Package namespace：`com.shang.jetpackmoviekmp`。版本統一管理於 `gradle/libs.versions.toml`。

## 常用指令

```bash
# 建置 Android debug app
./gradlew :androidApp:assembleDebug

# 執行某個 shared 模組的 Android host 測試（單元測試主要跑這個）
./gradlew :shared:data:testAndroidHostTest

# 執行單一測試類別
./gradlew :shared:data:testAndroidHostTest --tests "com.shang.jetpackmoviekmp.data.repository.MovieRepositoryImplTest"

# 執行 iOS simulator 測試
./gradlew :shared:data:iosSimulatorArm64Test

# ktlint 檢查 / 自動格式化（根專案自訂 task）
./gradlew ktlintCheck
./gradlew ktlintFormat

# 覆蓋率驗證（data、network 模組有 80% 下限規則）
./gradlew :shared:data:koverVerify :shared:network:koverVerify

# 完整驗證（含 ktlint）
./gradlew check
```

注意：`preBuild` 已掛上 `ktlintFormat` + `ktlintCheck`，任何建置都會先跑 ktlint，格式錯誤會直接讓建置失敗。

## API 金鑰設定

TMDB API 金鑰放在根目錄 `key.properties`（不進版控，範本見 `key.properties.example`）。`shared/network` 透過 buildconfig plugin 產生 `BuildConfig.TMDB_API_KEY`。

## 架構

### 模組依賴圖（由下往上）

```
shared/model      ── 純資料 model（*Bean、UserData、ThemeMode、LanguageMode）
shared/common     ── 共用抽象：NetworkException、BaseHostUrlProvider、LanguageProvider、JsonConfig
shared/network    ── Ktor client、TMDB API DataSource、*Response DTO（CIO for Android / Darwin for iOS）
shared/database   ── Room（KMP）：AppDatabase、DAO、Entity，platform 各自有 expect/actual builder
shared/datastore  ── DataStore 偏好設定，並提供 common 介面的實作（DatastoreLanguageProvider 等）
shared/data       ── Repository 實作、Paging（app 端 PagingSource）、Response→Bean Mapper
shared/domain     ── UseCase（Get*UseCase），只依賴 data
shared/app        ── 組裝層：InitKoin 彙整所有 Koin module；iOS framework（baseName "Shared"）從這裡輸出
core/designsystem ── Android Compose 元件庫（JM* 前綴元件、theme）
core/ui           ── Android 共用 UI（MovieCard、MovieListPagerScreen、Coil HostInterceptor）
androidApp        ── Android 入口（Application 初始化 Koin、MainActivity、Navigation3）
```

資料流向：`network/database/datastore → data（Repository + Mapper）→ domain（UseCase）→ UI`。網路層 DTO 命名 `*Response`，對外 model 命名 `*Bean`，轉換集中在 `shared/data` 的 `MovieMapper`。

### DI（Koin）

每個模組有自己的 `di/*Module.kt`（如 `NetworkModule`、`DataModule`、`DomainModule`）。`shared/app` 的 `InitKoin.kt` 統一組裝，platform 入口分別為 `InitKoinAndroid.kt`（由 `JetpackMovieApplication` 呼叫）與 `InitKoinIos.kt`（由 Swift 端呼叫）。新增注入時記得把模組加進 InitKoin。

### 測試結構

- Source set：`commonTest`（共用測試 + 測試工具如 `TestDatabaseBuilder`、`InMemoryPreferencesDataStore`）、`androidHostTest`（JVM 上跑）、`iosTest`。
- 測試採 AAA 模式（Arrange / Act / Assert）。
- `shared/data` 與 `shared/network` 有 Kover 80% 最低覆蓋率驗證規則，修改這兩個模組時需補測試。
- 網路測試使用 `ktor-client-mock`。

### expect/actual 慣例

平台差異透過 expect/actual 處理，檔名慣例為 `Xxx.kt`（common）+ `Xxx.android.kt` / `Xxx.ios.kt`，例如 `AppDatabase`、`TestDatabaseBuilder`、`SystemLanguage`。

## 開發規範

- 優先沿用既有模式（Repository / UseCase / Koin module / Mapper），偏離時需說明原因。
- 可跨平台共用的邏輯放 `shared`；Android-only 放 `androidApp`、`core/*` 或 `androidMain`；iOS-only 放 `iosApp` 或 `iosMain`。
- 涉及資料庫 schema 變更時需考慮 Room migration 策略（schema 輸出在 `shared/database/schemas`）。
- 修改範圍聚焦在使用者要求的行為，避免無關重構。
- `local.properties`、`key.properties`、簽章檔、build 產物不進版控。

## Commit 流程

使用者要求 commit 時，預設使用 `caveman-commit` skill。只 stage 與本次要求相關的檔案，建立單一聚焦的 commit。Commit 前需通過 `ktlintCheck`。

## OpenSpec

本專案使用 openspec（spec-driven）管理變更提案，設定在 `openspec/config.yaml`：proposal 的 Impact 段落須列出受影響模組；specs 的 Requirement/Scenario 以繁體中文撰寫；tasks 以模組為單位分組。
