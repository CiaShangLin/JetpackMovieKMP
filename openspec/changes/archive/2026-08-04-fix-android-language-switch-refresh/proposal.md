## Why

Android 端切換語言後有兩個交錯的 bug：(1) 系統字串與 Navigation3 畫面內容不會立即刷新（需要旋轉螢幕或系統重建 Activity 才會生效）；(2) 已經從 TMDB API 抓過的資料（電影列表、詳情、演員、推薦）在切換語言後仍停留在舊語言，因為 ViewModel 是 Activity-scoped、不會隨畫面重建而重新查詢。此外 `androidApp` 目前只有繁體中文字串資源，缺少英文版本，即使刷新機制修好，切到英文時這些文字仍會 fallback 回中文；network 層也殘留一個未被實際使用、僅供測試用的「預設 provider」分支，增加不必要的設定面。

## What Changes

- `MainActivity` 語言切換流程改為套用新 Locale 後呼叫 `activity.recreate()`，取代目前手動 `resources.updateConfiguration()` 加 `key(languageMode)` 的競態時序寫法；移除包住 `rememberNavBackStack(HomeKey)` 的 `key(languageMode)`，讓 Navigation3 的 backstack 在 `recreate()` 後維持原有序列化保存機制，使用者停留在原本所在畫面，不被重置回首頁。
- `HomeContentViewModel` 的電影列表 Paging 資料流改為觀察 `userDataRepository.userData` 的 `languageMode`（`distinctUntilChanged()`），語言改變時以 `flatMapLatest` 重新建立 Pager 並重新查詢，取代目前「ViewModel 建構時只查一次、`cachedIn` 鎖住」的行為。
- `MovieDetailViewModel` 的 `movieDetail`／`movieRecommendations`／`movieActors` 資料流比照既有 `retryTrigger` 慣例，加入語言改變事件（與手動 retry 用 `merge()` 合併），語言改變時自動重新查詢，取代目前「只在 ViewModel 建構時查一次、除非手動 retry 否則不重抓」的行為。
- `androidApp`、`feature/collect`、`feature/history` 三個模組補上 `values-en-rUS/strings.xml`（英文），`values/strings.xml` 維持繁體中文為預設語系。
- 移除 `shared/network` 的 `DefaultLanguageProvider` 類別與 `networkModule()` 的 `provideDefaultLanguageProvider` 參數，改為直接依賴 `shared/datastore` 提供的 `DatastoreLanguageProvider`；同步更新 `InitKoin.kt`、`shared/data`／`shared/domain` 測試檔案的呼叫端，刪除 `DefaultLanguageProviderTest.kt`。`LanguageMode.SYSTEM_DEFAULT` 的 `toLanguageCode()` 轉換邏輯不變。

## Capabilities

### New Capabilities

（無，本次為既有能力的行為修正，不新增獨立能力）

### Modified Capabilities

- `android-app-entry`：新增語言切換後 `MainActivity` MUST 重建畫面（`recreate()`）且 MUST 保留原有 backstack／導覽位置的需求。
- `android-home-module`：新增語言改變時首頁電影列表 Paging MUST 以新語言重新查詢的需求。
- `android-movie-detail-module`：新增語言改變時電影詳情／演員／推薦內容 MUST 以新語言重新查詢的需求。
- `android-collect-module`：新增英文語系字串資源齊備的需求。
- `android-history-module`：新增英文語系字串資源齊備的需求。

## Impact

- **androidApp**：`ui/MainActivity.kt`（語言切換流程、移除 `key(languageMode)`）、`utils/LanguageSettingUtils.kt`（沿用，行為不變）、`res/values/strings.xml`（維持中文）、新增 `res/values-en-rUS/strings.xml`。
- **feature/home**：`HomeContentViewModel.kt`（Paging 資料流加入語言 reactive 邏輯）。
- **feature/detail**：`MovieDetailViewModel.kt`（`movieDetail`／`movieRecommendations`／`movieActors` 加入語言 reactive 邏輯）。
- **feature/collect**：新增 `res/values-en-rUS/strings.xml`。
- **feature/history**：新增 `res/values-en-rUS/strings.xml`。
- **shared/network**：刪除 `provider/DefaultLanguageProvider.kt`、`di/NetworkModule.kt` 的 `provideDefaultLanguageProvider` 參數、刪除 `DefaultLanguageProviderTest.kt`。
- **shared/app**：`InitKoin.kt` 呼叫 `networkModule()` 的參數簽名同步調整。
- **shared/data、shared/domain**：`DataModuleTest.kt`、`DomainModuleTest.kt` 呼叫 `networkModule()` 的參數簽名同步調整。
- 不涉及新增依賴，不需要修改 buildSrc。
