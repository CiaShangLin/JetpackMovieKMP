import Shared

/// `MovieRepository` 是完整的 Kotlin interface；這個 Fake 只提供編譯所需的最小實作，
/// 讓測試能建構持有 `MovieRepository` 的 ViewModel，測試本身不驗證這裡的行為。
///
/// 只需實作 `async throws` 版本：Swift 會自動為其產生對應的 completion-handler
/// ObjC 進入點，滿足協定要求的兩個 selector；若同時手動實作兩者會導致 ObjC selector 衝突。
final class FakeMovieRepository: MovieRepository {
    func getConfiguration() -> SkieSwiftFlow<Any> {
        fatalError("not used in these tests")
    }

    func getMovieGenres() -> SkieSwiftFlow<any AppResult> {
        fatalError("not used in these tests")
    }

    func getMovieListPager(withGenres: String) -> SkieSwiftFlow<Paging_commonPagingData<MovieCardResult>> {
        fatalError("not used in these tests")
    }

    func getMovieSearchPager(query: String) -> SkieSwiftFlow<Paging_commonPagingData<MovieCardResult>> {
        fatalError("not used in these tests")
    }

    func getMovieDetail(id: Int32) -> SkieSwiftFlow<Any> {
        fatalError("not used in these tests")
    }

    func getMovieRecommendations(id: Int32) -> SkieSwiftFlow<Any> {
        fatalError("not used in these tests")
    }

    func getMovieActor(id: Int32) -> SkieSwiftFlow<any AppResult> {
        fatalError("not used in these tests")
    }

    func __insertMovieCollect(movieResult: MovieCardResult) async throws {
        fatalError("not used in these tests")
    }

    func __deleteMovieCollect(movieResult: MovieCardResult) async throws {
        fatalError("not used in these tests")
    }

    func getCollectedMovieIds() -> SkieSwiftFlow<[KotlinInt]> {
        fatalError("not used in these tests")
    }

    func getAllMovieCollect() -> SkieSwiftFlow<[MovieCardResult]> {
        fatalError("not used in these tests")
    }

    func getMovieCollectEntityById(id: Int32) -> SkieSwiftOptionalFlow<MovieCardResult> {
        fatalError("not used in these tests")
    }

    func getAllMovieHistory() -> SkieSwiftFlow<[MovieCardResult]> {
        fatalError("not used in these tests")
    }

    func __insertMovieHistory(movieResult: MovieCardResult) async throws {
        fatalError("not used in these tests")
    }

    func __deleteMovieHistory(movieResult: MovieCardResult) async throws {
        fatalError("not used in these tests")
    }

    func __deleteAllMovieHistory() async throws -> KotlinBoolean {
        fatalError("not used in these tests")
    }
}
