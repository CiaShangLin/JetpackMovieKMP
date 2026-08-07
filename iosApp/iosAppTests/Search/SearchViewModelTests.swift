@testable import JetpackMovieKMP
import Shared
import XCTest

@MainActor
final class SearchViewModelTests: XCTestCase {
    func testSubmit_withBlankQuery_doesNotCreatePresenterAndStaysInitial() {
        // Arrange
        var presenterCreated = false
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in
                presenterCreated = true
                return FakeSearchPresenter()
            }
        )

        // Act
        viewModel.submit(query: "   ")

        // Assert
        guard case .initial = viewModel.state else {
            return XCTFail("空白 query 應維持 initial state")
        }
        XCTAssertFalse(presenterCreated)
    }

    func testSubmit_withDifferentQuery_clearsPreviousPresenterAndCreatesNew() {
        // Arrange
        var presenters: [FakeSearchPresenter] = []
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in
                let presenter = FakeSearchPresenter()
                presenters.append(presenter)
                return presenter
            }
        )

        // Act
        viewModel.submit(query: "dune")
        viewModel.submit(query: "matrix")

        // Assert
        XCTAssertEqual(presenters.count, 2)
        XCTAssertEqual(presenters[0].clearCallCount, 1, "換 query 必須清理前一個 presenter")
        XCTAssertEqual(presenters[1].clearCallCount, 0)
    }

    func testSubmit_withPresenterHavingSnapshotAlready_backfillsResultsSynchronously() {
        // Arrange
        let presenter = FakeSearchPresenter()
        presenter.items = [makeMovie(id: 1)]
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )

        // Act
        viewModel.submit(query: "dune")

        // Assert
        guard case .results = viewModel.state else {
            return XCTFail("presenter 已有 snapshot 時應立即轉為 results，不必等 pages-updated flow")
        }
        XCTAssertEqual(viewModel.movies.map(\.id), [1])
    }

    func testRetry_delegatesToCurrentPresenter() {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )
        viewModel.submit(query: "dune")

        // Act
        viewModel.retry()

        // Assert
        XCTAssertEqual(presenter.retryCallCount, 1)
    }

    func testRefresh_withSubmittedQuery_recreatesPresenterInsteadOfCallingRefresh() {
        // Arrange
        var presenters: [FakeSearchPresenter] = []
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in
                let presenter = FakeSearchPresenter()
                presenters.append(presenter)
                return presenter
            }
        )
        viewModel.submit(query: "dune")

        // Act
        viewModel.refresh()

        // Assert
        XCTAssertEqual(presenters.count, 2, "refresh 必須從第 1 頁重建 presenter，而不是呼叫既有 presenter 的 refresh()")
        XCTAssertEqual(presenters[0].clearCallCount, 1)
        XCTAssertEqual(presenters[0].refreshCallCount, 0)
    }

    func testRefresh_withoutSubmittedQuery_doesNothing() {
        // Arrange
        var presenters: [FakeSearchPresenter] = []
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in
                let presenter = FakeSearchPresenter()
                presenters.append(presenter)
                return presenter
            }
        )

        // Act
        viewModel.refresh()

        // Assert
        XCTAssertTrue(presenters.isEmpty, "從未提交過 query 時 refresh 不該建立 presenter")
    }

    func testPrefetch_delegatesGetToCurrentPresenter() {
        // Arrange
        let presenter = FakeSearchPresenter()
        presenter.items = [makeMovie(id: 1)]
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )
        viewModel.submit(query: "dune")

        // Act
        viewModel.prefetch(index: 0)

        // Assert
        XCTAssertEqual(presenter.requestedIndices, [0])
    }

    func testToggleMovieCollectStatus_withUncollectedMovie_insertsMovie() async {
        // Arrange
        let repository = FakeMovieCollectionToggling()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: repository),
            createPresenter: { _ in FakeSearchPresenter() }
        )
        let data = makeMovieCardData(isCollect: false)

        // Act
        await viewModel.toggleMovieCollectStatus(data: data)

        // Assert
        XCTAssertEqual(repository.insertedMovies.first?.id, data.movieCardId)
        XCTAssertTrue(repository.deletedMovies.isEmpty)
    }

    func testToggleMovieCollectStatus_withCollectedMovie_deletesMovie() async {
        // Arrange
        let repository = FakeMovieCollectionToggling()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: repository),
            createPresenter: { _ in FakeSearchPresenter() }
        )
        let data = makeMovieCardData(isCollect: true)

        // Act
        await viewModel.toggleMovieCollectStatus(data: data)

        // Assert
        XCTAssertEqual(repository.deletedMovies.first?.id, data.movieCardId)
        XCTAssertTrue(repository.insertedMovies.isEmpty)
    }

    func testToggleMovieCollectStatus_whileAlreadyUpdating_ignoresConcurrentCall() async {
        // Arrange
        let repository = FakeMovieCollectionToggling()
        var releaseFirstInsert: (() -> Void)?
        repository.beforeInsert = {
            await withCheckedContinuation { continuation in
                releaseFirstInsert = { continuation.resume() }
            }
        }
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: repository),
            createPresenter: { _ in FakeSearchPresenter() }
        )
        let data = makeMovieCardData(isCollect: false)

        // Act
        let firstCall = Task { await viewModel.toggleMovieCollectStatus(data: data) }
        while releaseFirstInsert == nil {
            await Task.yield()
        }
        await viewModel.toggleMovieCollectStatus(data: data)
        releaseFirstInsert?()
        await firstCall.value

        // Assert
        XCTAssertEqual(repository.insertCallCount, 1, "第一次呼叫尚未完成時，第二次呼叫應被旗標擋下")
    }

    private func makeMovie(id: Int32) -> MovieCardResult {
        MovieCardResult(
            adult: false,
            backdropPath: "",
            genreIds: [],
            id: id,
            originalLanguage: "en",
            originalTitle: "Movie \(id)",
            overview: "",
            popularity: 0,
            posterPath: "",
            releaseDate: "2026-01-01",
            title: "Movie \(id)",
            video: false,
            voteAverage: 0,
            voteCount: 0,
            isCollect: false,
            timestamp: 0
        )
    }

    private func makeMovieCardData(isCollect: Bool) -> MovieCardData {
        MovieCardData(
            movieCardId: 42,
            movieCardTitle: "Search result movie",
            movieCardPosterPath: "",
            movieCardReleaseDate: "2026-01-01",
            movieCardVoteAverage: 8,
            movieCardIsCollect: isCollect,
            movieCardTimestamp: 0
        )
    }
}
