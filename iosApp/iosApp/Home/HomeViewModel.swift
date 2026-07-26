import Observation
import Shared

@Observable
@MainActor
final class HomeViewModel {
    private let movieRepository: MovieRepository
    private(set) var uiState: HomeUiState = .loading

    init(movieRepository: MovieRepository) {
        self.movieRepository = movieRepository
    }

    func loadHome() async {
        uiState = HomeUiState.loading
        for await result in movieRepository.getMovieGenres() {
            switch onEnum(of: result) {
            case let .success(success):
                let genres = (success.data as! MovieGenreBean).genres
                uiState = .success(genres: genres)
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    uiState = .failure(debugMessage: network.exception.message ?? "網路錯誤，請稍後再試")
                case .unknown:
                    uiState = .failure(debugMessage: "發生未知錯誤")
                }
            }
            return
        }
    }

    func retry() async {
        await loadHome()
    }
}
