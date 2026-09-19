package com.simplecityapps.shuttle.ui.screens.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.common.components.CircularLoadingState
import com.simplecityapps.shuttle.ui.common.components.LoadingStatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    uiState: BackupRestoreUiState,
    onBackUpClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
) {
    val isOperationInProgress = uiState is BackupRestoreUiState.InProgress

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(id = R.string.settings_menu_backup_database)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_content_copy),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = !isOperationInProgress)
                        { onBackUpClick() },
                )
                ListItem(
                    headlineContent = { Text(stringResource(id = R.string.settings_menu_restore_database)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = !isOperationInProgress)
                        { onRestoreClick() },
                )
            }

            if (isOperationInProgress) {
                val messageKey = if (uiState is BackupRestoreUiState.BackupInProgress) {
                    R.string.settings_menu_backup_database
                } else {
                    R.string.settings_menu_restore_database
                }
                LoadingStatusIndicator(
                    state = CircularLoadingState.Loading(stringResource(id = messageKey)),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
