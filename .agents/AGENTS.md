# AGENTS.md

本檔案提供 `JetpackMovieKMP` 專案的代理作業規範。

## 回答語言

- 預設使用繁體中文（zh-TW）回答使用者。
- 程式碼、指令、檔名、API 名稱、錯誤訊息與 commit message 可依原文保留。
- 若使用者明確指定其他語言，才改用指定語言。

## 專案概覽

- 專案類型：Kotlin Multiplatform，目標平台包含 Android 與 iOS。
- 根專案名稱：`JetpackMovieKMP`
- 模組：
  - `androidApp`：Android app 入口。
  - `shared`：Compose Multiplatform 與 Kotlin 共用程式碼。
  - `iosApp`：iOS app 入口。
- Package namespace：`com.shang.jetpackmoviekmp`
- 主要版本集中管理於 `gradle/libs.versions.toml`。

## 參考專案

需要查找既有 app 架構、命名規則、實作模式、release workflow 或其他設計參考時，可以參考本機專案：

`C:\Users\User\AndroidStudioProjects\JetpackMovieCompose`

此專案只作為參考來源。不要直接複製程式碼；需要依照本 KMP 專案的模組結構與目前依賴進行調整。

## 常用指令

請從 repository 根目錄執行。

```bash
# 建置 Android debug app
./gradlew :androidApp:assembleDebug

# 執行 shared Android host tests
./gradlew :shared:testAndroidHostTest

# 執行 shared iOS simulator tests
./gradlew :shared:iosSimulatorArm64Test

# 執行較完整的 Gradle 驗證（若可用）
./gradlew check
```

Windows PowerShell 使用：

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :shared:iosSimulatorArm64Test
.\gradlew.bat check
```

## 開發規範

- 優先沿用專案既有模式，再考慮新增結構。
- 可跨 Android 與 iOS 共用的 business logic 與 UI logic 優先放在 `shared`。
- Android-only 行為放在 `androidApp` 或 `shared/src/androidMain`。
- iOS-only 行為放在 `iosApp` 或 `shared/src/iosMain`。
- Compose Multiplatform 相關實作遵循 `shared/src/commonMain` 既有慣例。
- 修改範圍應聚焦在使用者要求的行為，避免無關重構。
- 不要提交 secrets 或機器本機專屬設定。
- 除非使用者明確要求，`local.properties`、簽章檔、API key、產生的 build output 都視為非原始碼產物。

## 固定 Commit Skill

使用者要求 commit 時，預設使用 `caveman-commit` skill/workflow。

Commit workflow：

1. 先檢查目前狀態：

   ```bash
   git status --short
   git diff HEAD
   git branch --show-current
   git log --oneline -10
   ```

2. 只 stage 與本次要求相關的檔案。
3. 建立單一且聚焦的 commit，commit message 必須清楚描述變更。
4. 不要包含無關的本機變更。
5. 除非使用者明確要求 push，否則不要 push。

當目前代理環境可用時，使用 `caveman-commit` 進行 commit 準備與 commit message 產生。

## Git Remote

GitHub remote：

```bash
origin https://github.com/CiaShangLin/JetpackMovieKMP.git
```

只有在使用者明確授權後才 push。
