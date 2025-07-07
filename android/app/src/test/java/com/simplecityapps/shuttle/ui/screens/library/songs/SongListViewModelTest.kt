import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.simplecityapps.mediaprovider.MediaImportObserver
import com.simplecityapps.mediaprovider.Progress
import com.simplecityapps.mediaprovider.SongImportState
import com.simplecityapps.mediaprovider.repository.songs.SongRepository
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.shuttle.model.Song
import com.simplecityapps.shuttle.query.SongQuery
import com.simplecityapps.shuttle.sorting.SongSortOrder
import com.simplecityapps.shuttle.ui.screens.library.SortPreferenceManager
import com.simplecityapps.shuttle.ui.screens.library.songs.SongListViewModel
import io.mockk.* // Import MockK functions
import io.mockk.impl.annotations.MockK // Import MockK annotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
// No need for MockitoJUnitRunner if you use @MockK and MockKAnnotations.init(this)

@ExperimentalCoroutinesApi
class SongListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule() // For LiveData/ViewModel testing

    private val testDispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var mockSongRepository: SongRepository

    @MockK
    private lateinit var mockPlaybackManager: PlaybackManager

    @MockK
    private lateinit var mockQueueManager: QueueManager

    @MockK
    private lateinit var mockSortPreferenceManager: SortPreferenceManager

    @MockK
    private lateinit var mockMediaImportObserver: MediaImportObserver

    @MockK
    private lateinit var mockApplication: android.app.Application

    private lateinit var viewModel: SongListViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this) // Initialize MockK annotations
        Dispatchers.setMain(testDispatcher)

        // Mock default behaviors using MockK's `every`
        every { mockSortPreferenceManager.sortOrderSongList } returns SongSortOrder.Default
        every { mockMediaImportObserver.songImportState } returns MutableStateFlow(SongImportState.Idle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll() // Optional: Clears all mocks after each test
    }

    @Test
    fun `viewState emits Loading initially`() = runTest {
        // Arrange
        every { mockSongRepository.getSongs(any<SongQuery.All>()) } returns flowOf(null)

        // Act
        viewModel = SongListViewModel(
            mockSongRepository,
            mockPlaybackManager,
            mockQueueManager,
            mockSortPreferenceManager,
            mockMediaImportObserver,
            mockApplication
        )

        // Assert
        assertEquals(SongListViewModel.ViewState.Loading, viewModel.viewState.value)
    }

    @Test
    fun `viewState emits Ready when songs are loaded`() = runTest {
        // Arrange
        val songs = listOf(Song(1L, "Song 1", "Artist 1", "Album 1", 1, 1, "path1", 0))
        every { mockSongRepository.getSongs(SongQuery.All(sortOrder = SongSortOrder.Default)) } returns
                flowOf(songs)

        // Act
        viewModel = SongListViewModel(
            mockSongRepository,
            mockPlaybackManager,
            mockQueueManager,
            mockSortPreferenceManager,
            mockMediaImportObserver,
            mockApplication
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val expectedState = SongListViewModel.ViewState.Ready(songs, emptySet(), __selectedSortOrder)
        assertEquals(expectedState, viewModel.viewState.value)
    }

    @Test
    fun `viewState emits Scanning when media import is in progress`() = runTest {
        // Arrange
        val progress = Progress(10, 100)
        every { mockSongRepository.getSongs(any<SongQuery.All>()) } returns flowOf(emptyList())
        every { mockMediaImportObserver.songImportState } returns MutableStateFlow(SongImportState.ImportProgress(progress))

        // Act
        viewModel = SongListViewModel(
            mockSongRepository,
            mockPlaybackManager,
            mockQueueManager,
            mockSortPreferenceManager,
            mockMediaImportObserver,
            mockApplication
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val expectedState = SongListViewModel.ViewState.Scanning(progress)
        assertEquals(expectedState, viewModel.viewState.value)
    }

    @Test
    fun `viewState updates with selected songs`() = runTest {
        // Arrange
        val song1 = Song(1L, "Song 1", "Artist 1", "Album 1", 1, 1, "path1", 0)
        val song2 = Song(2L, "Song 2", "Artist 2", "Album 2", 2, 2, "path2", 0)
        val initialSongs = listOf(song1, song2)
        every { mockSongRepository.getSongs(SongQuery.All(sortOrder = SongSortOrder.Default)) } returns
                flowOf(initialSongs)

        viewModel = SongListViewModel(
            mockSongRepository,
            mockPlaybackManager,
            mockQueueManager,
            mockSortPreferenceManager,
            mockMediaImportObserver,
            mockApplication
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.onSongLongClick(song1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val viewState = viewModel.viewState.value
        assertTrue(viewState is SongListViewModel.ViewState.Ready)
        val readyState = viewState as SongListViewModel.ViewState.Ready
        assertEquals(setOf(song1), readyState.selectedSongs)
        assertEquals(initialSongs, readyState.songs)
    }

    @Test
    fun `viewState updates when sort order changes`() = runTest {
        // Arrange
        val song1 = Song(1L, "A Song", "Artist Z", "Album C", 1, 1, "path1", 0, dateAdded = 100)
        val song2 = Song(2L, "B Song", "Artist Y", "Album B", 2, 2, "path2", 0, dateAdded = 200)
        val song3 = Song(3L, "C Song", "Artist X", "Album A", 3, 3, "path3", 0, dateAdded = 50)
        val initialSongs = listOf(song1, song2, song3)

        // Mock initial sort order (Default)
        every { mockSortPreferenceManager.sortOrderSongList } returns SongSortOrder.Default
        every { mockSongRepository.getSongs(SongQuery.All(sortOrder = SongSortOrder.Default)) } returns
                flowOf(initialSongs)

        viewModel = SongListViewModel(
            mockSongRepository,
            mockPlaybackManager,
            mockQueueManager,
            mockSortPreferenceManager,
            mockMediaImportObserver,
            mockApplication
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val initialViewState = viewModel.viewState.value
        assertTrue(initialViewState is SongListViewModel.ViewState.Ready)
        assertEquals(initialSongs, (initialViewState as SongListViewModel.ViewState.Ready).songs)

        // Act: Change sort order to ByDateAdded (Ascending)
        val newSortOrder = SongSortOrder.ByDateAdded
        val songsSortedByDateAdded = listOf(song3, song1, song2)

        // When setSortOrder is called, it updates sortPreferenceManager.
        // We need to ensure that when `sortOrderSongList` is accessed *after* this call,
        // it returns the new sort order.
        // We can achieve this by making the previous `every` block for sortOrderSongList less specific,
        // or by using `every { ... } answers { ... }` or `coEvery` for more dynamic responses if needed.
        // For this case, simply defining a new behavior for the subsequent call is fine if the
        // preference manager itself is stateful in the ViewModel, or if the ViewModel directly uses
        // the value passed to setSortOrder for its internal logic.

        // If setSortOrder internally calls mockSortPreferenceManager.sortOrderSongList = newSortOrder
        // then we might need to verify that interaction or ensure the mock reflects that change.
        // However, the ViewModel's `_selectedSortOrder` Flow is updated directly.
        // The crucial part is that the `combine` operator in the ViewModel will re-trigger
        // using the new `_selectedSortOrder.value`.

        // Let's ensure the preference manager will return the new sort order when asked
        // by the ViewModel's internal logic after the setSortOrder call.
        every { mockSortPreferenceManager.sortOrderSongList } returns newSortOrder
        // If setSortOrder has side effects on the mockSortPreferenceManager (like calling a setter),
        // you'd mock that setter: `every { mockSortPreferenceManager.sortOrderSongList = newSortOrder } just Runs`

        viewModel.setSortOrder(newSortOrder)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val updatedViewState = viewModel.viewState.value
        assertTrue(updatedViewState is SongListViewModel.ViewState.Ready)
        val readyState = updatedViewState as SongListViewModel.ViewState.Ready
        assertEquals(songsSortedByDateAdded, readyState.songs)

        // Optional: Verify that the preference manager was updated (if that's a direct interaction)
        // verify { mockSortPreferenceManager.sortOrderSongList = newSortOrder } // If your SortPreferenceManager had a setter
    }
}

