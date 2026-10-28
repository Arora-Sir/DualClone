package com.mohitarora.dualclone.di

import android.content.Context
import androidx.room.Room
import com.mohitarora.dualclone.data.db.AppDao
import com.mohitarora.dualclone.data.db.AppDatabase
import com.mohitarora.dualclone.data.db.AppOpsPresetDao
import com.mohitarora.dualclone.data.db.CloneStateDao
import com.mohitarora.dualclone.data.db.CommandLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // NOTE: Pre release development builds wrote a different schema to this file name, so it is removed once.
        context.deleteDatabase(AppDatabase.LEGACY_DEV_NAME)
        // NOTE: No destructive migration. Every schema change after the first release ships with a tested Migration.
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()
    }

    @Provides
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

    @Provides
    fun provideCloneStateDao(database: AppDatabase): CloneStateDao = database.cloneStateDao()

    @Provides
    fun provideAppOpsPresetDao(database: AppDatabase): AppOpsPresetDao = database.appOpsPresetDao()

    @Provides
    fun provideCommandLogDao(database: AppDatabase): CommandLogDao = database.commandLogDao()
}
