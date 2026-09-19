package com.simplecityapps.shuttle.ui.screens.settings.screens

import com.simplecityapps.localmediaprovider.local.data.room.BackupRestoreError

sealed class BackupRestoreUiState {
    data object Idle : BackupRestoreUiState()

    sealed class InProgress : BackupRestoreUiState()
    data object BackupInProgress : InProgress()
    data object RestoreInProgress : InProgress()

    data object BackupSuccess : BackupRestoreUiState()
    data object RestoreSuccess : BackupRestoreUiState()

    data class Error(val error: BackupRestoreError) : BackupRestoreUiState()
}
