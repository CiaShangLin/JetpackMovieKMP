import Shared
import SwiftUI

struct HomeContentView: View {
    let movieGenre: MovieGenreBean.MovieGenre

    @State
    private var viewModel: HomeContentViewModel

    init(
        movieGenre: MovieGenreBean.MovieGenre,
        homeViewModel: HomeViewModel
    ) {
        self.movieGenre = movieGenre
        _viewModel = State(
            initialValue: HomeContentViewModel(
                movieGenre: movieGenre,
                homeViewModel: homeViewModel
            )
        )
    }

    var body: some View {
        content.task {
            await viewModel.start()
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            LoadingView()
        case let .success(itemCount):
            ScrollView {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 160), spacing: 12)],
                    spacing: 12
                ) {
                    ForEach(0 ..< itemCount, id: \.self) { index in
                        if let movie = viewModel.item(at: index) {
                            MovieCardView(
                                data: movie.asMovieCardData()
                            )
                        }
                    }
                }.padding(.horizontal, 16)

                appendFooter
            }
            .refreshable {
                viewModel.refresh()
            }
        case let .failure(message):
            ErrorView(
                message: LocalizedStringKey(message),
                onRetry: {
                    viewModel.retry()
                }
            )
        }
    }

    @ViewBuilder
    private var appendFooter: some View {
        if let appendLoadState = viewModel.appendLoadState {
            switch onEnum(of: appendLoadState) {
            case .idle:
                EmptyView()
            case .loading:
                ProgressView()
                    .padding()
            case .error:
                Button("home_retry_button") {
                    viewModel.retry()
                }
                .padding()
            }
        }
    }
}
