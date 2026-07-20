## Context

`:shared` 目前是唯一的 KMP module，`commonMain` 底下以 package 區分七層：`common`（跨層介面／NetworkException／Koin dispatcher-scope module）、`model`（純資料模型）、`network`（datasource／DTO／provider／DI）、`datastore`（UserPreferenceDataSource／DI）、`database`（Room dao／entity／DI）、`data`（repository／mapper／paging／DI）、`domain`（usecase／DI）。根目錄另外放了組裝根 `InitKoin.kt`（統一安裝六個 Koin module）、iOS framework 匯出設定、以及 KMP 範本殘留 `Greeting.kt`／`GreetingUtil.kt`／`Platform.kt`（僅 `Greeting.kt` 使用 `Platform.kt`，兩者皆無實際呼叫端）。

現況依賴關係（依實際 import 盤點，非理論設計）：

```
common   ← 無內部依賴
model    ← 無內部依賴
network  ← common, model（provider 子集額外依賴 datastore，見下方風險）
datastore← common, model
database ← common, model
data     ← common, model, network, datastore, database
domain   ← common, model, data
(root)   ← 依賴以上全部，含 InitKoin／iOS framework 匯出／Greeting-Platform 殘留
```

其中 `network.provider.DatastoreBaseHostUrlProvider`／`DatastoreLanguageProvider` 兩個類別放在 `network` package 卻 import `datastore.UserPreferenceDataSource`，而 `datastore.di.DatastoreModule` 又反過來 import 這兩個類別做 Koin 綁定，形成 package 層級的雙向依賴。現在同一 module 內 Kotlin 編譯器不會擋，但若照現況邊界直接拆成 Gradle module，`shared:network` 與 `shared:datastore` 會互相依賴，Gradle 會直接因為 circular project dependency 建置失敗。

`settings.gradle.kts` 已啟用 `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`，因此各子模組間可用 `projects.shared.common` 這類型別安全存取子模組，不需手動維護字串路徑常數。

先前已封存的 `establish-kmp-dependency-catalog` change 產出的 `kmp-dependency-catalog` spec，已把未來拆分後的模組寫成 `core:model`／`core:domain` 等命名；本次討論已與使用者確認改為維持 `shared:` 前綴（`shared:common`／`shared:model`…），並在本 change 中修正該 spec 內的命名描述，避免 spec 與實際模組結構長期不一致。

## Goals / Non-Goals

**Goals:**
- 把現有 7 個 package 邊界（`common`／`model`／`network`／`datastore`／`database`／`data`／`domain`）各自轉成獨立 KMP Gradle module，模組間依賴改由 Gradle `implementation(projects.shared.xxx)` 宣告，讓依賴方向由建置系統強制驗證。
- 新增 `shared/app` module 作為組裝根，收斂 `InitKoin.kt`、iOS framework 匯出設定，成為 `androidApp`／`iosApp` 唯一依賴的 shared 進入點。
- 修正 `network`↔`datastore` 的雙向依賴（provider 類別搬到 `shared/datastore`）。
- 依實際使用範圍重新分配 Gradle plugin（`room`／`ksp`／`buildconfig`／`kotlin-serialization`）與 Kover 覆蓋率設定。
- 同步修正既有 openspec spec 對 `shared` 模組位置的描述，避免長期與實際結構脫節。

**Non-Goals:**
- 不改變現有分層的依賴「方向」設計：`domain` 直接依賴 `data`（而非透過 domain 定義 repository 介面、data 實作的 Clean Architecture 反轉模式）。這是既有 `kmp-movie-domain-usecases` spec 已定案的模式，本次只做「同一份依賴圖的模組化」，不做架構反轉。
- 不新增 `feature/home` 或任何 UI 層 module；backlog.md 中「首頁 feature 層消費 domain UseCase」的設計留待對應 change 處理，本次不涉及。
- 不引入任何 `expect`/`actual`（承接 backlog.md 已有結論：這 5 個 UseCase 是純 commonMain 邏輯，模組化與 `expect`/`actual` 是兩個不相干的問題）。
- 不變更 app 的執行期行為、UI 或 API 回應格式；純粹是建置結構重組。
- 不處理 Room schema／migration（本次不新增或修改 database schema）。
- 不追加新的第三方依賴或升級既有版本。

## Decisions

### 1. 模組命名維持 `shared:` 前綴，改寫舊 spec 而非改名 `core:`
`kmp-dependency-catalog` spec 先前把未來模組寫成 `core:xxx`，但目前只有這一份 spec 用過這個命名、實際程式碼從未採用過。改用 `core:` 前綴需要同時處理「`shared` 資料夾要不要跟著改名」的問題，且 `shared` 這個詞已經是 iOS framework 的 `baseName`（`Shared`），改名影響面更大。維持 `shared:xxx` 前綴，只需修正 1 份 spec 的命名描述即可讓文件與實作一致，改動最小且不影響既有 iOS 端命名。

### 2. 依現有 package 邊界一比一切成子模組，不額外合併或再拆分
`model` 目前沒有任何內部依賴，且理論上會被 `network`／`datastore`／`database`／`data`／`domain` 全部依賴。考慮過把 `model` 併入 `common`，但 `common` 目前的職責是「跨層介面與 Koin dispatcher/scope 綁定」，語意上和「純資料模型」不同；合併會讓 `common` 變成職責混雜的大雜燴 module，且往後任何只需要 model（例如未來的 feature 層）都會被迫連帶依賴 `common` 的 Koin/DI 相關程式碼。維持 `model` 獨立 module，讓依賴圖最扁平。

