package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.BulkItemResult
import com.mohitarora.dualclone.domain.model.CloneOriginKind
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OrphanEntry
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.pm.ParsedUser
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.pm.isUnavailable
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.CloneRepository
import com.mohitarora.dualclone.domain.repo.CloneStateStore
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway

/**
 * Finds packages that live in a secondary profile and have no host app in User 0.
 * System apps, profile owners and device policy controllers are never reported.
 */
class OrphanRepository(
    private val gateway: ShizukuGateway,
    private val store: CloneStateStore,
    private val profiles: ProfileRepository,
    private val clones: CloneRepository,
    private val host: HostIdentity
) {

    suspend fun detect(userId: Int): ListResult<List<OrphanEntry>> {
        val kind = profiles.profiles.value.firstOrNull { it.userId == userId }?.kind
            ?: ProfilePolicy.kindOf(ParsedUser(userId, "", 0L, true))
        if (!ProfilePolicy.canRunShell(kind)) return ListResult.Unavailable("Profile does not accept shell commands")

        val inProfile = gateway.run("pm", "list", "packages", "-3", "--user", userId.toString())
        if (inProfile.isUnavailable) return ListResult.Unavailable("Shizuku is not available")
        if (inProfile.exitCode != 0 || inProfile.timedOut) return ListResult.Unavailable("Could not list the profile")
        val inHost = gateway.run("pm", "list", "packages", "--user", "0")
        if (inHost.exitCode != 0 || inHost.timedOut) return ListResult.Unavailable("Could not list the host user")

        val profilePackages = PmOutputParser.parsePackages(inProfile.stdout)
        val hostPackages = PmOutputParser.parsePackages(inHost.stdout)
        if (hostPackages.isEmpty()) return ListResult.Unavailable("Host package list was empty")

        // NOTE: -3 already leaves out system apps. The system list and stored SYSTEM rows are a second guard.
        val systemPackages = PmOutputParser.parsePackages(
            gateway.run("pm", "list", "packages", "-s", "--user", userId.toString()).stdout
        )
        val owners = PmOutputParser.parseOwnerPackages(gateway.run("dpm", "list-owners").stdout) + ProfilePolicy.KNOWN_PROFILE_MANAGERS
        val prior = store.load(userId)

        val orphans = (profilePackages - hostPackages - owners - systemPackages)
            .filter { !host.isHost(it) && prior[it]?.origin.let { o -> o != CloneOriginKind.SYSTEM && o != CloneOriginKind.PROFILE_OWNER } }
            .sorted()
            .map { pkg ->
                val known = prior[pkg]
                OrphanEntry(
                    packageName = pkg,
                    userId = userId,
                    onlyInProfile = known?.origin != CloneOriginKind.DUALCLONE,
                    lastVerifiedMillis = known?.verifiedAtMillis ?: 0L
                )
            }
        return ListResult.Loaded(orphans)
    }

    /** Detects across every shell capable profile. */
    suspend fun detectAll(): Map<Int, ListResult<List<OrphanEntry>>> =
        profiles.profiles.value
            .filter { ProfilePolicy.canRunShell(it) && it.kind != ProfileKind.OTHER_USER }
            .associate { it.userId to detect(it.userId) }

    /** Removes the given packages with the same verified path as a normal removal. */
    suspend fun purge(packageNames: List<String>, userId: Int): List<BulkItemResult> =
        clones.removeAll(packageNames, userId)
}
