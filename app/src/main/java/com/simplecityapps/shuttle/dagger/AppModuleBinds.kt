package com.simplecityapps.shuttle.dagger

import com.simplecityapps.shuttle.appinitializers.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModuleBinds {

    @Binds
    @IntoSet
    abstract fun provideBugsnagInitializer(bind: CrashReportingInitializer): AppInitializer

    @Binds
    @IntoSet
    abstract fun provideTimberInitializer(bind: TimberInitializer): AppInitializer

    @Binds
    @IntoSet
    abstract fun providePlaybackInitializer(bind: PlaybackInitializer): AppInitializer

    @Binds
    @IntoSet
    abstract fun provideWidgetInitializer(bind: WidgetInitializer): AppInitializer

    @Binds
    @IntoSet
    abstract fun provideMediaProviderInitializer(bind: MediaProviderInitializer): AppInitializer
}