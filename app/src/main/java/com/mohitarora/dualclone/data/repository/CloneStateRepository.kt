package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.BulkItemResult
import com.mohitarora.dualclone.domain.model.CloneEntry
import com.mohitarora.dualclone.domain.model.CloneOriginKind
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.model.SamsungLauncher
import com.mohitarora.dualclone.domain.model.StoredClone
import com.mohitarora.dualclone.domain.pm.ParsedUser
import com.mohitarora.dualclone.domain.pm.PmOutcome
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.pm.isUnavailable
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.Clock
import com.mohitarora.dualclone.domain.repo.CloneRepository
import com.mohitarora.dualclone.domain.repo.CloneStateStore
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.HostPackages
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import com.mohitarora.dualclone.util.PackageValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Single writer for clone state.
 *
 * Every mutation of one user runs under that user's mutex and bumps a generation counter.
 * A reconcile reads the generation before it lists packages and discards its snapshot if the counter moved,
 * so a stale snapshot can never overwrite the result of a newer operation.
 * A failed or empty listing never replaces state. It only marks the user as stale.
 */
class CloneStateRepository(
    private val gateway: ShizukuGateway,
    private val store: CloneStateStore,
    private val audit: CommandAudit,
    private val hostPackages: HostPackages,
    private val profiles: ProfileRepository,
    private val host: HostIdentity,
    private val clock: Clock
) : CloneRepository {

    private val locks = ConcurrentHashMap<Int, Mutex>()
    private val generations = ConcurrentHashMap<Int, AtomicLong>()

    private val _staleUsers = MutableStateFlow<Set<Int>>(emptySet())

    /** Users whose last listing failed, so the shown state may be out of date. */
    val staleUsers: StateFlow<Set<Int>> = _staleUsers.asStateFlow()

    private val _launcherRefreshHint = MutableStateFlow(false)

    /** True after a verified removal in a secondary profile, until the launcher is refreshed. */
    val launcherRefreshHint: StateFlow<Boolean> = _launcherRefreshHint.asStateFlow()

    private fun lock(userId: Int) = locks.getOrPut(userId) { Mutex() }
    private fun generation(userId: Int) = generations.getOrPut(userId) { AtomicLong() }

    override fun clonesFor(userId: Int): Flow<Map<String, CloneEntry>> =
        combine(store.observe(userId), hostPackages.observe()) { stored, hostPkgs ->
            stored.values
                .filter { !host.isHost(it.packageName) }
                .associate { it.packageName to it.toEntry(hostInstalled = it.packageName in hostPkgs) }
        }

    /** User ids that hold a copy of this app, so the Home screen can offer a repair. */
    fun hostCopies(): Flow<List<Pair<Int, String>>> = store.observeAll().map { all ->
        all.filter { host.isHost(it.packageName) && it.state == CloneState.INSTALLED }
            .map { it.userId to it.packageName }
    }

    private fun kindOf(userId: Int): ProfileKind =
        profiles.profiles.value.firstOrNull { it.userId == userId }?.kind
            ?: ProfilePolicy.kindOf(ParsedUser(userId, "", 0L, true))

    // region reconcile

    override suspend fun reconcile(userId: Int): ListResult<Int> {
        if (!ProfilePolicy.canRunShell(kindOf(userId))) {
            return ListResult.Unavailable("Profile does not accept shell commands")
        }
        repeat(MAX_RECONCILE_ATTEMPTS) {
            val startGeneration = generation(userId).get()
            val installed = when (val listed = listPackages(userId, includeUninstalled = false)) {
                is ListResult.Unavailable -> {
                    markStale(userId)
                    return listed
                }
                is ListResult.Loaded -> listed.value
            }
            if (installed.isEmpty()) {
                // NOTE: Every real profile holds at least some system packages, so an empty list means the command misbehaved.
                markStale(userId)
                return ListResult.Unavailable("Package list was empty")
            }
            val prior = store.load(userId)
            // NOTE: System apps of the profile are not clones. If this listing fails the previous classification is kept.
            val systemInProfile = (listPackages(userId, includeUninstalled = false, systemOnly = true) as? ListResult.Loaded)?.value
            // NOTE: The profile owner (Island, Shelter, a DPC) is found through dpm and a built-in list. It is never a clone.
            val ownerOut = gateway.run("dpm", "list-owners")
            val owners = (if (ownerOut.exitCode == 0) PmOutputParser.parseOwnerPackagesForUser(ownerOut.stdout, userId) else emptySet()) +
                ProfilePolicy.KNOWN_PROFILE_MANAGERS
            fun originOf(pkg: String): CloneOriginKind = when {
                pkg in owners -> CloneOriginKind.PROFILE_OWNER
                systemInProfile != null && pkg in systemInProfile -> CloneOriginKind.SYSTEM
                systemInProfile == null -> prior[pkg]?.origin?.takeIf { it != CloneOriginKind.PROFILE_OWNER } ?: CloneOriginKind.EXTERNAL
                else -> prior[pkg]?.origin?.takeIf { it != CloneOriginKind.SYSTEM && it != CloneOriginKind.PROFILE_OWNER } ?: CloneOriginKind.EXTERNAL
            }
            val probed = probeAmbiguous(userId, installed, prior.keys)
            var applied = false
            lock(userId).withLock {
                if (generation(userId).get() == startGeneration) {
                    val now = clock.now()
                    val entries = installed.map { pkg ->
                        StoredClone(pkg, userId, CloneState.INSTALLED, originOf(pkg), now)
                    } + probed.map { (pkg, state) ->
                        StoredClone(pkg, userId, state, originOf(pkg), now)
                    }
                    store.replace(userId, entries)
                    applied = true
                }
            }
            if (applied) {
                _staleUsers.update { it - userId }
                return ListResult.Loaded(installed.size)
            }
            Timber.tag("CloneState").d("Discarded stale snapshot for user %d, retrying", userId)
        }
        // NOTE: Operations kept changing state during every attempt, so the store already holds the newest facts.
        return ListResult.Loaded(store.load(userId).count { it.value.state == CloneState.INSTALLED })
    }

    private suspend fun listPackages(userId: Int, includeUninstalled: Boolean, systemOnly: Boolean = false): ListResult<Set<String>> {
        val args = buildList {
            addAll(listOf("pm", "list", "packages"))
            if (includeUninstalled) add("-u")
            if (systemOnly) add("-s")
            addAll(listOf("--user", userId.toString()))
        }
        val out = gateway.run(*args.toTypedArray())
        return when {
            out.isUnavailable -> ListResult.Unavailable("Shizuku is not available")
            out.timedOut -> ListResult.Unavailable("Command timed out")
            out.exitCode != 0 -> ListResult.Unavailable(out.stderr.ifBlank { "pm list packages failed with exit code ${out.exitCode}" })
            else -> ListResult.Loaded(PmOutputParser.parsePackages(out.stdout))
        }
    }

    /**
     * Packages that appear only in `pm list packages -u` are ambiguous. They may be hidden or suspended by a
     * profile owner such as Island, or simply uninstalled. Only packages DualClone already knows about are probed.
     */
    private suspend fun probeAmbiguous(userId: Int, installed: Set<String>, known: Set<String>): Map<String, CloneState> {
        val withUninstalled = (listPackages(userId, includeUninstalled = true) as? ListResult.Loaded)?.value ?: return emptyMap()
        val candidates = (withUninstalled - installed).filter { it in known && !host.isHost(it) }.take(MAX_PROBES)
        val result = LinkedHashMap<String, CloneState>()
        for (pkg in candidates) {
            val dump = gateway.run("dumpsys", "package", pkg)
            if (dump.exitCode != 0) continue
            val state = PmOutputParser.parsePackageUserState(dump.stdout, userId) ?: continue
            when {
                state.hidden -> result[pkg] = CloneState.HIDDEN
                state.installed && state.suspended -> result[pkg] = CloneState.SUSPENDED
            }
        }
        return result
    }

    private fun markStale(userId: Int) = _staleUsers.update { it + userId }

    // endregion

    // region mutations

    override suspend fun clone(packageName: String, userId: Int): OpResult {
        precheck(packageName, userId, allowHost = false)?.let { return it }
        return lock(userId).withLock {
            val cmd = arrayOf("pm", "install-existing", "--user", userId.toString(), packageName)
            val out = gateway.run(*cmd)
            if (out.isUnavailable) return@withLock OpResult.ShizukuUnavailable
            val outcome = PmOutputParser.parseInstallExisting(out)
            audit.record(userId, packageName, cmd.joinToString(" "), out, outcome is PmOutcome.Success)
            if (outcome is PmOutcome.Failure) return@withLock OpResult.Failure(outcome.code, outcome.raw)

            val verify = verifyPath(userId, packageName)
            when (verify) {
                Verification.PRESENT, Verification.UNKNOWN -> {
                    store.put(StoredClone(packageName, userId, CloneState.INSTALLED, CloneOriginKind.DUALCLONE,
                        if (verify == Verification.PRESENT) clock.now() else 0L))
                    generation(userId).incrementAndGet()
                    OpResult.Success(verified = verify == Verification.PRESENT)
                }
                Verification.ABSENT -> OpResult.Failure(
                    PmErrorCode.NOT_INSTALLED_FOR_USER,
                    "pm reported success but pm path found no APK for user $userId"
                )
            }
        }
    }

    override suspend fun remove(packageName: String, userId: Int): OpResult {
        precheck(packageName, userId, allowHost = true)?.let { return it }
        if (packageName in ProfilePolicy.KNOWN_PROFILE_MANAGERS || store.load(userId)[packageName]?.origin == CloneOriginKind.PROFILE_OWNER) {
            return OpResult.Failure(PmErrorCode.OWNER_BLOCKED, "The profile owner is never removed because that would delete the profile")
        }
        if (store.load(userId)[packageName]?.origin == CloneOriginKind.SYSTEM) {
            return OpResult.Failure(PmErrorCode.USER_RESTRICTED, "System apps of a profile are not clones and are never removed")
        }
        return lock(userId).withLock {
            val cmd = arrayOf("pm", "uninstall", "--user", userId.toString(), packageName)
            val out = gateway.run(*cmd)
            if (out.isUnavailable) return@withLock OpResult.ShizukuUnavailable
            val outcome = PmOutputParser.parseUninstall(out)
            audit.record(userId, packageName, cmd.joinToString(" "), out, outcome is PmOutcome.Success)

            if (outcome is PmOutcome.Failure && outcome.code != PmErrorCode.NOT_INSTALLED_FOR_USER) {
                return@withLock OpResult.Failure(outcome.code, outcome.raw)
            }
            // NOTE: "not installed for user" means the package is already gone, so state is healed instead of reported as an error.
            val verify = verifyPath(userId, packageName)
            if (verify == Verification.PRESENT) {
                return@withLock OpResult.Failure(PmErrorCode.UNKNOWN, "Package is still present for user $userId after uninstall")
            }
            store.remove(packageName, userId)
            generation(userId).incrementAndGet()
            if (kindOf(userId) != ProfileKind.PRIMARY) _launcherRefreshHint.value = true
            OpResult.Success(verified = verify == Verification.ABSENT)
        }
    }

    override suspend fun cloneAll(packageNames: List<String>, userId: Int): List<BulkItemResult> =
        bulk(packageNames) { clone(it, userId) }

    override suspend fun removeAll(packageNames: List<String>, userId: Int): List<BulkItemResult> =
        bulk(packageNames) { remove(it, userId) }

    private suspend fun bulk(packages: List<String>, op: suspend (String) -> OpResult): List<BulkItemResult> {
        val results = ArrayList<BulkItemResult>(packages.size)
        var unavailable = false
        for (pkg in packages.distinct()) {
            val result = if (unavailable) OpResult.ShizukuUnavailable else try {
                op(pkg)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                OpResult.Failure(PmErrorCode.UNKNOWN, t.message.orEmpty())
            }
            if (result is OpResult.ShizukuUnavailable) unavailable = true
            results.add(BulkItemResult(pkg, result))
        }
        return results
    }

    /**
     * Restarts One UI Home to clear stale icons. Runs only when the Samsung launcher package exists,
     * so the call is a harmless no-op on other devices.
     */
    override suspend fun refreshLauncher(): Boolean {
        val exists = PmOutputParser.parsePathExists(gateway.run("pm", "path", SamsungLauncher.PACKAGE))
        if (!exists) return false
        val cmd = arrayOf("am", "force-stop", SamsungLauncher.PACKAGE)
        val out = gateway.run(*cmd)
        audit.record(0, SamsungLauncher.PACKAGE, cmd.joinToString(" "), out, out.exitCode == 0)
        if (out.exitCode == 0) _launcherRefreshHint.value = false
        return out.exitCode == 0
    }

    fun dismissLauncherHint() {
        _launcherRefreshHint.value = false
    }

    private fun precheck(packageName: String, userId: Int, allowHost: Boolean): OpResult.Failure? {
        if (!PackageValidator.isValidPackageName(packageName)) {
            return OpResult.Failure(PmErrorCode.UNKNOWN, "Invalid package name")
        }
        if (!allowHost && host.isHost(packageName)) {
            return OpResult.Failure(PmErrorCode.USER_RESTRICTED, "DualClone cannot clone itself")
        }
        if (!ProfilePolicy.canRunShell(kindOf(userId))) {
            return OpResult.Failure(PmErrorCode.USER_RESTRICTED, "Profile does not accept shell commands")
        }
        return null
    }

    private enum class Verification { PRESENT, ABSENT, UNKNOWN }

    private suspend fun verifyPath(userId: Int, packageName: String): Verification {
        val cmd = arrayOf("pm", "path", "--user", userId.toString(), packageName)
        val out: CommandOutput = gateway.run(*cmd)
        audit.record(userId, packageName, cmd.joinToString(" "), out, out.exitCode == 0)
        return when {
            out.isUnavailable || out.timedOut -> Verification.UNKNOWN
            PmOutputParser.parsePathExists(out) -> Verification.PRESENT
            else -> Verification.ABSENT
        }
    }

    // endregion

    /** Drops state for users that no longer exist. */
    suspend fun pruneUsers(existingUserIds: Set<Int>) = store.retainUsers(existingUserIds)

    companion object {
        private const val MAX_RECONCILE_ATTEMPTS = 3
        private const val MAX_PROBES = 20
    }
}
