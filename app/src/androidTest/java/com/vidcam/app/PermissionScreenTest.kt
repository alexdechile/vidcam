package com.vidcam.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vidcam.app.ui.PermissionScreen
import com.vidcam.app.ui.theme.VidCamTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PermissionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsExplanationAndRequestButton() {
        composeRule.setContent {
            VidCamTheme { PermissionScreen(onRequest = {}, onOpenSettings = {}) }
        }

        composeRule.onNodeWithText("VidCam").assertIsDisplayed()
        composeRule.onNodeWithText("Conceder permisos").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir ajustes de la app").assertIsDisplayed()
    }

    @Test
    fun requestButtonInvokesCallback() {
        var requested = false
        composeRule.setContent {
            VidCamTheme {
                PermissionScreen(onRequest = { requested = true }, onOpenSettings = {})
            }
        }

        composeRule.onNodeWithText("Conceder permisos").performClick()

        assertTrue(requested)
    }
}
