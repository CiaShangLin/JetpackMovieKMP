## Context

Android 端已有完整的首頁實作可對照：`feature/home` 的 `HomeViewModel` 先取得 `MovieGenreBean`（分類清單）決定 Tab 數量，`HomeSuccessScreen` 用 `JMScrollableTabRow` + `HorizontalPager` 呈現分類分頁，每個分頁各自建立一個 `HomeContentViewModel`，透過 `GetHomeMovieListUseCase(genre.id, scope)` 取得 `Flow<PagingData<MovieCardResult>>`，並用 `core/ui` 的 `MovieCard`／`LoadingScreen`（Lottie）／`ErrorScreen` 呈現畫面。

iOS 端目前只有 `MainView` + `MainTab` 提供的底部導覽骨架，`HomeView` 是純文字 placeholder（`ios-main-bottom-navigation`）。iOS 專案採「SwiftUI 原生 UI + Xcode 專案（無 CocoaPods，僅 `.xcodeproj`）」的既有慣例，`shared` 模組目前只透過 `KoinHelper` 對外暴露少量具名 accessor（`getConfigurationUseCase()`、`userDataRepository()`、`getMovieDetailUseCase()`），且**沒有** Compose Multiplatform UI 層——本專案 `core/ui`／`core/designsystem` 是 Android-only 模組，不在 `commonMain`。也就是說，iOS 電影卡片元件必然是一個新的、iOS-native 的 SwiftUI View，無法直接重用 Android 的 `MovieCard`。

另一個關鍵限制：`GetHomeMovieListUseCase` 回傳 `Flow<PagingData<MovieCardResult>>`（Kotlin `androidx.paging`），而 `PagingData` 沒有對應的 Swift 匯出型別，SKIE 也不支援跨平台匯出 `PagingData`；`cachedIn(scope)` 更是綁定 Android `CoroutineScope` 語意。這代表 iOS 端**不能**直接沿用 Android 現有的分頁 UseCase 介面。

使用者將**手動**撰寫這部分 Swift／iOS 程式碼（AI 只負責產出可執行的步驟拆解與 Android 對照說明），因此 `tasks.md` 必須是細粒度、可逐步驗證的清單。

## Goals / Non-Goals

**Goals:**
- 定義 iOS 首頁的畫面結構（Tab + 橫向分頁）、狀態管理（`HomeUiState`：Loading／Success／Error）與資料流（假資料 → 真實 API）
- 定義一個可被收藏／搜尋／歷史等頁面重用的 iOS 電影卡片元件，決定其放置位置與資料模型
- 定義 Loading（Lottie）與失敗畫面（含重試）的呈現方式與資源來源
- 給出「先假資料、後真實 API」的明確切換點，讓使用者可以逐步手動實作並驗證

**Non-Goals:**
- 不在本次 change 內實作真正的無限捲動分頁（Paging 3 的 iOS 對應方案）；第一版首頁清單為「一次性載入單頁資料」，分頁能力列為後續 change 的 backlog
- 不新增或修改 Android 端 `feature/home`、`core/ui` 的既有實作
- 不處理電影詳情頁、收藏頁、搜尋頁本身的畫面實作，只確保電影卡片元件的介面設計上可供它們日後重用

## Decisions

### 1. 電影卡片元件放在哪、如何跨頁面共用（已與使用者確認）

**決策**：在 `iosApp/iosApp/Common/MovieCard/` 新增 iOS-native SwiftUI 元件（例如 `MovieCardView.swift`），不嘗試放進 `shared` 模組。

**理由**：本專案目前的架構慣例是「`shared` 只承載商業邏輯（network/database/datastore/data/domain），UI 100% 平台原生」——Android 的 `MovieCard` 也只存在於 Android-only 的 `core/ui`，並未走 Compose Multiplatform 共用 UI 這條路。iOS 這邊維持相同慣例最省事：日後收藏／搜尋／歷史頁面只需 `import` 這個 SwiftUI View 並帶入資料與 callback 即可重用，不需要跨平台編譯 UI。

