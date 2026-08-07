@testable import JetpackMovieKMP
import Shared
import XCTest

@MainActor
final class MovieCollectTogglerTests: XCTestCase {
    func testToggle_withUncollectedMovie_insertsMovie() async {
        // Arrange
        let repository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: repository)
        let movie = makeMovieCardResult(id: 1)

        // Act
        await toggler.toggle(currentIsCollect: false, movie: movie)

        // Assert
        XCTAssertEqual(repository.insertedMovies.first?.id, movie.id)
        XCTAssertTrue(repository.deletedMovies.isEmpty)
    }

    func testToggle_withCollectedMovie_deletesMovie() async {
        // Arrange
        let repository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: repository)
        let movie = makeMovieCardResult(id: 1)

        // Act
        await toggler.toggle(currentIsCollect: true, movie: movie)

        // Assert
        XCTAssertEqual(repository.deletedMovies.first?.id, movie.id)
        XCTAssertTrue(repository.insertedMovies.isEmpty)
    }

    func testToggle_whileAlreadyUpdating_ignoresConcurrentCall() async {
        // Arrange
        let repository = FakeMovieCollectionToggling()
        var releaseFirstInsert: (() -> Void)?
        repository.beforeInsert = {
            await withCheckedContinuation { continuation in
                releaseFirstInsert = { continuation.resume() }
            }
        }
        let toggler = MovieCollectToggler(repository: repository)
        let movie = makeMovieCardResult(id: 1)

        // Act
        let firstCall = Task { await toggler.toggle(currentIsCollect: false, movie: movie) }
        while releaseFirstInsert == nil {
            await Task.yield()
        }
        await toggler.toggle(currentIsCollect: false, movie: movie)
        releaseFirstInsert?()
        await firstCall.value

        // Assert
        XCTAssertEqual(repository.insertCallCount, 1, "第一次呼叫尚未完成時，第二次呼叫應被旗標擋下")
    }

    func testToggle_whenRepositoryThrows_doesNotCrashOrPropagate() async {
        // Arrange
        let repository = ThrowingMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: repository)
        let movie = makeMovieCardResult(id: 1)

        // Act & Assert
        await toggler.toggle(currentIsCollect: false, movie: movie)
    }
}

private struct StubError: Error {}

private final class ThrowingMovieCollectionToggling: MovieCollectionToggling {
    func insertMovieCollect(movieResult: MovieCardResult) async throws {
        throw StubError()
    }

    func deleteMovieCollect(movieResult: MovieCardResult) async throws {
        throw StubError()
    }
}
