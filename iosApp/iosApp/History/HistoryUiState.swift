import Shared

enum HistoryUiState {
    case empty
    case success(data: [MovieCardResult])

    static func make(movies: [MovieCardResult]) -> HistoryUiState {
        movies.isEmpty ? .empty : .success(data: movies)
    }
}
