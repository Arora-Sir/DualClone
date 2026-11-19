package com.mohitarora.dualclone.data

import com.mohitarora.dualclone.data.repository.SamsungAgentGuard
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.pm.CommandExit
import com.mohitarora.dualclone.shizuku.FakeShizukuGateway
import com.mohitarora.dualclone.testing.RecordingAudit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungAgentGuardTest {

    private val gateway = FakeShizukuGateway()
    private val audit = RecordingAudit()
    private val guard = SamsungAgentGuard(gateway, audit)
    private val agent = "com.samsung.android.da.daagent"

    private fun installed() = gateway.on("pm path $agent", "package:/system/app/DAAgent/DAAgent.apk")
    private fun disabledList(vararg names: String) = gateway.on("pm list packages -d --user 0", names.joinToString("\n") { "package:$it" })

    @Test
    fun readsTheDisabledStateFromTheDevice() = runBlocking {
        installed()
        disabledList(agent, "com.example.other")
        assertEquals(true, guard.isDisabled())
        disabledList("com.example.other")
        assertEquals(false, guard.isDisabled())
    }

    @Test
    fun reportsUnknownWhenTheAgentIsNotInstalled() = runBlocking {
        gateway.on("pm path $agent", "", exitCode = 1)
        assertNull(guard.isDisabled())
        assertTrue(gateway.commandsStartingWith("pm list packages").isEmpty())
    }

    @Test
    fun reportsUnknownWhenShizukuIsUnavailable() = runBlocking {
        gateway.on("pm path $agent", "", exitCode = CommandExit.UNAVAILABLE)
        assertNull(guard.isDisabled())
    }

    @Test
    fun disablingRunsDisableUserAndVerifiesByReadingBack() = runBlocking {
        installed()
        disabledList()
        gateway.on("pm disable-user --user 0 $agent", "Package $agent new state: disabled-user")
        gateway.onDynamic("pm list packages -d --user 0") { com.mohitarora.dualclone.domain.repo.CommandOutput(0, "package:$agent", "") }
        assertEquals(OpResult.Success(verified = true), guard.setDisabled(true))
        assertEquals(listOf("pm disable-user --user 0 $agent"), gateway.commandsStartingWith("pm disable-user"))
        assertEquals(1, audit.rows.size)
        assertTrue(audit.rows.single().success)
    }

    @Test
    fun enablingRunsEnableAndVerifiesByReadingBack() = runBlocking {
        installed()
        gateway.on("pm enable --user 0 $agent", "Package $agent new state: enabled")
        disabledList()
        assertEquals(OpResult.Success(verified = true), guard.setDisabled(false))
        assertEquals(1, gateway.commandsStartingWith("pm enable --user 0 $agent").size)
    }

    @Test
    fun aCommandThatSucceedsButChangesNothingIsAFailure() = runBlocking {
        installed()
        disabledList()
        gateway.on("pm disable-user --user 0 $agent", "Package $agent new state: disabled-user")
        assertTrue(guard.setDisabled(true) is OpResult.Failure)
    }

    @Test
    fun aSecurityExceptionIsClassified() = runBlocking {
        gateway.on("pm disable-user --user 0 $agent", "", exitCode = 1, stderr = "java.lang.SecurityException: not allowed")
        val result = guard.setDisabled(true) as OpResult.Failure
        assertEquals(PmErrorCode.SECURITY_EXCEPTION, result.code)
        assertTrue(audit.rows.none { it.success })
    }

    @Test
    fun shizukuLossIsReportedAndNothingIsAudited() = runBlocking {
        gateway.on("pm disable-user --user 0 $agent", "", exitCode = CommandExit.UNAVAILABLE)
        assertEquals(OpResult.ShizukuUnavailable, guard.setDisabled(true))
        assertTrue(audit.rows.isEmpty())
    }
}
