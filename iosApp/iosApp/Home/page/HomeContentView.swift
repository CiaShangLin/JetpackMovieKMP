import Shared
import SwiftUI

struct HomeContentView: View {
    let movieGenre: MovieGenreBean.MovieGenre

    @State
    private var viewModel: HomeContentViewModel

    init(movieGenre: MovieGenreBean.MovieGenre, homeViewModel: HomeViewModel) {
        self.movieGenre = movieGenre
        self.viewModel = HomeContentViewModel(
            movieGenre: movieGenre,
            movieRepository: KoinHelper.shared.getMovieRepository(),
            homeViewModel: homeViewModel
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
        case .success(let itemCount):
            ScrollView {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 160), spacing: 12)],
                    spacing: 12
                ) {
                    ForEach(0..<itemCount, id: \.self) { index in
                        if let movie = viewModel.item(at: index) {
                            MovieCardView(data: movie.asMovieCardData())
                        }
                    }
                }.padding(.horizontal, 16)
                
                appendFooter
            }
            .refreshable {
                viewModel.refresh()
            }
        case .failure(let message):
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
