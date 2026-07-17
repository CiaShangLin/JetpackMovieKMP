## 背景

參考 Android 專案使用 Proto DataStore、Hilt modules 與 generated protobuf classes 儲存使用者偏好設定。這個 KMP 專案的模組化方式不同，且 `shared/commonMain` 使用 Koin。專案目前已經有 `UserData`、`ThemeMode`、`LanguageMode`、`ConfigurationBean`，以及 network 的 `LanguageProvider` interface。

因為這次遷移目標是 `commonMain`，Android-specific 的 `Context.dataStoreFile`、Hilt `@Module`、JVM-only serializer 假設都必須替換成 KMP 相容做法。

## 目標 / 非目標

**目標：**

- 將使用者偏好設定邏輯保留在 `shared/commonMain`。
- 保留參考專案對 configuration、theme、language preferences 的行為。
- 讓 `LanguageProvider.getLanguageCode()` 能同步回傳 datastore 推導出的最新語言，供 network layer 使用。
- 透過 Koin modules 提供 DI。
- 透過一個小型 button-driven test surface 驗證 Android wiring。

**非目標：**

- 完整 settings UI。
- 獨立 datastore Gradle module。
- preferences 的 repository/use-case layer。
- 除了 `language` query parameter 來源以外，不改變 TMDB endpoint 行為。

## 決策

### 1. Datastore 保留在 `shared/commonMain`

Datastore code 會放在 `com.shang.jetpackmoviekmp.datastore`。這符合目前不建立新 module 的限制，也避免在 shared app architecture 還不完整時過早切出 module 邊界。

### 2. 優先使用 KMP-safe persistence，不盲目照搬 Android proto setup

參考專案使用 `user_preferences.proto` 與 generated JVM/Android classes。這次 KMP 遷移需要選擇可以在 KMP build 通過的 DataStore representation。如果 protobuf generation 已經能支援 KMP，則保留等價 proto schema；如果尚未配置，則使用 Preferences DataStore，並用明確 keys 儲存：

- configuration JSON 或等價序列化值
- theme mode
- language mode

驗收重點是行為等價與 KMP 可編譯，不要求保留完全相同的 generated class names。

### 3. DI 使用 Koin

新增 `datastoreModule`，並在 `networkModule` 之前或一起安裝。當 datastore-backed provider 可用時，network module 不應再綁定 `DefaultLanguageProvider`。建議形狀：

```kotlin
modules(
    datastoreModule(...),
    networkModule(isDebug),
)
```

`networkModule` 應維持依賴 `LanguageProvider`；binding 來源改成 datastore。

### 4. 透過快取 datastore flow 值，維持 `LanguageProvider` 同步 API

Ktor 的 `defaultRequest` 目前同步讀取 `languageProvider.getLanguageCode()`。datastore-backed implementation 應在 application-level scope 收集 `UserPreferenceDataSource.userData`，並更新 volatile/cache-backed language code。preferences emit 前預設值維持 `zh-TW`。

Mapping：

- `LanguageMode.ENGLISH` -> `en-US`
- `LanguageMode.TRADITIONAL_CHINESE` -> `zh-TW`
- `LanguageMode.SYSTEM_DEFAULT` -> 若可取得則使用 platform/system language，否則 fallback 到 `zh-TW`

### 5. 使用暫時 Android button 驗證

Android app 目前在啟動時呼叫 `getConfiguration()`。這次可替換或擴充這個 debug surface，加入簡易 button 變更語言並呼叫 network layer。這應該明確維持為簡單測試 UI，不是 production settings screen。

## 風險 / 取捨

- **DataStore 格式風險**：Proto DataStore 可能需要額外 KMP protobuf 設定。如果這個設定對本次變更過大，可接受使用 Preferences DataStore，只要行為等價且測試涵蓋 mapping。
- **同步語言風險**：datastore emit 前快取語言可能短暫使用預設值。這對 startup 可接受；測試需驗證 collection 後語言變更會傳遞。
- **DI 順序風險**：如果 `networkModule` 仍綁定 `DefaultLanguageProvider`，datastore binding 可能被遮蔽或覆蓋。測試需確認 Koin resolve 到 datastore-backed provider。
- **Android-only 驗證風險**：button 只能證明 Android wiring。shared tests 必須涵蓋 common datastore 行為，讓 iOS 至少受到 compile/test 保護。

## 遷移計畫

1. 在 `shared/commonMain` 新增 datastore data source 與 mapping。
2. 在檔案路徑具平台差異的位置新增 platform datastore creation APIs。
3. 新增 Koin `datastoreModule` 並更新 `JetpackMovieApplication`。
4. 將 network language provider binding 替換成 datastore-backed binding。
5. 在 `androidApp` 新增 button 驗證。
6. 新增測試並執行 shared/Android 驗證。
