package com.mohitarora.dualclone.shizuku

import com.mohitarora.dualclone.domain.pm.CommandExit
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/** Starts a privileged process for an argv array. Production uses the Shizuku reflection helper. */
fun interface ProcessFactory {
    fun start(command: Array<String>): Process
}

/**
 * Runs commands as separate processes and owns each process lifecycle.
 * A watchdog destroys the process at the deadline so blocking pipe reads return, which a plain
 * withTimeout cannot do. Cancellation of the caller destroys the process too.
 */
class ProcessShizukuGateway(
    private val processFactory: ProcessFactory,
    private val isAvailable: () -> Boolean,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) : ShizukuGateway {

    override suspend fun run(vararg command: String): CommandOutput = withContext(io) {
        if (!isAvailable()) {
            return@withContext CommandOutput(CommandExit.UNAVAILABLE, "", "Shizuku is not available")
        }
        val proc = try {
            processFactory.start(arrayOf(*command))
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            return@withContext CommandOutput(CommandExit.START_FAILED, "", t.message ?: t.javaClass.simpleName)
        }
        val timedOut = AtomicBoolean(false)
        coroutineScope {
            // NOTE: The reaper runs only when this scope is cancelled or finishes, and then destroys the process to unblock readers.
            val reaper = launch { try { awaitCancellation() } finally { proc.destroyQuietly() } }
            val watchdog = launch {
                delay(timeoutMillis)
                timedOut.set(true)
                proc.destroyQuietly()
            }
            val out = async { proc.inputStream.bufferedReader().use { it.readText() } }
            val err = async { proc.errorStream.bufferedReader().use { it.readText() } }
            val stdout = out.await()
            val stderr = err.await()
            val exit = withTimeoutOrNull(WAIT_AFTER_EOF_MILLIS) { runInterruptible { proc.waitFor() } }
                ?: if (timedOut.get()) -1 else CommandExit.START_FAILED
            watchdog.cancel()
            reaper.cancel()
            CommandOutput(exit, stdout.trim(), stderr.trim(), timedOut.get())
        }
    }

    private fun Process.destroyQuietly() {
        try {
            destroy()
        } catch (_: Throwable) {
            // Process already gone.
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 12_000L
        private const val WAIT_AFTER_EOF_MILLIS = 3_000L
    }
}
