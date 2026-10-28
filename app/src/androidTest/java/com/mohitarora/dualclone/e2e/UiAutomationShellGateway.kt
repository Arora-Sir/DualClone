package com.mohitarora.dualclone.e2e

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Runs commands as the shell user through UiAutomation, so CI needs no Shizuku install.
 * UiAutomation exposes stdout only, so each command runs through sh with stderr merged and the exit code appended as a marker.
 */
class UiAutomationShellGateway @Inject constructor() : ShizukuGateway {

    private val automation: UiAutomation
        get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    override suspend fun run(vararg command: String): CommandOutput = try {
        withTimeout(TIMEOUT_MILLIS) { withContext(Dispatchers.IO) { execute(command) } }
    } catch (e: TimeoutCancellationException) {
        CommandOutput(exitCode = -1, stdout = "", stderr = "timed out", timedOut = true)
    }

    private fun execute(command: Array<out String>): CommandOutput {
        val line = command.joinToString(" ") { quote(it) }
        // NOTE: UiAutomation splits the command on spaces and starts no shell, so a quoted sh -c string arrives in pieces.
        // NOTE: Starting plain sh with a read and write pipe and sending the script on stdin avoids all tokenizing.
        val pipes = automation.executeShellCommandRw("sh")
        ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
            it.write("$line 2>&1\necho $EXIT_MARKER\$?\n".toByteArray(Charsets.UTF_8))
        }
        val raw = ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
        val markerIndex = raw.lastIndexOf(EXIT_MARKER)
        if (markerIndex < 0) return CommandOutput(exitCode = -1, stdout = raw, stderr = "exit marker missing")
        val exit = raw.substring(markerIndex + EXIT_MARKER.length).trim().toIntOrNull() ?: -1
        val output = raw.substring(0, markerIndex)
        return if (exit == 0) CommandOutput(0, output, "") else CommandOutput(exit, "", output)
    }

    private fun quote(arg: String) = "'" + arg.replace("'", "'\\''") + "'"

    private companion object {
        const val EXIT_MARKER = "__DC_EXIT:"
        const val TIMEOUT_MILLIS = 15_000L
    }
}
