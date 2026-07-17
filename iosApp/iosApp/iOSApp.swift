import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        InitKoinIosKt.doInitKoinIos(isDebug: false)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
