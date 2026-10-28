package com.mohitarora.dualclone.shizuku

import android.content.pm.PackageManager
import com.mohitarora.dualclone.domain.pm.CommandExit
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/** Production gateway that executes commands through the Shizuku binder. */
@Singleton
class ShizukuGatewayImpl @Inject constructor() : ShizukuGateway {

    private val selfTest: Result<Unit> by lazy { ShizukuProcessHelper.selfTest() }

    private val delegate = ProcessShizukuGateway(
        processFactory = { ShizukuProcessHelper.newProcess(it) },
        isAvailable = ::binderReady
    )

    override suspend fun run(vararg command: String): CommandOutput {
        selfTest.exceptionOrNull()?.let {
            return CommandOutput(CommandExit.START_FAILED, "", "Shizuku.newProcess is missing: ${it.message}")
        }
        return delegate.run(*command)
    }

    private fun binderReady(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }
}
