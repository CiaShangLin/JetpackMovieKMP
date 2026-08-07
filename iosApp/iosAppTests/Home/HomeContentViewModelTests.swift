@testable import JetpackMovieKMP
import Shared
import XCTest

@MainActor
final class HomeContentViewModelTests: XCTestCase {
    func testToggleMovieCollectStatus_withUncollectedMovie_delegatesInsertToToggler() async {
        // Arrange
        let togglerRepository = FakeMovieCollectionToggling()
        let toggler = MovieCollectToggler(repository: togglerRepository)
        let viewModel = makeViewModel(toggler: toggler)
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
        let viewModel = makeViewModel(toggler: toggler)
        let data = makeMovieCardData(id: 1, isCollect: true)

        // Act
        await viewModel.toggleMovieCollectStatus(data: data)

        // Assert
        XCTAssertEqual(togglerRepository.deletedMovies.first?.id, 1)
        XCTAssertTrue(togglerRepository.insertedMovies.isEmpty)
    }

    private func makeViewModel(toggler: MovieCollectToggler) -> HomeContentViewModel {
        HomeContentViewModel(
            movieGenre: MovieGenreBean.MovieGenre(id: 1, name: "Action"),
            homeViewModel: HomeViewModel(
                movieRepository: FakeMovieRepository(),
                userDataRepository: FakeUserDataRepository()
            ),
            toggler: toggler
        )
    }
}
