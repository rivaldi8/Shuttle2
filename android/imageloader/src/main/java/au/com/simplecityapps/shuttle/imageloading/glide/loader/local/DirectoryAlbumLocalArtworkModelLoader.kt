package au.com.simplecityapps.shuttle.imageloading.glide.loader.local

import android.content.Context
import au.com.simplecityapps.shuttle.imageloading.glide.loader.common.AlbumArtworkProvider
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.simplecityapps.mediaprovider.repository.songs.SongRepository
import com.simplecityapps.shuttle.model.Album
import com.simplecityapps.shuttle.query.SongQuery
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.io.InputStream

class DirectoryAlbumLocalArtworkModelLoader(
    private val context: Context,
    private val localArtworkModelLoader: LocalArtworkModelLoader,
    private val songRepository: SongRepository
) : ModelLoader<Album, InputStream> {
    override fun buildLoadData(
        model: Album,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> = localArtworkModelLoader.buildLoadData(DirectoryAlbumLocalArtworkProvider(context, model, songRepository), width, height, options)

    override fun handles(model: Album): Boolean = true

    class Factory(
        private val context: Context,
        private val songRepository: SongRepository
    ) : ModelLoaderFactory<Album, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Album, InputStream> = DirectoryAlbumLocalArtworkModelLoader(context, multiFactory.build(LocalArtworkProvider::class.java, InputStream::class.java) as LocalArtworkModelLoader, songRepository)

        override fun teardown() {
        }
    }

    class DirectoryAlbumLocalArtworkProvider(
        private val context: Context,
        private val album: Album,
        private val songRepository: SongRepository
    ) : AlbumArtworkProvider(album),
        LocalArtworkProvider {
        override fun getInputStream(): InputStream? = runBlocking {
            songRepository.getSongs(SongQuery.AlbumGroupKeys(listOf(SongQuery.AlbumGroupKey(album.groupKey))))
                .firstOrNull()
                ?.firstOrNull()
                ?.let { song ->
                    LocalArtworkFinder(context, song.path).find()
                }
        }
    }
}
