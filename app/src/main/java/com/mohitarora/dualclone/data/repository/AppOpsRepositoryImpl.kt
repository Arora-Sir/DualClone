package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.AppOpModes
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.pm.ParsedUser
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.pm.isUnavailable
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.AppOpsPreset
import com.mohitarora.dualclone.domain.repo.AppOpsPresetStore
import com.mohitarora.dualclone.domain.repo.AppOpsRepository
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import com.mohitarora.dualclone.util.PackageValidator

class AppOpsRepositoryImpl(
    private val gateway: ShizukuGateway,
    private val audit: CommandAudit,
    private val presets: AppOpsPresetStore,
    private val profiles: ProfileRepository
) : AppOpsRepository {

    override suspend fun get(packageName: String, op: String, userId: Int): ListResult<String> {
        if (!PackageValidator.isValidPackageName(packageName) || !PackageValidator.isValidAppOp(op)) {
            return ListResult.Unavailable("Invalid parameters")
        }
        if (!canTarget(userId)) return ListResult.Unavailable("Profile does not accept shell commands")
        val out = gateway.run("appops", "get", "--user", userId.toString(), packageName, op)
        if (out.isUnavailable) return ListResult.Unavailable("Shizuku is not available")
        if (out.exitCode != 0 || out.timedOut) return ListResult.Unavailable(out.stderr.ifBlank { "appops get failed" })
        return ListResult.Loaded(parseMode(out.stdout, op))
    }

    override suspend fun set(packageName: String, op: String, mode: String, userId: Int): OpResult {
        if (!PackageValidator.isValidPackageName(packageName)) return OpResult.Failure(PmErrorCode.UNKNOWN, "Invalid package name")
        if (!PackageValidator.isValidAppOp(op)) return OpResult.Failure(PmErrorCode.UNKNOWN, "Invalid AppOp")
        if (mode !in AppOpModes.ALLOWED) return OpResult.Failure(PmErrorCode.UNKNOWN, "Mode not allowed: $mode")
        if (!canTarget(userId)) return OpResult.Failure(PmErrorCode.USER_RESTRICTED, "Profile does not accept shell commands")
        val cmd = arrayOf("appops", "set", "--user", userId.toString(), packageName, op, mode)
        val out = gateway.run(*cmd)
        if (out.isUnavailable) return OpResult.ShizukuUnavailable
        audit.record(userId, packageName, cmd.joinToString(" "), out, out.exitCode == 0)
        if (out.exitCode != 0) return OpResult.Failure(PmOutputParser.classify(out.stderr + out.stdout), (out.stderr + out.stdout).trim())
        val readBack = (get(packageName, op, userId) as? ListResult.Loaded)?.value
        return OpResult.Success(verified = readBack == mode)
    }

    /** Sets a mode and stores or clears the remembered preset in one step. */
    suspend fun setRemembered(packageName: String, op: String, mode: String, userId: Int, remember: Boolean): OpResult {
        val result = set(packageName, op, mode, userId)
        if (result is OpResult.Success) {
            if (remember) presets.upsert(AppOpsPreset(userId, packageName, op, mode)) else presets.delete(userId, packageName, op)
        }
        return result
    }

    private fun canTarget(userId: Int): Boolean {
        val kind = profiles.profiles.value.firstOrNull { it.userId == userId }?.kind
            ?: ProfilePolicy.kindOf(ParsedUser(userId, "", 0L, true))
        return ProfilePolicy.canRunShell(kind) && kind != ProfileKind.PRIMARY
    }

    /** Output looks like `MANAGE_EXTERNAL_STORAGE: allow` or `No operations.` for defaults. */
    internal fun parseMode(stdout: String, op: String): String {
        val line = stdout.lineSequence().firstOrNull { it.trim().startsWith("$op:") } ?: return "default"
        return line.substringAfter(':').trim().substringBefore(';').trim().ifEmpty { "default" }
    }
}
