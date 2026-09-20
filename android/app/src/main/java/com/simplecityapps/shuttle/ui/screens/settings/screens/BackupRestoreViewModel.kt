package com.simplecityapps.shuttle.ui.screens.settings.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplecityapps.localmediaprovider.local.data.room.BackupRestoreError
import com.simplecityapps.localmediaprovider.local.data.room.backUpDatabase
import com.simplecityapps.localmediaprovider.local.data.room.database.MediaDatabase
import com.simplecityapps.localmediaprovider.local.data.room.restoreDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val database: MediaDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun backUpDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = BackupRestoreUiState.BackupInProgress
            delay(2000L.milliseconds)
            try {
                backUpDatabase(context, database, uri)
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = BackupRestoreUiState.RestoreInProgress
            try {
                restoreDatabase(context, uri, database)
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