**替代方案考慮**：把 UI 改為 Compose Multiplatform 共用元件——需要在 iOS 引入 Compose Multiplatform runtime、大幅偏離目前「iOS = SwiftUI」的既有慣例與 `ios-code-style`／`ios-skie-interop` 既有規格假設，屬於架構級變更，超出本次 change 範圍，故不採用。

### 2. 電影卡片的資料模型（已與使用者確認調整）

**決策**：把 Android 既有的 `core/ui.MovieCardData` 搬到 `shared/model`（沿用 `MovieCardData` 這個名稱與既有欄位命名 `movieCardXxx`，避免不必要的重新命名），讓 Android `core/ui` 與 iOS 共用同一份型別定義；`asMovieCardResult()`/`asMovieCardData()` 轉換函式一併搬過去。iOS 的 `MovieCardView` 直接使用這個經 SKIE 匯出的共用型別，不再另外定義 iOS-only wrapper。

**理由**：使用者明確希望比照 Android 既有的 `MovieCardData` 模式，而不是讓 iOS 端直接消費 `MovieCardResult`；與其在 iOS 端重新定義一份欄位相同的型別（造成兩平台各自維護一份幾乎相同的 wrapper），不如直接把這個 UI 資料模型搬到 `shared/model`，兩平台共用同一份定義與轉換邏輯，符合 KMP 專案「能共用就共用」的精神。`core/ui` 依賴 `shared/model` 本來就是 `android-ui-module` spec 既有允許的依賴方向，不需要新增例外規則。

**原本考慮過的替代方案（已由使用者否決）**：
- 直接用 `MovieCardResult`，不建立 wrapper——被否決，因為使用者希望維持 Android 既有的 `MovieCardData` 抽象，不想讓 UI 層直接綁死 network/repository 回傳的原始 model 型別
- 只在 iOS 端另建一份等價但獨立的 wrapper（不動 Android）——被否決，因為會產生兩份重複定義，日後兩邊分別修改容易失去同步

**對 Android 的影響**：`core/ui/MovieCardData.kt` 移除，改為使用 `shared/model` 新位置的型別；`core/ui/MovieCard.kt`、`feature/home` 內對 `MovieCardData` 的 import 需要更新。欄位內容、轉換邏輯本身不變，Android 端可觀察行為（畫面呈現）不受影響。

### 3. 分類 Tab 的資料來源與 UseCase 邊界（已與使用者確認）

**決策**：新增 `GetMovieGenresUseCase`（`shared/domain`），比照 `GetConfigurationUseCase` 的既有慣例，把 `MovieRepository.getMovieGenres()` 回傳的 `Flow<Result<MovieGenreBean>>` 在邊界處轉成 `Flow<AppResult<MovieGenreBean>>`，並在 `KoinHelper` 新增 `getMovieGenresUseCase()` accessor 供 iOS 呼叫。

**理由**：Android 的 `HomeViewModel` 是直接注入 `MovieRepository` 呼叫 `getMovieGenres()`（因為 Android 端本來就慣於直接處理 `kotlin.Result`），但 iOS 邊界目前的既有慣例（`GetConfigurationUseCase`）是「凡是 Swift 要消費的資料流，一律在 UseCase 層轉成 `AppResult`，讓 SKIE 能匯出成乾淨的 Swift enum」。延續這個慣例可讓 iOS 端所有透過 `KoinHelper` 取得的資料流處理方式一致（`onEnum(of:)` 模式），不需要額外處理 `kotlin.Result` 的匯出細節。

**替代方案考慮**：在 iOS 端直接呼叫 `KoinHelper.movieRepository()` 取得 `MovieRepository` 再自行呼叫 `getMovieGenres()`——這會讓 iOS 直接依賴 `kotlin.Result`（而非 `AppResult`），與 `GetConfigurationUseCase` 建立的既有慣例不一致，故不採用。

### 4. 電影清單的分頁策略（本次 change 的關鍵取捨，已與使用者確認：本次先不做持續載入/無限捲動）

