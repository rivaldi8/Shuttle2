package com.simplecityapps.shuttle.ui.screens.settings.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.simplecityapps.localmediaprovider.local.data.room.BackupRestoreError
import com.simplecityapps.localmediaprovider.local.data.room.database.MediaDatabase
import com.simplecityapps.localmediaprovider.local.data.room.exportDatabase
import com.simplecityapps.localmediaprovider.local.data.room.importDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val database: MediaDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = BackupRestoreUiState.Loading
            try {
                exportDatabase(context, database, uri)
                _uiState.value = BackupRestoreUiState.ExportSuccess
            } catch (e: BackupRestoreError) {
                reportError(e)
                _uiState.value = BackupRestoreUiState.Error(e)
            } catch (e: Exception) {
                val wrappedError = BackupRestoreError.IO(e)
                reportError(wrappedError)
                _uiState.value = BackupRestoreUiState.Error(wrappedError)
            }
        }
    }

    fun restoreDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = BackupRestoreUiState.Loading
            try {
                importDatabase(context, uri, database)
                _uiState.value = BackupRestoreUiState.RestoreSuccess
            } catch (e: BackupRestoreError) {
                reportError(e)
                _uiState.value = BackupRestoreUiState.Error(e)
            } catch (e: Exception) {
                val wrappedError = BackupRestoreError.IO(e)
                reportError(wrappedError)
                _uiState.value = BackupRestoreUiState.Error(wrappedError)
            }
        }
    }

    fun resetState() {
        _uiState.value = BackupRestoreUiState.Idle
    }

    private fun reportError(error: BackupRestoreError) {
        Firebase.crashlytics.apply {
            recordException(error)
            setCustomKey("error_type", error.javaClass.simpleName)
            when (error) {
                is BackupRestoreError.VersionMismatch -> {
                    setCustomKey("imported_version", error.imported)
                    setCustomKey("current_version", error.current)
                }
                is BackupRestoreError.SizeMismatch -> {
                    setCustomKey("expected_size", error.expected)
                    setCustomKey("actual_size", error.actual)
                }
                else -> {}
            }
        }
    }
}
