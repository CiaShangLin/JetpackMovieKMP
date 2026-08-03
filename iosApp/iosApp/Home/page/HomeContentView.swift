import Shared
import SwiftUI

struct HomeContentView: View {
    let movieGenre: MovieGenreBean.MovieGenre

    @State
    private var viewModel: HomeContentViewModel

    @Binding
    private var path: NavigationPath

    init(
        movieGenre: MovieGenreBean.MovieGenre,
        homeViewModel: HomeViewModel,
        path: Binding<NavigationPath>
    ) {
        self.movieGenre = movieGenre
        _viewModel = State(initialValue: HomeContentViewModel(
            movieRepository: KoinHelper.shared.getMovieRepository(),
            movieGenre: movieGenre,
            homeViewModel: homeViewModel
        ))
        _path = path
    }

    var body: some View {
        content
            .task {
                await viewModel.start()
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            LoadingView()
        case .success:
            ScrollView {
                LazyVGrid(
                    columns: [
                        GridItem(
                            .adaptive(minimum: JMSize.movieGridMinWidth),
                            spacing: JMSpacing.spacing12
                        )
                    ],
                    spacing: JMSpacing.spacing12
                ) {
                    ForEach(viewModel.movies.indices, id: \.self) { index in
                        let movie = viewModel.movies[index]
                        MovieCardView(
                            data: movie.asMovieCardData(),
                            onMovieTap: { movie in
                                path.append(Int(movie.movieCardId))
                            },
                            onCollectTap: { movie in
                                Task {
                                    await viewModel.toggleMovieCollectStatus(data: movie)
                                }
                            }
                        )
                        .onAppear {
                            viewModel.prefetch(index: index)
                        }
                    }
                }.padding(.horizontal, JMSpacing.spacing16)
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
