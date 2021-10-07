package com.simplecityapps.shuttle.ui.screens.playback

import android.content.Context
import com.simplecityapps.mediaprovider.repository.albums.AlbumQuery
import com.simplecityapps.mediaprovider.repository.albums.AlbumRepository
import com.simplecityapps.mediaprovider.repository.artists.AlbumArtistQuery
import com.simplecityapps.mediaprovider.repository.artists.AlbumArtistRepository
import com.simplecityapps.mediaprovider.repository.playlists.PlaylistRepository
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.playback.PlaybackState
import com.simplecityapps.playback.PlaybackWatcher
import com.simplecityapps.playback.PlaybackWatcherCallback
import com.simplecityapps.playback.queue.QueueChangeCallback
import com.simplecityapps.playback.queue.QueueItem
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.playback.queue.QueueWatcher
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import com.simplecityapps.shuttle.ui.lyrics.QuickLyricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class PlaybackPresenter @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val playbackWatcher: PlaybackWatcher,
    private val queueManager: QueueManager,
    private val queueWatcher: QueueWatcher,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val albumArtistRepository: AlbumArtistRepository,
    @ApplicationContext private val context: Context
) : BasePresenter<PlaybackContract.View>(),
    PlaybackContract.Presenter,
    QueueChangeCallback,
    PlaybackWatcherCallback {

    private var favoriteUpdater: Job? = null

    override fun bindView(view: PlaybackContract.View) {
        super.bindView(view)

        playbackWatcher.addCallback(this)
        queueWatcher.addCallback(this)

        // One time update of all UI components
        updateProgress()
        updateShuffleMode(queueManager.getShuffleMode())
        updateRepeatMode(queueManager.getRepeatMode())
        updateQueue(queueManager.getQueue())
        updateQueuePosition(queueManager.getCurrentPosition())
        updateCurrentSong(queueManager.getCurrentItem()?.song)
        updatePlaybackState(playbackManager.playbackState())
        updateFavorite()
    }

    override fun unbindView() {
        playbackWatcher.removeCallback(this)
        queueWatcher.removeCallback(this)

        updateFavorite()

        super.unbindView()
    }


    // Private

    private fun updateProgress() {
        queueManager.getCurrentItem()?.song?.let { currentSong ->
            onProgressChanged(
                position = playbackManager.getProgress() ?: 0,
                duration = playbackManager.getDuration() ?: currentSong.duration ?: 0,
                fromUser = false
            )
        }
    }

    private fun updateFavorite() {
        favoriteUpdater?.cancel()
        val job = launch {
            val isFavorite = playlistRepository
                .getSongsForPlaylist(playlistRepository.getFavoritesPlaylist())
                .firstOrNull()
                .orEmpty()
                .map { it.song }
                .contains(queueManager.getCurrentItem()?.song)
            this@PlaybackPresenter.view?.setIsFavorite(isFavorite)
        }

        favoriteUpdater = job
    }

    private fun updateQueue(queue: List<QueueItem>) {
        view?.clearQueue()
        view?.setQueue(queue)
    }

    private fun updateQueuePosition(newPosition: Int?) {
        view?.setQueuePosition(newPosition, queueManager.getSize())
    }

    private fun updateCurrentSong(song: com.simplecityapps.shuttle.model.Song?) {
        view?.setCurrentSong(song)
    }

    private fun updateShuffleMode(shuffleMode: QueueManager.ShuffleMode) {
        view?.setShuffleMode(shuffleMode)
    }

    private fun updateRepeatMode(repeatMode: QueueManager.RepeatMode) {
        view?.setRepeatMode(repeatMode)
    }

    private fun updatePlaybackState(playbackState: PlaybackState) {
        view?.setPlaybackState(playbackState)
    }


    // PlaybackContract.Presenter Implementation

    override fun togglePlayback() {
        playbackManager.togglePlayback()
    }

    override fun toggleShuffle() {
        launch {
            queueManager.toggleShuffleMode()
        }
    }

    override fun toggleRepeat() {
        queueManager.toggleRepeatMode()
    }

    override fun skipNext() {
        playbackManager.skipToNext(ignoreRepeat = true)
    }

    override fun skipPrev() {
        playbackManager.skipToPrev()
    }

    override fun skipTo(position: Int) {
        playbackManager.skipTo(position)
    }

    override fun seekForward(seconds: Int) {
        Timber.v("seekForward() seconds: $seconds")
        playbackManager.getProgress()?.let { position ->
            playbackManager.seekTo(position + seconds * 1000)
        }
    }

    override fun seekBackward(seconds: Int) {
        Timber.v("seekBackward() seconds: $seconds")
        playbackManager.getProgress()?.let { position ->
            playbackManager.seekTo(position - seconds * 1000)
        }
    }

    override fun seek(fraction: Float) {
        queueManager.getCurrentItem()?.song?.let { currentSong ->
            playbackManager.seekTo(((playbackManager.getDuration() ?: (currentSong.duration ?: 0)) * fraction).toInt())
        } ?: Timber.v("seek() failed, current song null")
    }

    override fun updateProgress(fraction: Float) {
        queueManager.getCurrentItem()?.song?.let { currentSong ->
            view?.setProgress(((playbackManager.getDuration() ?: (currentSong.duration ?: 0)) * fraction).toInt(), (playbackManager.getDuration() ?: currentSong.duration ?: 0).toInt())
        } ?: Timber.v("seek() failed, current song null")
    }

    override fun sleepTimerClicked() {
        view?.presentSleepTimer()
    }

    override fun setFavorite(isFavorite: Boolean) {
        launch {
            queueManager.getCurrentItem()?.song?.let { song ->
                val favoritesPlaylist = playlistRepository.getFavoritesPlaylist()
                if (isFavorite) {
                    playlistRepository.addToPlaylist(favoritesPlaylist, listOf(song))
                } else {
                    playlistRepository.removeSongsFromPlaylist(favoritesPlaylist, listOf(song))
                }
            }
        }
    }

    override fun goToAlbum() {
        launch {
            queueManager.getCurrentItem()?.song?.let { song ->
                val albums = albumRepository.getAlbums(AlbumQuery.AlbumGroupKey(song.albumGroupKey)).firstOrNull().orEmpty()
                albums.firstOrNull()?.let { album ->
                    view?.goToAlbum(album)
                } ?: Timber.e("Failed to retrieve album for song: ${song.name}")
            }
        }
    }

    override fun goToArtist() {
        launch {
            queueManager.getCurrentItem()?.song?.let { song ->
                val artists = albumArtistRepository.getAlbumArtists(AlbumArtistQuery.AlbumArtistGroupKey(key = song.albumArtistGroupKey)).firstOrNull().orEmpty()
                artists.firstOrNull()?.let { artist ->
                    view?.goToArtist(artist)
                } ?: Timber.e("Failed to retrieve album artist for song: ${song.name}")
            }
        }
    }

    override fun showSongInfo() {
        queueManager.getCurrentItem()?.let { queueItem ->
            view?.showSongInfoDialog(queueItem.song)
        }
    }

    override fun showOrLaunchLyrics() {
        queueManager.getCurrentItem()?.let { queueItem ->
            queueItem.song.lyrics?.let { lyrics ->
                view?.displayLyrics(lyrics)
            } ?: launchQuickLyric()
        }
    }

    override fun launchQuickLyric() {
        queueManager.getCurrentItem()?.let { queueItem ->
            if (QuickLyricManager.isQuickLyricInstalled(context)) {
                view?.launchQuickLyric(queueItem.song.albumArtist ?: context.getString(R.string.unknown), queueItem.song.name ?: context.getString(R.string.unknown))
            } else {
                if (QuickLyricManager.canDownloadQuickLyric(context)) {
                    view?.getQuickLyric()
                } else {
                    view?.showQuickLyricUnavailable()
                }
            }
        }
    }

    override fun clearQueue() {
        playbackManager.clearQueue()
    }


    // PlaybackWatcherCallback Implementation

    override fun onPlaybackStateChanged(playbackState: PlaybackState) {
        updatePlaybackState(playbackState)
    }


    // PlaybackManager.ProgressCallback

    override fun onProgressChanged(position: Int, duration: Int, fromUser: Boolean) {
        view?.setProgress(position, duration)
    }


    // QueueChangeCallback Implementation

    override fun onQueueRestored() {
        updateQueue(queueManager.getQueue())
        updateCurrentSong(queueManager.getCurrentItem()?.song)
        updateQueuePosition(queueManager.getCurrentPosition())
    }

    override fun onQueueChanged(reason: QueueChangeCallback.QueueChangeReason) {
        updateQueue(queueManager.getQueue())
    }

    override fun onQueuePositionChanged(oldPosition: Int?, newPosition: Int?) {
        updateCurrentSong(queueManager.getCurrentItem()?.song)
        updateQueuePosition(newPosition)
        updateFavorite()
    }

    override fun onShuffleChanged(shuffleMode: QueueManager.ShuffleMode) {
        updateShuffleMode(shuffleMode)
    }

    override fun onRepeatChanged(repeatMode: QueueManager.RepeatMode) {
        updateRepeatMode(repeatMode)
    }
}