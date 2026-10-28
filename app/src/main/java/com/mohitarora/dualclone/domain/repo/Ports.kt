package com.mohitarora.dualclone.domain.repo

import com.mohitarora.dualclone.domain.model.CatalogApp
import com.mohitarora.dualclone.domain.model.StoredClone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// NOTE: Small ports implemented by the data layer. They keep the clone logic free of Room and Android types so it runs in JVM tests.

fun interface Clock {
    fun now(): Long
}

/** Persistence for per-profile clone state. */
interface CloneStateStore {
    fun observe(userId: Int): Flow<Map<String, StoredClone>>
    fun observeAll(): Flow<List<StoredClone>>
    suspend fun load(userId: Int): Map<String, StoredClone>
    suspend fun replace(userId: Int, entries: Collection<StoredClone>)
    suspend fun put(entry: StoredClone)
    suspend fun remove(packageName: String, userId: Int)
    suspend fun retainUsers(userIds: Set<Int>)
}

/** Writes one audit row per executed command. */
interface CommandAudit {
    suspend fun record(userId: Int, packageName: String, command: String, output: CommandOutput, success: Boolean)
}

/** Packages installed for the primary user. */
interface HostPackages {
    fun observe(): Flow<Set<String>>
    suspend fun snapshot(): Set<String>
}

data class AppOpsPreset(val userId: Int, val packageName: String, val op: String, val mode: String)

interface AppOpsPresetStore {
    suspend fun all(): List<AppOpsPreset>
    suspend fun upsert(preset: AppOpsPreset)
    suspend fun upsertAll(presets: List<AppOpsPreset>)
    suspend fun delete(userId: Int, packageName: String, op: String)
}

interface CatalogRepository {
    fun observe(query: String = ""): Flow<List<CatalogApp>>
    val userAppCount: Flow<Int>
    val systemAppCount: Flow<Int>
    suspend fun isEmpty(): Boolean
    suspend fun refresh()
    suspend fun onPackageAdded(packageName: String)
    suspend fun onPackageRemoved(packageName: String)
}

/** User toggles and small bits of state that survive restarts. */
interface AppSettings {
    val showAdvancedUsers: StateFlow<Boolean>
    val autoRefreshLauncher: StateFlow<Boolean>
    val selectedProfileId: StateFlow<Int?>
    val lastReconcile: StateFlow<ReconcileStatus>
    fun setShowAdvancedUsers(value: Boolean)
    fun setAutoRefreshLauncher(value: Boolean)
    fun setSelectedProfileId(userId: Int?)
    fun setLastReconcile(status: ReconcileStatus)
}

data class ReconcileStatus(val atMillis: Long = 0L, val ok: Boolean = true, val detail: String = "")
