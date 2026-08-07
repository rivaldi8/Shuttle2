package com.simplecityapps.localmediaprovider.local.data.room

import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Exports the app's database to the provided [destinationUri].
 *
 * @throws FileNotFoundException if the database file does not exist.
 * @throws IOException if an error occurs during the copy process.
 */
fun exportDatabase(database: RoomDatabase, context: Context, destinationUri: Uri) {
    val databaseName = database.openHelper.databaseName!!
    val dbFile = context.getDatabasePath(databaseName)
    if (!dbFile.exists()) {
        throw FileNotFoundException("Database file '$databaseName' not found")
    }

    // Step 1: Copy to a temporary file in the local filesystem
    val tempFile = File.createTempFile("${databaseName}_export", ".tmp", context.cacheDir)
    // Perform checkpoint to merge WAL files into the main database file
    walCheckpoint(database)

    try {
        copyStreams(dbFile.inputStream(), tempFile.outputStream())

        // Step 2: Copy the temporary file to the final destination
        val finalDestinationStream = context.contentResolver.openOutputStream(destinationUri)
            ?: throw IOException("Could not open output stream for URI: $destinationUri")
        copyStreams(tempFile.inputStream(), finalDestinationStream)
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}

private fun copyStreams(source: InputStream, destination: OutputStream) {
    source.use { input ->
        destination.use { output ->
            input.copyTo(output)
        }
    }
}

private fun walCheckpoint(database: RoomDatabase) {
    database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { cursor ->
        cursor.moveToFirst()
    }
}
