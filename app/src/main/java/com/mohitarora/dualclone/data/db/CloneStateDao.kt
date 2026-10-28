package com.mohitarora.dualclone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CloneStateDao {
    @Query("SELECT * FROM clone_state WHERE userId = :userId")
    abstract fun observe(userId: Int): Flow<List<CloneStateEntity>>

    @Query("SELECT * FROM clone_state")
    abstract fun observeAll(): Flow<List<CloneStateEntity>>

    @Query("SELECT * FROM clone_state WHERE userId = :userId")
    abstract suspend fun get(userId: Int): List<CloneStateEntity>

    @Query("SELECT COUNT(*) FROM clone_state WHERE state = 'INSTALLED'")
    abstract suspend fun installedCount(): Int

    @Query("SELECT DISTINCT userId FROM clone_state")
    abstract suspend fun userIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: CloneStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(entities: List<CloneStateEntity>)

    @Query("DELETE FROM clone_state WHERE packageName = :packageName AND userId = :userId")
    abstract suspend fun delete(packageName: String, userId: Int)

    @Query("DELETE FROM clone_state WHERE userId = :userId")
    abstract suspend fun deleteForUser(userId: Int)

    @Transaction
    open suspend fun replaceForUser(userId: Int, entities: List<CloneStateEntity>) {
        deleteForUser(userId)
        upsertAll(entities)
    }
}
