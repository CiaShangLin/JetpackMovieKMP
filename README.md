This is a Kotlin Multiplatform movie browsing app (powered by the TMDB API) targeting Android and iOS.

* [/iosApp](./iosApp/iosApp) contains the iOS application. Even though the UI is shared via Compose
  Multiplatform on Android, iOS still needs this entry point. This is also where SwiftUI code for the
  project lives.

* [/shared](./shared) contains the Kotlin Multiplatform modules shared across platforms, split by layer:
  - `model` — plain data models (`*Bean`, `UserData`, `ThemeMode`, `LanguageMode`)
  - `common` — shared abstractions (`NetworkException`, `BaseHostUrlProvider`, `LanguageProvider`, `JsonConfig`)
  - `network` — Ktor client and TMDB API data sources (`*Response` DTOs)
  - `database` — Room (KMP) database, DAOs and entities
  - `datastore` — DataStore-backed user preferences
  - `data` — repository implementations, paging, and DTO → Bean mappers
  - `domain` — use cases, depends only on `data`
  - `app` — assembly layer; wires up all Koin modules via `InitKoin` and is the source of the
    `Shared` framework consumed by iOS

  Each module has `commonMain` for platform-agnostic code, plus `androidMain` / `iosMain` for
  platform-specific implementations (expect/actual pattern).

* [/core](./core) contains Android-only UI building blocks:
  - `designsystem` — Compose design system components and theme
  - `ui` — shared Android screens/components built on top of the design system

* [/androidApp](./androidApp) is the Android app entry point (`Application`, `MainActivity`, navigation).

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks per module, e.g.:

- Android host tests: `./gradlew :shared:data:testAndroidHostTest`
- iOS simulator tests: `./gradlew :shared:data:iosSimulatorArm64Test`

Swap `:shared:data` for any other shared module (`network`, `domain`, `database`, `datastore`, `app`, …).

### Code style & coverage

- `./gradlew ktlintCheck` / `./gradlew ktlintFormat` — Kotlin code style check/format (also runs
  automatically before every build via `preBuild`).
- `./gradlew iosFormat` — format iOS Swift code with SwiftFormat.
- `./gradlew iosFormatCheck` — check iOS Swift formatting without modifying files.
- `./gradlew iosLint` — lint iOS Swift code with SwiftLint.
- `./gradlew iosCodeStyleCheck` — run iOS Swift format check and lint.
- `./gradlew check` — full Kotlin/Android verification, including ktlint.
- `shared/data` and `shared/network` enforce an 80% minimum test coverage via Kover
  (`./gradlew :shared:data:koverVerify :shared:network:koverVerify`).

iOS Swift code style tasks require `swiftformat` and `swiftlint` to be installed. Windows environments
may only be able to verify that missing-tool errors are reported clearly; run the full iOS style check
on a macOS machine with the Swift tooling installed.

### API key setup

The app calls the TMDB API and expects a key in a root-level `key.properties` file (not committed to
version control). Copy `key.properties.example` to `key.properties` and fill in `TMDB_API_KEY`.

### Release CI（GitHub Actions）

`.github/workflows/release-apk.yml` 會在 push 符合 `v*.*.*` 格式的 tag（或手動
`workflow_dispatch`）時觸發：先跑 `test` job 執行單元測試，通過後才進入
`build-and-release` job 打包 release APK，並自動建立 GitHub Release、上傳 APK
附件。只打包 APK，不產出 AAB。

首次啟用前，需先在 GitHub repo 的 **Settings → Secrets and variables →
Actions** 設定以下 Secrets：

| Secret 名稱 | 對應用途 |
|---|---|
| `TMDB_API_KEY` | 產生 CI 端的 `key.properties`，供 `shared/network` 讀取 |
| `KEYSTORE_BASE64` | Release 簽章 keystore 檔案的 base64 內容（本機用 `base64` 指令編碼要用來簽章的 `.jks`/`.keystore` 檔案） |
| `KEYSTORE_PASSWORD` | 對應 `androidApp/build.gradle.kts` 讀取的環境變數 `storePassword` |
| `KEY_ALIAS` | 對應環境變數 `keyAlias` |
| `KEY_PASSWORD` | 對應環境變數 `keyPassword` |

CI 會把 `KEYSTORE_BASE64` 解碼寫入 `keystore/release.jks`（對齊
`androidApp/build.gradle.kts` 的 `rootProject.file("keystore/release.jks")`
讀取路徑），本機開發者也應把簽章檔放在同一路徑。

**Baseline Profile**（`androidApp/src/main/baseline-prof.txt`）維持本機手動
生成、commit 進 repo 的流程：透過 `:benchmark` 模組的 Macrobenchmark
（`BaselineProfileGenerator`）在實機或 emulator 上跑 instrumented test 產出，
CI 不會自動重新生成或更新，只會在打包時使用 repo 內既有的檔案。若有重大導航
或程式碼路徑變更，建議重新在本機生成一次並 commit。

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
