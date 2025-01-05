package com.simplecityapps.localmediaprovider.local.data.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import com.simplecityapps.localmediaprovider.local.data.room.database.MediaDatabase
import com.simplecityapps.shuttle.di.ApplicationContext
import com.simplecityapps.shuttle.model.BackupRestoreError
import com.simplecityapps.shuttle.model.BackupRestoreError.IO
import com.simplecityapps.shuttle.model.DatabaseBackupRestore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@SingleIn(AppScope::class)
@Inject
class DatabaseBackupRestoreManager(
    private val database: MediaDatabase,
    @ApplicationContext private val context: Context,
) : DatabaseBackupRestore {
    override suspend fun backUp(destinationUri: Uri) {
        // Step 1: Copy to a temporary file in the local filesystem
        val tempDatabaseBackupFile = createTemporaryFile(context.cacheDir)
        // Merge changes from auxiliary Write-Ahead Log files into the main database file
        database.performWalCheckpoint()
        try {
            copyStream(
                database.getInputStream(context),
                tempDatabaseBackupFile.outputStream()
            )
            verifyDatabaseIntegrity(tempDatabaseBackupFile)

            // Step 2: Copy the temporary file to the final destination
            val finalDestinationStream = context.contentResolver.openOutputStream(destinationUri)
                ?: throw IO(IOException("Could not open output stream for URI: $destinationUri"))
            copyStream(tempDatabaseBackupFile.inputStream(), finalDestinationStream)

            verifyBackupSize(context, destinationUri, expectedSize = tempDatabaseBackupFile.length())
        } catch (e: BackupRestoreError) {
            throw e
        } catch (e: Exception) {
            throw IO(e)
        } finally {
            tempDatabaseBackupFile.delete()
        }
    }

    override suspend fun restore(sourceUri: Uri) {
        val databaseDirectory = database.getDirectory(context)
            ?: throw IO(IOException("Database directory not found"))
        // Step 1: Copy from the source URI to a temporary file in the local filesystem
        val tempDatabaseRestoreFile = createTemporaryFile(databaseDirectory)
        try {
            val sourceStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IO(IOException("Could not open input stream for URI: $sourceUri"))
            copyStream(sourceStream, tempDatabaseRestoreFile.outputStream())

            validateDatabaseForRestore(tempDatabaseRestoreFile, database)

            // Step 2: Replace the existing database file
            database.closeAndDelete()

            if (!tempDatabaseRestoreFile.renameTo(database.getFile(context))) {
                throw IO(IOException("Failed to rename restored database file"))
            }
        } catch (e: BackupRestoreError) {
            tempDatabaseRestoreFile.delete()
            throw e
        } catch (e: Exception) {
            tempDatabaseRestoreFile.delete()
            throw IO(e)
        }
    }

    private fun createTemporaryFile(directory: File): File = File.createTempFile("database_backup", ".tmp", directory)

    private fun copyStream(source: InputStream, destination: OutputStream) {
        source.use { input ->
            destination.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun validateDatabaseForRestore(databaseFile: File, currentDatabase: RoomDatabase) {
        openSQLiteDatabase(databaseFile).use { db ->
            if (!db.isDatabaseIntegrityOk) {
                throw BackupRestoreError.IntegrityCheckFailed()
            }

            val currentVersion = currentDatabase.openHelper.readableDatabase.version
            if (db.version > currentVersion) {
                throw BackupRestoreError.VersionMismatch(restored = db.version, current = currentVersion)
            }
        }
    }

    private fun verifyDatabaseIntegrity(databaseFile: File) {
        openSQLiteDatabase(databaseFile).use { database ->
            if (!database.isDatabaseIntegrityOk) {
                throw BackupRestoreError.IntegrityCheckFailed()
            }
        }
    }

    private fun openSQLiteDatabase(databaseFile: File): SQLiteDatabase = SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

    private fun verifyBackupSize(context: Context, fileUri: Uri, expectedSize: Long) {
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
            throw BackupRestoreError.SizeMismatch(expected = expectedSize, actual = size ?: 0L)
        }
    }

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
        val name = openHelper.databaseName ?: throw BackupRestoreError.IO(IOException("Database name not found"))
        return context.getDatabasePath(name)
    }

    private fun RoomDatabase.performWalCheckpoint() {
        query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun RoomDatabase.closeAndDelete() {
        val databasePath = openHelper.writableDatabase.path
            ?: throw BackupRestoreError.IO(IOException("Database not found"))

        close()

        File(databasePath).delete()
        File("$databasePath-wal").delete()
        File("$databasePath-shm").delete()
    }
}
