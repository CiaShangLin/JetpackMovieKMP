import Observation
import Shared

@Observable
@MainActor
final class SettingViewModel {
    private let userDataRepository: UserDataRepository

    private(set) var userData: UserData = .companion.getDefault()

    init(userDataRepository: UserDataRepository) {
        self.userDataRepository = userDataRepository
    }

    func observeUserData() async {
        for await userData in userDataRepository.userData {
            guard !Task.isCancelled else {
                return
            }
            self.userData = userData
        }
    }

    func setThemeMode(_ mode: ThemeMode) async {
        do {
            try await userDataRepository.setThemeMode(themeMode: mode)
        } catch {
            print("切換主題失敗:\(error.localizedDescription)")
        }
    }

    func setLanguageMode(_ mode: LanguageMode) async {
        do {
            try await userDataRepository.setLanguageMode(languageMode: mode)
        } catch {
            print("切換語言失敗:\(error.localizedDescription)")
        }
    }
}
