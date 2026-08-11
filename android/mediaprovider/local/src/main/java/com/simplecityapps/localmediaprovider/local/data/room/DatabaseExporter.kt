package com.simplecityapps.localmediaprovider.local.data.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
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
    val databaseFile = context.getDatabasePath(databaseName)
    if (!databaseFile.exists()) {
        throw FileNotFoundException("Database file '$databaseName' not found")
    }

    // Step 1: Copy to a temporary file in the local filesystem
    val tempFile = createTempFileIn(context.cacheDir)
    // Perform checkpoint to merge WAL files into the main database file
    walCheckpoint(database)

    try {
        copyStream(databaseFile.inputStream(), tempFile.outputStream())
        verifyDatabaseIntegrity(tempFile)

        // Step 2: Copy the temporary file to the final destination
        val finalDestinationStream = context.contentResolver.openOutputStream(destinationUri)
            ?: throw IOException("Could not open output stream for URI: $destinationUri")
        copyStream(tempFile.inputStream(), finalDestinationStream)

        verifyFileSize(context, tempFile.length(), destinationUri)
    } finally {
        tempFile.delete()
    }
}

/**
 * Restores the app's database from the provided [sourceUri].
 *
 * @throws IOException if an error occurs during the copy process.
 */
fun importDatabase(database: RoomDatabase, context: Context, sourceUri: Uri) {
    val databaseName = database.openHelper.databaseName!!
    val databaseFile = context.getDatabasePath(databaseName)
    val databaseDirectory = databaseFile.parentFile
        ?: throw IOException("Database without directory")

    // Step 1: Copy from the source URI to a temporary file in the local filesystem
    val tempFile = createTempFileIn(databaseDirectory)
    val sourceStream = context.contentResolver.openInputStream(sourceUri)
        ?: throw IOException("Could not open input stream for URI: $sourceUri")
    copyStream(sourceStream, tempFile.outputStream())

    verifyDatabaseToImport(tempFile, database)

    // Step 2: Replace the existing database file
    database.close()
    deleteDatabaseAndWalFiles(databaseFile)

    tempFile.renameTo(databaseFile)
}

private fun deleteDatabaseAndWalFiles(databaseFile: File) {
    File(databaseFile.path + "-wal").delete()
    File(databaseFile.path + "-shm").delete()
    databaseFile.delete()
}

private fun createTempFileIn(directory: File): File = File.createTempFile("database_export", ".tmp", directory)

private fun copyStream(source: InputStream, destination: OutputStream) {
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

fun verifyDatabaseToImport(databaseFile: File, database: RoomDatabase) {
    verifyDatabaseIntegrity(databaseFile)

    // FIXME: Code duplication
    SQLiteDatabase.openDatabase(
        databaseFile.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY
    ).use { databaseToImport ->
        if (databaseToImport.version > database.openHelper.readableDatabase.version) {
            throw IOException("Trying to import a database with a newer version")
        }
    }
}

fun verifyDatabaseIntegrity(databaseFile: File) {
    SQLiteDatabase.openDatabase(
        databaseFile.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY
    ).use { database ->
        database.isDatabaseIntegrityOk

        if (!database.isDatabaseIntegrityOk) {
            throw IOException("Database integrity check failed")
        }
    }
}

private fun verifyFileSize(context: Context, expectedSize: Long, uri: Uri) {
    val size = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                null
            }
        } else {
            null
        }
    }

    if (size != expectedSize) {
        throw IOException("Exported file size mismatch: expected $expectedSize, got $size")
    }
}

