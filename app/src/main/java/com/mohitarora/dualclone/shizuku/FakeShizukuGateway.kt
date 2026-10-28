package com.mohitarora.dualclone.shizuku

import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Scripted gateway for JVM and instrumented tests. Rules match on the space joined command prefix.
 * The most recently registered matching rule wins, so a test can change device state mid test.
 * Unmatched commands return exit code 1.
 */
class FakeShizukuGateway : ShizukuGateway {

    private class Rule(val prefix: String, val respond: (String) -> CommandOutput)

    private val rules = CopyOnWriteArrayList<Rule>()
    val history: MutableList<String> = CopyOnWriteArrayList()

    /** Optional hook that runs before each command, for example to suspend and simulate a slow call. */
    var beforeEach: suspend (String) -> Unit = {}

    fun on(prefix: String, stdout: String = "", exitCode: Int = 0, stderr: String = "") {
        rules.add(0, Rule(prefix) { CommandOutput(exitCode, stdout, stderr) })
    }

    fun on(prefix: String, output: CommandOutput) {
        rules.add(0, Rule(prefix) { output })
    }

    fun onDynamic(prefix: String, respond: (String) -> CommandOutput) {
        rules.add(0, Rule(prefix, respond))
    }

    fun commandsStartingWith(prefix: String): List<String> = history.filter { it.startsWith(prefix) }

    override suspend fun run(vararg command: String): CommandOutput {
        val joined = command.joinToString(" ")
        history.add(joined)
        beforeEach(joined)
        val rule = rules.firstOrNull { joined.startsWith(it.prefix) }
        return rule?.respond?.invoke(joined) ?: CommandOutput(1, "", "no fake rule for: $joined")
    }
}
