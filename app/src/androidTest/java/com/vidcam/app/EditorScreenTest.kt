package com.vidcam.app

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.vidcam.app.editor.EditorScreen
import com.vidcam.app.editor.EditorViewModel
import com.vidcam.app.ui.theme.VidCamTheme
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsImportFlowWhenCameraPermissionMissing() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = EditorViewModel(application)

        composeRule.setContent {
            VidCamTheme {
                EditorScreen(
                    viewModel = viewModel,
                    canRecord = false,
                    onRequestPermissions = {},
                )
            }
        }

        composeRule.onNodeWithText("Importar").assertIsDisplayed()
        composeRule.onNodeWithText("Música").assertIsDisplayed()
        composeRule.onNodeWithText("Sin acceso a la cámara").assertIsDisplayed()
        composeRule.onNodeWithText("Grabar").assertDoesNotExist()
    }
}
