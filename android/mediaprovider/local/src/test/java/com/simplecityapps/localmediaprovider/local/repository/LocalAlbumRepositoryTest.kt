package com.simplecityapps.localmediaprovider.local.repository

import app.cash.turbine.test
import com.simplecityapps.localmediaprovider.local.data.room.dao.SongDataDao
import com.simplecityapps.mediaprovider.repository.albums.AlbumQuery
import com.simplecityapps.shuttle.model.Album
import com.simplecityapps.shuttle.model.MediaProviderType
import com.simplecityapps.shuttle.model.Song
import createSong
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAlbumRepositoryTest {
    private lateinit var repository: LocalAlbumRepository
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    private lateinit var mockSongDataDao: SongDataDao

    @Before
    fun setUp() {
        mockSongDataDao = mockk(relaxed = true)

        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)

        repository = LocalAlbumRepository(testScope, mockSongDataDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAlbums - returns proper albums`() = testScope.runTest {
        val song = createSong(
            albumArtist = "album-artist",
            album = "album-name",
            duration = 1,
            date = LocalDate(2024, 2, 11),
            playCount = 0,
            lastPlayed = Clock.System.now(),
            lastCompleted = Clock.System.now(),
            mediaProvider = MediaProviderType.Shuttle,
        )
        every { mockSongDataDao.getAll() } returns flowOf(listOf(song))

        val albumsFlow = repository.getAlbums(AlbumQuery.All())

        albumsFlow.test {
            val albums = awaitItem()

            // The output of shouldContainExactly is useless so, check each element
            // instead
            albums.shouldHaveSize(1)
            albums[0] shouldBe
                Album(
                    name = song.album,
                    albumArtist = song.albumArtist,
                    artists = song.artists,
                    songCount = 1,
                    duration = song.duration,
                    year = song.date?.year,
                    playCount = song.playCount,
                    lastSongPlayed = song.lastPlayed,
                    lastSongCompleted = song.lastCompleted,
                    groupKey = song.albumGroupKey,
                    mediaProviders = listOf(song.mediaProvider),
                )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getInProgressAlbums - returns in progress albums`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            albumArtist = ARTIST_NAME,
            name = ALBUM_NAME,
            songsPlayCount = listOf(1, 0),
        )
        every { mockSongDataDao.getAll() } returns flowOf(albumSongs)

        val inProgressAlbumsFlow = repository.getInProgressAlbums()

        inProgressAlbumsFlow.test {
            val inProgressAlbums = awaitItem()

            inProgressAlbums.shouldHaveSize(1)
            inProgressAlbums[0].albumArtist shouldBe ARTIST_NAME
            inProgressAlbums[0].name shouldBe ALBUM_NAME

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getInProgressAlbums - doesn't return albums with a single (played) song`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            songsPlayCount = listOf(1),
        )
        every { mockSongDataDao.getAll() } returns flowOf(albumSongs)

        val inProgressAlbumsFlow = repository.getInProgressAlbums()

        inProgressAlbumsFlow.test {
            val inProgressAlbums = awaitItem()

            inProgressAlbums.shouldBeEmpty()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getInProgressAlbums - doesn't return albums with a single (non-played) song`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            songsPlayCount = listOf(0),
        )
        every { mockSongDataDao.getAll() } returns flowOf(albumSongs)

        val inProgressAlbumsFlow = repository.getInProgressAlbums()

        inProgressAlbumsFlow.test {
            val inProgressAlbums = awaitItem()

            inProgressAlbums.shouldBeEmpty()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getInProgressAlbums - doesn't return albums with songs played in alternation`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            songsPlayCount = listOf(1, 0, 1),
        )
        every { mockSongDataDao.getAll() } returns flowOf(albumSongs)

        val inProgressAlbumsFlow = repository.getInProgressAlbums()

        inProgressAlbumsFlow.test {
            val inProgressAlbums = awaitItem()

            inProgressAlbums.shouldBeEmpty()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getInProgressAlbums - doesn't return albums if they haven't been started playing from the beginning`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            songsPlayCount = listOf(0, 1),
        )
        every { mockSongDataDao.getAll() } returns flowOf(albumSongs)

        val inProgressAlbumsFlow = repository.getInProgressAlbums()

        inProgressAlbumsFlow.test {
            val inProgressAlbums = awaitItem()

            inProgressAlbums.shouldBeEmpty()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getInProgressAlbums - doesn't return albums that all its songs have been played`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            songsPlayCount = listOf(1, 1),
        )
        every { mockSongDataDao.getAll() } returns flowOf(albumSongs)

        val inProgressAlbumsFlow = repository.getInProgressAlbums()

        inProgressAlbumsFlow.test {
            val inProgressAlbums = awaitItem()

            inProgressAlbums.shouldBeEmpty()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // **** End of LocalAlbumRepositoryTests. Tests for creation function:

    @Test
    fun `unit test creation function - createAlbumSongsWithPlayCounts - works`() = testScope.runTest {
        val albumSongs = createAlbumSongsWithPlayCounts(
            name = ALBUM_NAME,
            albumArtist = ARTIST_NAME,
            songsPlayCount = listOf(11, 22),
        )

        // The output of shouldContainExactly is useless so, check each element
        // instead
        albumSongs.shouldHaveSize(2)
        albumSongs[0] shouldBe
            createSong(
                name = "song-1",
                album = ALBUM_NAME,
                albumArtist = ARTIST_NAME,
                track = 1,
                playCount = 11,
            )
        albumSongs[1] shouldBe
            createSong(
                name = "song-2",
                album = ALBUM_NAME,
                albumArtist = ARTIST_NAME,
                track = 2,
                playCount = 22,
            )
    }
}

private const val ARTIST_NAME = "artist-name"
private const val ALBUM_NAME = "album-name"

private fun createAlbumSongsWithPlayCounts(
    name: String = ALBUM_NAME,
    albumArtist: String = "album-artist",
    songsPlayCount: List<Int> = emptyList(),
): List<Song> = songsPlayCount.mapIndexed { index, playCount ->
    createSong(
        name = "song-${index + 1}",
        albumArtist = albumArtist,
        album = name,
        track = index + 1,
        playCount = playCount,
    )
}
