@testable import JetpackMovieKMP
import Shared

final class FakeMovieCollectionToggling: MovieCollectionToggling {
    private(set) var insertedMovies: [MovieCardResult] = []
    private(set) var deletedMovies: [MovieCardResult] = []
    private(set) var insertCallCount = 0

    /// 測試併發 guard 用：在真正寫入前先卡住，讓測試能製造「第一次呼叫還沒完成」的時間點。
    var beforeInsert: (() async -> Void)?

    func insertMovieCollect(movieResult: MovieCardResult) async throws {
        insertCallCount += 1
        await beforeInsert?()
        insertedMovies.append(movieResult)
    }

    func deleteMovieCollect(movieResult: MovieCardResult) async throws {
        deletedMovies.append(movieResult)
    }
}
