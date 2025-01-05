package com.simplecityapps.shuttle.ui.actions

import com.simplecityapps.shuttle.model.DatabaseBackupRestore
import dev.zacsweers.metro.Inject
import android.net.Uri

@Inject
class RestoreDatabase(
    private val databaseBackupRestore: DatabaseBackupRestore,
) {
    suspend operator fun invoke(sourceUri: Uri) {
        databaseBackupRestore.restore(sourceUri)
    }
}
