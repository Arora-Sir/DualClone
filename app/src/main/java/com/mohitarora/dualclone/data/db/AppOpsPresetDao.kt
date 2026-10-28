package com.mohitarora.dualclone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOpsPresetDao {
    @Query("SELECT * FROM appops_preset")
    suspend fun all(): List<AppOpsPresetEntity>

    @Query("SELECT * FROM appops_preset")
    fun observeAll(): Flow<List<AppOpsPresetEntity>>

    @Query("SELECT * FROM appops_preset WHERE userId = :userId AND packageName = :packageName")
    suspend fun forPackage(userId: Int, packageName: String): List<AppOpsPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: AppOpsPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(presets: List<AppOpsPresetEntity>)

    @Query("DELETE FROM appops_preset WHERE userId = :userId AND packageName = :packageName AND op = :op")
    suspend fun delete(userId: Int, packageName: String, op: String)

    @Query("DELETE FROM appops_preset WHERE userId = :userId AND packageName = :packageName")
    suspend fun deleteForPackage(userId: Int, packageName: String)
}
