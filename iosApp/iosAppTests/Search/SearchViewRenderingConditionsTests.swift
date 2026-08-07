import Shared
import XCTest
@testable import JetpackMovieKMP

/// 對應 `SearchView.content` 依 `viewModel.state`／`movies`／`appendLoadState`
/// 切換畫面的各種條件；SwiftUI 沒有現成的 view-inspection 工具，因此在
/// ViewModel 層驗證這些條件會不會被正確設成該有的值。
///
/// `submit()` 觀察 Flow 的工作是在背景 `Task` 裡進行的，`FakeSearchPresenter`
/// 送出事件後，ViewModel 要等那個 Task 真的被排程執行才會更新狀態；因此斷言前
/// 一律用 `waitUntil` 輪詢，而不是單次 `Task.yield()`（單次 yield 不保證背景
/// Task 已經跑到那一輪 for-await）。
@MainActor
final class SearchViewRenderingConditionsTests: XCTestCase {
    func testState_whileWaitingForFirstPagesEvent_staysLoadingEvenIfLoadStateIsIdle() async {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )

        // Act
        viewModel.submit(query: "dune")
        // Paging 在真正送出請求前就可能先發一次預設的 idle；此時還沒收到任何
        // pages-updated 事件，不該被誤判成「查完了、沒結果」。
        presenter.emitLoadStates(makeLoadStates(refresh: .idle, append: .idle))
        // 這裡刻意等一段時間，讓「誤判」有機會發生，再確認狀態真的沒被誤改。
        try? await Task.sleep(nanoseconds: 100_000_000)

        // Assert
        guard case .loading = viewModel.state else {
            return XCTFail("尚未收到 pages-updated 事件前，不該提早離開 loading")
        }
    }

    func testState_afterEmptyPagesEventFollowedByIdleRefresh_becomesResultsWithEmptyMovies() async {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )

        // Act：真實 Paging 在完成一次 refresh 前一定會先發出 loading，
        // 這裡刻意照相同順序送 loading → idle，驗證「查完了、結果是空的」判斷。
        viewModel.submit(query: "asdkjfhaksjdhf")
        presenter.items = []
        presenter.emitLoadStates(makeLoadStates(refresh: .loading, append: .idle))
        presenter.emitPagesUpdated()
        presenter.emitLoadStates(makeLoadStates(refresh: .idle, append: .idle))
        await waitUntil { isResultsState(viewModel.state) }

        // Assert：對應 SearchView `.results` + `movies.isEmpty` 的「無結果」分支
        guard case .results = viewModel.state else {
            return XCTFail("真的查完且結果為空時，應轉為 results 讓畫面顯示無結果訊息")
        }
        XCTAssertTrue(viewModel.movies.isEmpty)
    }

    func testState_afterPagesEventWithMovies_becomesResultsWithMovies() async {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )

        // Act
        viewModel.submit(query: "dune")
        presenter.items = [makeMovie(id: 1), makeMovie(id: 2)]
        presenter.emitPagesUpdated()
        await waitUntil { !viewModel.movies.isEmpty }

        // Assert：對應 SearchView `.results` + 非空 movies 的 grid 分支
        guard case .results = viewModel.state else {
            return XCTFail("收到有資料的 pages-updated 事件後應轉為 results")
        }
        XCTAssertEqual(viewModel.movies.map(\.id), [1, 2])
    }

    func testState_withRefreshErrorAndNoExistingResults_becomesFailure() async {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )

        // Act
        viewModel.submit(query: "dune")
        presenter.emitLoadStates(makeLoadStates(refresh: .error(message: "離線"), append: .idle))
        await waitUntil { isFailureState(viewModel.state) }

        // Assert：對應 SearchView 的 ErrorView 分支
        guard case .failure(let message) = viewModel.state else {
            return XCTFail("目前沒有既有結果時，refresh 失敗應整頁轉為 failure")
        }
        XCTAssertEqual(message, "離線")
    }

    func testState_withAppendErrorWhileResultsAlreadyPresent_keepsResultsAndOnlyUpdatesFooter() async {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )
        viewModel.submit(query: "dune")
        presenter.items = [makeMovie(id: 1)]
        presenter.emitPagesUpdated()
        await waitUntil { !viewModel.movies.isEmpty }

        // Act：既有結果存在時，append 失敗不該蓋掉整頁結果，只更新 footer。
        presenter.emitLoadStates(makeLoadStates(refresh: .idle, append: .error(message: "載入下一頁失敗")))
        await waitUntil { viewModel.appendLoadState != nil && isErrorLoadState(viewModel.appendLoadState) }

        // Assert
        guard case .results = viewModel.state else {
            return XCTFail("已有結果時，append 失敗不該把整頁狀態改成 failure")
        }
        XCTAssertFalse(viewModel.movies.isEmpty)
        XCTAssertTrue(isErrorLoadState(viewModel.appendLoadState), "append footer 應反映失敗狀態，讓 SearchView 顯示重試按鈕")
    }

    func testAppendLoadState_reflectsLoadingForFooterSpinner() async {
        // Arrange
        let presenter = FakeSearchPresenter()
        let viewModel = SearchViewModel(
            toggler: MovieCollectToggler(repository: FakeMovieCollectionToggling()),
            createPresenter: { _ in presenter }
        )
        viewModel.submit(query: "dune")
        presenter.items = [makeMovie(id: 1)]
        presenter.emitPagesUpdated()
        await waitUntil { !viewModel.movies.isEmpty }

        // Act：對應 SearchView appendFooter 的 `.loading` → ProgressView 分支
        presenter.emitLoadStates(makeLoadStates(refresh: .idle, append: .loading))
        await waitUntil { isLoadingLoadState(viewModel.appendLoadState) }

        // Assert
        XCTAssertTrue(isLoadingLoadState(viewModel.appendLoadState), "append 正在載入下一頁時，appendLoadState 應為 loading")
    }

    private func waitUntil(
        timeout: TimeInterval = 2,
        _ condition: () -> Bool
    ) async {
        let deadline = Date().addingTimeInterval(timeout)
        while !condition(), Date() < deadline {
            await Task.yield()
        }
    }

    private func isResultsState(_ state: SearchUiState) -> Bool {
        if case .results = state { return true }
        return false
    }

    private func isFailureState(_ state: SearchUiState) -> Bool {
        if case .failure = state { return true }
        return false
    }

    private func isLoadingLoadState(_ state: (any SearchMovieListLoadState)?) -> Bool {
        guard let state else { return false }
        if case .loading = onEnum(of: state) { return true }
        return false
    }

    private func isErrorLoadState(_ state: (any SearchMovieListLoadState)?) -> Bool {
        guard let state else { return false }
        if case .error = onEnum(of: state) { return true }
        return false
    }

    private func makeLoadStates(
        refresh: LoadStateCase,
        append: LoadStateCase
    ) -> SearchMovieListLoadStates {
        SearchMovieListLoadStates(refresh: refresh.toKotlin(), append: append.toKotlin())
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
}

/// 測試用的輕量 Swift 表示法，避免每次都要拼出完整 Kotlin 型別名稱。
private enum LoadStateCase {
    case idle
    case loading
    case error(message: String)

    func toKotlin() -> any SearchMovieListLoadState {
        switch self {
        case .idle:
            return SearchMovieListLoadStateIdle.shared
        case .loading:
            return SearchMovieListLoadStateLoading.shared
        case .error(let message):
            return SearchMovieListLoadStateError(message: message)
        }
    }
}
