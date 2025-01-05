package com.simplecityapps.shuttle.ui.actions

import com.simplecityapps.shuttle.model.DatabaseBackupRestore
import dev.zacsweers.metro.Inject
import android.net.Uri

@Inject
class BackUpDatabase(
    private val databaseBackupRestore: DatabaseBackupRestore,
) {
    suspend operator fun invoke(destinationUri: Uri) {
        databaseBackupRestore.backUp(destinationUri)
    }
}
