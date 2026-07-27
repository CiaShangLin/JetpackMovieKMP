import Shared

enum HomeContentUiState {
    case loading
    case success(itemCount: Int)
    case failure(message: String)
}
