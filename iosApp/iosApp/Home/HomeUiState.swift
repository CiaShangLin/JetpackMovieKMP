import Shared

enum HomeUiState {
    case loading
    case success(genres: [MovieGenreBean.MovieGenre])
    case failure(debugMessage: String)
}
