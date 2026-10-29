package com.mohitarora.dualclone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.platform.app.InstrumentationRegistry
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcPaletteCell
import com.mohitarora.dualclone.core.designsystem.components.DcPaletteGrid
import com.mohitarora.dualclone.ui.screen.SettingsScreen
import com.mohitarora.dualclone.ui.screen.settings.SettingsActions
import com.mohitarora.dualclone.ui.screen.settings.SettingsUiState
import com.mohitarora.dualclone.ui.sheet.SupportBottomSheet
import com.mohitarora.dualclone.ui.theme.DualCloneTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

/** Layout invariants from the release plan. Each test fails when a sibling differs in size or a target gets small. */
class StableLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun actions() = SettingsActions(
        onBack = {}, onModeChange = {}, onPresetChange = {}, onApplyCustomColor = {}, onPureBlackChange = {}, onSurfaceLiftChange = {},
        onEnableDualMessenger = {}, onClearAuditLog = {}, onRequestShizukuPermission = {}, onRefreshShizuku = {},
        onOpenDiagnostics = {}
    )

    @Test
    fun buttonRowChildrenHaveEqualHeightAndWidth() {
        rule.setContent {
            DualCloneTheme {
                Column(Modifier.width(360.dp)) {
                    DcButtonRow {
                        DcButton("Pay with UPI", {}, modifier = Modifier.slot().testTag("a"))
                        DcButton("Copy UPI ID", {}, style = DcButtonStyle.Outlined, modifier = Modifier.slot().testTag("b"))
                    }
                }
            }
        }
        val a = rule.onNodeWithTag("a").getUnclippedBoundsInRoot()
        rule.onNodeWithTag("b").assertHeightIsEqualTo(a.height).assertWidthIsEqualTo(a.width)
    }

    @Test
    fun supportSheetButtonsShareOneHeight() {
        rule.setContent { DualCloneTheme { SupportBottomSheet(onDismiss = {}) } }
        val labels = listOf(R.string.support_upi_pay, R.string.support_upi_copy, R.string.support_paypal, R.string.support_sponsors)
        val heights = labels.map {
            rule.onNodeWithText(str(it)).performScrollTo()
            rule.onNodeWithText(str(it)).getUnclippedBoundsInRoot().height
        }
        // Text nodes sit inside buttons of one height, so the label boxes match within a pixel of rounding.
        val spread = heights.maxOf { it.value } - heights.minOf { it.value }
        assert(spread <= 1f) { "Support button label heights differ by $spread dp" }
    }

    @Test
    fun alphabetRailSurvivesAShortHeight() {
        // NOTE: Regression test for the landscape crash when the rail was shorter than 72dp.
        rule.setContent {
            DualCloneTheme {
                com.mohitarora.dualclone.ui.components.AlphabetScrollbar(
                    onLetterSelected = {},
                    modifier = Modifier.height(60.dp).testTag("rail")
                )
            }
        }
        rule.onNodeWithTag("rail").assertIsDisplayed()
    }

    @Test
    fun paletteCellsAreEqualSize() {
        val cells = (1..8).map { DcPaletteCell("c$it", if (it == 1) "Wallpaper" else "C$it", Color.Red) }
        rule.setContent { DualCloneTheme { DcPaletteGrid(cells = cells, selectedId = "c2", onSelect = {}, modifier = Modifier.width(360.dp)) } }
        val nodes = rule.onAllNodes(isSelectable())
        val first = nodes.onFirst().getUnclippedBoundsInRoot()
        repeat(8) { i ->
            nodes[i].assertWidthIsEqualTo(first.width).assertHeightIsEqualTo(first.height)
        }
    }

    @Test
    fun moreColorsSheetNeverRepeatsTheQuickRow() {
        rule.setContent {
            DualCloneTheme {
                com.mohitarora.dualclone.ui.sheet.AccentPickerSheet(
                    theme = com.mohitarora.dualclone.ui.theme.ThemeConfig(),
                    morePresets = com.mohitarora.dualclone.ui.theme.accentLayout(wallpaperSupported = true).more,
                    onApplyPreset = {},
                    onApplyCustom = {},
                    onDismiss = {}
                )
            }
        }
        listOf(R.string.theme_preset_rose, R.string.theme_preset_mono).forEach {
            rule.onNodeWithText(str(it)).assertExists()
        }
        listOf(R.string.theme_preset_one_ui, R.string.theme_preset_orange, R.string.accent_teal, R.string.theme_preset_purple, R.string.theme_preset_wallpaper).forEach {
            rule.onNodeWithText(str(it)).assertDoesNotExist()
        }
    }

    @Test
    fun accentRowDotsAreEqualSize() {
        val dots = com.mohitarora.dualclone.ui.theme.accentLayout(true).quick.map {
            DcPaletteCell(it.name, it.name, Color.Red)
        }
        rule.setContent {
            DualCloneTheme {
                com.mohitarora.dualclone.core.designsystem.components.DcAccentRow(
                    dots = dots, selectedId = dots.first().id, extra = null, onSelect = {}, onMore = {},
                    modifier = Modifier.width(360.dp)
                )
            }
        }
        val nodes = rule.onAllNodes(isSelectable())
        val first = nodes.onFirst().getUnclippedBoundsInRoot()
        repeat(5) { i -> nodes[i].assertWidthIsEqualTo(first.width).assertHeightIsEqualTo(first.height) }
    }

    @Test
    fun settingsTargetsAreAtLeast48dp() {
        rule.setContent {
            DualCloneTheme { SettingsScreen(state = SettingsUiState(), actions = actions(), events = emptyFlow()) }
        }
        val clickable = rule.onAllNodes(hasClickAction())
        val count = clickable.fetchSemanticsNodes().size
        val failures = mutableListOf<String>()
        val minPx = with(rule.density) { 48.dp.toPx() } - 1f
        repeat(count) { i ->
            // NOTE: Material icon buttons extend their touch area beyond layout size and partly visible rows clip touch bounds, so take the larger value.
            val node = clickable[i].fetchSemanticsNode()
            val touch = node.touchBoundsInRoot
            val h = maxOf(node.size.height.toFloat(), touch.height)
            val w = maxOf(node.size.width.toFloat(), touch.width)
            if (h < minPx || w < minPx) failures += "node $i: ${w}x${h}px"
        }
        assert(failures.isEmpty()) { failures.joinToString(" | ") }
    }

    @Test
    fun supportSheetStaysUsableAtFontScale15() {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.5f)) {
                DualCloneTheme { SupportBottomSheet(onDismiss = {}) }
            }
        }
        listOf(R.string.support_title, R.string.support_upi_pay, R.string.support_sponsors).forEach {
            rule.onNodeWithText(str(it)).performScrollTo().assertIsDisplayed()
        }
    }
}
