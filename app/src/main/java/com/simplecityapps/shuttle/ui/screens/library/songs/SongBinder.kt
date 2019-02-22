package com.simplecityapps.shuttle.ui.screens.library.songs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import au.com.simplecityapps.shuttle.imageloading.ArtworkImageLoader
import com.simplecityapps.adapter.ViewBinder
import com.simplecityapps.mediaprovider.model.Song
import com.simplecityapps.shuttle.R

class SongBinder(val song: Song, val imageLoader: ArtworkImageLoader) : ViewBinder {

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_song, parent, false))
    }

    override fun viewType(): ViewBinder.ViewType {
        return ViewBinder.ViewType.Song
    }

    override fun sectionName(): String? {
        return song.name.firstOrNull().toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SongBinder) return false

        if (song != other.song) return false

        return true
    }

    override fun hashCode(): Int {
        return song.hashCode()
    }


    class ViewHolder(itemView: View) : ViewBinder.ViewHolder<SongBinder>(itemView) {

        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)
        private val imageView = itemView.findViewById<ImageView>(R.id.imageView)

        override fun bind(viewBinder: SongBinder) {
            super.bind(viewBinder)

            title.text = viewBinder.song.name
            subtitle.text = "${viewBinder.song.albumArtistName} • ${viewBinder.song.albumName}"
            viewBinder.imageLoader.loadArtwork(imageView, viewBinder.song, ArtworkImageLoader.Options.RoundedCorners(16))
        }

        override fun recycle() {
            viewBinder?.imageLoader?.clear(imageView)
        }
    }
}