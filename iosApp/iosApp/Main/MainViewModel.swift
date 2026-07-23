import Foundation
import Shared

@MainActor
final class MainViewModel: ObservableObject {
    private let userDataRepository: UserDataRepository

    init(userDataRepository: UserDataRepository) {
        self.userDataRepository = userDataRepository
    }
}
