package com.simplecityapps.localmediaprovider.local.data.room

sealed class BackupRestoreError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    data class IO(
        override val cause: Throwable,
    ) : BackupRestoreError(
        "IO error during export",
        cause,
    )
    data class IntegrityCheckFailed(
        override val cause: Throwable? = null,
    ) : BackupRestoreError(
        "Database integrity check failed",
        cause,
    )
    data class SizeMismatch(
        val expected: Long,
        val actual: Long,
    ) : BackupRestoreError(
        "Exported file size mismatch: expected $expected, got $actual",
    )
    data class VersionMismatch(
        val imported: Int,
        val current: Int,
    ) : BackupRestoreError(
        "Cannot import a database from a newer app version (Imported: $imported, Current: $current)",
    )
}
