import Foundation
import Shared
import SwiftUI

struct MovieDetailView: View {
    private let movieId: Int

    @State
    private var viewModel: MovieDetailViewModel

    init(movieId: Int) {
        self.movieId = movieId
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
                isCollect: viewModel.isCollect,
                onCollectTap: {
                    Task {
                        await viewModel.toggleMovieCollectStatus(
                            data: data.asMovieCardResult()
                        )
                    }
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
        let isCollect: Bool
        let onCollectTap: () -> Void

        @Environment(\.dismiss)
        private var dismiss

        init(
            detailData: MovieDetailBean,
            actorData: MovieActorUiState,
            isCollect: Bool,
            onCollectTap: @escaping () -> Void
        ) {
            self.detailData = detailData
            self.actorData = actorData
            self.isCollect = isCollect
            self.onCollectTap = onCollectTap
        }

        var body: some View {
            VStack {
                backdropSection
                ScrollView {
                    titleSection
                    overviewSection
                    actorSection
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
