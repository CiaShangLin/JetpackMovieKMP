import Shared
import SwiftUI

/// 顯示觀看歷史、收藏操作與清空功能的分頁。
struct HistoryView: View {
    @State
    private var viewModel: HistoryViewModel
    @State
    private var path = NavigationPath()

    init() {
        _viewModel = State(
            initialValue: HistoryViewModel(
                movieRepository: KoinHelper.shared.getMovieRepository(),
                getHistoryMovieListUseCase: KoinHelper.shared.getHistoryMovieListUseCase()
            )
        )
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: JMSpacing.spacing0) {
                header
                Divider()
                    .overlay(Color.secondary)
                    .padding(.horizontal, JMSpacing.spacing16)
                content
            }
            .navigationDestination(for: Int.self) { movieId in
                MovieDetailView(movieId: movieId, path: $path)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .task {
            await viewModel.loadHistory()
        }
    }

    private var header: some View {
        HStack {
            Text("history_title")
                .font(.title)
            Spacer()
            if case .success = viewModel.uiState {
                Button {
                    Task {
                        await viewModel.clearHistory()
                    }
                } label: {
                    Image(systemName: "trash.fill")
                        .font(.system(size: JMSize.size24, weight: .light))
                        .symbolRenderingMode(.hierarchical)
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel(Text("history_clear"))
                .buttonStyle(.plain)
            }
        }
        .padding(JMSpacing.spacing16)
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .empty:
            VStack(spacing: JMSpacing.spacing12) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: JMSize.size44, weight: .light))
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(.secondary)
                Text("history_empty")
                    .font(.headline)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case let .success(data):
            historyGrid(movies: data)
        }
    }

    private func historyGrid(movies: [MovieCardResult]) -> some View {
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
                ForEach(movies, id: \.id) { movie in
                    MovieCardView(
                        data: movie.asMovieCardData(),
                        onMovieTap: { movie in
                            path.append(Int(movie.movieCardId))
                        },
                        onCollectTap: { movie in
                            Task {
                                await viewModel.toggleMovieCollect(data: movie)
                            }
                        }
                    )
                }
            }
            .padding(.horizontal, JMSpacing.spacing16)
            .padding(.vertical, JMSpacing.spacing12)
        }
    }
}
