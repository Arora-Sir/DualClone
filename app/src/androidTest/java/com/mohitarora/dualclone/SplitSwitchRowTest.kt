package com.mohitarora.dualclone

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mohitarora.dualclone.core.designsystem.components.DcSplitSwitchRow
import com.mohitarora.dualclone.ui.theme.DualCloneTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The row body opens the detail and the switch only toggles, so the two taps never mix. */
class SplitSwitchRowTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bodyOpensTheDetailAndTheSwitchOnlyToggles() {
        var checked by mutableStateOf(true)
        var opened = 0
        rule.setContent {
            DualCloneTheme {
                DcSplitSwitchRow(
                    title = "Row title",
                    subtitle = "Row state",
                    checked = checked,
                    onCheckedChange = { checked = it },
                    onOpen = { opened++ }
                )
            }
        }
        rule.onNodeWithText("Row title").performClick()
        assertEquals(1, opened)
        rule.onNode(isToggleable()).assertIsOn().performClick()
        rule.onNode(isToggleable()).assertIsOff()
        assertEquals("the switch must not open the detail", 1, opened)
    }

    @Test
    fun withoutADetailTheWholeRowToggles() {
        var checked by mutableStateOf(false)
        rule.setContent {
            DualCloneTheme {
                DcSplitSwitchRow(title = "Row title", checked = checked, onCheckedChange = { checked = it }, onOpen = null)
            }
        }
        rule.onNodeWithText("Row title").performClick()
        rule.onNode(isToggleable()).assertIsOn()
    }
}
