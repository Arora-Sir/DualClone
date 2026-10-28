package com.mohitarora.dualclone.domain

import com.mohitarora.dualclone.domain.backup.BackupCodec
import com.mohitarora.dualclone.domain.backup.BackupModel
import com.mohitarora.dualclone.domain.backup.BackupParts
import com.mohitarora.dualclone.domain.backup.BackupPreset
import com.mohitarora.dualclone.domain.backup.BackupProfile
import com.mohitarora.dualclone.domain.backup.BackupReadResult
import com.mohitarora.dualclone.domain.backup.BackupTheme
import com.mohitarora.dualclone.domain.backup.BackupUpdateSettings
import com.mohitarora.dualclone.domain.backup.MiniJson
import com.mohitarora.dualclone.domain.backup.RestorePlanner
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.testing.profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupPartsTest {

    private val profiles = listOf(BackupProfile(95, ProfileKind.DUAL_MESSENGER, listOf("com.whatsapp")))
    private val presets = listOf(BackupPreset(ProfileKind.DUAL_MESSENGER, 95, "com.whatsapp", "MANAGE_EXTERNAL_STORAGE", "allow"))
    private val theme = BackupTheme("ONE_UI_BLUE", false, 0xFF00BCD4L, "DARK")
    private val update = BackupUpdateSettings(dailyCheck = true)
    private val targets = listOf(profile(95, ProfileKind.DUAL_MESSENGER))
    private val host = setOf("com.whatsapp")

    private fun model(parts: BackupParts) = BackupModel(
        createdAtMillis = 5L, appVersion = "1.0.0",
        profiles = if (parts.cloneList) profiles else emptyList(),
        appOpsPresets = if (parts.appOpsPresets) presets else emptyList(),
        theme = if (parts.settings) theme else null,
        updateSettings = if (parts.settings) update else null,
        parts = parts
    )

    private fun write(m: BackupModel, password: CharArray? = null): ByteArray =
        ByteArrayOutputStream().also { BackupCodec.write(m, it, password, iterations = 1_000) }.toByteArray()

    private fun read(bytes: ByteArray, password: CharArray? = null) =
        (BackupCodec.read(ByteArrayInputStream(bytes), password) as BackupReadResult.Ok).model

    private val allCombinations = listOf(true, false).flatMap { a -> listOf(true, false).flatMap { b -> listOf(true, false).map { c -> BackupParts(a, b, c) } } }
        .filter { it.isValid }

    @Test
    fun everyValidCombinationRoundTripsPlainAndEncrypted() {
        assertEquals(7, allCombinations.size)
        for (parts in allCombinations) {
            val m = model(parts)
            assertEquals("plain $parts", m, read(write(m)))
            assertEquals("encrypted $parts", m, read(write(m, "pw".toCharArray()), "pw".toCharArray()))
        }
    }

    @Test
    fun unselectedSectionsAreOmittedFromTheFileAndTheHeaderListsThePartsPresent() {
        for (parts in allCombinations) {
            val root = MiniJson.parse(String(write(model(parts)))) as Map<*, *>
            assertEquals(parts.names(), root["parts"])
            assertEquals("profiles $parts", parts.cloneList, root.containsKey("profiles"))
            assertEquals("presets $parts", parts.appOpsPresets, root.containsKey("appOpsPresets"))
            assertEquals("theme $parts", parts.settings, root.containsKey("theme"))
            assertEquals("update $parts", parts.settings, root.containsKey("updateSettings"))
            assertEquals(1L, root["schemaVersion"])
        }
    }

    @Test
    fun presetConstants() {
        assertEquals(BackupParts(true, true, true), BackupParts.EVERYTHING)
        assertEquals(BackupParts(true, true, false), BackupParts.CLONE_LIST_ONLY)
        assertEquals(BackupParts(false, false, true), BackupParts.SETTINGS_ONLY)
        assertFalse(BackupParts(false, false, false).isValid)
    }

    @Test
    fun settingsOnlyFileHasNoProfilesAndPlansOnlySettings() {
        val m = read(write(model(BackupParts.SETTINGS_ONLY)))
        assertTrue(m.profiles.isEmpty())
        val plan = RestorePlanner.plan(m, host, targets)
        assertTrue(plan.clones.isEmpty() && plan.presets.isEmpty())
        assertFalse(plan.hasCloneList)
        assertFalse(plan.hasAppOpsPresets)
        assertTrue(plan.hasSettings)
        assertEquals(theme, plan.theme)
        assertEquals(update, plan.updateSettings)
        assertTrue(plan.applyTheme && plan.applyUpdateSettings)
    }

    @Test
    fun cloneListOnlyFileHasNoThemeAndPlansClonesAndPresets() {
        val m = read(write(model(BackupParts.CLONE_LIST_ONLY)))
        assertNull(m.theme)
        assertNull(m.updateSettings)
        val plan = RestorePlanner.plan(m, host, targets)
        assertEquals(listOf("com.whatsapp"), plan.clones.map { it.packageName })
        assertEquals(1, plan.presets.size)
        assertTrue(plan.hasCloneList && plan.hasAppOpsPresets)
        assertFalse(plan.hasSettings)
        assertNull(plan.theme)
        assertFalse(plan.applyTheme)
        assertFalse(plan.applyUpdateSettings)
    }

    @Test
    fun presetsOnlyFilePlansNoClones() {
        val parts = BackupParts(cloneList = false, appOpsPresets = true, settings = false)
        val plan = RestorePlanner.plan(read(write(model(parts))), host, targets)
        assertTrue(plan.clones.isEmpty())
        assertEquals(1, plan.presets.size)
        assertFalse(plan.hasCloneList)
    }

    @Test
    fun planningRespectsPartsEvenIfTheModelCarriesExtraData() {
        val sneaky = model(BackupParts.EVERYTHING).copy(parts = BackupParts.SETTINGS_ONLY)
        val plan = RestorePlanner.plan(sneaky, host, targets)
        assertTrue(plan.clones.isEmpty() && plan.presets.isEmpty())
    }

    @Test
    fun oldFullBackupWithoutPartsHeaderStillReads() {
        val old = """{"format":"dualclone-backup-v1","schemaVersion":1,"createdAtMillis":9,"appVersion":"0.9",
            "profiles":[{"userId":95,"kind":"DUAL_MESSENGER","clones":["com.whatsapp"]}],
            "appOpsPresets":[{"kind":"DUAL_MESSENGER","userId":95,"packageName":"com.whatsapp","op":"MANAGE_EXTERNAL_STORAGE","mode":"allow"}],
            "theme":{"preset":"CUSTOM","pureBlack":true,"customColorHex":4278238420,"mode":"DARK"}}"""
        val m = read(old.toByteArray())
        assertEquals(BackupParts.EVERYTHING, m.parts)
        assertEquals(1, m.profiles.size)
        assertEquals(1, m.appOpsPresets.size)
        assertEquals("CUSTOM", m.theme?.preset)
        assertNull(m.updateSettings)
        val plan = RestorePlanner.plan(m, host, targets)
        assertTrue(plan.hasCloneList && plan.hasAppOpsPresets && plan.hasSettings)
        assertEquals(1, plan.clones.size)
    }

    @Test
    fun headerWithNoPartsIsRejected() {
        val bad = """{"format":"dualclone-backup-v1","schemaVersion":1,"parts":[]}"""
        assertTrue(BackupCodec.read(ByteArrayInputStream(bad.toByteArray())) is BackupReadResult.Invalid)
    }

    @Test
    fun noBackupFileContainsCommandLogOrSecrets() {
        val text = String(write(model(BackupParts.EVERYTHING))).lowercase()
        for (word in listOf("command", "password", "token", "keystore", "secret")) assertFalse(word, text.contains(word))
    }
}
