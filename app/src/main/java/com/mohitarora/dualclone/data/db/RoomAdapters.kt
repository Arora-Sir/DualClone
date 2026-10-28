package com.mohitarora.dualclone.data.db

import com.mohitarora.dualclone.domain.model.CloneOriginKind
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.StoredClone
import com.mohitarora.dualclone.domain.repo.AppOpsPreset
import com.mohitarora.dualclone.domain.repo.AppOpsPresetStore
import com.mohitarora.dualclone.domain.repo.CloneStateStore
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.domain.repo.HostPackages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class RoomCloneStateStore(private val dao: CloneStateDao) : CloneStateStore {

    override fun observe(userId: Int): Flow<Map<String, StoredClone>> =
        dao.observe(userId).map { rows -> rows.associate { it.packageName to it.toDomain() } }

    override fun observeAll(): Flow<List<StoredClone>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun load(userId: Int): Map<String, StoredClone> =
        dao.get(userId).associate { it.packageName to it.toDomain() }

    override suspend fun replace(userId: Int, entries: Collection<StoredClone>) =
        dao.replaceForUser(userId, entries.map { it.toEntity() })

    override suspend fun put(entry: StoredClone) = dao.upsert(entry.toEntity())

    override suspend fun remove(packageName: String, userId: Int) = dao.delete(packageName, userId)

    override suspend fun retainUsers(userIds: Set<Int>) {
        dao.userIds().filter { it !in userIds }.forEach { dao.deleteForUser(it) }
    }

    private fun CloneStateEntity.toDomain() = StoredClone(
        packageName = packageName,
        userId = userId,
        state = runCatching { CloneState.valueOf(state) }.getOrDefault(CloneState.UNKNOWN),
        origin = runCatching { CloneOriginKind.valueOf(origin) }.getOrDefault(CloneOriginKind.UNKNOWN),
        verifiedAtMillis = verifiedAt
    )

    private fun StoredClone.toEntity() = CloneStateEntity(packageName, userId, state.name, origin.name, verifiedAtMillis)
}

class RoomCommandAudit(private val dao: CommandLogDao) : CommandAudit {
    override suspend fun record(userId: Int, packageName: String, command: String, output: CommandOutput, success: Boolean) {
        try {
            dao.insert(
                CommandLogEntry(
                    packageName = packageName,
                    command = command,
                    result = (output.stdout + "\n" + output.stderr).trim().take(MAX_RESULT_CHARS),
                    success = success,
                    userId = userId,
                    exitCode = output.exitCode
                )
            )
        } catch (t: Throwable) {
            Timber.tag("Audit").w(t, "Failed to persist command log")
        }
    }

    private companion object {
        const val MAX_RESULT_CHARS = 2000
    }
}

class RoomHostPackages(private val dao: AppDao) : HostPackages {
    override fun observe(): Flow<Set<String>> = dao.getPackageNamesFlow().map { it.toSet() }
    override suspend fun snapshot(): Set<String> = dao.getPackageNames().toSet()
}

class RoomAppOpsPresetStore(private val dao: AppOpsPresetDao) : AppOpsPresetStore {
    override suspend fun all(): List<AppOpsPreset> = dao.all().map { AppOpsPreset(it.userId, it.packageName, it.op, it.mode) }

    override suspend fun upsert(preset: AppOpsPreset) =
        dao.upsert(AppOpsPresetEntity(preset.userId, preset.packageName, preset.op, preset.mode))

    override suspend fun upsertAll(presets: List<AppOpsPreset>) =
        dao.upsertAll(presets.map { AppOpsPresetEntity(it.userId, it.packageName, it.op, it.mode) })

    override suspend fun delete(userId: Int, packageName: String, op: String) = dao.delete(userId, packageName, op)
}
