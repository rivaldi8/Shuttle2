package com.simplecityapps.shuttle.model

import com.simplecityapps.shuttle.parcel.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Parcelize
@TypeParceler<Instant?, InstantParceler>
@TypeParceler<LocalDate?, LocalDateParceler>
data class Song(
    val id: Long,
    val name: String?,
    val albumArtist: String?,
    val artists: List<String>,
    val album: String?,
    val track: Int?,
    val disc: Int?,
    val duration: Int,
    val date: LocalDate?,
    val genres: List<String>,
    val path: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Instant?,
    val lastPlayed: Instant?,
    val lastCompleted: Instant?,
    val playCount: Int,
    val playbackPosition: Int,
    val blacklisted: Boolean,
    val externalId: String? = null,
    val mediaProvider: MediaProviderType,
    val replayGainTrack: Double? = null,
    val replayGainAlbum: Double? = null,
    val lyrics: String?,
    val grouping: String?,
    val bitRate: Int?,
    val bitDepth: Int?,
    val sampleRate: Int?,
    val channelCount: Int?
) : Parcelable {

    val type: Type
        get() {
            return when {
                path.contains("audiobook", true) || path.endsWith("m4b", true) -> Type.Audiobook
                path.contains("podcast", true) -> Type.Podcast
                else -> Type.Audio
            }
        }

    val albumArtistGroupKey: AlbumArtistGroupKey = AlbumArtistGroupKey(
        albumArtist?.lowercase()?.removeArticles()
            ?: artists.joinToString(", ") { it.lowercase().removeArticles() }.ifEmpty { null }
    )

    val albumGroupKey = AlbumGroupKey(album?.lowercase()?.removeArticles(), albumArtistGroupKey)

    enum class Type {
        Audio, Audiobook, Podcast
    }

    val friendlyArtistName: String? = if (artists.isNotEmpty()) {
        if (artists.size == 1) {
            artists.first()
        } else {
            artists.groupBy { it.lowercase().removeArticles() }
                .map { map -> map.value.maxByOrNull { it.length } }
                .joinToString(", ")
                .ifEmpty { null }
        }
    } else {
        null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Song

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
