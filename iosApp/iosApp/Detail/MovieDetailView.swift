import Foundation
import Shared
import SwiftUI

struct MovieDetailView: View {

    private let movieId: Int

    @State
    private var viewModel: MovieDetailViewModel

    init(movieId: Int) {
        self.movieId = movieId
        self.viewModel = MovieDetailViewModel(
            movieId: movieId,
            movieRepository: KoinHelper.shared.getMovieRepository(),
            getMovieDetailUseCase: KoinHelper.shared.getMovieDetailUseCase(),
            getMovieRecommendUseCase: KoinHelper.shared.getMovieRecommendUseCase()
        )
    }

    var body: some View {
        content
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .loading:
            LoadingView()
        case .success:
            EmptyView()
        case .failure(let errorMessage):
            ErrorView(
                onRetry: {

                }
            )
        }
    }
}
