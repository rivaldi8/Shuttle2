package com.simplecityapps.localmediaprovider.local.repository

import com.simplecityapps.localmediaprovider.local.data.room.dao.SongDataDao
import com.simplecityapps.localmediaprovider.local.data.room.entity.toSongData
import com.simplecityapps.localmediaprovider.local.data.room.entity.toSongDataUpdate
import com.simplecityapps.mediaprovider.MediaProvider
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.mediaprovider.repository.SongQuery
import com.simplecityapps.mediaprovider.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber

class LocalSongRepository(
    val scope: CoroutineScope,
    private val songDataDao: SongDataDao
) : SongRepository {

    private val songsRelay: StateFlow<List<Song>?> by lazy {
        songDataDao
            .getAll()
            .flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Lazily, null)
    }

    override fun getSongs(query: SongQuery): Flow<List<Song>?> {
        return songsRelay
            .map { songs ->
                var result = songs

                if (!query.includeExcluded) {
                    result = songs?.filterNot { it.blacklisted }
                }

                query.providerType?.let { providerType ->
                    result = songs?.filter { song -> song.mediaProvider == providerType }
                }

                result
                    ?.filter(query.predicate)
                    ?.sortedWith(query.sortOrder.comparator)
            }
    }

    override suspend fun insert(songs: List<Song>, mediaProviderType: MediaProvider.Type) {
        songDataDao.insert(songs.toSongData(mediaProviderType))
    }

    override suspend fun update(songs: List<Song>) {
        songDataDao.update(songs.toSongDataUpdate())
    }

    override suspend fun update(song: Song): Int {
        return songDataDao.update(song.toSongDataUpdate())
    }

    override suspend fun remove(song: Song) {
        Timber.v("Deleting song")
        songDataDao.delete(song.id)
    }

    override suspend fun removeAll(mediaProviderType: MediaProvider.Type) {
        songDataDao.deleteAll(mediaProviderType)
    }

    override suspend fun insertUpdateAndDelete(inserts: List<Song>, updates: List<Song>, deletes: List<Song>, mediaProviderType: MediaProvider.Type): Triple<Int, Int, Int> {
        return songDataDao.insertUpdateAndDelete(inserts.toSongData(mediaProviderType), updates.toSongDataUpdate(), deletes.toSongData(mediaProviderType))
    }

    override suspend fun incrementPlayCount(song: Song) {
        Timber.v("Incrementing play count for song: ${song.name}")
        songDataDao.incrementPlayCount(song.id)
    }

    override suspend fun setPlaybackPosition(song: Song, playbackPosition: Int) {
        Timber.v("Setting playback position to $playbackPosition for song: ${song.name}")
        songDataDao.updatePlaybackPosition(song.id, playbackPosition)
    }

    override suspend fun setExcluded(songs: List<Song>, excluded: Boolean) {
        val count = songDataDao.setExcluded(songs.map { it.id }, excluded)
        Timber.v("$count song(s) excluded")
    }

    override suspend fun clearExcludeList() {
        Timber.v("Clearing excluded")
        songDataDao.clearExcludeList()
    }
}