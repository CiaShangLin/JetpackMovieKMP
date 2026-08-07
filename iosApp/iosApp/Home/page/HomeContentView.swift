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
                if let appendLoadState = viewModel.appendLoadState {
                    PagingAppendFooterView(
                        loadState: appendLoadState.asAppendLoadState(),
                        onRetry: { viewModel.retry() }
                    )
                }
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
}

extension HomeMovieListLoadState {
    /// 將 shared 層的 `HomeMovieListLoadState` 轉換為共用 Footer 元件使用的 `AppendLoadState`。
    func asAppendLoadState() -> AppendLoadState {
        switch onEnum(of: self) {
        case .idle:
            .idle
        case .loading:
            .loading
        case let .error(error):
            .error(message: error.message)
        }
    }
}
