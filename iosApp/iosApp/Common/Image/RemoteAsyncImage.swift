import Kingfisher
import SwiftUI

enum RemoteAsyncImageContentState: Equatable {
    case image
    case error
}

enum RemoteAsyncImageContentStateMapper {
    static func state(
        hasValidURL: Bool,
        didFail: Bool
    ) -> RemoteAsyncImageContentState {
        if !hasValidURL || didFail {
            return .error
        }

        return .image
    }
}

struct RemoteAsyncImage<LoadingContent: View, SuccessContent: View, ErrorContent: View>: View {
    private let path: String
    private let resolver: MovieImageURLResolving
    private let loadingContent: () -> LoadingContent
    private let successContent: (KFImage) -> SuccessContent
    private let errorContent: () -> ErrorContent

    @State
    private var failedPath: String?

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
        let contentState = RemoteAsyncImageContentStateMapper.state(
            hasValidURL: resolvedURL != nil,
            didFail: failedPath == path
        )

        switch contentState {
        case .image:
            if let resolvedURL {
                successContent(
                    KFImage.url(resolvedURL)
                        .placeholder {
                            loadingContent()
                        }
                        .onSuccess { _ in
                            failedPath = nil
                        }
                        .onFailure { _ in
                            failedPath = path
                        }
                )
            }
        case .error:
            errorContent()
        }
    }
}

extension RemoteAsyncImage where LoadingContent == AnyView, SuccessContent == AnyView, ErrorContent == AnyView {
    init(path: String) {
        self.init(
            path: path,
            loadingContent: {
                AnyView(
                    ZStack {
                        Color.gray.opacity(0.2)
                        ProgressView()
                    }
                )
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
