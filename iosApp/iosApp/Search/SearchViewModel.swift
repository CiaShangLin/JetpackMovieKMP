import Observation
import Shared

/// `SearchViewModel` 收藏切換依賴的抽象介面；只取 `MovieRepository` 用到的兩支方法，
/// 讓測試不需要為完整的 `MovieRepository` 建立 Fake。
///
/// `MovieRepository`本身是 Kotlin interface 匯出的 Swift protocol，protocol 不能用
/// extension 補上新的繼承關係，因此改用一個轉發用的 adapter 包住真正的 repository。
protocol MovieCollectionToggling {
    func insertMovieCollect(movieResult: MovieCardResult) async throws
    func deleteMovieCollect(movieResult: MovieCardResult) async throws
}

private struct MovieRepositoryCollectionAdapter: MovieCollectionToggling {
    let repository: MovieRepository

    func insertMovieCollect(movieResult: MovieCardResult) async throws {
        try await repository.insertMovieCollect(movieResult: movieResult)
    }

    func deleteMovieCollect(movieResult: MovieCardResult) async throws {
        try await repository.deleteMovieCollect(movieResult: movieResult)
    }
}

/// `SearchViewModel` 依賴的抽象介面，讓測試可以用 Fake 取代真正的 Paging Presenter。
protocol SearchPresenting: AnyObject {
    func get(index: Int32) -> MovieCardResult?
    func snapshotItems() -> [MovieCardResult]
    func retry()
    func refresh()
    func clear()
    var onPagesUpdatedStream: AsyncStream<Void> { get }
    var loadStateStream: AsyncStream<SearchMovieListLoadStates> { get }
}

extension SearchMovieListPresenter: SearchPresenting {
    func snapshotItems() -> [MovieCardResult] {
        snapshot().compactMap { $0 as? MovieCardResult }
    }

