package com.mohitarora.dualclone.domain.repo

import com.mohitarora.dualclone.domain.model.BulkItemResult
import com.mohitarora.dualclone.domain.model.CloneEntry
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Raw privileged command execution. Implemented by Shizuku in the app and by UiAutomation in CI tests. */
interface ShizukuGateway {
    suspend fun run(vararg command: String): CommandOutput
}

data class CommandOutput(val exitCode: Int, val stdout: String, val stderr: String, val timedOut: Boolean = false)

interface ProfileRepository {
    val profiles: StateFlow<List<Profile>>
    val selected: StateFlow<Profile?>
    suspend fun refresh()
    fun select(userId: Int)
}

interface CloneRepository {
    fun clonesFor(userId: Int): Flow<Map<String, CloneEntry>>
    suspend fun reconcile(userId: Int): ListResult<Int>
    suspend fun clone(packageName: String, userId: Int): OpResult
    suspend fun remove(packageName: String, userId: Int): OpResult
    suspend fun cloneAll(packageNames: List<String>, userId: Int): List<BulkItemResult>
    suspend fun removeAll(packageNames: List<String>, userId: Int): List<BulkItemResult>
    suspend fun refreshLauncher(): Boolean
}

interface AppOpsRepository {
    suspend fun get(packageName: String, op: String, userId: Int): ListResult<String>
    suspend fun set(packageName: String, op: String, mode: String, userId: Int): OpResult
}
