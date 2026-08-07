@testable import JetpackMovieKMP
import Shared
import XCTest

final class FavoritesUiStateTests: XCTestCase {
    func testMake_withEmptyMovies_returnsEmptyState() {
        // Arrange
        let movies: [MovieCardResult] = []

        // Act
        let state = FavoritesUiState.make(movies: movies)

        // Assert
        guard case .empty = state else {
            return XCTFail("空收藏清單應產生 empty state")
        }
    }

    func testMake_withMovies_returnsSuccessState() {
        // Arrange
        let movie = MovieCardResult(
            adult: false,
            backdropPath: "",
            genreIds: [],
            id: 1,
            originalLanguage: "en",
            originalTitle: "Original title",
            overview: "Overview",
            popularity: 1,
            posterPath: "",
            releaseDate: "2026-01-01",
            title: "Favorite movie",
            video: false,
            voteAverage: 8,
            voteCount: 1,
            isCollect: true,
            timestamp: 0
        )

        // Act
        let state = FavoritesUiState.make(movies: [movie])

        // Assert
        guard case let .success(data) = state else {
            return XCTFail("有收藏資料時應產生 success state")
        }
        XCTAssertEqual(data.count, 1)
        XCTAssertEqual(data.first?.id, movie.id)
    }

    func testMake_preservesMoviesInSharedRepositoryOrder() {
        // Arrange
        let newestMovie = makeMovie(id: 2, title: "Newest")
        let oldestMovie = makeMovie(id: 1, title: "Oldest")

        // Act
        let state = FavoritesUiState.make(movies: [newestMovie, oldestMovie])

        // Assert
        guard case let .success(data) = state else {
            return XCTFail("有收藏資料時應產生 success state")
        }
        XCTAssertEqual(data.map(\.id), [newestMovie.id, oldestMovie.id])
    }

    private func makeMovie(id: Int32, title: String) -> MovieCardResult {
        MovieCardResult(
            adult: false,
            backdropPath: "",
            genreIds: [],
            id: id,
            originalLanguage: "en",
            originalTitle: title,
            overview: "",
            popularity: 0,
            posterPath: "",
            releaseDate: "2026-01-01",
            title: title,
            video: false,
            voteAverage: 0,
            voteCount: 0,
            isCollect: true,
            timestamp: 0
        )
    }
}
