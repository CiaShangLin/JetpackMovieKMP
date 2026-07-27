import Shared
import SwiftUI

struct MovieCardView: View {
    let data: MovieCardData
    var onMovieTap: (MovieCardData) -> Void = { _ in }
    var onCollectTap: (MovieCardData) -> Void = { _ in }

    var body: some View {
        VStack(alignment: .leading, spacing: JMSpacing.spacing0) {
            posterSection
            titleSection
            releaseDateSection
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: JMRadius.radius12))
        .overlay(
            RoundedRectangle(cornerRadius: JMRadius.radius12)
                .stroke(Color.primary.opacity(JMOpacity.opacity10), lineWidth: JMSize.size1)
        )
        .shadow(radius: JMSize.size4)
        .onTapGesture {
            onMovieTap(data)
        }
    }

    private var posterSection: some View {
        ZStack(alignment: .bottomLeading) {
            RemoteAsyncImage(path: data.movieCardPosterPath)
                .aspectRatio(3.0 / 4.0, contentMode: .fill)
                .clipped()
            ratingBadge
        }
        .overlay(alignment: .topTrailing) {
            collectButton
        }
    }

    private var ratingBadge: some View {
        HStack(spacing: JMSpacing.spacing4) {
            Image(systemName: "star.fill")
                .foregroundStyle(.yellow)
            Text(String(format: "%.1f", data.movieCardVoteAverage))
                .font(.footnote.bold())
        }
        .padding(.horizontal, JMSpacing.spacing8)
        .padding(.vertical, JMSpacing.spacing2)
        .background(Color(.secondarySystemBackground), in: Capsule())
        .padding(JMSpacing.spacing8)
    }

    private var collectButton: some View {
        Button {
            onCollectTap(data)
        } label: {
            Image(systemName: data.movieCardIsCollect ? "heart.fill" : "heart")
        }
        .padding(JMSpacing.spacing8)
        .background(Color(.tertiarySystemBackground), in: Circle())
        .buttonStyle(.plain)
        .padding(JMSpacing.spacing8)
    }

    private var titleSection: some View {
        Text(data.movieCardTitle)
            .font(.headline)
            .lineLimit(2, reservesSpace: true)
            .padding(.horizontal, JMSpacing.spacing8)
            .padding(.top, JMSpacing.spacing8)
    }

    private var releaseDateSection: some View {
        HStack(spacing: JMSpacing.spacing8) {
            Image(systemName: "calendar")
            Text(data.movieCardReleaseDate)
                .font(.subheadline)
        }
        .padding(JMSpacing.spacing8)
    }
}

#Preview {
    MovieCardView(
        data: MovieCardData(
            movieCardId: 1,
            movieCardTitle: "Sample Movie",
            movieCardPosterPath: "https://fastly.picsum.photos/id/1020/400/300.jpg",
            movieCardReleaseDate: "2023-10-01",
            movieCardVoteAverage: 8.7,
            movieCardIsCollect: false,
            movieCardTimestamp: 0
        )
    )
    .frame(width: 180)
    .padding()
}
