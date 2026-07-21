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
- `./gradlew check` — full verification, including ktlint.
- `shared/data` and `shared/network` enforce an 80% minimum test coverage via Kover
  (`./gradlew :shared:data:koverVerify :shared:network:koverVerify`).

### API key setup

The app calls the TMDB API and expects a key in a root-level `key.properties` file (not committed to
version control). Copy `key.properties.example` to `key.properties` and fill in `TMDB_API_KEY`.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…