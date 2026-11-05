package com.mohitarora.dualclone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import androidx.test.platform.app.InstrumentationRegistry
import com.mohitarora.dualclone.ui.dialog.HomeNotice
import com.mohitarora.dualclone.ui.dialog.HomeNoticeDialog
import com.mohitarora.dualclone.ui.theme.DualCloneTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The profile notice dialog replaced banners that shifted the list. These tests pin its button layout and callbacks. */
class HomeNoticeDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val paused = HomeNotice(
        key = "paused:10",
        icon = Icons.Default.PauseCircle,
        title = "Profile paused",
        body = "Work profile is paused. Resume it before cloning or removing apps."
    )

    @Test
    fun aNoticeWithoutAnActionShowsOneDismissButtonOnTheRight() {
        rule.setContent { DualCloneTheme { HomeNoticeDialog(notice = paused, onDismiss = {}) } }
        rule.onNodeWithText(paused.title).assertIsDisplayed()
        rule.onNodeWithText(paused.body).assertIsDisplayed()
        val dialogWidth = rule.onNode(isDialog()).getUnclippedBoundsInRoot().width
        val dismiss = rule.onNodeWithText(str(R.string.banner_dismiss)).getUnclippedBoundsInRoot()
        assertTrue("Dismiss should sit in the right half of the dialog", dismiss.left + dismiss.width / 2 > dialogWidth / 2)
    }

    @Test
    fun dismissingCallsOnDismiss() {
        var dismissed = 0
        rule.setContent { DualCloneTheme { HomeNoticeDialog(notice = paused, onDismiss = { dismissed++ }) } }
        rule.onNodeWithText(str(R.string.banner_dismiss)).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun aNoticeWithAnActionPutsDismissOnTheLeftAndTheActionOnTheRight() {
        val withAction = HomeNotice(
            key = "stale",
            icon = Icons.Default.Info,
            title = "Clone list may be out of date",
            body = "The last profile scan failed, so the list shows the previous result.",
            actionLabel = "Retry",
            onAction = {}
        )
        rule.setContent { DualCloneTheme { HomeNoticeDialog(notice = withAction, onDismiss = {}) } }
        val dismiss = rule.onNodeWithText(str(R.string.banner_dismiss)).getUnclippedBoundsInRoot()
        val action = rule.onNodeWithText("Retry").getUnclippedBoundsInRoot()
        assertTrue("Dismiss should be left of the action", dismiss.left < action.left)
    }

    @Test
    fun theActionClosesTheDialogAndRunsTheAction() {
        var dismissed = 0
        var acted = 0
        val withAction = HomeNotice(
            key = "stale", icon = Icons.Default.Info, title = "Out of date", body = "Body",
            actionLabel = "Retry", onAction = { acted++ }
        )
        rule.setContent { DualCloneTheme { HomeNoticeDialog(notice = withAction, onDismiss = { dismissed++ }) } }
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, dismissed)
        assertEquals(1, acted)
    }
}
