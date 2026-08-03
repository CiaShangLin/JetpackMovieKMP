import Shared
import SwiftUI

struct FavoritesView: View {
    @State
    private var viewModel: FavoritesViewModel
    @State
    private var path = NavigationPath()

    init() {
        _viewModel = State(initialValue: FavoritesViewModel(movieRepository: KoinHelper.shared.getMovieRepository()))
    }

    var body: some View {
        NavigationStack(path: $path) {
            FavoritesContentView(
                uiState: viewModel.uiState,
                onCollectTap: { movie in
                    Task {
                        await viewModel.toggleMovieCollectStatus(data: movie)
                    }
                },
                onMovieTap: { movie in
                    path.append(Int(movie.movieCardId))
                }
            )
            .navigationDestination(for: Int.self) { movieId in
                MovieDetailView(movieId: movieId, path: $path)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task {
            await viewModel.loadFavorites()
        }
    }
}

private struct FavoritesContentView: View {
    let uiState: FavoritesUiState
    let onCollectTap: (MovieCardData) -> Void
    let onMovieTap: (MovieCardData) -> Void

    var body: some View {
        content
    }

    @ViewBuilder
    private var content: some View {
        switch uiState {
        case .empty:
            FavoritesEmptyView()
        case let .success(movies):
            favoritesGrid(movies: movies)
        }
    }

    private func favoritesGrid(movies: [MovieCardResult]) -> some View {
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
                        onMovieTap: onMovieTap,
                        onCollectTap: onCollectTap
                    )
                }
            }
            .padding(.horizontal, JMSpacing.spacing16)
            .padding(.vertical, JMSpacing.spacing12)
        }
    }

    private struct FavoritesEmptyView: View {
        var body: some View {
            VStack(spacing: JMSpacing.spacing12) {
                Image(systemName: "heart.slash")
                    .font(.system(size: JMSize.size44, weight: .light))
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(.secondary)
                Text("favorites_empty")
                    .font(.headline)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

#Preview("Empty") {
    FavoritesContentView(
        uiState: .empty,
        onCollectTap: { _ in },
        onMovieTap: { _ in }
    )
}

#Preview("With favorites") {
    FavoritesContentView(
        uiState: .success(data: [
            MovieCardResult(
                adult: false,
                backdropPath: "",
                genreIds: [],
                id: 1,
                originalLanguage: "en",
                originalTitle: "Preview movie",
                overview: "",
                popularity: 0,
                posterPath: "",
                releaseDate: "2026-01-01",
                title: "Preview movie",
                video: false,
                voteAverage: 8,
                voteCount: 1,
                isCollect: true,
                timestamp: 0
            )
        ]),
        onCollectTap: { _ in },
        onMovieTap: { _ in }
    )
}
