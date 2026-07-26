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
            HomeSuccessView(genres: genres)

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

        @State
        private var selectedTabIndex = 0

        var body: some View {
            VStack(spacing: 0) {
                genreTabBar

                // TODO: tasks.md 9.3 完成後（GetHomeMovieListSnapshotUseCase 接上真實電影清單），
                // 在這裡用 genrePage(for:).tag(index) 恢復每個分類的電影卡片渲染。
                TabView(selection: $selectedTabIndex) {
                    ForEach(Array(genres.enumerated()), id: \.element.id) { index, _ in
                        Color.clear
                            .tag(index)
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
