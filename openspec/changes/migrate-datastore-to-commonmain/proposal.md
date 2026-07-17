## 為什麼

`JetpackMovieCompose/core/datastore` 已經負責使用者偏好設定持久化，也提供 network request 使用的語言設定；但目前 KMP 專案在 `shared/commonMain` 只有 model 型別。network layer 仍然把 `LanguageProvider` 綁定到 `DefaultLanguageProvider`，因此 TMDB request 無法反映已持久化的使用者語言設定。

這次變更會把 datastore 行為遷移到 `shared/src/commonMain`，暫時不建立新的 Gradle module。同時會把 datastore 接進 Koin，讓 Android 端可以透過一個簡易測試按鈕切換語言並呼叫 network layer，確認整條路徑有接通。

## 變更內容

- 在 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/` 下新增 `datastore` package，負責使用者偏好設定持久化。
- 將參考專案的 `UserPreferenceDataSource` 行為遷移到 KMP：
  - 暴露 `userData: Flow<UserData>`
  - 持久化 `ConfigurationBean`
  - 持久化 `ThemeMode`
  - 持久化 `LanguageMode`
- 在 shared code 中使用 KMP 相容的 DataStore 設定，不直接照搬參考專案的 Android/Hilt module 形狀。
- 在需要平台檔案路徑的位置加入 platform-aware datastore 建立方式。
- 新增 Koin `datastoreModule`，提供 DataStore、`UserPreferenceDataSource` 與 datastore-backed `LanguageProvider`。
- 透過 DI 將 network layer 原本的預設語言 binding 替換成 datastore-backed provider。
- 更新 Android app 啟動流程，同時安裝 datastore 與 network modules。
- 新增簡易 Android app button：
  - 在繁體中文與英文之間切換
  - 觸發 network call
  - 顯示足夠的 response/status 文字，確認 request path 已接通
- 新增聚焦測試，涵蓋偏好設定 mapping、language provider 行為、DI resolution 與 network language 傳遞。

## Capabilities

### New Capabilities

- `kmp-user-preferences-datastore`：shared KMP 使用者偏好設定持久化，包含 configuration、theme、language。

### Modified Capabilities

- `ktor-movie-network`：`LanguageProvider` 必須由 datastore-backed 使用者偏好設定提供，而不是固定預設 provider。

## 影響範圍

- **受影響 module**：`shared`、`androidApp`
- **受影響 source sets**：
  - `shared/commonMain`：datastore data source、serializer/model mapping、Koin module、language provider implementation
  - `shared/androidMain`：需要時建立 Android datastore file
  - `shared/iosMain`：需要時建立 iOS datastore file
  - `shared/commonTest`：datastore 與 DI tests
  - `androidApp`：Koin module startup 與暫時性的 button 驗證 UI
- **Dependencies**：`shared/build.gradle.kts` 目前已存在 DataStore dependencies；實作時需確認是否還需要額外加入 okio/datastore-core platform 設定或 aliases。

## 非目標

- 不建立獨立的 `core:datastore` Gradle module。
- 不遷移參考專案的 Hilt modules 或 Android-only annotations。
- 不建立正式 production settings screen。
- 不重新設計 network layer，僅替換 language provider binding。
- 不引入 secrets，也不變更 TMDB API key 處理方式。
