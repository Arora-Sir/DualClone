package com.mohitarora.dualclone.domain.pm

import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.repo.CommandOutput

/** Exit codes synthesized by the gateway for conditions where no process ran to completion. */
object CommandExit {
    const val UNAVAILABLE = -100
    const val START_FAILED = -101
}

val CommandOutput.isUnavailable: Boolean get() = exitCode == CommandExit.UNAVAILABLE

/** Flag bits from android.content.pm.UserInfo plus the Samsung Knox bits seen on One UI devices. */
object UserFlags {
    const val RESTRICTED = 0x08L
    const val MANAGED_PROFILE = 0x20L
    const val DISABLED = 0x40L
    const val QUIET_MODE = 0x80L
    const val FULL = 0x400L
    const val PROFILE = 0x1000L
    const val KNOX_MASK = 0x60000L
}

data class ParsedUser(val userId: Int, val name: String, val flags: Long, val running: Boolean) {
    val isManaged: Boolean get() = flags and UserFlags.MANAGED_PROFILE != 0L
    val isQuiet: Boolean get() = flags and UserFlags.QUIET_MODE != 0L
    val isKnox: Boolean get() = flags and UserFlags.KNOX_MASK != 0L
}

/** Per-user package state from dumpsys package output. */
data class PackageUserState(val installed: Boolean, val hidden: Boolean, val suspended: Boolean)

sealed interface PmOutcome {
    data object Success : PmOutcome
    data class Failure(val code: PmErrorCode, val raw: String) : PmOutcome
}

/** Pure parsing of pm, dumpsys and dpm output. No Android imports so it runs in plain JVM tests. */
object PmOutputParser {

    private val USER_REGEX = Regex("""UserInfo\{(\d+):(.*):([0-9a-fA-F]+)\}(.*)""")
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
    private val OWNER_REGEX = Regex("""admin=([A-Za-z0-9_.]+)/""")

    fun parseUsers(output: String): List<ParsedUser> = output.lineSequence().mapNotNull { line ->
        val m = USER_REGEX.find(line) ?: return@mapNotNull null
        val id = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val flags = m.groupValues[3].toLongOrNull(16) ?: 0L
        // NOTE: Only the text after the closing brace is checked so a user named "running" is not misread.
        val running = m.groupValues[4].contains("running", ignoreCase = true)
        ParsedUser(id, m.groupValues[2].trim(), flags, running)
    }.toList()

    fun parsePackages(output: String): Set<String> = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").substringAfterLast('=').trim() }
        .filter { PACKAGE_NAME_REGEX.matches(it) }
        .toSet()

    /** True when `pm path` printed at least one APK path for the user. */
    fun parsePathExists(out: CommandOutput): Boolean =
        out.exitCode == 0 && out.stdout.lineSequence().any { it.trim().startsWith("package:") }

    fun parseInstallExisting(out: CommandOutput): PmOutcome {
        val text = combined(out)
        if (out.timedOut) return PmOutcome.Failure(PmErrorCode.TIMEOUT, text)
        if (out.exitCode == 0 && text.contains("installed for user", ignoreCase = true)) return PmOutcome.Success
        return PmOutcome.Failure(classify(text), text)
    }

    fun parseUninstall(out: CommandOutput): PmOutcome {
        val text = combined(out)
        if (out.timedOut) return PmOutcome.Failure(PmErrorCode.TIMEOUT, text)
        val success = text.lineSequence().any { it.trim().equals("Success", ignoreCase = true) }
        if (out.exitCode == 0 && success) return PmOutcome.Success
        return PmOutcome.Failure(classify(text), text)
    }

    /** Maps raw pm text to a PmErrorCode. Order matters because several messages nest. */
    fun classify(text: String): PmErrorCode = when {
        text.contains("DELETE_FAILED_DEVICE_POLICY_MANAGER") -> PmErrorCode.DEVICE_POLICY_BLOCKED
        text.contains("DELETE_FAILED_OWNER_BLOCKED") -> PmErrorCode.OWNER_BLOCKED
        text.contains("DELETE_FAILED_USER_RESTRICTED") || text.contains("INSTALL_FAILED_USER_RESTRICTED") ||
            text.contains("user restriction", ignoreCase = true) -> PmErrorCode.USER_RESTRICTED
        text.contains("SecurityException") || text.contains("Permission Denial") -> PmErrorCode.SECURITY_EXCEPTION
        text.contains("not installed for", ignoreCase = true) ||
            text.contains("doesn't exist", ignoreCase = true) -> PmErrorCode.NOT_INSTALLED_FOR_USER
        else -> PmErrorCode.UNKNOWN
    }

    fun parsePackageUserState(dumpsys: String, userId: Int): PackageUserState? {
        val prefix = Regex("""^\s*User $userId: .*installed=""")
        val line = dumpsys.lineSequence().firstOrNull { prefix.containsMatchIn(it) } ?: return null
        fun flag(name: String): Boolean? =
            Regex("""\b$name=(true|false)""").find(line)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        return PackageUserState(
            installed = flag("installed") ?: return null,
            hidden = flag("hidden") ?: false,
            suspended = flag("suspended") ?: false
        )
    }

    /** Packages that act as device or profile owner according to `dpm list-owners`. */
    fun parseOwnerPackages(output: String): Set<String> =
        OWNER_REGEX.findAll(output).map { it.groupValues[1] }.toSet()

    /** Owner packages named for one user in `dpm list-owners`, for example `User 10: admin=com.oasisfeng.island/...`. */
    fun parseOwnerPackagesForUser(output: String, userId: Int): Set<String> = output.lineSequence()
        .filter { it.trimStart().startsWith("User $userId:") }
        .flatMap { line -> OWNER_REGEX.findAll(line).map { it.groupValues[1] } }
        .toSet()

    private fun combined(out: CommandOutput): String = (out.stdout + "\n" + out.stderr).trim()
}
