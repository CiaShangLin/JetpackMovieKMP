import Observation
import Shared

enum MovieCollectAction {
    case insert(MovieCardResult)
    case delete(MovieCardResult)

    init(data: MovieCardData) {
        let movie = data.asMovieCardResult()
        self = data.movieCardIsCollect ? .delete(movie) : .insert(movie)
    }
}

@MainActor
@Observable
final class FavoritesViewModel {
    private let movieRepository: MovieRepository
    private var isUpdatingCollection = false

    private(set) var uiState: FavoritesUiState = .empty

    init(movieRepository: MovieRepository) {
        self.movieRepository = movieRepository
    }

    func loadFavorites() async {
        for await movies in movieRepository.getAllMovieCollect() {
            guard !Task.isCancelled else { return }

            uiState = FavoritesUiState.make(movies: movies)
        }
    }

    func toggleMovieCollectStatus(data: MovieCardData) async {
        guard !isUpdatingCollection else { return }

        isUpdatingCollection = true
        defer { isUpdatingCollection = false }

        do {
            switch MovieCollectAction(data: data) {
            case let .delete(movie):
                try await movieRepository.deleteMovieCollect(movieResult: movie)
            case let .insert(movie):
                try await movieRepository.insertMovieCollect(movieResult: movie)
            }
        } catch {
            print("切換收藏失敗：\(error.localizedDescription)")
        }
    }
}
