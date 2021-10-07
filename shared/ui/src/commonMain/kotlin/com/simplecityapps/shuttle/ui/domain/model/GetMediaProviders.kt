package com.simplecityapps.shuttle.domain.model

import com.simplecityapps.shuttle.inject.Inject
import com.simplecityapps.shuttle.model.MediaProviderType
import com.simplecityapps.shuttle.preferences.GeneralPreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GetMediaProviders @Inject constructor(
    private val preferenceManager: GeneralPreferenceManager
) {
    operator fun invoke(
        default: List<MediaProviderType> = emptyList()
    ): Flow<List<MediaProviderType>> = preferenceManager.getMediaProviders().map { mediaProviders -> mediaProviders ?: default }
}