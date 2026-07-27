import Lottie
import Shared
import SwiftUI

/// 首頁分頁的暫時內容頁。
struct HomeView: View {
    @State
    private var viewModel = HomeViewModel(
        movieRepository: KoinHelper.shared.getMovieRepository()
    )

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
            HomeSuccessView(genres: genres, viewModel: viewModel)

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

        @State
        private var selectedTabIndex = 0

        var body: some View {
            VStack(spacing: 0) {
                genreTabBar
                TabView(selection: $selectedTabIndex) {
                    ForEach(Array(genres.enumerated()), id: \.element.id) { index, _ in
                        HomeContentView(
                            movieGenre: genres[index],
                            homeViewModel: viewModel
                        ).tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }

        private var genreTabBar: some View {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(Array(genres.enumerated()), id: \.element.id) { index, genre in
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
                        .padding(.vertical, 8)
                    }
                }
                .padding(.horizontal, 16)
            }
            .frame(height: 44)
        }
    }
}
