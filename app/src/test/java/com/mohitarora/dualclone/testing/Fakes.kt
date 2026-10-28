package com.mohitarora.dualclone.testing

import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.model.StoredClone
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.Clock
import com.mohitarora.dualclone.domain.repo.CloneStateStore
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.HostPackages
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ReconcileStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class InMemoryCloneStateStore : CloneStateStore {
    private val rows = MutableStateFlow<Map<Pair<Int, String>, StoredClone>>(emptyMap())
    var replaceCount = 0
        private set

    override fun observe(userId: Int): Flow<Map<String, StoredClone>> =
        rows.map { all -> all.filterKeys { it.first == userId }.mapKeys { it.key.second } }

    override fun observeAll(): Flow<List<StoredClone>> = rows.map { it.values.toList() }

    override suspend fun load(userId: Int): Map<String, StoredClone> =
        rows.value.filterKeys { it.first == userId }.mapKeys { it.key.second }

    override suspend fun replace(userId: Int, entries: Collection<StoredClone>) {
        replaceCount++
        rows.value = rows.value.filterKeys { it.first != userId } + entries.associateBy { it.userId to it.packageName }
    }

    override suspend fun put(entry: StoredClone) {
        rows.value = rows.value + ((entry.userId to entry.packageName) to entry)
    }

    override suspend fun remove(packageName: String, userId: Int) {
        rows.value = rows.value - (userId to packageName)
    }

    override suspend fun retainUsers(userIds: Set<Int>) {
        rows.value = rows.value.filterKeys { it.first in userIds }
    }
}

class RecordingAudit : CommandAudit {
    data class Row(val userId: Int, val packageName: String, val command: String, val exitCode: Int, val success: Boolean)

    val rows = mutableListOf<Row>()

    override suspend fun record(userId: Int, packageName: String, command: String, output: CommandOutput, success: Boolean) {
        rows.add(Row(userId, packageName, command, output.exitCode, success))
    }
}

class FakeHostPackages(initial: Set<String> = emptySet()) : HostPackages {
    private val state = MutableStateFlow(initial)
    override fun observe(): Flow<Set<String>> = state
    override suspend fun snapshot(): Set<String> = state.value
}

class FakeProfileRepository(initial: List<Profile> = emptyList()) : ProfileRepository {
    private val list = MutableStateFlow(initial)
    private val sel = MutableStateFlow(initial.firstOrNull())
    override val profiles: StateFlow<List<Profile>> = list.asStateFlow()
    override val selected: StateFlow<Profile?> = sel.asStateFlow()
    override suspend fun refresh() = Unit
    override fun select(userId: Int) {
        sel.value = list.value.firstOrNull { it.userId == userId }
    }
}

class FakeClock(var nowMillis: Long = 1_000L) : Clock {
    override fun now(): Long = nowMillis
}

class InMemorySettings : AppSettings {
    private val advanced = MutableStateFlow(false)
    private val auto = MutableStateFlow(false)
    private val selected = MutableStateFlow<Int?>(null)
    private val reconcile = MutableStateFlow(ReconcileStatus())
    override val showAdvancedUsers: StateFlow<Boolean> = advanced
    override val autoRefreshLauncher: StateFlow<Boolean> = auto
    override val selectedProfileId: StateFlow<Int?> = selected
    override val lastReconcile: StateFlow<ReconcileStatus> = reconcile
    override fun setShowAdvancedUsers(value: Boolean) { advanced.value = value }
    override fun setAutoRefreshLauncher(value: Boolean) { auto.value = value }
    override fun setSelectedProfileId(userId: Int?) { selected.value = userId }
    override fun setLastReconcile(status: ReconcileStatus) { reconcile.value = status }
}

fun profile(userId: Int, kind: ProfileKind, paused: Boolean = false) =
    Profile(userId = userId, name = kind.name, kind = kind, isPaused = paused, isRestricted = kind == ProfileKind.SECURE_FOLDER)

/** Real `pm list users` output captured on a Tab S10 Ultra and a Galaxy S24 Ultra, 2026-10-24. */
object Fixtures {
    const val USERS_TABLET = """Users:
	UserInfo{0:null:4c13} running
	UserInfo{10: Island :1030} running
	UserInfo{11:Anu Arora:410}
	UserInfo{95:DUAL_APP:20001010} running
	UserInfo{150:Secure Folder:61010} running
"""
    const val USERS_PHONE_PAUSED = """Users:
	UserInfo{0:Owner:4c13} running
	UserInfo{10: Island :10b0}
	UserInfo{95:DUAL_APP:20001010} running
	UserInfo{150:Secure Folder:61010} running
"""
    const val PACKAGES = """package:com.android.settings
package:com.popularapp.thirtydayfitnesschallenge
package:com.whatsapp
"""
    const val DUMPSYS_USER_LINES = """    User 0: ceDataInode=109046 deDataInode=110856 installed=true hidden=false suspended=false distractionFlags=0 stopped=true notLaunched=true enabled=4 instant=false virtual=false quarantined=false
    User 10: ceDataInode=-1 deDataInode=-1 installed=false hidden=true suspended=false distractionFlags=0 stopped=true notLaunched=true enabled=0 instant=false virtual=false quarantined=false
    User 95: ceDataInode=0 deDataInode=0 installed=true hidden=false suspended=true distractionFlags=0 stopped=false notLaunched=false enabled=0 instant=false virtual=false quarantined=false
"""
    const val DPM_OWNERS = """User 10: admin=com.oasisfeng.island/.IslandDeviceAdminReceiver,ManagedProfileOwner(parentUserId=0)
User 150: admin=com.samsung.knox.securefolder/.containeragent.detector.KnoxDeviceAdminReceiver,ManagedProfileOwner(parentUserId=0)
"""
}
