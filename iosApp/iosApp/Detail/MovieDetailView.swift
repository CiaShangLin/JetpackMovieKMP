import Foundation
import Shared
import SwiftUI

struct MovieDetailView: View {
    private let movieId: Int

    @State
    private var viewModel: MovieDetailViewModel

    @Binding
    private var path: NavigationPath

    init(movieId: Int, path: Binding<NavigationPath>) {
        self.movieId = movieId
        _path = path
        _viewModel = State(
            initialValue: MovieDetailViewModel(
                movieId: movieId,
                movieRepository: KoinHelper.shared.getMovieRepository(),
                getMovieDetailUseCase: KoinHelper.shared.getMovieDetailUseCase(),
                getMovieRecommendUseCase: KoinHelper.shared.getMovieRecommendUseCase()
            )
        )
    }

    var body: some View {
        content
            .task {
                await viewModel.fetchMovieDetail()
            }
            .task {
                await viewModel.observeCollectStatus()
            }
            .task {
                await viewModel.fetchMovieActor()
            }
            .task {
                await viewModel.fetchMovieRecommend()
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .loading:
            LoadingView()
        case let .success(data):
            SuccessView(
                detailData: data,
                actorData: viewModel.actorUiState,
                recommendData: viewModel.recommendUiState,
                isCollect: viewModel.isCollect,
                onCollectTap: {
                    Task {
                        await viewModel.toggleMovieCollectStatus(
                            data: data.asMovieCardResult()
                        )
                    }
                },
                onRecommendCollectTap: { movie in
                    Task {
                        await viewModel.toggleRecommendCollectStatus(data: movie)
                    }
                },
                onRecommendMovieTap: { movie in
                    path.append(Int(movie.movieCardId))
                }
            )
        case let .failure(errorMessage):
            ErrorView(
                message: LocalizedStringKey(errorMessage),
                onRetry: {
                    Task {
                        await viewModel.fetchMovieDetail()
                    }
                }
            )
        }
    }

    struct SuccessView: View {
        let detailData: MovieDetailBean
        let actorData: MovieActorUiState
        let recommendData: MovieRecommendUiState
        let isCollect: Bool
        let onCollectTap: () -> Void
        let onRecommendCollectTap: (MovieCardData) -> Void
        let onRecommendMovieTap: (MovieCardData) -> Void

        @Environment(\.dismiss)
        private var dismiss

        init(
            detailData: MovieDetailBean,
            actorData: MovieActorUiState,
            recommendData: MovieRecommendUiState,
            isCollect: Bool,
            onCollectTap: @escaping () -> Void,
            onRecommendCollectTap: @escaping (MovieCardData) -> Void,
            onRecommendMovieTap: @escaping (MovieCardData) -> Void
        ) {
            self.detailData = detailData
            self.actorData = actorData
            self.recommendData = recommendData
            self.isCollect = isCollect
            self.onCollectTap = onCollectTap
            self.onRecommendCollectTap = onRecommendCollectTap
            self.onRecommendMovieTap = onRecommendMovieTap
        }

        var body: some View {
            VStack {
                backdropSection
                ScrollView {
                    titleSection
                    overviewSection
                    actorSection
                    recommendSection
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }

        private var backdropSection: some View {
            Color.clear
                .aspectRatio(JMRatio.movieBackdrop, contentMode: .fit)
                .frame(maxWidth: .infinity)
                .overlay {
                    RemoteAsyncImage(path: detailData.backdropPath)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipped()
                }
                .overlay(alignment: .topTrailing) {
                    Button {
                        onCollectTap()
                    } label: {
                        Image(systemName: isCollect ? "heart.fill" : "heart")
                    }
                    .padding(JMSpacing.spacing8)
                    .background(Color(.tertiarySystemBackground), in: Circle())
                    .buttonStyle(.plain)
                    .padding(JMSpacing.spacing8)
                }
        }

        private var titleSection: some View {
            VStack {
                Text(detailData.title)
                    .font(.title)
                    .lineLimit(2)
                    .padding(.horizontal, JMSpacing.spacing8)
                    .padding(.top, JMSpacing.spacing8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack {
                    HStack(spacing: JMSpacing.spacing8) {
                        Image(systemName: "star.fill")
                            .foregroundStyle(.yellow)
                        Text(String(format: "%.1f", detailData.voteAverage))
                            .font(.footnote.bold())
                    }
                    HStack(spacing: JMSpacing.spacing8) {
                        Image(systemName: "calendar")
                        Text(detailData.releaseDate)
                            .font(.subheadline)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }

                    HStack(spacing: JMSpacing.spacing8) {
                        Image(systemName: "clock")
                        Text(String(format: String(localized: "detail_runtime_minutes"), detailData.runtime))
                            .font(.subheadline)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                }
                .padding(.horizontal, JMSpacing.spacing8)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }

        private var overviewSection: some View {
            VStack(alignment: .leading) {
                Text("detail_overview_title")
                    .font(.headline)
                    .padding(.top, JMSpacing.spacing8)
                    .padding(.leading, JMSpacing.spacing8)

                Text(detailData.overview)
                    .font(.body)
                    .lineLimit(nil)
                    .padding(JMSpacing.spacing8)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }

        @ViewBuilder
        private var actorSection: some View {
            switch actorData {
            case .loading:
                LoadingView()
            case let .success(actors) where !actors.cast.isEmpty:
                VStack(alignment: .leading) {
                    Text("detail_cast_title")
                        .font(.headline)
                        .padding(.top, JMSpacing.spacing8)
                        .padding(.leading, JMSpacing.spacing8)
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(spacing: JMSpacing.spacing8) {
                            ForEach(actors.cast, id: \.id) { cast in
                                Color.clear
                                    .overlay {
                                        RemoteAsyncImage(path: cast.profilePath)
                                    }
                                    .aspectRatio(JMRatio.movieActor, contentMode: .fit)
                                    .frame(width: JMSize.size96, height: JMSize.size96)
                                    .clipShape(Circle())
                                    .overlay(
                                        Circle()
                                            .stroke(Color.primary.opacity(JMOpacity.opacity10), lineWidth: JMSize.size1)
                                    )
                            }
                        }
                        .padding(JMSpacing.spacing8)
                    }
                }
            case .success, .failure:
                EmptyView()
            }
        }

        @ViewBuilder
        private var recommendSection: some View {
            switch recommendData {
            case .loading:
                LoadingView()
            case let .success(recommendData) where !recommendData.isEmpty:
                VStack(alignment: .leading) {
                    Text("detail_recommend_title")
                        .font(.headline)
                        .padding(.top, JMSpacing.spacing8)
                        .padding(.leading, JMSpacing.spacing8)
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(spacing: JMSpacing.spacing8) {
                            ForEach(recommendData, id: \.id) { movie in
                                MovieCardView(
                                    data: movie.asMovieCardData(),
                                    onMovieTap: { movie in
                                        onRecommendMovieTap(movie)
                                    },
                                    onCollectTap: { movie in
                                        onRecommendCollectTap(movie)
                                    }
                                ).frame(width: JMSize.movieGridMinWidth)
                            }
                        }
                        .padding(JMSpacing.spacing8)
                    }
                }
            case .success, .failure:
                EmptyView()
            }
        }
    }
}
