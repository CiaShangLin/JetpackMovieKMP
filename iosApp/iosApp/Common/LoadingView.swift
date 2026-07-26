import Lottie
import SwiftUI

struct LoadingView: View {
    var body: some View {
        LottieView(animation: .named("loading")).looping()
    }
}
