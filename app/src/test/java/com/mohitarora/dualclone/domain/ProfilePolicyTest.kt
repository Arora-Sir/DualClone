package com.mohitarora.dualclone.domain

import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePolicyTest {

    private val tablet = PmOutputParser.parseUsers(Fixtures.USERS_TABLET)

    @Test
    fun classifiesEveryKindOnTheTablet() {
        val kinds = tablet.associate { it.userId to ProfilePolicy.kindOf(it) }
        assertEquals(ProfileKind.PRIMARY, kinds[0])
        assertEquals(ProfileKind.WORK, kinds[10])
        assertEquals(ProfileKind.OTHER_USER, kinds[11])
        assertEquals(ProfileKind.DUAL_MESSENGER, kinds[95])
        assertEquals(ProfileKind.SECURE_FOLDER, kinds[150])
    }

    @Test
    fun fullUserIsHiddenUnlessAdvancedIsOn() {
        assertEquals(listOf(10, 95, 150), ProfilePolicy.visibleTargets(tablet, showAdvancedUsers = false).map { it.userId })
        assertEquals(listOf(10, 11, 95, 150), ProfilePolicy.visibleTargets(tablet, showAdvancedUsers = true).map { it.userId })
    }

    @Test
    fun secureFolderNeverAcceptsShellCommands() {
        val knox = ProfilePolicy.visibleTargets(tablet, false).first { it.userId == 150 }
        assertTrue(knox.isRestricted)
        assertFalse(ProfilePolicy.canRunShell(knox))
        assertFalse(ProfilePolicy.canRunShell(ProfileKind.PRIMARY))
        assertTrue(ProfilePolicy.canRunShell(ProfileKind.DUAL_MESSENGER))
        assertTrue(ProfilePolicy.canRunShell(ProfileKind.WORK))
    }

    @Test
    fun pausedChipComesFromQuietFlag() {
        val phone = ProfilePolicy.visibleTargets(PmOutputParser.parseUsers(Fixtures.USERS_PHONE_PAUSED), false)
        assertTrue(phone.first { it.userId == 10 }.isPaused)
        assertFalse(phone.first { it.userId == 95 }.isPaused)
    }

    @Test
    fun defaultSelectionPrefersDualMessengerThenWork() {
        assertEquals(95, ProfilePolicy.defaultSelection(ProfilePolicy.visibleTargets(tablet, false))?.userId)
        val noDual = ProfilePolicy.visibleTargets(tablet.filter { it.userId != 95 }, false)
        assertEquals(10, ProfilePolicy.defaultSelection(noDual)?.userId)
    }

    @Test
    fun hostIdentityCoversReleaseAndDebugIds() {
        val fromDebug = HostIdentity("com.mohitarora.dualclone.debug")
        val fromRelease = HostIdentity("com.mohitarora.dualclone")
        for (h in listOf(fromDebug, fromRelease)) {
            assertTrue(h.isHost("com.mohitarora.dualclone"))
            assertTrue(h.isHost("com.mohitarora.dualclone.debug"))
            assertFalse(h.isHost("com.mohitarora.dualclone.other"))
        }
    }
}
