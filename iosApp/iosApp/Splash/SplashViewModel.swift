import Observation
import Shared

@Observable
@MainActor
final class SplashViewModel {
    private let configurationLoader: IosConfigurationLoader

    private(set) var uiState: SplashUiState = .loading

    var isConfigurationLoaded: Bool {
        if case .success = uiState {
            return true
        }

        return false
    }

    init(configurationLoader: IosConfigurationLoader) {
        self.configurationLoader = configurationLoader
    }

    func loadConfiguration() async {
        uiState = .loading
        for await state in configurationLoader.invoke() {
            if let success = state as? IosConfigurationLoadStateSuccess {
                uiState = .success(data: success.data)
                return
            }

            if let failure = state as? IosConfigurationLoadStateFailure {
                uiState = .failure(error: failure.message)
                return
            }

            uiState = .failure(error: "載入失敗，請稍後再試")
            return
        }

        uiState = .failure(error: "載入失敗，請稍後再試")
    }

    func retry() async {
        await loadConfiguration()
    }
}
