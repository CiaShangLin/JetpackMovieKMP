## 1. feature/search 模組建立與依賴

- [x] 1.1 新增 `feature/search` Android library，設定 namespace、JVM target、Compose compiler、serialization 與既有 feature 一致的 Android 設定。
- [x] 1.2 為 `feature/search` 宣告 `core:designsystem`、`core:ui`、`shared:common`、`shared:data`、`shared:model` 與 Compose/Paging/Koin/Navigation3 等既有版本 catalog 依賴；不新增第三方依賴。
- [x] 1.3 在 `settings.gradle.kts` 納入 `:feature:search`，並新增 feature 專屬預設、`values-zh-rTW`、`values-en-rUS` 搜尋字串資源。

## 2. feature/search 搜尋 UI 與狀態

- [x] 2.1 建立 `SearchViewModel`，注入 `MovieRepository`，以已提交 query、300ms debounce 與 retry trigger 產生並快取 `PagingData<MovieCardResult>`；空 query 不得建立 repository 搜尋。
- [x] 2.2 建立 Koin `searchModule()`，註冊 `SearchViewModel` 並遵循既有 feature module 的 KDoc 與 package 命名。
- [x] 2.3 建立 `SearchScreen` 與初始提示 UI：維持暫存輸入文字、在 IME Search 提交非空 query 時隱藏鍵盤並觸發 ViewModel 搜尋。
- [x] 2.4 以 `JMLazyVerticalGrid`、`MovieCard`、`LoadingScreen` 與 `ErrorScreen` 顯示 refresh、append、錯誤重試與分頁結尾狀態；append 重試使用 Paging 的 `retry()`。
- [x] 2.5 建立 `SearchKey` 與 `searchEntry()`，以 Navigation3 `NavEntry` 將搜尋目的地提供給 app 導覽層。
- [x] 2.6 為 `SearchViewModel` 撰寫 AAA JVM 單元測試與 fake repository，覆蓋初始空 query、提交 query、切換 query、retry 重新取得目前 query pager 等行為，並維持模組 80% 最低覆蓋率。

## 3. androidApp 導覽整合

- [x] 3.1 在 `androidApp` 加入 `feature:search` 依賴。
- [x] 3.2 在 `MainNavItem` 啟用 Search 導覽項目與本地化標籤，使用 `SearchKey` 和既有 selected/unselected Search icons。
- [x] 3.3 在 `MainActivity` 的 Navigation3 entry provider 註冊 `searchEntry()`，使底部導覽可顯示搜尋畫面而非 placeholder。

## 4. 驗證

- [x] 4.1 執行 `./gradlew :feature:search:testDebugUnitTest`，確認 SearchViewModel 單元測試通過。
- [x] 4.2 執行 `./gradlew ktlintCheck`，確認新增 Kotlin 與 Gradle 檔案符合格式規範。
- [x] 4.3 執行 `./gradlew :androidApp:assembleDebug`，確認搜尋 module 與 Android App 可完整建置。

## 5. 實作後 Android UI 調整

- [x] 5.1 調整 `MainNavItem` 宣告順序，使搜尋為首頁、收藏之後的第三個底部導覽項目，並新增對應 JVM 單元測試。
- [x] 5.2 以來源專案的 `icon_empty.webp` 取代 `feature/history` 既有的同名 vector XML 空狀態資產。
