package com.simplecityapps.shuttle.dagger

import com.simplecityapps.playback.dagger.PlaybackModule
import com.simplecityapps.provider.emby.di.EmbyMediaProviderModule
import com.simplecityapps.provider.jellyfin.di.JellyfinMediaProviderModule
import com.simplecityapps.provider.plex.di.PlexMediaProviderModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        AppAssistedModule::class,
        AppModule::class,
        PlaybackModule::class,
        RepositoryModule::class,
        NetworkingModule::class,
        PersistenceModule::class,
        MediaProviderModule::class,
        EmbyMediaProviderModule::class,
        JellyfinMediaProviderModule::class,
        PlexMediaProviderModule::class,
        ImageLoaderModule::class
    ]
)
interface AppComponent {}