import Kingfisher
import SwiftUI

enum RemoteAsyncImageLoadState: Equatable {
    case loading
    case success
    case error
}

enum RemoteAsyncImageStateMapper {
    static func state(
        hasValidURL: Bool,
        didLoadSuccessfully: Bool,
        didFail: Bool
    ) -> RemoteAsyncImageLoadState {
        if !hasValidURL || didFail {
            return .error
        }

        if didLoadSuccessfully {
            return .success
        }

        return .loading
    }
}

struct RemoteAsyncImage<LoadingContent: View, SuccessContent: View, ErrorContent: View>: View {
    private let path: String
    private let resolver: MovieImageURLResolving
    private let loadingContent: () -> LoadingContent
    private let successContent: (KFImage) -> SuccessContent
    private let errorContent: () -> ErrorContent

    @State
    private var didLoadSuccessfully = false

    @State
    private var didFail = false

    init(
        path: String,
        resolver: MovieImageURLResolving = MovieImageURLResolver(),
        @ViewBuilder loadingContent: @escaping () -> LoadingContent,
        @ViewBuilder successContent: @escaping (KFImage) -> SuccessContent,
        @ViewBuilder errorContent: @escaping () -> ErrorContent
    ) {
        self.path = path
        self.resolver = resolver
        self.loadingContent = loadingContent
        self.successContent = successContent
        self.errorContent = errorContent
    }

    var body: some View {
        let resolvedURL = resolver.resolve(path: path)
        let loadState = RemoteAsyncImageStateMapper.state(
            hasValidURL: resolvedURL != nil,
            didLoadSuccessfully: didLoadSuccessfully,
            didFail: didFail
        )

        ZStack {
            if let resolvedURL {
                successContent(
                    KFImage.url(resolvedURL)
                        .placeholder {
                            loadingContent()
                        }
                        .onSuccess { _ in
                            didLoadSuccessfully = true
                            didFail = false
                        }
                        .onFailure { _ in
                            didLoadSuccessfully = false
                            didFail = true
                        }
                )
            }

            if loadState == .error {
                errorContent()
            }
        }
    }
}

extension RemoteAsyncImage where LoadingContent == AnyView, SuccessContent == AnyView, ErrorContent == AnyView {
    init(path: String) {
        self.init(
            path: path,
            loadingContent: {
                AnyView(Color.gray.opacity(0.2))
            },
            successContent: { image in
                AnyView(image.resizable().aspectRatio(contentMode: .fill))
            },
            errorContent: {
                AnyView(
                    ZStack {
                        Color.gray.opacity(0.2)
                        Image(systemName: "photo")
                            .foregroundStyle(.secondary)
                    }
                )
            }
        )
    }
}
