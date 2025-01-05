package com.simplecityapps.shuttle.ui.screens.settings.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplecityapps.shuttle.model.BackupRestoreError
import com.simplecityapps.shuttle.ui.actions.BackUpDatabase
import com.simplecityapps.shuttle.ui.actions.RestoreDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@ViewModelKey(BackupRestoreViewModel::class)
@ContributesIntoMap(AppScope::class)
class BackupRestoreViewModel @Inject constructor(
    private val backUpDatabaseUseCase: BackUpDatabase,
    private val restoreDatabaseUseCase: RestoreDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun backUpDatabase(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = BackupRestoreUiState.BackupInProgress
            try {
                backUpDatabaseUseCase(uri)
                _uiState.value = BackupRestoreUiState.BackupSuccess
            } catch (e: BackupRestoreError) {
                _uiState.value = BackupRestoreUiState.Error(e)
            } catch (e: Exception) {
                val wrappedError = BackupRestoreError.IO(e)
                _uiState.value = BackupRestoreUiState.Error(wrappedError)
            }
        }
    }

    fun restoreDatabase(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = BackupRestoreUiState.RestoreInProgress
            try {
                restoreDatabaseUseCase(uri)
                _uiState.value = BackupRestoreUiState.RestoreSuccess
            } catch (e: BackupRestoreError) {
                _uiState.value = BackupRestoreUiState.Error(e)
            } catch (e: Exception) {
                val wrappedError = BackupRestoreError.IO(e)
                _uiState.value = BackupRestoreUiState.Error(wrappedError)
            }
        }
    }

    fun resetState() {
        _uiState.value = BackupRestoreUiState.Idle
    }
}

sealed class BackupRestoreUiState {
    data object Idle : BackupRestoreUiState()

    sealed class InProgress : BackupRestoreUiState()
    data object BackupInProgress : InProgress()
    data object RestoreInProgress : InProgress()

    data object BackupSuccess : BackupRestoreUiState()
    data object RestoreSuccess : BackupRestoreUiState()

    data class Error(val error: BackupRestoreError) : BackupRestoreUiState()
}
