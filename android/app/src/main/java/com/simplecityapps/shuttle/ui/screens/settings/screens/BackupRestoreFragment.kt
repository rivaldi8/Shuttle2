package com.simplecityapps.shuttle.ui.screens.settings.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.simplecityapps.localmediaprovider.local.data.room.BackupRestoreError
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.persistence.GeneralPreferenceManager
import com.simplecityapps.shuttle.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class BackupRestoreFragment : Fragment() {

    private val viewModel: BackupRestoreViewModel by viewModels()

    @Inject
    lateinit var preferenceManager: GeneralPreferenceManager

    private val backupDatabaseLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3"),
    ) { uri ->
        uri?.let {
            viewModel.backUpDatabase(it)
        }
    }

    private val restoreDatabaseLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.restoreDatabase(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        viewModel.uiState
            .onEach { state ->
                when (state) {
                    is BackupRestoreUiState.BackupSuccess -> {
                        Toast.makeText(requireContext(), R.string.settings_backup_success, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    is BackupRestoreUiState.RestoreSuccess -> {
                        Toast.makeText(requireContext(), R.string.settings_restore_success, Toast.LENGTH_LONG).show()
                        restartApp()
                    }
                    is BackupRestoreUiState.Error -> {
                        val message = when (val error = state.error) {
                            is BackupRestoreError.VersionMismatch -> getString(R.string.settings_restore_failed, "Newer database version (Restored: ${error.restored}, Current: ${error.current})")
                            is BackupRestoreError.IntegrityCheckFailed -> getString(R.string.settings_restore_failed, "Database integrity check failed")
                            else -> getString(R.string.settings_restore_failed, state.error.message)
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        setContent {
            val theme by preferenceManager.theme(viewLifecycleOwner.lifecycleScope).collectAsStateWithLifecycle()
            val accent by preferenceManager.accent(viewLifecycleOwner.lifecycleScope).collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AppTheme(theme = theme, accent = accent) {
                BackupRestoreScreen(
                    uiState = uiState,
                    onBackUpClick = {
                        val today = LocalDate.now()
                        backupDatabaseLauncher.launch("s2-backup-$today.db")
                    },
                    onRestoreClick = {
                        restoreDatabaseLauncher.launch(
                            arrayOf(
                                "application/x-sqlite3",
                                "application/octet-stream",
                                "*/*",
                            ),
                        )
                    },
                    onBackClick = { findNavController().popBackStack() },
                )
            }
        }
    }

    private fun restartApp() {
        val context = requireContext()
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.setPackage(context.packageName)
        context.startActivity(mainIntent)
        exitProcess(0)
    }
}

