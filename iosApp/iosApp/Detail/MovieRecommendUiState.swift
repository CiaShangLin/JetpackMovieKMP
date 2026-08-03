import Foundation
import Shared

enum MovieRecommendUiState {
    case loading
    case success([MovieCardResult])
    case failure(String)
}
