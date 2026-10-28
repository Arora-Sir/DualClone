package com.mohitarora.dualclone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CommandLogDao {
    @Query("SELECT * FROM command_log ORDER BY id DESC LIMIT 200")
    abstract fun getRecentLogsFlow(): Flow<List<CommandLogEntry>>

    @Insert
    protected abstract suspend fun insertRaw(entry: CommandLogEntry)

    @Query("DELETE FROM command_log WHERE id NOT IN (SELECT id FROM command_log ORDER BY id DESC LIMIT $MAX_ROWS)")
    protected abstract suspend fun trim()

    /** Inserts and prunes in one transaction so the log never exceeds MAX_ROWS rows. */
    @Transaction
    open suspend fun insert(entry: CommandLogEntry) {
        insertRaw(entry)
        trim()
    }

    @Query("SELECT COUNT(*) FROM command_log")
    abstract suspend fun count(): Int

    @Query("DELETE FROM command_log")
    abstract suspend fun clearAll()

    companion object {
        const val MAX_ROWS = 500
    }
}
