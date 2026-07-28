import Shared
import SwiftUI

@main
struct IosApp: App {
    private let movieRepository: MovieRepository

    @State
    private var isSplashFinished = false

    init() {
        InitKoinIosKt.doInitKoinIos(isDebug: AppDebugFlag.isDebugLoggingEnabled)
        movieRepository = KoinHelper.shared.getMovieRepository()
    }

    var body: some Scene {
        WindowGroup {
            if isSplashFinished {
                MainView(movieRepository: movieRepository)
            } else {
                SplashView {
                    withAnimation {
                        isSplashFinished = true
                    }
                }
            }
        }
    }
}
