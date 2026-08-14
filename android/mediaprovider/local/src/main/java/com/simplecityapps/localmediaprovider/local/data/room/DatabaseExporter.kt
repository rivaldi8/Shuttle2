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
fun exportDatabase(context: Context, database: RoomDatabase, destinationUri: Uri) {
    // Step 1: Copy to a temporary file in the local filesystem
    val tempDatabaseExportFile = createTemporaryFile(context.cacheDir)
    // Perform checkpoint to merge WAL files into the main database file
    database.performWalCheckpoint()

    try {
        copyStream(
            database.getInputStream(context),
            tempDatabaseExportFile.outputStream()
        )
        verifyDatabaseIntegrity(tempDatabaseExportFile)

        // Step 2: Copy the temporary file to the final destination
        val finalDestinationStream = context.contentResolver.openOutputStream(destinationUri)
            ?: throw IOException("Could not open output stream for URI: $destinationUri")
        copyStream(tempDatabaseExportFile.inputStream(), finalDestinationStream)

        verifyExportedSize(context, destinationUri, expectedSize = tempDatabaseExportFile.length())
    } finally {
        tempDatabaseExportFile.delete()
    }
}

/**
 * Restores the app's database from the provided [sourceUri].
 *
 * @throws IOException if an error occurs during the copy process.
 */
fun importDatabase(context: Context, sourceUri: Uri, database: RoomDatabase) {
    val databaseDirectory = database.getDirectory(context)
        ?: throw IOException("Database directory not found")

    // Step 1: Copy from the source URI to a temporary file in the local filesystem
    val tempDatabaseImportFile = createTemporaryFile(databaseDirectory)
    val sourceStream = context.contentResolver.openInputStream(sourceUri)
        ?: throw IOException("Could not open input stream for URI: $sourceUri")
    copyStream(sourceStream, tempDatabaseImportFile.outputStream())

    validateDatabaseForImport(tempDatabaseImportFile, database)

    // Step 2: Replace the existing database file
    database.closeAndDelete()

    if (!tempDatabaseImportFile.renameTo(database.getFile(context))) {
        throw IOException("Failed to rename restored database file")
    }
}

// Private Helpers

private fun RoomDatabase.getInputStream(context: Context): InputStream {
    val databaseFile = getFile(context)
    if (!databaseFile.exists()) {
        throw FileNotFoundException("Database file '${databaseFile.name}' not found")
    }

    return databaseFile.inputStream()
}

private fun RoomDatabase.getDirectory(context: Context): File? {
    val databaseFile = getFile(context)
    return databaseFile.parentFile
}

private fun RoomDatabase.getFile(context: Context): File {
    val name = openHelper.databaseName ?: throw IOException("Database name not found")
    return context.getDatabasePath(name)
}

private fun createTemporaryFile(directory: File): File = File.createTempFile("database_export", ".tmp", directory)

private fun copyStream(source: InputStream, destination: OutputStream) {
    source.use { input ->
        destination.use { output ->
            input.copyTo(output)
        }
    }
}

private fun RoomDatabase.performWalCheckpoint() {
    query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { cursor ->
        cursor.moveToFirst()
    }
}

private fun RoomDatabase.closeAndDelete() {
    val databasePath = openHelper.writableDatabase.path
        ?: throw IOException("Database not found")

    close()

    File(databasePath).delete()
    File("$databasePath-wal").delete()
    File("$databasePath-shm").delete()
}

private fun validateDatabaseForImport(databaseFile: File, currentDatabase: RoomDatabase) {
    SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        if (!db.isDatabaseIntegrityOk) {
            throw IOException("Imported database integrity check failed")
        }

        val currentVersion = currentDatabase.openHelper.readableDatabase.version
        if (db.version > currentVersion) {
            throw IOException("Cannot import a database with a newer version (Imported: ${db.version}, Current: $currentVersion)")
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

private fun verifyExportedSize(context: Context, fileUri: Uri, expectedSize: Long) {
    val size = context.contentResolver.query(
        fileUri,
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
