import Shared

enum FavoritesUiState {
    case empty
    case success(data: [MovieCardResult])

    static func make(movies: [MovieCardResult]) -> FavoritesUiState {
        movies.isEmpty ? .empty : .success(data: movies)
    }
}
