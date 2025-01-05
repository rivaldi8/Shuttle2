package com.simplecityapps.shuttle.model
import android.net.Uri

interface DatabaseBackupRestore {
    suspend fun backUp(destinationUri: Uri)
    suspend fun restore(sourceUri: Uri)
}
