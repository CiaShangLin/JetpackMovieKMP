import Shared
import SwiftUI

struct SplashView: View {
    @State
    private var viewModel = SplashViewModel(
        configurationLoader: KoinHelper.shared.getConfigurationLoader()
    )

    @State
    private var isLogoVisible = false

    let onFinished: () -> Void

    var body: some View {
        VStack(spacing: 28) {
            Image(systemName: "film.stack.fill")
                .font(.system(size: 72, weight: .semibold))
                .foregroundStyle(.blue)
                .opacity(isLogoVisible ? 1 : 0)
                .scaleEffect(isLogoVisible ? 1 : 0.92)
            content
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
        .background(Color(.systemBackground))
        .task {
            withAnimation(.easeOut(duration: 0.6)) {
                isLogoVisible = true
            }
            await viewModel.loadConfiguration()

            if viewModel.isConfigurationLoaded {
                onFinished()
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .loading:
            ProgressView("載入中")
        case .success:
            ProgressView("準備完成")
        case let .failure(error):
            VStack(spacing: 12) {
                Text(error)
                    .font(.body)
                    .multilineTextAlignment(.center)

                Button("重試") {
                    Task {
                        await viewModel.retry()

                        if viewModel.isConfigurationLoaded {
                            onFinished()
                        }
                    }
                }
            }
        }
    }
}
