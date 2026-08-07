import Shared
import XCTest
@testable import JetpackMovieKMP

/// 涵蓋 `HomeMovieListLoadState`／`SearchMovieListLoadState` 映射到共用
/// `AppendLoadState` 的正確性，確保訊息內容不遺失。
final class AppendLoadStateMappingTests: XCTestCase {
    func testHomeMovieListLoadState_idle_mapsToIdle() {
        // Act
        let result = HomeMovieListLoadStateIdle.shared.asAppendLoadState()

        // Assert
        guard case .idle = result else {
            return XCTFail("Idle 應映射為 .idle")
        }
    }

    func testHomeMovieListLoadState_loading_mapsToLoading() {
        // Act
        let result = HomeMovieListLoadStateLoading.shared.asAppendLoadState()

        // Assert
        guard case .loading = result else {
            return XCTFail("Loading 應映射為 .loading")
        }
    }

    func testHomeMovieListLoadState_error_mapsToErrorWithMessage() {
        // Act
        let result = HomeMovieListLoadStateError(message: "home 載入失敗").asAppendLoadState()

        // Assert
        guard case let .error(message) = result else {
            return XCTFail("Error 應映射為 .error")
        }
        XCTAssertEqual(message, "home 載入失敗")
    }

    func testSearchMovieListLoadState_idle_mapsToIdle() {
        // Act
        let result = SearchMovieListLoadStateIdle.shared.asAppendLoadState()

        // Assert
        guard case .idle = result else {
            return XCTFail("Idle 應映射為 .idle")
        }
    }

    func testSearchMovieListLoadState_loading_mapsToLoading() {
        // Act
        let result = SearchMovieListLoadStateLoading.shared.asAppendLoadState()

        // Assert
        guard case .loading = result else {
            return XCTFail("Loading 應映射為 .loading")
        }
    }

    func testSearchMovieListLoadState_error_mapsToErrorWithMessage() {
        // Act
        let result = SearchMovieListLoadStateError(message: "search 載入失敗").asAppendLoadState()

        // Assert
        guard case let .error(message) = result else {
            return XCTFail("Error 應映射為 .error")
        }
        XCTAssertEqual(message, "search 載入失敗")
    }
}
