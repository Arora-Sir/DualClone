package com.mohitarora.dualclone.domain

import com.mohitarora.dualclone.domain.backup.BackupCodec
import com.mohitarora.dualclone.domain.backup.BackupModel
import com.mohitarora.dualclone.domain.backup.BackupParts
import com.mohitarora.dualclone.domain.backup.BackupReadResult
import com.mohitarora.dualclone.domain.backup.BackupTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupSurfaceLiftTest {

    private fun roundTrip(theme: BackupTheme): BackupTheme {
        val out = ByteArrayOutputStream()
        BackupCodec.write(BackupModel(createdAtMillis = 1L, theme = theme, parts = BackupParts.SETTINGS_ONLY), out)
        val result = BackupCodec.read(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is BackupReadResult.Ok)
        return (result as BackupReadResult.Ok).model.theme!!
    }

    @Test
    fun theSurfaceLiftSurvivesAnExportAndImport() {
        assertEquals(80, roundTrip(BackupTheme("ONE_UI_BLUE", true, 0L, "DARK", 80)).surfaceLift)
        assertEquals(0, roundTrip(BackupTheme("ONE_UI_BLUE", true, 0L, "DARK", 0)).surfaceLift)
    }

    /** A backup written before the slider existed has no field and restores at the recommended look. */
    @Test
    fun anOlderBackupWithoutTheFieldRestoresAtTheRecommendedLift() {
        val json = """{"format":"dualclone-backup-v1","schemaVersion":1,"parts":["SETTINGS"],"createdAtMillis":1,
            "appVersion":"1.0.1","theme":{"preset":"ONE_UI_BLUE","pureBlack":true,"customColorHex":0,"mode":"DARK"}}"""
        val result = BackupCodec.read(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result is BackupReadResult.Ok)
        assertEquals(50, (result as BackupReadResult.Ok).model.theme!!.surfaceLift)
    }
}
