import Shared

enum SearchUiState {
    case initial
    case loading
    case results
    case failure(message: String)
}
