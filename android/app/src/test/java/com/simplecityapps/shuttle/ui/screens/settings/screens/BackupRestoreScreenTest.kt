package com.simplecityapps.shuttle.ui.screens.settings.screens

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRestoreScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays title and backup restore options in idle state`() {
        composeTestRule.setContent {
            BackupRestoreScreen(
                uiState = BackupRestoreUiState.Idle,
                onBackUpClick = {},
                onRestoreClick = {},
                onNavigateUp = {}
            )
        }

        composeTestRule.onNode(hasText("Backup & Restore")).assertExists()
        composeTestRule.onNode(hasText("Back up Database")).assertExists().assertHasClickAction()
        composeTestRule.onNode(hasText("Restore Database")).assertExists().assertHasClickAction()
    }

    @Test
    fun `clicking backup database triggers onBackUpClick`() {
        var backupClicked = false
        composeTestRule.setContent {
            BackupRestoreScreen(
                uiState = BackupRestoreUiState.Idle,
                onBackUpClick = { backupClicked = true },
                onRestoreClick = {},
                onNavigateUp = {}
            )
        }

        composeTestRule.onNode(hasText("Back up Database")).performClick()

        backupClicked shouldBe true
    }

    @Test
    fun `clicking restore database triggers onRestoreClick`() {
        var restoreClicked = false
        composeTestRule.setContent {
            BackupRestoreScreen(
                uiState = BackupRestoreUiState.Idle,
                onBackUpClick = {},
                onRestoreClick = { restoreClicked = true },
                onNavigateUp = {}
            )
        }

        composeTestRule.onNode(hasText("Restore Database")).performClick()

        restoreClicked shouldBe true
    }

    @Test
    fun `disables backup and restore options when backup is in progress`() {
        composeTestRule.setContent {
            BackupRestoreScreen(
                uiState = BackupRestoreUiState.BackupInProgress,
                onBackUpClick = {},
                onRestoreClick = {},
                onNavigateUp = {}
            )
        }

        composeTestRule.onNode(hasText("Back up Database") and hasClickAction()).assertIsNotEnabled()
        composeTestRule.onNode(hasText("Restore Database") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun `disables backup and restore options when restore is in progress`() {
        composeTestRule.setContent {
            BackupRestoreScreen(
                uiState = BackupRestoreUiState.RestoreInProgress,
                onBackUpClick = {},
                onRestoreClick = {},
                onNavigateUp = {}
            )
        }

        composeTestRule.onNode(hasText("Back up Database") and hasClickAction()).assertIsNotEnabled()
        composeTestRule.onNode(hasText("Restore Database") and hasClickAction()).assertIsNotEnabled()
    }
}
