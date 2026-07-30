import Shared
@testable import JetpackMovieKMP

final class FakeSearchPresenter: SearchPresenting {
    private(set) var retryCallCount = 0
    private(set) var refreshCallCount = 0
    private(set) var clearCallCount = 0
    private(set) var requestedIndices: [Int32] = []

    var items: [MovieCardResult] = []

    let onPagesUpdatedStream: AsyncStream<Void>
    let loadStateStream: AsyncStream<SearchMovieListLoadStates>

    private let pagesContinuation: AsyncStream<Void>.Continuation
    private let loadStateContinuation: AsyncStream<SearchMovieListLoadStates>.Continuation

    init() {
        var pagesContinuation: AsyncStream<Void>.Continuation!
        onPagesUpdatedStream = AsyncStream { pagesContinuation = $0 }
        self.pagesContinuation = pagesContinuation

        var loadStateContinuation: AsyncStream<SearchMovieListLoadStates>.Continuation!
        loadStateStream = AsyncStream { loadStateContinuation = $0 }
        self.loadStateContinuation = loadStateContinuation
    }

    func get(index: Int32) -> MovieCardResult? {
        requestedIndices.append(index)
        return items.indices.contains(Int(index)) ? items[Int(index)] : nil
    }

    func snapshotItems() -> [MovieCardResult] {
        items
    }

    func retry() {
        retryCallCount += 1
    }

    func refresh() {
        refreshCallCount += 1
    }

    func clear() {
        clearCallCount += 1
    }

    func emitPagesUpdated() {
        pagesContinuation.yield(())
    }

    func emitLoadStates(_ states: SearchMovieListLoadStates) {
        loadStateContinuation.yield(states)
    }
}
