import com.simplecityapps.shuttle.model.Album
import com.simplecityapps.shuttle.model.AlbumArtistGroupKey
import com.simplecityapps.shuttle.model.AlbumGroupKey
import com.simplecityapps.shuttle.model.MediaProviderType
import com.simplecityapps.shuttle.model.Song
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

fun createSong(
    name: String = "song-name",
    albumArtist: String = "album-artist",
    album: String = "album-name",
    track: Int = 1,
    duration: Int = 1,
    date: LocalDate = LocalDate(2024, 2, 11),
    playCount: Int = 0,
    lastPlayed: Instant = Instant.fromEpochSeconds(1),
    lastCompleted: Instant = Instant.fromEpochSeconds(1),
    mediaProvider: MediaProviderType = MediaProviderType.Shuttle,
) = Song(
    id = 1,
    name = name,
    albumArtist = albumArtist,
    artists = emptyList(),
    album = album,
    track = track,
    disc = 1,
    duration = duration,
    date = date,
    genres = emptyList(),
    path = "/path/to/song",
    size = 1,
    mimeType = "ogg",
    lastModified = Instant.fromEpochSeconds(1),
    lastPlayed = lastPlayed,
    lastCompleted = lastCompleted,
    playCount = playCount,
    playbackPosition = 1,
    blacklisted = false,
    externalId = null,
    mediaProvider = mediaProvider,
    replayGainTrack = null,
    replayGainAlbum = null,
    lyrics = null,
    grouping = null,
    bitRate = null,
    bitDepth = null,
    sampleRate = null,
    channelCount = null,
)

// TODO: use it
fun createAlbum(
    name: String = "album-name",
    albumArtist: String = "album-artist",
    artists: List<String> = emptyList(),
    songCount: Int = 11,
    duration: Int = 22,
    year: Int = 2024,
    playCount: Int = 33,
    lastSongPlayed: Instant = Instant.fromEpochSeconds(1),
    lastSongCompleted: Instant = Instant.fromEpochSeconds(1),
    groupKey: AlbumGroupKey = AlbumGroupKey("group-key", AlbumArtistGroupKey("album-artist-group-key")),
    mediaProviders: List<MediaProviderType> = emptyList(),
) = Album(
    name = name,
    albumArtist = albumArtist,
    artists = artists,
    songCount = songCount,
    duration = duration,
    year = year,
    playCount = playCount,
    lastSongPlayed = lastSongPlayed,
    lastSongCompleted = lastSongCompleted,
    groupKey = groupKey,
    mediaProviders = mediaProviders,
)

