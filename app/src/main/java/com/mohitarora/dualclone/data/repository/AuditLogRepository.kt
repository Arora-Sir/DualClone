package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.data.db.CommandLogDao
import com.mohitarora.dualclone.data.db.CommandLogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Read and clear access to the local command audit log. The log is capped at 500 rows on insert. */
@Singleton
class AuditLogRepository @Inject constructor(private val dao: CommandLogDao) {
    val recent: Flow<List<CommandLogEntry>> get() = dao.getRecentLogsFlow()
    suspend fun clear() = dao.clearAll()
}
