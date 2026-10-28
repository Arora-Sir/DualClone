package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.SamsungDualMessenger
import com.mohitarora.dualclone.domain.pm.isUnavailable
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import kotlinx.coroutines.delay

/** Brings up the Samsung Dual Messenger agent so User 95 exists. One component constant serves every caller. */
class DualMessengerSetup(
    private val gateway: ShizukuGateway,
    private val audit: CommandAudit,
    private val profiles: ProfileRepository
) {
    /** Returns null on success or a short reason on failure. */
    suspend fun enable(): String? {
        val install = arrayOf("pm", "install-existing", "--user", "0", SamsungDualMessenger.PACKAGE)
        val installOut = gateway.run(*install)
        if (installOut.isUnavailable) return "Shizuku is not available"
        audit.record(0, SamsungDualMessenger.PACKAGE, install.joinToString(" "), installOut, installOut.exitCode == 0)

        val start = arrayOf("am", "start", "-n", SamsungDualMessenger.COMPONENT)
        val startOut = gateway.run(*start)
        audit.record(0, SamsungDualMessenger.PACKAGE, start.joinToString(" "), startOut, startOut.exitCode == 0)
        if (installOut.exitCode != 0 && startOut.exitCode != 0) {
            return startOut.stderr.ifBlank { startOut.stdout }.ifBlank { "The Dual Messenger agent could not be started" }
        }
        delay(SETTLE_MILLIS)
        profiles.refresh()
        return null
    }

    private companion object {
        const val SETTLE_MILLIS = 1_500L
    }
}
