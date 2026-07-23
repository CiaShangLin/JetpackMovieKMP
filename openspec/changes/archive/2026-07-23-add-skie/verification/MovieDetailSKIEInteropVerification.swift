import Foundation
import Shared

enum MovieDetailSKIEInteropVerification {

    static func verifySuccessCase(movieId: Int32 = 550) async -> MovieDetailBean? {
        InitKoinIosKt.doInitKoinIos(isDebug: true)

        let useCase = KoinHelper.shared.getMovieDetailUseCase()
        let results = useCase.invoke(movieId: movieId)

        for await result in results {
            return result as? MovieDetailBean
        }

        return nil
    }

    static func verifyFailureCase(movieId: Int32 = -1) async -> Bool {
        let useCase = KoinHelper.shared.getMovieDetailUseCase()
        let results = useCase.invoke(movieId: movieId)

        for await result in results {
            return !(result is MovieDetailBean)
        }

        return false
    }
}
