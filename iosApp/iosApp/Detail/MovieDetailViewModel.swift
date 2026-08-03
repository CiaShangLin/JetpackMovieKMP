import Foundation
import Shared

@MainActor
@Observable
final class MovieDetailViewModel {
    private let movieId: Int
    private let movieRepository: MovieRepository
    private let getMovieDetailUseCase: GetMovieDetailUseCase
    private let getMovieRecommendUseCase: GetMovieRecommendUseCase

    var uiState: MovieDetailUiState = .loading

    init(
        movieId: Int,
        movieRepository: MovieRepository,
        getMovieDetailUseCase: GetMovieDetailUseCase,
        getMovieRecommendUseCase: GetMovieRecommendUseCase
    ) {
        self.movieId = movieId
        self.movieRepository = movieRepository
        self.getMovieDetailUseCase = getMovieDetailUseCase
        self.getMovieRecommendUseCase = getMovieRecommendUseCase
    }

    func fetchMovieDetail() async {
        for await result in getMovieDetailUseCase.invoke(movieId: Int32(movieId)) {
            switch onEnum(of: result) {
            case let .success(success):
                guard let movieDetail = success.data as? MovieDetailBean else {
                    uiState = .failure("電影詳情資料格式錯誤")
                    return
                }
                uiState = .success(movieDetail)
            case let .failure(failure):
                switch onEnum(of: failure.error) {
                case let .network(network):
                    uiState = .failure(network.exception.message ?? "網路錯誤，請稍後再試")
                case .unknown:
                    uiState = .failure("發生未知錯誤")
                }
            }
            return
        }
    }
}
