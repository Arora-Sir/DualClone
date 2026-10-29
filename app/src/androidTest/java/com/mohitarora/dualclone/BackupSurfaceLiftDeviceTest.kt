package com.mohitarora.dualclone

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohitarora.dualclone.domain.backup.BackupCodec
import com.mohitarora.dualclone.domain.backup.BackupModel
import com.mohitarora.dualclone.domain.backup.BackupParts
import com.mohitarora.dualclone.domain.backup.BackupReadResult
import com.mohitarora.dualclone.domain.backup.BackupTheme
import com.mohitarora.dualclone.ui.theme.DEFAULT_SURFACE_LIFT
import com.mohitarora.dualclone.ui.theme.ThemePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** The lift on a real Android runtime, where org.json is the platform one and preferences are real files. */
@RunWith(AndroidJUnit4::class)
class BackupSurfaceLiftDeviceTest {

    @Test
    fun theLiftSurvivesAnExportAndImportOnDevice() {
        val out = ByteArrayOutputStream()
        BackupCodec.write(
            BackupModel(createdAtMillis = 1L, theme = BackupTheme("ONE_UI_BLUE", true, 0L, "DARK", 70), parts = BackupParts.SETTINGS_ONLY),
            out
        )
        val read = BackupCodec.read(ByteArrayInputStream(out.toByteArray()))
        assertTrue(read is BackupReadResult.Ok)
        assertEquals(70, (read as BackupReadResult.Ok).model.theme!!.surfaceLift)
    }

    @Test
    fun theLiftIsStoredAndLoadedFromPreferences() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ThemePreferences(context)
        val before = prefs.themeConfig.value.surfaceLift
        try {
            prefs.setSurfaceLift(30)
            assertEquals(30, ThemePreferences(context).themeConfig.value.surfaceLift)
            prefs.setSurfaceLift(999)
            assertEquals(100, ThemePreferences(context).themeConfig.value.surfaceLift)
        } finally {
            prefs.setSurfaceLift(before.takeIf { it in 0..100 } ?: DEFAULT_SURFACE_LIFT)
        }
    }
}
