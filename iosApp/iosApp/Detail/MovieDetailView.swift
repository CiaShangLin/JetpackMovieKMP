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
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.uiState {
        case .loading:
            LoadingView()
        case let .success(data):
            SuccessView(data: data)
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
        let data: MovieDetailBean

        @Environment(\.dismiss)
        private var dismiss

        init(data: MovieDetailBean) {
            self.data = data
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
                    RemoteAsyncImage(path: data.backdropPath)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipped()
                }
                .overlay(alignment: .topTrailing) {
                    Button {
                        // onCollectTap(data)
                    } label: {
                        Image(systemName: "heart")
                    }
                    .padding(JMSpacing.spacing8)
                    .background(Color(.tertiarySystemBackground), in: Circle())
                    .buttonStyle(.plain)
                    .padding(JMSpacing.spacing8)
                }
        }

        private var titleSection: some View {
            VStack {
                Text(data.title)
                    .font(.title)
                    .lineLimit(2)
                    .padding(.horizontal, JMSpacing.spacing8)
                    .padding(.top, JMSpacing.spacing8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack {
                    HStack(spacing: JMSpacing.spacing8) {
                        Image(systemName: "star.fill")
                            .foregroundStyle(.yellow)
                        Text(String(format: "%.1f", data.voteAverage))
                            .font(.footnote.bold())
                    }
                    HStack(spacing: JMSpacing.spacing8) {
                        Image(systemName: "calendar")
                        Text(data.releaseDate)
                            .font(.subheadline)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }

                    HStack(spacing: JMSpacing.spacing8) {
                        Image(systemName: "clock")
                        Text(String(format: String(localized: "detail_runtime_minutes"), data.runtime))
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

                Text(data.overview)
                    .font(.body)
                    .lineLimit(nil)
                    .padding(JMSpacing.spacing8)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }

        private var actorSection: some View {
            VStack(alignment: .leading) {
                Text("detail_cast_title")
                    .font(.headline)
                    .padding(.top, JMSpacing.spacing8)
                    .padding(.leading, JMSpacing.spacing8)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: JMSpacing.spacing8) {}
                        .padding(JMSpacing.spacing8)
                }
            }
        }
    }
}
