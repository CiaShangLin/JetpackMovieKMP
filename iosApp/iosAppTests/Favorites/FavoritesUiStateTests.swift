import Shared
import XCTest
@testable import JetpackMovieKMP

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
}
