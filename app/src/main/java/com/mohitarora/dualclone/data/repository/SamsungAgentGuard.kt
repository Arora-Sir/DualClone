package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.model.SamsungDualMessenger
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.pm.isUnavailable
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.ShizukuGateway

/**
 * Switches Samsung's Dual App agent off and on for the primary user.
 *
 * The agent deletes launchable apps from the Dual Messenger profile when it receives BOOT_COMPLETED, unless they are whitelisted or were installed through adb.
 * Android does not let the shell change a single component of another app, so the only lever Shizuku has is the whole package.
 * A disabled agent receives no broadcast, so the cleanup never runs and clones survive a reboot.
 * The agent also owns the Samsung Dual Messenger settings screen and its whitelist sync, so this stays an opt-in switch and the state is always read back from the device.
 */
class SamsungAgentGuard(
    private val gateway: ShizukuGateway,
    private val audit: CommandAudit
) {
    /** True when the agent is disabled, false when it is enabled, null when it is not installed or Shizuku cannot tell. */
    suspend fun isDisabled(): Boolean? {
        val path = gateway.run("pm", "path", PACKAGE)
        if (path.isUnavailable || path.timedOut || !PmOutputParser.parsePathExists(path)) return null
        val list = gateway.run("pm", "list", "packages", "-d", "--user", "0")
        if (list.isUnavailable || list.timedOut || list.exitCode != 0) return null
        return PACKAGE in PmOutputParser.parsePackages(list.stdout)
    }

    suspend fun setDisabled(disabled: Boolean): OpResult {
        val cmd = if (disabled) arrayOf("pm", "disable-user", "--user", "0", PACKAGE) else arrayOf("pm", "enable", "--user", "0", PACKAGE)
        val out = gateway.run(*cmd)
        if (out.isUnavailable) return OpResult.ShizukuUnavailable
        audit.record(0, PACKAGE, cmd.joinToString(" "), out, out.exitCode == 0 && !out.timedOut)
        val text = (out.stdout + "\n" + out.stderr).trim()
        if (out.timedOut) return OpResult.Failure(PmErrorCode.TIMEOUT, text)
        if (out.exitCode != 0) {
            val code = if (text.contains("SecurityException", ignoreCase = true)) PmErrorCode.SECURITY_EXCEPTION else PmErrorCode.UNKNOWN
            return OpResult.Failure(code, text)
        }
        // NOTE: The state is read back because a device policy can accept the command and still leave the package as it was.
        return if (isDisabled() == disabled) OpResult.Success(verified = true) else OpResult.Failure(PmErrorCode.UNKNOWN, "The agent state did not change")
    }

    private companion object {
        const val PACKAGE = SamsungDualMessenger.PACKAGE
    }
}