    var onPagesUpdatedStream: AsyncStream<Void> {
        AsyncStream { continuation in
            let task = Task { [onPagesUpdatedFlow] in
                for await _ in onPagesUpdatedFlow {
                    continuation.yield(())
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    var loadStateStream: AsyncStream<SearchMovieListLoadStates> {
        AsyncStream { continuation in
            let task = Task { [loadStateFlow] in
                for await loadStates in loadStateFlow {
                    continuation.yield(loadStates)
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

@Observable
@MainActor
final class SearchViewModel {
    /// Repository 僅處理收藏寫入；搜尋分頁與載入狀態由 Kotlin presenter 負責。
    private let movieRepository: MovieCollectionToggling

    /// Search presenter 必須在使用者送出 query 後才建立，因此注入 factory 而不是單一 presenter。
    /// 測試時也能以 fake factory 取代 Koin，避免依賴完整的 shared DI graph。
    private let createPresenter: (String) -> SearchPresenting

    /// 集中持有 presenter 與收集 Flow 的 Swift Tasks。這個 holder 釋放時會同時停止
    /// Swift 端 Flow collection 與 Kotlin 端 Paging coroutine，避免任一端背景工作殘留。
    private let presenterLifetime = SearchPresenterLifetime()

    /// 保存最近一次有效提交的 query；系統搜尋欄清空不會改它，refresh 才能重建同一搜尋。
    private var submittedQuery: String?

    /// 收藏寫入是非同步的；用旗標避免連點造成 insert/delete 同時執行而得到不一致結果。
    private var isUpdatingCollection = false

    /// 首頁 loading/error 與結果畫面的狀態。
    private(set) var state: SearchUiState = .initial

    /// Kotlin ItemSnapshotList 跨語言橋接後的 Swift 畫面陣列。
    private(set) var movies: [MovieCardResult] = []

    /// append 狀態獨立保留，讓 View 能在既有結果底部顯示 loading 或 retry，而不覆蓋整頁。
    private(set) var appendLoadState: SearchMovieListLoadState?

    /// Paging 在 presenter 剛建立、真正送出請求前就會先發出一次預設的 idle load state；
    /// 只在 observeLoadStates 這條 Task 自己的序列裡追蹤「有沒有先看過 loading」，
    /// 藉此區分「還沒真的查過」跟「查過了、結果是空的」。故意不用另一條 Task
    /// （observePagesUpdated）的旗標——兩條 Task 各自收集獨立的 Flow，彼此排程順序
    /// 沒有保證，跨 Task 判斷會有極少數情況下順序顛倒、永遠判斷不出無結果的 race。
    private var hasSeenRefreshLoading = false

    init(
        movieRepository: MovieCollectionToggling = MovieRepositoryCollectionAdapter(
            repository: KoinHelper.shared.getMovieRepository()
        ),
        createPresenter: @escaping (String) -> SearchPresenting = {
            KoinHelper.shared.createSearchMovieListPresenter(query: $0)
        }
    ) {
        self.movieRepository = movieRepository
        self.createPresenter = createPresenter
    }

    func submit(query: String) {
        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        // 空白提交不清除上一次結果，也不建立空 query 的 presenter。
        guard !trimmedQuery.isEmpty else { return }

        // 新 query 的結果不能被舊 query 覆寫：先停止 Swift 兩條 Flow 的 collection，
        // 再取消 Kotlin presenter 內部收集 PagingData 的 CoroutineScope。
        cancelObservationTasks()
        presenterLifetime.presenter?.clear()

        submittedQuery = trimmedQuery
        movies = []
        appendLoadState = nil
        hasSeenRefreshLoading = false
        state = .loading

        let presenter = createPresenter(trimmedQuery)
        presenterLifetime.presenter = presenter

        // onPagesUpdatedFlow 不會重播。若資料在 Swift 開始監聽前已載入，
        // 必須先同步讀 snapshot，否則畫面可能永遠留在 loading。
        updateMovies(from: presenter)
        if !movies.isEmpty {
            state = .results
        }

        let pagesTask = Task { [weak self] in
            // Task 被 presenterLifetime 持有；weak self 可避免 ViewModel 與 Task 互相強持有。
            guard let self else { return }
            await observePagesUpdated(presenter: presenter)
        }
        let loadStatesTask = Task { [weak self] in
            guard let self else { return }
            await observeLoadStates(presenter: presenter)
        }

        // Flow 本身沒有 cancel；真正的訂閱工作是這兩個 Task。換 query 時必須記錄並取消它們。
        presenterLifetime.observationTasks = [pagesTask, loadStatesTask]
    }

    func prefetch(index: Int) {
        // 讀取接近尾端的 index 會通知 Paging 依 prefetchDistance 載入下一頁。
        _ = presenterLifetime.presenter?.get(index: Int32(index))
    }

    func retry() {
        presenterLifetime.presenter?.retry()
    }

    func refresh() {
        guard let submittedQuery, !submittedQuery.isEmpty else { return }
        // 不直接呼叫 Paging refresh：重建 presenter 可保證搜尋從第 1 頁開始。
        submit(query: submittedQuery)
    }

    func toggleMovieCollectStatus(data: MovieCardData) async {
        guard !isUpdatingCollection else { return }

        isUpdatingCollection = true
        defer { isUpdatingCollection = false }

        do {
            switch MovieCollectAction(data: data) {
            case let .delete(movie):
                try await movieRepository.deleteMovieCollect(movieResult: movie)
            case let .insert(movie):
                try await movieRepository.insertMovieCollect(movieResult: movie)
            }
        } catch {
            print("切換收藏失敗：\(error.localizedDescription)")
        }
    }

    private func observePagesUpdated(presenter: SearchPresenting) async {
        for await _ in presenter.onPagesUpdatedStream {
            // 即使取消前舊 presenter 剛好送出事件，也不得覆寫目前 query 的畫面。
            guard !Task.isCancelled, presenter === presenterLifetime.presenter
            else { return }

            updateMovies(from: presenter)
            if !movies.isEmpty {
                state = .results
            }
        }
    }

    private func observeLoadStates(presenter: SearchPresenting) async {
        for await loadStates in presenter.loadStateStream {
            guard !Task.isCancelled, presenter === presenterLifetime.presenter
            else { return }

            appendLoadState = loadStates.append
            // 已有結果時，refresh/append 的 error 只能顯示在 footer，不能取代完整結果畫面。
            guard movies.isEmpty else { continue }

            switch onEnum(of: loadStates.refresh) {
            case .loading:
                hasSeenRefreshLoading = true
                state = .loading
            case .idle:
                // presenter 剛建立時 Paging 會先發一次預設 idle；只有在這條 Task
                // 自己序列裡先看過一次 loading，才代表「查完了、結果是空的」。
                if hasSeenRefreshLoading {
                    state = .results
                }
            case let .error(error):
                state = .failure(message: error.message)
            }
        }
    }

    private func updateMovies(from presenter: SearchPresenting) {
        movies = presenter.snapshotItems()
    }

    private func cancelObservationTasks() {
        presenterLifetime.observationTasks.forEach { $0.cancel() }
        presenterLifetime.observationTasks.removeAll()
    }
}

private final class SearchPresenterLifetime {
    /// Swift Task 是實際收集 Kotlin Flow 的訂閱；Flow 本身不提供直接取消 API。
    var observationTasks: [Task<Void, Never>] = []

    /// presenter.clear() 會取消 Kotlin 端 SupervisorJob 與其 Paging stream。
    var presenter: SearchPresenting?

    deinit {
        // ViewModel 釋放時讓兩端的長生命週期工作一起結束。
        observationTasks.forEach { $0.cancel() }
        presenter?.clear()
    }
}
