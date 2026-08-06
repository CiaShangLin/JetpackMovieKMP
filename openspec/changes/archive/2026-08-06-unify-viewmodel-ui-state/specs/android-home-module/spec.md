## MODIFIED Requirements

### Requirement: `feature/home` 的 import 路徑 MUST 對齊本專案實際命名空間

`HomeViewModel`、`HomeContentViewModel`、`HomeScreen` MUST 使用本專案實際存在的 package（`com.shang.jetpackmoviekmp.data.repository.*`、`com.shang.jetpackmoviekmp.model.*`、`com.shang.jetpackmoviekmp.core.ui.*`、`com.shang.jetpackmoviekmp.core.designsystem.*`、`com.shang.jetpackmoviekmp.domain.usecase.*`），MUST NOT 殘留來源專案的舊 package（`com.shang.data.*`、`com.shang.model.*`、`com.shang.designsystem.*`、`com.shang.ui.*` 等不含 `jetpackmoviekmp` 中綴的路徑）。`HomeViewModel.movieGenres` 的載入狀態 MUST 使用 `shared/common` 提供的 `com.shang.jetpackmoviekmp.common.UiState<MovieGenreBean>`，不得在 `feature/home` 內另行定義獨立的 `HomeUiState` 型別。

#### Scenario: 不存在舊 package import

- **WHEN** 檢查 `feature/home/src/main/java` 下所有 `.kt` 檔案的 import 陳述式
- **THEN** MUST NOT 出現 `com.shang.data.`、`com.shang.model.`、`com.shang.designsystem.`、`com.shang.ui.`（不含 `jetpackmoviekmp`）開頭的 import

#### Scenario: `feature/home` 自身原始碼 package 對齊 namespace

- **WHEN** 檢查 `feature/home` 模組 `src/main`／`src/test`／`src/androidTest` 下所有 `.kt` 檔案的 `package` 宣告
- **THEN** MUST 使用 `com.shang.jetpackmoviekmp.feature.home`（含 `.ui`／`.navigation`／`.di` 子套件）為字首，對齊 `build.gradle.kts` 已宣告的 `namespace = "com.shang.jetpackmoviekmp.feature.home"`
- **AND** MUST NOT 殘留來源專案遺留的 `com.shang.home.*`（不含 `jetpackmoviekmp` 中綴）

#### Scenario: `movieGenres` 使用共用 UiState 型別

- **WHEN** 解析 `HomeViewModel.movieGenres` 的宣告型別
- **THEN** MUST 為 `kotlinx.coroutines.flow.StateFlow<com.shang.jetpackmoviekmp.common.UiState<com.shang.jetpackmoviekmp.model.MovieGenreBean>>`
- **AND** `feature/home` 模組內 MUST NOT 存在獨立宣告的 `HomeUiState` 型別
