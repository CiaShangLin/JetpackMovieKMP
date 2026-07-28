import Lottie
import Shared
import SwiftUI

/// 首頁分頁的暫時內容頁。
struct HomeView: View {
    private let movieRepository: MovieRepository

    @State
    private var viewModel: HomeViewModel

    init(
        movieRepository: MovieRepository
    ) {
        self.movieRepository = movieRepository
        _viewModel = State(initialValue: HomeViewModel(movieRepository: movieRepository))
    }

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .task {
                await viewModel.loadHome()
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .loading:
            LoadingView()

        case let .success(genres):
            HomeSuccessView(
                genres: genres,
                viewModel: viewModel,
                movieRepository: movieRepository
            )

        case .failure:
            ErrorView(onRetry: {
                Task {
                    await viewModel.retry()
                }
            })
        }
    }

    struct HomeSuccessView: View {
        let genres: [MovieGenreBean.MovieGenre]
        let viewModel: HomeViewModel
        let movieRepository: MovieRepository

        @State
        private var selectedTabIndex = 0

        var body: some View {
            VStack(spacing: JMSpacing.spacing0) {
                genreTabBar
                TabView(selection: $selectedTabIndex) {
                    ForEach(genres.indices, id: \.self) { index in
                        HomeContentView(
                            movieGenre: genres[index],
                            homeViewModel: viewModel,
                            movieRepository: movieRepository
                        ).tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }

        private var genreTabBar: some View {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: JMSpacing.spacing16) {
                    ForEach(genres.indices, id: \.self) { index in
                        let genre = genres[index]
                        Button {
                            selectedTabIndex = index
                        } label: {
                            Text(genre.name)
                                .font(.subheadline)
                                .fontWeight(selectedTabIndex == index ? .semibold : .regular)
                                .foregroundStyle(
                                    selectedTabIndex == index ? Color.accentColor : Color.secondary
                                )
                        }
                        .padding(.vertical, JMSpacing.spacing8)
                    }
                }
                .padding(.horizontal, JMSpacing.spacing16)
            }
            .frame(height: JMSize.size44)
        }
    }
}
