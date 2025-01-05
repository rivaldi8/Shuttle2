package com.simplecityapps.shuttle.ui.screens.settings.screens

import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.designsystem.component.LinkSetting
import com.simplecityapps.shuttle.designsystem.component.SettingsGroup
import com.simplecityapps.shuttle.ui.common.components.CircularLoadingState
import com.simplecityapps.shuttle.ui.common.components.LoadingStatusIndicator
import com.simplecityapps.shuttle.ui.screens.settings.SettingsScaffold

@Composable
fun BackupRestoreScreen(
    uiState: BackupRestoreUiState,
    onBackUpClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isProgressState = uiState is BackupRestoreUiState.InProgress

    SettingsScaffold(
        title = stringResource(R.string.pref_category_title_backup_restore),
        onNavigateUp = onNavigateUp,
        modifier = modifier
    ) {
        item(key = "backup_restore_group") {
            SettingsGroup(
                rows = listOf(
                    { shapes: ListItemShapes ->
                        LinkSetting(
                            title = stringResource(R.string.settings_menu_backup_database),
                            enabled = !isProgressState,
                            onClick = onBackUpClick,
                            shapes = shapes
                        )
                    },
                    { shapes: ListItemShapes ->
                        LinkSetting(
                            title = stringResource(R.string.settings_menu_restore_database),
                            enabled = !isProgressState,
                            onClick = onRestoreClick,
                            shapes = shapes
                        )
                    }
                )
            )
        }

        if (isProgressState) {
            item(key = "progress_indicator") {
                val message = when (uiState) {
                    BackupRestoreUiState.BackupInProgress -> stringResource(R.string.database_backup_in_progress)
                    BackupRestoreUiState.RestoreInProgress -> stringResource(R.string.database_restore_in_progress)
                }
                LoadingStatusIndicator(
                    state = CircularLoadingState.Loading(message)
                )
            }
        }
    }
}
