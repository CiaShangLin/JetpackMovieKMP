import Foundation
import Shared
import SwiftUI

struct MovieDetailView: View {
    private let movieId: Int

    @State
    private var viewModel: MovieDetailViewModel

    init(movieId: Int) {
        self.movieId = movieId
        _viewModel = State(initialValue: MovieDetailViewModel(
            movieId: movieId,
            movieRepository: KoinHelper.shared.getMovieRepository(),
            getMovieDetailUseCase: KoinHelper.shared.getMovieDetailUseCase(),
            getMovieRecommendUseCase: KoinHelper.shared.getMovieRecommendUseCase()
        ))
    }

    var body: some View {
        content
            .task {
                await viewModel.fetchMovieDetail()
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .loading:
            LoadingView()
        case let .success(data):
            EmptyView()
        case let .failure(errorMessage):
            ErrorView(
                onRetry: {}
            )
        }
    }
}
