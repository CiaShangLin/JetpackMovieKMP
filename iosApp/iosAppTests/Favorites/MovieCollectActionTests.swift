import Shared
import XCTest
@testable import JetpackMovieKMP

final class MovieCollectActionTests: XCTestCase {
    func testInit_withUncollectedMovie_createsInsertAction() {
        // Arrange
        let data = makeMovieCardData(isCollect: false)

        // Act
        let action = MovieCollectAction(data: data)

        // Assert
        guard case let .insert(movie) = action else {
            return XCTFail("未收藏電影應建立 insert action")
        }
        XCTAssertEqual(movie.id, data.movieCardId)
    }

    func testInit_withCollectedMovie_createsDeleteAction() {
        // Arrange
        let data = makeMovieCardData(isCollect: true)

        // Act
        let action = MovieCollectAction(data: data)

        // Assert
        guard case let .delete(movie) = action else {
            return XCTFail("已收藏電影應建立 delete action")
        }
        XCTAssertEqual(movie.id, data.movieCardId)
    }

    private func makeMovieCardData(isCollect: Bool) -> MovieCardData {
        MovieCardData(
            movieCardId: 42,
            movieCardTitle: "Favorite movie",
            movieCardPosterPath: "",
            movieCardReleaseDate: "2026-01-01",
            movieCardVoteAverage: 8,
            movieCardIsCollect: isCollect,
            movieCardTimestamp: 0
        )
    }
}
