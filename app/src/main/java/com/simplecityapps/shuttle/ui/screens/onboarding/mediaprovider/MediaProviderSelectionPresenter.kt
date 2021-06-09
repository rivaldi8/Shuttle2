package com.simplecityapps.shuttle.ui.screens.onboarding.mediaprovider

import com.simplecityapps.localmediaprovider.local.provider.mediastore.MediaStoreMediaProvider
import com.simplecityapps.localmediaprovider.local.provider.taglib.TaglibMediaProvider
import com.simplecityapps.mediaprovider.MediaImporter
import com.simplecityapps.mediaprovider.MediaProvider
import com.simplecityapps.mediaprovider.repository.SongRepository
import com.simplecityapps.playback.PlaybackManager
import com.simplecityapps.playback.persistence.PlaybackPreferenceManager
import com.simplecityapps.playback.queue.QueueManager
import com.simplecityapps.provider.emby.EmbyMediaProvider
import com.simplecityapps.provider.jellyfin.JellyfinMediaProvider
import com.simplecityapps.provider.plex.PlexMediaProvider
import com.simplecityapps.shuttle.ui.common.mvp.BasePresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

interface MediaProviderSelectionContract {

    interface Presenter {
        fun addProviderClicked()
        fun addMediaProviderType(mediaProviderType: MediaProvider.Type)
        fun removeMediaProviderType(mediaProviderType: MediaProvider.Type)
    }

    interface View {
        fun showMediaProviderSelectionDialog(mediaProviderTypes: List<MediaProvider.Type>)
        fun setMediaProviders(mediaProviderTypes: List<MediaProvider.Type>)
    }
}

class MediaProviderSelectionPresenter @Inject constructor(
    private val playbackPreferenceManager: PlaybackPreferenceManager,
    private val mediaImporter: MediaImporter,
    private val taglibMediaProvider: TaglibMediaProvider,
    private val mediaStoreMediaProvider: MediaStoreMediaProvider,
    private val embyMediaProvider: EmbyMediaProvider,
    private val jellyfinMediaProvider: JellyfinMediaProvider,
    private val plexMediaProvider: PlexMediaProvider,
    private val songRepository: SongRepository,
    private val queueManager: QueueManager,
    private val playbackManager: PlaybackManager,
    @Named("AppCoroutineScope") private val appCoroutineScope: CoroutineScope,
) : BasePresenter<MediaProviderSelectionContract.View>(),
    MediaProviderSelectionContract.Presenter {

    override fun bindView(view: MediaProviderSelectionContract.View) {
        super.bindView(view)

        view.setMediaProviders(playbackPreferenceManager.mediaProviderTypes)
    }

    override fun addProviderClicked() {
        view?.showMediaProviderSelectionDialog(
            (MediaProvider.Type.values().toList() - playbackPreferenceManager.mediaProviderTypes).filterNot { it == MediaProvider.Type.Plex }
        )
    }

    override fun addMediaProviderType(mediaProviderType: MediaProvider.Type) {
        if (!playbackPreferenceManager.mediaProviderTypes.contains(mediaProviderType)) {
            playbackPreferenceManager.mediaProviderTypes = playbackPreferenceManager.mediaProviderTypes + mediaProviderType
        }

        mediaImporter.mediaProviders += mediaProviderType.toMediaProvider()

        view?.setMediaProviders(playbackPreferenceManager.mediaProviderTypes)
    }

    override fun removeMediaProviderType(mediaProviderType: MediaProvider.Type) {
        if (playbackPreferenceManager.mediaProviderTypes.contains(mediaProviderType)) {
            playbackPreferenceManager.mediaProviderTypes = playbackPreferenceManager.mediaProviderTypes - mediaProviderType
        }

        mediaImporter.mediaProviders -= mediaProviderType.toMediaProvider()

        view?.setMediaProviders(playbackPreferenceManager.mediaProviderTypes)

        queueManager.getCurrentItem()?.let {
            if (it.song.mediaProvider == mediaProviderType) {
                playbackManager.pause()
            }
        }
        queueManager.remove(queueManager.getQueue().filter { it.song.mediaProvider == mediaProviderType })

        appCoroutineScope.launch {
            songRepository.removeAll(mediaProviderType)
        }
    }

    private fun MediaProvider.Type.toMediaProvider(): MediaProvider {
        return when (this) {
            MediaProvider.Type.MediaStore -> mediaStoreMediaProvider
            MediaProvider.Type.Shuttle -> taglibMediaProvider
            MediaProvider.Type.Emby -> embyMediaProvider
            MediaProvider.Type.Jellyfin -> jellyfinMediaProvider
            MediaProvider.Type.Plex -> plexMediaProvider
        }
    }
}