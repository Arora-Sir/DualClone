package com.mohitarora.dualclone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// NOTE: Version 1 is the released baseline. Every later change needs a Migration and a MigrationTestHelper test.
@Database(
    entities = [
        AppEntity::class,
        CloneStateEntity::class,
        AppOpsPresetEntity::class,
        CommandLogEntry::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun cloneStateDao(): CloneStateDao
    abstract fun appOpsPresetDao(): AppOpsPresetDao
    abstract fun commandLogDao(): CommandLogDao

    companion object {
        const val NAME = "dualclone_v1.db"

        /** Pre release development builds used this file with a different version 1 schema. */
        const val LEGACY_DEV_NAME = "dualclone.db"
    }
}
