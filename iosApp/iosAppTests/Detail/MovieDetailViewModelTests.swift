@testable import JetpackMovieKMP
import Shared
import XCTest

@MainActor
final class MovieDetailViewModelTests: XCTestCase {
    func testToggleMovieCollectStatus_whenNotCollected_delegatesInsertToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = makeViewModel(toggler: toggler)
        let movie = makeMovieCardResult(id: 1)

        // Act
        await viewModel.toggleMovieCollectStatus(data: movie)

        // Assert：isCollect 預設為 false，等同傳入未收藏狀態
        XCTAssertEqual(togglerRepository.insertedMovies.first?.id, 1)
        XCTAssertTrue(togglerRepository.deletedMovies.isEmpty)
    }

    func testToggleMovieCollectStatus_whenCollected_delegatesDeleteToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = makeViewModel(toggler: toggler)
        viewModel.isCollect = true
        let movie = makeMovieCardResult(id: 1)

        // Act
        await viewModel.toggleMovieCollectStatus(data: movie)

        // Assert
        XCTAssertEqual(togglerRepository.deletedMovies.first?.id, 1)
        XCTAssertTrue(togglerRepository.insertedMovies.isEmpty)
    }

    func testToggleRecommendCollectStatus_withUncollectedMovie_delegatesInsertToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = makeViewModel(toggler: toggler)
        let data = makeMovieCardData(id: 2, isCollect: false)

        // Act
        await viewModel.toggleRecommendCollectStatus(data: data)

        // Assert
        XCTAssertEqual(togglerRepository.insertedMovies.first?.id, 2)
        XCTAssertTrue(togglerRepository.deletedMovies.isEmpty)
    }

    func testToggleRecommendCollectStatus_withCollectedMovie_delegatesDeleteToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = makeViewModel(toggler: toggler)
        let data = makeMovieCardData(id: 2, isCollect: true)

        // Act
        await viewModel.toggleRecommendCollectStatus(data: data)

        // Assert
        XCTAssertEqual(togglerRepository.deletedMovies.first?.id, 2)
        XCTAssertTrue(togglerRepository.insertedMovies.isEmpty)
    }

    private func makeViewModel(toggler: MovieCollectToggler) -> MovieDetailViewModel {
        MovieDetailViewModel(
            movieId: 1,
            movieRepository: FakeMovieRepository(),
            getMovieDetailUseCase: GetMovieDetailUseCase(
                movieRepository: FakeMovieRepository(),
                ioDispatcher: FakeCoroutineDispatcher()
            ),
            getMovieRecommendUseCase: GetMovieRecommendUseCase(
                movieRepository: FakeMovieRepository(),
                ioDispatcher: FakeCoroutineDispatcher()
            ),
            toggler: toggler
        )
    }
}