**決策**：新增一支 iOS 專用、**不做分頁**的 UseCase：`GetHomeMovieListSnapshotUseCase`（暫定名稱，實作時可調整），內部呼叫既有 `MovieRepository.getMovieListPager(withGenres)` 取得 `Flow<PagingData<MovieCardResult>>` 後，於 `shared/domain` 內部用 `PagingData` 的 `Flow<PagingData<T>>` 轉一次性 `List<T>`（例如透過既有 repository 是否有非分頁 API，若無則討論是否讓 repository 額外提供「取第一頁」的方法），對外回傳 `Flow<AppResult<List<MovieCardResult>>>`。真正的「捲到底載入下一頁」不在本次範圍內。

**理由**：`PagingData` 無法乾淨匯出給 Swift，若要在本次 change 內處理「Kotlin Paging ↔ Swift 分頁」的完整方案，需要額外研究（例如自建游標式分頁 API、或引入 SKIE 以外的橋接機制），會大幅拖慢「使用者手動一步步做」的節奏，也不是使用者這次提出的核心訴求（核心是「Tab + 卡片列表 + Loading/Error + 之後接 API」）。先用「單頁快照」滿足「畫面能顯示真實資料、能重試」的需求，分頁本身列為 backlog，待首頁基本體驗完成後再開新 change 處理。

**風險**：若 TMDB 分類清單資料量大，使用者捲到底不會自動載入下一頁，體驗上是「有限清單」而非「無限捲動」——這點會在 Risks / Trade-offs 中明確記錄，並建議記錄到 `openspec/backlog.md`。

### 5. 假資料到真實 API 的切換方式（已與使用者確認）

**決策**：`HomeViewModel` 建構時注入的相依（`GetMovieGenresUseCase`、新的電影清單 UseCase）在第一階段先不注入、改由 `HomeViewModel` 內一個私有的 `mockMovieGenres` / `mockMovieList` 產生假資料並直接指派給 `uiState`；等 UI／Tab／卡片／Loading／Error 畫面都手動刻完並肉眼驗證過後，再把 `HomeViewModel` 改為注入上述兩個 UseCase、比照 `SplashViewModel` 的 `for await result in useCase.invoke()` + `onEnum(of:)` 模式改寫資料來源。這個切換點會在 `tasks.md` 中拆成清楚的兩個階段。

**理由**：讓使用者可以先專注在畫面／狀態機的手刻練習，不被 Koin／SKIE／非同步串接的複雜度打斷；等畫面正確後再一次性替換資料來源，risk 較低、每一步都可獨立驗證。

## Risks / Trade-offs

- **[Risk] 分頁能力缺失** → 首頁清單第一版是有限筆數（單頁快照），使用者捲到底沒有自動載入下一頁的體驗，與 Android 端不一致 → **Mitigation**：在 UI 上不做「假裝支援無限捲動」的視覺暗示（例如不加底部 loading spinner），並將「iOS 首頁分頁／無限捲動」記錄為後續 change 的待辦
- **[Risk] `PagingData` → `List` 轉換若在 `shared/domain` 內用 `firstOrNull()` 或類似方式擷取快照，行為需要額外測試覆蓋** → **Mitigation**：`shared/domain` 屬於需維持 80% Kover 覆蓋率的模組之一，新增的 UseCase 需補齊對應單元測試（AAA 模式），比照 `GetConfigurationUseCaseTest` 的既有寫法
- **[Risk] Lottie iOS 依賴為本專案首次導入** → 需要在 Xcode 專案設定 Swift Package 依賴、確認 `loading.json` 資源可正確被 bundle 讀取 → **Mitigation**：`tasks.md` 拆出獨立、可先行驗證的步驟（先讓 Lottie 動畫在單一測試畫面跑起來，確認資源載入無誤，再接進 Home Loading 狀態）
- **[Trade-off] 電影卡片不做跨平台 UI 共用** → 犧牲「一份 UI 程式碼兩平台共用」的理論收益，換取符合本專案既有「UI 100% 平台原生」慣例、不引入 Compose Multiplatform 的額外複雜度

## Open Questions

- `GetMovieGenresUseCase`／`GetHomeMovieListSnapshotUseCase` 的確切命名與參數（例如是否需要 `page` 參數以利未來擴充分頁）留待實作時與使用者確認
- 是否需要把「iOS 分頁／無限捲動首頁清單」現在就記錄到 `openspec/backlog.md`（建議在本次討論收尾前用 `/flow-note` 記錄）
