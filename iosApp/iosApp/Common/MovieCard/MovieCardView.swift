import Shared
import SwiftUI

struct MovieCardView: View {
    let data: MovieCardData
    var onMovieTap: (MovieCardData) -> Void = { _ in }
    var onCollectTap: (MovieCardData) -> Void = { _ in }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            posterSection
            titleSection
            releaseDateSection
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.primary.opacity(0.1), lineWidth: 1))
        .shadow(radius: 4)
        .onTapGesture {
            onMovieTap(data)
        }
    }

    private var posterSection: some View {
        ZStack(alignment: .bottom) {
            AsyncImage(url: URL(string: data.movieCardPosterPath)) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Color.gray.opacity(0.2)
            }
            .aspectRatio(3.0 / 4.0, contentMode: .fill)
            .clipped()

            ratingBadge
        }
        .overlay(alignment: .topTrailing) {
            collectButton
        }
    }

    private var ratingBadge: some View {
        HStack(spacing: 4) {
            Image(systemName: "star.fill")
                .foregroundStyle(.yellow)
            Text(String(format: "%.1f", data.movieCardVoteAverage))
                .font(.footnote.bold())
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 2)
        .background(Color(.secondarySystemBackground), in: Capsule())
        .padding(8)
    }

    private var collectButton: some View {
        Button {
            onCollectTap(data)
        } label: {
            Image(systemName: data.movieCardIsCollect ? "heart.fill" : "heart")
        }
        .padding(8)
        .background(Color(.tertiarySystemBackground), in: Circle())
        .buttonStyle(.plain)
        .padding(8)
    }

    private var titleSection: some View {
        Text(data.movieCardTitle)
            .font(.headline)
            .lineLimit(2)
            .padding(.horizontal, 8)
            .padding(.top, 8)
    }

    private var releaseDateSection: some View {
        HStack(spacing: 8) {
            Image(systemName: "calendar")
            Text(data.movieCardReleaseDate)
                .font(.subheadline)
        }
        .padding(8)
    }
}

#Preview {
    MovieCardView(data: HomeMockData.movieCards[0])
        .frame(width: 180)
        .padding()
}
