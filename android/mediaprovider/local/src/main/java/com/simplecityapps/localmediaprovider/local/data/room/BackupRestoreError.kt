package com.simplecityapps.localmediaprovider.local.data.room

sealed class BackupRestoreError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    sealed class ExportError(message: String, cause: Throwable? = null) : BackupRestoreError(message, cause) {
        data class IO(override val cause: Throwable) : ExportError("IO error during export", cause)
        data class IntegrityCheckFailed(override val cause: Throwable? = null) : ExportError("Database integrity check failed", cause)
        data class SizeMismatch(val expected: Long, val actual: Long) : ExportError("Exported file size mismatch: expected $expected, got $actual")
    }

    sealed class ImportError(message: String, cause: Throwable? = null) : BackupRestoreError(message, cause) {
        data class IO(override val cause: Throwable) : ImportError("IO error during import", cause)
        data class IntegrityCheckFailed(override val cause: Throwable? = null) : ImportError("Imported database integrity check failed", cause)
        data class VersionMismatch(val imported: Int, val current: Int) : ImportError("Cannot import a database from a newer app version (Imported: $imported, Current: $current)")
    }
}
