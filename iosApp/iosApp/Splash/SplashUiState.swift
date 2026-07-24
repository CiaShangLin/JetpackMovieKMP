import Shared

enum SplashUiState {
    case loading
    case success(data: ConfigurationBean)
    case failure(debugMessage: String)
}
