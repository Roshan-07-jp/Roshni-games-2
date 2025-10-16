package com.roshni.games.core.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.roshni.games.core.designsystem.theme.RoshniGamesTheme
import org.junit.Rule
import org.junit.Test

class GamingButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gamingButton_shouldDisplayText() {
        // Given
        val buttonText = "Test Button"
        var clicked = false

        // When
        composeTestRule.setContent {
            RoshniGamesTheme {
                GamingButton(
                    text = buttonText,
                    onClick = { clicked = true }
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
    }

    @Test
    fun gamingButton_shouldTriggerOnClick() {
        // Given
        val buttonText = "Clickable Button"
        var clicked = false

        composeTestRule.setContent {
            RoshniGamesTheme {
                GamingButton(
                    text = buttonText,
                    onClick = { clicked = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText(buttonText).performClick()

        // Then
        assert(clicked) { "Button click should trigger onClick callback" }
    }

    @Test
    fun gamingButton_shouldShowDifferentVariants() {
        // Given
        val variants = listOf(
            ButtonVariant.Primary,
            ButtonVariant.Secondary,
            ButtonVariant.Success,
            ButtonVariant.Gaming
        )

        variants.forEach { variant ->
            // When
            composeTestRule.setContent {
                RoshniGamesTheme {
                    GamingButton(
                        text = "${variant.name} Button",
                        onClick = {},
                        variant = variant
                    )
                }
            }

            // Then
            composeTestRule.onNodeWithText("${variant.name} Button").assertIsDisplayed()
        }
    }

    @Test
    fun gamingButton_shouldShowDifferentSizes() {
        // Given
        val sizes = listOf(
            ButtonSize.Small,
            ButtonSize.Medium,
            ButtonSize.Large
        )

        sizes.forEach { size ->
            // When
            composeTestRule.setContent {
                RoshniGamesTheme {
                    GamingButton(
                        text = "${size.name} Button",
                        onClick = {},
                        size = size
                    )
                }
            }

            // Then
            composeTestRule.onNodeWithText("${size.name} Button").assertIsDisplayed()
        }
    }
}