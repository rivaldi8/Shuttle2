package com.simplecityapps.shuttle.ui.screens.settings.screens

import android.net.Uri
import com.simplecityapps.shuttle.model.BackupRestoreError
import com.simplecityapps.shuttle.ui.actions.BackUpDatabase
import com.simplecityapps.shuttle.ui.actions.RestoreDatabase
import com.simplecityapps.testing.MainDispatcherRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val backUpDatabase: BackUpDatabase = mockk(relaxed = true)
    private val restoreDatabase: RestoreDatabase = mockk(relaxed = true)

    private fun viewModel() = BackupRestoreViewModel(backUpDatabase, restoreDatabase, ioDispatcher = testDispatcher)

    @Test
    fun `initial uiState is Idle`() {
        viewModel().uiState.value shouldBe BackupRestoreUiState.Idle
    }

    @Test
    fun `backUpDatabase calls backUpDatabase use case and sets state to BackupSuccess on success`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://com.android.providers.downloads/backup.db")

        viewModel.backUpDatabase(uri)

        coVerify { backUpDatabase(uri) }
        viewModel.uiState.value shouldBe BackupRestoreUiState.BackupSuccess
    }

    @Test
    fun `backUpDatabase sets state to Error when BackupRestoreError is thrown`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://com.android.providers.downloads/backup.db")
        val expectedError = BackupRestoreError.IntegrityCheckFailed()
        coEvery { backUpDatabase(uri) } throws expectedError

        viewModel.backUpDatabase(uri)

        viewModel.uiState.value shouldBe BackupRestoreUiState.Error(expectedError)
    }

    @Test
    fun `backUpDatabase wraps generic Exception in BackupRestoreError IO and sets state to Error`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://com.android.providers.downloads/backup.db")
        val cause = RuntimeException("Disk error")
        coEvery { backUpDatabase(uri) } throws cause

        viewModel.backUpDatabase(uri)

        viewModel.uiState.value shouldBe BackupRestoreUiState.Error(BackupRestoreError.IO(cause))
    }

    @Test
    fun `restoreDatabase calls restoreDatabase use case and sets state to RestoreSuccess on success`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://com.android.providers.downloads/restore.db")

        viewModel.restoreDatabase(uri)

        coVerify { restoreDatabase(uri) }
        viewModel.uiState.value shouldBe BackupRestoreUiState.RestoreSuccess
    }

    @Test
    fun `restoreDatabase sets state to Error when BackupRestoreError is thrown`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://com.android.providers.downloads/restore.db")
        val expectedError = BackupRestoreError.VersionMismatch(restored = 10, current = 5)
        coEvery { restoreDatabase(uri) } throws expectedError

        viewModel.restoreDatabase(uri)

        viewModel.uiState.value shouldBe BackupRestoreUiState.Error(expectedError)
    }

    @Test
    fun `restoreDatabase wraps generic Exception in BackupRestoreError IO and sets state to Error`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://com.android.providers.downloads/restore.db")
        val cause = RuntimeException("Corrupt file")
        coEvery { restoreDatabase(uri) } throws cause

        viewModel.restoreDatabase(uri)

        viewModel.uiState.value shouldBe BackupRestoreUiState.Error(BackupRestoreError.IO(cause))
    }

    @Test
    fun `resetState resets uiState to Idle`() = runTest {
        val viewModel = viewModel()
        viewModel.backUpDatabase(Uri.parse("content://test.db"))
        viewModel.uiState.value shouldBe BackupRestoreUiState.BackupSuccess

        viewModel.resetState()

        viewModel.uiState.value shouldBe BackupRestoreUiState.Idle
    }
}
