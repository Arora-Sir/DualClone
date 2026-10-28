package com.mohitarora.dualclone.shizuku

import com.mohitarora.dualclone.domain.pm.CommandExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class ProcessShizukuGatewayTest {

    /** A process whose pipes block until destroy() is called, like a hung pm command. */
    private class HungProcess : Process() {
        val destroyed = AtomicBoolean(false)
        private val latch = CountDownLatch(1)

        private fun blockingStream() = object : InputStream() {
            override fun read(): Int {
                latch.await()
                return -1
            }
        }

        private val out = blockingStream()
        private val err = blockingStream()
        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
        override fun getInputStream(): InputStream = out
        override fun getErrorStream(): InputStream = err
        override fun waitFor(): Int {
            latch.await()
            return 143
        }
        override fun exitValue(): Int = 143
        override fun destroy() {
            destroyed.set(true)
            latch.countDown()
        }
    }

    private class DoneProcess(private val stdout: String, private val stderr: String, private val code: Int) : Process() {
        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
        override fun getInputStream(): InputStream = ByteArrayInputStream(stdout.toByteArray())
        override fun getErrorStream(): InputStream = ByteArrayInputStream(stderr.toByteArray())
        override fun waitFor(): Int = code
        override fun exitValue(): Int = code
        override fun destroy() = Unit
    }

    @Test
    fun watchdogDestroysHungProcessAtDeadline() = runBlocking {
        val proc = HungProcess()
        val gateway = ProcessShizukuGateway({ proc }, { true }, Dispatchers.IO, timeoutMillis = 200)
        val started = System.nanoTime()
        val out = gateway.run("pm", "list", "packages")
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(out.timedOut)
        assertTrue(proc.destroyed.get())
        assertTrue("took $elapsedMs ms", elapsedMs < 5_000)
    }

    @Test
    fun collectsStdoutStderrAndExitCode() = runBlocking {
        val gateway = ProcessShizukuGateway({ DoneProcess(" hello \n", "warn", 3) }, { true }, Dispatchers.IO, 1_000)
        val out = gateway.run("x")
        assertEquals(3, out.exitCode)
        assertEquals("hello", out.stdout)
        assertEquals("warn", out.stderr)
        assertFalse(out.timedOut)
    }

    @Test
    fun unavailableShizukuNeverStartsAProcess() = runBlocking {
        var started = false
        val gateway = ProcessShizukuGateway({ started = true; DoneProcess("", "", 0) }, { false }, Dispatchers.IO, 1_000)
        val out = gateway.run("x")
        assertEquals(CommandExit.UNAVAILABLE, out.exitCode)
        assertFalse(started)
    }

    @Test
    fun startFailureIsReportedNotThrown() = runBlocking {
        val gateway = ProcessShizukuGateway({ error("boom") }, { true }, Dispatchers.IO, 1_000)
        val out = gateway.run("x")
        assertEquals(CommandExit.START_FAILED, out.exitCode)
        assertTrue(out.stderr.contains("boom"))
    }
}