### 3. Provider 雙向依賴修正：搬到 `shared/datastore`，不建獨立 binding 模組
討論過兩個方案：(a) 把 `DatastoreBaseHostUrlProvider`／`DatastoreLanguageProvider` 搬到 `datastore` package／module；(b) 新增一個更上層的 binding module（例如 `shared/di`）專門做跨模組 Koin 綁定。選擇 (a)：這兩個類別的實作邏輯本來就是讀取 `UserPreferenceDataSource`，語意上屬於 datastore 的職責，只是命名沿用了 `Datastore*` 前綴卻放錯 package；搬過去後 `datastore.di.DatastoreModule` 對它們的綁定變成模組內部依賴，`network` module 完全不需要知道 `datastore` 的存在。方案 (b) 會多一個模組、多一層間接，且目前沒有其他跨模組綁定案例需要這種通用機制，屬於過度設計。

### 4. 組裝根獨立成 `shared/app`，`shared/` 不保留 `build.gradle.kts`
`InitKoin.kt` 需要依賴全部 6 個底層 Koin module 才能組裝，iOS framework 匯出（`isStatic = true`、`baseName = "Shared"`）也必須依賴全部子模組的內容才能編出完整 framework。若讓 `shared/` 保留自己的 `build.gradle.kts` 同時兼任組裝根，會讓「哪些程式碼實際屬於 shared 這一層、哪些只是聚合」失去清楚邊界，且不符合使用者原本「`shared` 資料夾應該是空的容器」的要求。獨立成 `shared/app` 後，`shared/` 底下只剩 8 個子模組目錄，本身沒有 Kotlin 原始碼與 build script。

### 5. `Greeting.kt`／`GreetingUtil.kt`／`Platform.kt` 予以刪除
三者互相是彼此唯一的呼叫端（`Greeting.kt` 呼叫 `getPlatform()`），專案其他地方（`androidApp`、iOS bridge）完全沒有引用，確認是 KMP 專案範本產生時的殘留 demo 程式碼。模組化本身不需要保留它們，且分不清楚該歸屬哪個子模組（沒有實際職責），趁本次拆分一併移除，避免之後隨意被放進某個子模組造成職責混淆。

### 6. Gradle plugin／Kover 依實際使用者重新分配
- `buildconfig` + `key.properties` 讀取：只有 `network/di/NetworkModule.kt` 使用 `BuildConfig.TMDB_API_KEY`（`commonTest` 的對應測試也在 `network` 底下），只需套用在 `shared/network`。
- `room` + `ksp`：只有 `database` package 使用，套用在 `shared/database`；`kspAndroid`／`kspIosArm64`／`kspIosSimulatorArm64` 的 Room compiler 依賴一併搬過去。
- `kotlin-serialization`：依實際 import `kotlinx.serialization` 的子模組個別套用（`network`、`datastore` 的 mapper 皆使用 `sharedJson`，`model` 的 `@Serializable` 資料類別也需要）。
- `kover`：現有的 filters／verify rule（`network.di`／`network.datasource`…／`data.repository`…／`domain.usecase`…等）依 package 對應拆到各子模組的 `build.gradle.kts`，每個子模組各自維持最低 80% 覆蓋率門檻。

### 7. `sharedJson`（`JsonConfig.kt`）歸屬 `shared/common`
`sharedJson` 被 `network`、`datastore` 兩個平行模組共用，且不依賴任何其他 package，符合 `common` 的「跨層共用工具」定位，搬到 `shared/common` 根package（非 `common` 子package，維持目前的頂層工具檔案定位）。

### 是否遵循既有架構模式
本次為純結構重組，不改變既有 Repository／Use Case／Koin DI 模式；`domain` 依賴 `data`（而非 data 依賴 domain 定義的介面）的既有選擇維持不變，理由見上方 Non-Goals。

## Risks / Trade-offs

- **[風險] 8 個子模組同時搬遷、牽動所有既有 spec 的位置描述，變更範圍大，一次性完成的回歸風險高** → 依 tasks.md 規劃分階段搬遷順序（先搬無依賴的 `common`／`model`，驗證建置成功後再搬下一層），每個階段搬完立即執行對應的 Android／iOS 編譯與既有測試，而不是全部搬完才驗證一次。
- **[風險] Kover filters 重新拆分後可能漏掉既有覆蓋率範圍，導致覆蓋率門檻靜默失效** → 拆分後逐一比對每個子模組的 filters 是否涵蓋原本 `shared/build.gradle.kts` filters 清單中對應的 package，並執行 `kover` verify 任務確認 80% 門檻仍然生效。
- **[風險] `androidApp`、iOS bridge 專案設定檔（Xcode project／Podfile 或等效設定）可能有寫死 `Shared.framework` 或 `:shared` 路徑的地方，遷移後才發現** → 遷移 `shared/app` 前先全域搜尋 `":shared"`、`Shared.framework`、`baseName` 等關鍵字，列出所有需要同步更新的設定檔。
- **[風險] `network.provider` 搬遷後若有測試檔案硬編 import 路徑，會直接編譯失敗但容易漏改** → 搬遷時一併搜尋 `commonTest`／`androidHostTest`／`iosTest` 底下對 `network.provider.Datastore*` 的 import，同步更新測試檔案路徑。
- **[取捨] `shared/app` 依賴全部 8 個子模組，任何底層模組變動都會讓 `shared/app` 重新編譯，模組化在增量建置速度上的效益主要體現在底層模組之間（例如只改 `domain` 不會重編 `network`），而非 `app` 這層本身** → 可接受，這是組裝根的本質限制，不影響本次「依賴方向由 Gradle 強制驗證」的主要目標。
- 本次無資料庫 schema 變更，無需 Room migration 策略。
