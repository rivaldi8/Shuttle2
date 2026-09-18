package com.simplecityapps.shuttle.ui.screens.settings.screens

import com.simplecityapps.localmediaprovider.local.data.room.BackupRestoreError

sealed class BackupRestoreUiState {
    data object Idle : BackupRestoreUiState()
    data object Loading : BackupRestoreUiState()
    data object BackupInProgress : BackupRestoreUiState()
    data object RestoreInProgress : BackupRestoreUiState()
    data object BackupSuccess : BackupRestoreUiState()
    data object RestoreSuccess : BackupRestoreUiState()
    data class Error(val error: BackupRestoreError) : BackupRestoreUiState()
}
