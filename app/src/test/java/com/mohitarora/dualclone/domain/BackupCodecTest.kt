package com.mohitarora.dualclone.domain

import com.mohitarora.dualclone.domain.backup.BackupCodec
import com.mohitarora.dualclone.domain.backup.BackupModel
import com.mohitarora.dualclone.domain.backup.BackupPreset
import com.mohitarora.dualclone.domain.backup.BackupProfile
import com.mohitarora.dualclone.domain.backup.BackupReadResult
import com.mohitarora.dualclone.domain.backup.BackupTheme
import com.mohitarora.dualclone.domain.backup.MiniJson
import com.mohitarora.dualclone.domain.backup.RestorePlanner
import com.mohitarora.dualclone.domain.backup.SkipReason
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.testing.profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupCodecTest {

    private val model = BackupModel(
        createdAtMillis = 1_700_000_000_000L,
        appVersion = "1.0.0",
        profiles = listOf(
            BackupProfile(95, ProfileKind.DUAL_MESSENGER, listOf("com.whatsapp", "com.example.alpha")),
            BackupProfile(10, ProfileKind.WORK, listOf("com.example.beta"))
        ),
        appOpsPresets = listOf(BackupPreset(ProfileKind.DUAL_MESSENGER, 95, "com.whatsapp", "MANAGE_EXTERNAL_STORAGE", "allow")),
        theme = BackupTheme("CUSTOM", true, 0xFF00BCD4L, "DARK")
    )

    private fun encode(password: CharArray? = null, iterations: Int = 1_000): ByteArray =
        ByteArrayOutputStream().also { BackupCodec.write(model, it, password, iterations = iterations) }.toByteArray()

    @Test
    fun plainRoundTrip() {
        val bytes = encode()
        val result = BackupCodec.read(ByteArrayInputStream(bytes)) as BackupReadResult.Ok
        assertEquals(model, result.model)
        assertTrue(String(bytes).contains("dualclone-backup-v1"))
    }

    @Test
    fun encryptedRoundTripAndNoPlaintextLeak() {
        val bytes = encode("correct horse".toCharArray())
        assertFalse(String(bytes).contains("com.whatsapp"))
        assertTrue(BackupCodec.isEncrypted(bytes))
        val result = BackupCodec.read(ByteArrayInputStream(bytes), "correct horse".toCharArray()) as BackupReadResult.Ok
        assertEquals(model, result.model)
    }

    @Test
    fun encryptedFileAsksForPasswordAndRejectsWrongOne() {
        val bytes = encode("secret".toCharArray())
        assertEquals(BackupReadResult.NeedsPassword, BackupCodec.read(ByteArrayInputStream(bytes)))
        assertEquals(
            BackupReadResult.WrongPasswordOrCorrupt,
            BackupCodec.read(ByteArrayInputStream(bytes), "other".toCharArray())
        )
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val text = String(encode("secret".toCharArray()))
        val marker = "\"ciphertext\":\""
        val at = text.indexOf(marker) + marker.length
        val flipped = text.substring(0, at) + (if (text[at] == 'A') 'B' else 'A') + text.substring(at + 1)
        val result = BackupCodec.read(ByteArrayInputStream(flipped.toByteArray()), "secret".toCharArray())
        assertEquals(BackupReadResult.WrongPasswordOrCorrupt, result)
    }

    @Test
    fun saltAndIvDifferBetweenExports() {
        val a = String(encode("pw".toCharArray()))
        val b = String(encode("pw".toCharArray()))
        assertFalse(a == b)
    }

    @Test
    fun defaultIterationCountIsThe310kFromTheBlueprint() {
        assertEquals(310_000, BackupCodec.PBKDF2_ITERATIONS)
    }

    @Test
    fun rejectsGarbageAndForeignFormats() {
        assertTrue(BackupCodec.read(ByteArrayInputStream("not json".toByteArray())) is BackupReadResult.Invalid)
        assertTrue(BackupCodec.read(ByteArrayInputStream("{\"format\":\"other\"}".toByteArray())) is BackupReadResult.Invalid)
        val newer = String(encode()).replace("\"schemaVersion\":1", "\"schemaVersion\":99")
        assertTrue(BackupCodec.read(ByteArrayInputStream(newer.toByteArray())) is BackupReadResult.Invalid)
    }

    @Test
    fun jsonEscapesRoundTrip() {
        val tricky = mapOf("k" to "line\nquote\" back\\ tab\t unicode é ctrl \u0001", "n" to listOf(1L, -2L, true, null))
        assertEquals(tricky, MiniJson.parse(MiniJson.write(tricky)))
    }

    @Test
    fun plannerSkipsPackagesMissingFromTheHostAndMatchesProfilesByKind() {
        val targets = listOf(profile(96, ProfileKind.DUAL_MESSENGER), profile(10, ProfileKind.WORK), profile(150, ProfileKind.SECURE_FOLDER))
        val plan = RestorePlanner.plan(model, hostPackages = setOf("com.whatsapp", "com.example.beta"), targets = targets)
        assertEquals(setOf("com.whatsapp" to 96, "com.example.beta" to 10), plan.clones.map { it.packageName to it.targetUserId }.toSet())
        assertEquals(SkipReason.NOT_INSTALLED_IN_HOST, plan.skipped.single { it.label == "com.example.alpha" }.reason)
        assertEquals(96, plan.presets.single().targetUserId)
        assertTrue(plan.applyTheme)
    }

    @Test
    fun plannerNeverTargetsSecureFolderOrMissingProfiles() {
        val onlyKnox = RestorePlanner.plan(model, setOf("com.whatsapp"), listOf(profile(150, ProfileKind.SECURE_FOLDER)))
        assertTrue(onlyKnox.clones.isEmpty())
        assertTrue(onlyKnox.skipped.all { it.reason == SkipReason.NO_MATCHING_PROFILE })
    }
}
