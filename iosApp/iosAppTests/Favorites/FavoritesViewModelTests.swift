@testable import JetpackMovieKMP
import Shared
import XCTest

@MainActor
final class FavoritesViewModelTests: XCTestCase {
    func testToggleMovieCollectStatus_withUncollectedMovie_delegatesInsertToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = FavoritesViewModel(movieRepository: FakeMovieRepository(), toggler: toggler)
        let data = makeMovieCardData(id: 1, isCollect: false)

        // Act
        await viewModel.toggleMovieCollectStatus(data: data)

        // Assert
        XCTAssertEqual(togglerRepository.insertedMovies.first?.id, 1)
        XCTAssertTrue(togglerRepository.deletedMovies.isEmpty)
    }

    func testToggleMovieCollectStatus_withCollectedMovie_delegatesDeleteToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = FavoritesViewModel(movieRepository: FakeMovieRepository(), toggler: toggler)
        let data = makeMovieCardData(id: 1, isCollect: true)

        // Act
        await viewModel.toggleMovieCollectStatus(data: data)

        // Assert
        XCTAssertEqual(togglerRepository.deletedMovies.first?.id, 1)
        XCTAssertTrue(togglerRepository.insertedMovies.isEmpty)
    }
}
