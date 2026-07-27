import Kingfisher
import Shared
import XCTest
@testable import JetpackMovieKMP

final class MovieImageURLResolverTests: XCTestCase {
    func testResolve_withRelativePath_appendsBaseHostAndOriginalSize() {
        // Arrange
        let resolver = MovieImageURLResolver(
            baseHostProvider: StubImageBaseHostProvider(baseHost: "https://image.tmdb.org/t/p/")
        )

        // Act
        let url = resolver.resolve(path: "/poster.jpg")

        // Assert
        XCTAssertEqual(url?.absoluteString, "https://image.tmdb.org/t/p/original/poster.jpg")
    }

    func testResolve_withHttpsURL_keepsOriginalURL() {
        // Arrange
        let resolver = MovieImageURLResolver(
            baseHostProvider: StubImageBaseHostProvider(baseHost: "https://image.tmdb.org/t/p/")
        )

        // Act
        let url = resolver.resolve(path: "https://example.com/poster.jpg")

        // Assert
        XCTAssertEqual(url?.absoluteString, "https://example.com/poster.jpg")
    }

    func testResolve_withHttpURL_keepsOriginalURL() {
        // Arrange
        let resolver = MovieImageURLResolver(
            baseHostProvider: StubImageBaseHostProvider(baseHost: "https://image.tmdb.org/t/p/")
        )

        // Act
        let url = resolver.resolve(path: "http://example.com/poster.jpg")

        // Assert
        XCTAssertEqual(url?.absoluteString, "http://example.com/poster.jpg")
    }

    func testResolve_withEmptyBaseHostAndRelativePathReturnsNil() {
        // Arrange
        let resolver = MovieImageURLResolver(baseHostProvider: StubImageBaseHostProvider(baseHost: ""))

        // Act
        let url = resolver.resolve(path: "/poster.jpg")

        // Assert
        XCTAssertNil(url)
    }

    func testStateMapper_withValidPendingURLReturnsLoading() {
        XCTAssertEqual(
            RemoteAsyncImageStateMapper.state(
                hasValidURL: true,
                didLoadSuccessfully: false,
                didFail: false
            ),
            .loading
        )
    }

    func testStateMapper_withSuccessfulLoadReturnsSuccess() {
        XCTAssertEqual(
            RemoteAsyncImageStateMapper.state(
                hasValidURL: true,
                didLoadSuccessfully: true,
                didFail: false
            ),
            .success
        )
    }

    func testStateMapper_withInvalidURLReturnsError() {
        XCTAssertEqual(
            RemoteAsyncImageStateMapper.state(
                hasValidURL: false,
                didLoadSuccessfully: false,
                didFail: false
            ),
            .error
        )
    }

    func testStateMapper_withFailureReturnsError() {
        XCTAssertEqual(
            RemoteAsyncImageStateMapper.state(
                hasValidURL: true,
                didLoadSuccessfully: false,
                didFail: true
            ),
            .error
        )
    }

    func testKingfisherDefaultCache_isAvailableForImageLoading() {
        XCTAssertFalse(String(describing: ImageCache.default).isEmpty)
    }

    private class StubImageBaseHostProvider: BaseHostUrlProvider {
        let baseHost: String

        init(baseHost: String) {
            self.baseHost = baseHost
        }

        func getBaseHostUrl() -> String {
            baseHost
        }
    }
}
