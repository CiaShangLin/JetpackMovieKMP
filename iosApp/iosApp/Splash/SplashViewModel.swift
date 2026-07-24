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
        de
        for await state in configurationLoader.invoke() {
            if let success = state as? IosConfigurationLoadStateSuccess {
                uiState = .success(data: success.data)
                return
            }

            if let failure = state as? IosConfigurationLoadStateFailure {
                uiState = .failure(debugMessage: failure.message)
                return
            }

            uiState = .failure(debugMessage: "Unknown IosConfigurationLoadState variant")
            return
        }

        uiState = .failure(debugMessage: "Configuration load flow completed without emitting a state")
    }

    func retry() async {
        await loadConfiguration()
    }
}
