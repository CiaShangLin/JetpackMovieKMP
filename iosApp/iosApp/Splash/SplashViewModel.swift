import Observation
import Shared

@Observable
@MainActor
final class SplashViewModel {
    private let getConfigurationUseCase: GetConfigurationUseCase

    private(set) var uiState: SplashUiState = .loading

    var isConfigurationLoaded: Bool {
        if case .success = uiState {
            return true
        }

        return false
    }

    init(getConfigurationUseCase: GetConfigurationUseCase) {
        self.getConfigurationUseCase = getConfigurationUseCase
    }

    func loadConfiguration() async {
        uiState = .loading

        for await result in getConfigurationUseCase.invoke() {
            switch onEnum(of: result) {
            case let .success(success):
                uiState = .success(data: success.data as! ConfigurationBean)
                return
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    uiState = .failure(
                        debugMessage: network.exception.message ?? "網路錯誤，請稍後再試"
                    )
                case .unknown:
                    uiState = .failure(debugMessage: "發生未知錯誤")
                }
                return
            }
        }

        uiState = .failure(
            debugMessage: "Configuration load flow completed without emitting a state"
        )
    }

    func retry() async {
        await loadConfiguration()
    }
}
