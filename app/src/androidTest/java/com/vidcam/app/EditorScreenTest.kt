package com.vidcam.app

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.vidcam.app.editor.EditorScreen
import com.vidcam.app.editor.EditorViewModel
import com.vidcam.app.ui.theme.VidCamTheme
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(): EditorViewModel =
        EditorViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun setEditorContent(viewModel: EditorViewModel) {
        composeRule.setContent {
            VidCamTheme {
                EditorScreen(
                    viewModel = viewModel,
                    canRecord = false,
                    onRequestPermissions = {},
                )
            }
        }
    }

    @Test
    fun showsImportFlowWhenCameraPermissionMissing() {
        val viewModel = viewModel()
        setEditorContent(viewModel)

        composeRule.onNodeWithText("Importar").assertIsDisplayed()
        composeRule.onNodeWithText("Música").assertIsDisplayed()
        composeRule.onNodeWithText("Sin acceso a la cámara").assertIsDisplayed()
        composeRule.onNodeWithText("Grabar").assertDoesNotExist()
    }

    @Test
    fun motionRecordingRequiresALayer() {
        val viewModel = viewModel()
        setEditorContent(viewModel)

        composeRule.onNodeWithText("Grabar movimiento")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeRule.runOnIdle {
            viewModel.addPngLayer(Uri.parse("asset://stickers/star.png"))
        }

        composeRule.onNodeWithText("Grabar movimiento").assertIsEnabled()
        composeRule.onNodeWithText("REC movimiento").assertDoesNotExist()
    }

    @Test
    fun newProjectDiscardsTheCurrentWork() {
        val viewModel = viewModel()
        setEditorContent(viewModel)

        composeRule.runOnIdle {
            viewModel.addPngLayer(Uri.parse("asset://stickers/star.png"))
        }
        composeRule.onNodeWithText("Capas").assertIsDisplayed()

        composeRule.onNodeWithText("Nuevo").performClick()
        composeRule.onNodeWithText("Nuevo proyecto").assertIsDisplayed()
        composeRule.onNodeWithText("Descartar").performClick()

        composeRule.onNodeWithText("Capas").assertDoesNotExist()
        composeRule.onNodeWithText("Graba o importa un vídeo para empezar")
            .assertIsDisplayed()
    }
}
