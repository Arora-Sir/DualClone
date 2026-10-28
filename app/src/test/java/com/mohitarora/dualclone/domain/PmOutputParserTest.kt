package com.mohitarora.dualclone.domain

import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.pm.PmOutcome
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PmOutputParserTest {

    @Test
    fun parsesTabletUsersWithSpacedNamesAndRunningState() {
        val users = PmOutputParser.parseUsers(Fixtures.USERS_TABLET)
        assertEquals(listOf(0, 10, 11, 95, 150), users.map { it.userId })
        assertEquals("Island", users[1].name)
        assertTrue(users[1].isManaged)
        assertFalse(users[1].isQuiet)
        assertFalse(users[2].running)
        assertTrue(users[4].isKnox)
    }

    @Test
    fun quietModeFlagMarksPausedProfile() {
        val island = PmOutputParser.parseUsers(Fixtures.USERS_PHONE_PAUSED).first { it.userId == 10 }
        assertEquals(0x10b0L, island.flags)
        assertTrue(island.isQuiet)
        assertTrue(island.isManaged)
        assertFalse(island.running)
    }

    @Test
    fun userNamedRunningIsNotMisreadAsRunning() {
        val users = PmOutputParser.parseUsers("\tUserInfo{12:running:10}\n")
        assertFalse(users.single().running)
    }

    @Test
    fun parsesPackageLists() {
        assertEquals(
            setOf("com.android.settings", "com.popularapp.thirtydayfitnesschallenge", "com.whatsapp"),
            PmOutputParser.parsePackages(Fixtures.PACKAGES)
        )
        assertEquals(setOf("com.x.y"), PmOutputParser.parsePackages("package:/data/app/base.apk=com.x.y\nerror line"))
        assertTrue(PmOutputParser.parsePackages("Error: unknown user").isEmpty())
    }

    @Test
    fun installExistingSuccessAndFailures() {
        assertEquals(
            PmOutcome.Success,
            PmOutputParser.parseInstallExisting(CommandOutput(0, "Package com.x.y installed for user: 95", ""))
        )
        val missing = PmOutputParser.parseInstallExisting(
            CommandOutput(1, "", "android.content.pm.PackageManager\$NameNotFoundException: Package com.x.y doesn't exist")
        ) as PmOutcome.Failure
        assertEquals(PmErrorCode.NOT_INSTALLED_FOR_USER, missing.code)
        val restricted = PmOutputParser.parseInstallExisting(CommandOutput(1, "Failure [INSTALL_FAILED_USER_RESTRICTED]", ""))
        assertEquals(PmErrorCode.USER_RESTRICTED, (restricted as PmOutcome.Failure).code)
        val timeout = PmOutputParser.parseInstallExisting(CommandOutput(-1, "", "", timedOut = true))
        assertEquals(PmErrorCode.TIMEOUT, (timeout as PmOutcome.Failure).code)
    }

    @Test
    fun uninstallOutputsMapToErrorCodes() {
        assertEquals(PmOutcome.Success, PmOutputParser.parseUninstall(CommandOutput(0, "Success", "")))
        fun code(text: String) = (PmOutputParser.parseUninstall(CommandOutput(1, text, "")) as PmOutcome.Failure).code
        assertEquals(PmErrorCode.NOT_INSTALLED_FOR_USER, code("Failure [not installed for 10]"))
        assertEquals(PmErrorCode.DEVICE_POLICY_BLOCKED, code("Failure [DELETE_FAILED_DEVICE_POLICY_MANAGER]"))
        assertEquals(PmErrorCode.OWNER_BLOCKED, code("Failure [DELETE_FAILED_OWNER_BLOCKED]"))
        assertEquals(PmErrorCode.USER_RESTRICTED, code("Failure [DELETE_FAILED_USER_RESTRICTED]"))
        assertEquals(PmErrorCode.SECURITY_EXCEPTION, code("java.lang.SecurityException: Permission Denial"))
        assertEquals(PmErrorCode.UNKNOWN, code("something odd"))
    }

    @Test
    fun exitZeroWithoutSuccessWordIsNotSuccess() {
        assertTrue(PmOutputParser.parseUninstall(CommandOutput(0, "Failure [x]", "")) is PmOutcome.Failure)
    }

    @Test
    fun pathProbe() {
        assertTrue(PmOutputParser.parsePathExists(CommandOutput(0, "package:/data/app/x/base.apk", "")))
        assertFalse(PmOutputParser.parsePathExists(CommandOutput(1, "", "")))
        assertFalse(PmOutputParser.parsePathExists(CommandOutput(0, "", "")))
    }

    @Test
    fun dumpsysUserStateDistinguishesHiddenAndSuspended() {
        val hidden = PmOutputParser.parsePackageUserState(Fixtures.DUMPSYS_USER_LINES, 10)!!
        assertTrue(hidden.hidden)
        assertFalse(hidden.installed)
        val suspended = PmOutputParser.parsePackageUserState(Fixtures.DUMPSYS_USER_LINES, 95)!!
        assertTrue(suspended.suspended)
        assertTrue(suspended.installed)
        assertNull(PmOutputParser.parsePackageUserState(Fixtures.DUMPSYS_USER_LINES, 11))
    }

    @Test
    fun ownerPackagesComeFromDpmOutput() {
        assertEquals(
            setOf("com.oasisfeng.island", "com.samsung.knox.securefolder"),
            PmOutputParser.parseOwnerPackages(Fixtures.DPM_OWNERS)
        )
    }
}
