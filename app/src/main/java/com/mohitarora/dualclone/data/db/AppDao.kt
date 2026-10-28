package com.mohitarora.dualclone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppDao {
    @Query("SELECT * FROM app ORDER BY appName COLLATE NOCASE ASC")
    abstract fun getAllAppsFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM app WHERE appName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY appName COLLATE NOCASE ASC")
    abstract fun searchAppsLikeFlow(query: String): Flow<List<AppEntity>>

    @Query("SELECT COUNT(*) FROM app WHERE isSystemApp = 0")
    abstract fun getUserAppCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app WHERE isSystemApp = 1")
    abstract fun getSystemAppCountFlow(): Flow<Int>

    @Query("SELECT packageName FROM app")
    abstract fun getPackageNamesFlow(): Flow<List<String>>

    @Query("SELECT packageName FROM app")
    abstract suspend fun getPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertApps(apps: List<AppEntity>)

    @Query("DELETE FROM app")
    protected abstract suspend fun deleteAll()

    /** Swaps the whole catalog atomically so observers never see a half loaded table. */
    @Transaction
    open suspend fun replaceCatalog(apps: List<AppEntity>) {
        deleteAll()
        upsertApps(apps)
    }

    @Query("DELETE FROM app WHERE packageName = :packageName")
    abstract suspend fun deleteByPackage(packageName: String)

    @Query("SELECT COUNT(*) FROM app")
    abstract suspend fun getAppCount(): Int

    @Query("SELECT * FROM app WHERE packageName = :packageName LIMIT 1")
    abstract suspend fun getAppByPackage(packageName: String): AppEntity?
}
