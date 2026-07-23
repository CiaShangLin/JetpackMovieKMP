## 1. root Gradle build configuration

- [x] 1.1 在 root `build.gradle.kts` 新增 `iosFormat` task，呼叫 SwiftFormat 格式化 `iosApp/iosApp` 範圍內的 Swift 檔案
- [x] 1.2 在 root `build.gradle.kts` 新增 `iosFormatCheck` task，呼叫 SwiftFormat `--lint` 做只讀格式檢查
- [x] 1.3 在 root `build.gradle.kts` 新增 `iosLint` task，呼叫 SwiftLint 做只讀 lint 檢查
- [x] 1.4 在 root `build.gradle.kts` 新增 `iosCodeStyleCheck` task，依賴 `iosFormatCheck` 與 `iosLint`
- [x] 1.5 確保 iOS code style tasks 在缺少 `swiftformat` 或 `swiftlint` 時以清楚錯誤訊息失敗
- [x] 1.6 確認第一版不讓 root `check`、`preBuild` 或 `androidPreBuild` 依賴 iOS code style tasks

## 2. iosApp code style configuration

- [x] 2.1 新增 root `.swiftformat`，設定 SwiftFormat 規則與 `iosApp/iosApp` 檢查範圍
- [x] 2.2 新增 root `.swiftlint.yml`，設定 SwiftLint 規則、include / exclude 範圍與和 SwiftFormat 衝突的規則處理
- [x] 2.3 排除 Xcode project、asset catalog、preview content、build output、`.gradle` 與 generated framework 類型輸出
- [x] 2.4 執行 `iosFormat` 套用既有 `iosApp/iosApp/**/*.swift` 格式，確認 diff 僅包含格式化結果

## 3. project documentation

- [x] 3.1 更新 `README.md` 的 Code style 區段，加入 `iosFormat`、`iosFormatCheck`、`iosLint`、`iosCodeStyleCheck` 指令
- [x] 3.2 更新 `AGENTS.md` 常用指令，加入 Windows PowerShell 與 Unix shell 的 iOS code style task 執行方式
- [x] 3.3 在文件中說明需要安裝 SwiftFormat / SwiftLint，並標明 Windows / macOS 驗證限制
- [x] 3.4 更新 `openspec/backlog.md`，移除或標記「iOS 端 lint 與自動排版流程」待辦為已處理

## 4. verification

- [x] 4.1 執行 `./gradlew ktlintCheck`，確認既有 Kotlin code style 檢查仍通過
- [x] 4.2 在已安裝 SwiftFormat / SwiftLint 的環境執行 `./gradlew iosFormat iosCodeStyleCheck`
- [x] 4.3 若目前環境缺少 SwiftFormat / SwiftLint，執行 iOS tasks 並確認錯誤訊息清楚指出缺少的工具
- [x] 4.4 執行 `openspec status --change "add-ios-lint-formatting"`，確認 change artifacts 已達 apply-ready 狀態
