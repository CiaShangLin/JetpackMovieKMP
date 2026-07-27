import Foundation
import Shared

protocol MovieImageURLResolving {
    func resolve(path: String) -> URL?
}

struct MovieImageURLResolver: MovieImageURLResolving {
    private let baseHostProvider: BaseHostUrlProvider?

    init(baseHostProvider: BaseHostUrlProvider? = nil) {
        self.baseHostProvider = baseHostProvider
    }

    func resolve(path: String) -> URL? {
        let resolvedPath: String

        if path.hasPrefix("http://") || path.hasPrefix("https://") {
            resolvedPath = path
        } else {
            let baseHost = (baseHostProvider ?? KoinHelper.shared.getBaseHostUrlProvider()).getBaseHostUrl()

            if baseHost.isEmpty {
                return nil
            }

            resolvedPath = "\(baseHost)original\(path)"
        }

        return URL(string: resolvedPath)
    }
}
