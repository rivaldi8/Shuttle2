package com.simplecityapps.localmediaprovider.local.data.room

import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Exports the app's database to the provided [destinationUri].
 *
 * @throws FileNotFoundException if the database file does not exist.
 * @throws IOException if an error occurs during the copy process.
 */
fun exportDatabase(database: RoomDatabase, context: Context, destinationUri: Uri) {
    // Perform checkpoint to merge WAL file into the main database file
    database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { cursor ->
        cursor.moveToFirst()
    }

    val dbFile = context.getDatabasePath(DATABASE_NAME)
    if (!dbFile.exists()) {
        throw FileNotFoundException("Database file $DATABASE_NAME not found")
    }

    // Step 1: Copy to a temporary file in the local filesystem
    val tempFile = File.createTempFile(DATABASE_NAME, ".tmp", context.cacheDir)
    try {
        dbFile.inputStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Step 2: Copy the temporary file to the final destination
        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IOException("Could not open output stream for URI: $destinationUri")
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
