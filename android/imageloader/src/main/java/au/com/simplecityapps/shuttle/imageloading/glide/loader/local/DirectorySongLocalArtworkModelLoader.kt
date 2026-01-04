package au.com.simplecityapps.shuttle.imageloading.glide.loader.local

import android.content.Context
import au.com.simplecityapps.shuttle.imageloading.glide.loader.common.SongArtworkProvider
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.simplecityapps.shuttle.model.Song
import java.io.InputStream

class DirectorySongLocalArtworkModelLoader(
    private val context: Context,
    private val localArtworkModelLoader: LocalArtworkModelLoader
) : ModelLoader<Song, InputStream> {
    override fun buildLoadData(
        model: Song,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? = localArtworkModelLoader.buildLoadData(DirectorySongLocalArtworkProvider(context, model), width, height, options)

    override fun handles(model: Song): Boolean = true

    class Factory(val context: Context) : ModelLoaderFactory<Song, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Song, InputStream> = DirectorySongLocalArtworkModelLoader(context, multiFactory.build(LocalArtworkProvider::class.java, InputStream::class.java) as LocalArtworkModelLoader)

        override fun teardown() {
        }
    }

    class DirectorySongLocalArtworkProvider(
        private val context: Context,
        song: Song
    ) : SongArtworkProvider(song),
        LocalArtworkProvider {
        override fun getInputStream(): InputStream? {
            return findLocalArtwork(context, song.path)
        }
    }
}
