package com.simplecityapps.shuttle.ui.screens.settings.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class BackupRestoreFragment : Fragment() {

    private val viewModel: BackupRestoreViewModel by viewModels()

    @Inject
    lateinit var preferenceManager: GeneralPreferenceManager

    private val exportDatabaseLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3"),
    ) { uri ->
        uri?.let {
            viewModel.exportDatabase(it)
        }
    }

    private val restoreDatabaseLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.restoreDatabase(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.uiState
            .onEach { state ->
                when (state) {
                    is BackupRestoreUiState.ExportSuccess -> {
                        Toast.makeText(requireContext(), R.string.settings_export_success, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    is BackupRestoreUiState.RestoreSuccess -> {
                        Toast.makeText(requireContext(), R.string.settings_restore_success, Toast.LENGTH_LONG).show()
                        restartApp()
                    }
                    is BackupRestoreUiState.Error -> {
                        val message = when (val error = state.error) {
                            is BackupRestoreError.ImportError.VersionMismatch -> getString(R.string.settings_restore_failed, "Newer database version (Imported: ${error.imported}, Current: ${error.current})")
                            is BackupRestoreError.ImportError.IntegrityCheckFailed, is BackupRestoreError.ExportError.IntegrityCheckFailed -> getString(R.string.settings_restore_failed, "Database integrity check failed")
                            else -> getString(R.string.settings_restore_failed, state.error.message)
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val theme by preferenceManager.theme(viewLifecycleOwner.lifecycleScope).collectAsStateWithLifecycle()
            val accent by preferenceManager.accent(viewLifecycleOwner.lifecycleScope).collectAsStateWithLifecycle()

            AppTheme(theme = theme, accent = accent) {
                BackupRestoreScreen(
                    onExportClick = {
                        val today = LocalDate.now()
                        exportDatabaseLauncher.launch("s2-backup-$today.db")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onExportClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.pref_category_title_backup_restore)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.settings_menu_export_database)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_content_copy),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable { onExportClick() },
            )
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.settings_menu_restore_database)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable { onRestoreClick() },
            )
        }
    }
}
