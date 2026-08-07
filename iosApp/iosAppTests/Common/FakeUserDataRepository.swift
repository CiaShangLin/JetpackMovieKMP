import Shared

/// `UserDataRepository` 是完整的 Kotlin interface；這個 Fake 只提供編譯所需的最小實作，
/// 讓測試能建構持有 `UserDataRepository` 的 ViewModel，測試本身不驗證這裡的行為。
final class FakeUserDataRepository: UserDataRepository {
    var userData: SkieSwiftFlow<UserData> {
        fatalError("not used in these tests")
    }

    func __setConfiguration(configuration: ConfigurationBean) async throws {
        fatalError("not used in these tests")
    }

    func __setLanguageMode(languageMode: LanguageMode) async throws {
        fatalError("not used in these tests")
    }

    func __setThemeMode(themeMode: ThemeMode) async throws {
        fatalError("not used in these tests")
    }
}
