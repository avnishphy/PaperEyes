package com.example.papereyes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.papereyes.MainActivity
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenRendersPrimaryActions() {
        composeRule.onNodeWithText("PaperEyes").assertIsDisplayed()
        composeRule.onNodeWithText("Live Scan").assertIsDisplayed()
        composeRule.onNodeWithText("Import").assertIsDisplayed()
    }
}
