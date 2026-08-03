import Foundation
import Shared

enum MovieDetailUiState {
    case loading
    case success(MovieDetailBean)
    case failure(String)
}
