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

    func start() async {
        // getMovieDetailUseCase.invoke(movieId: movieId)
        // getMovieRecommendUseCase.invoke(movieId: movieId)
    }
}
