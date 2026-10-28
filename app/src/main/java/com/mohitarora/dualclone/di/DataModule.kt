package com.mohitarora.dualclone.di

import android.content.Context
import com.mohitarora.dualclone.BuildConfig
import com.mohitarora.dualclone.data.backup.BackupRepository
import com.mohitarora.dualclone.data.catalog.CatalogRepositoryImpl
import com.mohitarora.dualclone.data.db.AppDao
import com.mohitarora.dualclone.data.db.AppOpsPresetDao
import com.mohitarora.dualclone.data.db.CloneStateDao
import com.mohitarora.dualclone.data.db.CommandLogDao
import com.mohitarora.dualclone.data.db.RoomAppOpsPresetStore
import com.mohitarora.dualclone.data.db.RoomCloneStateStore
import com.mohitarora.dualclone.data.db.RoomCommandAudit
import com.mohitarora.dualclone.data.db.RoomHostPackages
import com.mohitarora.dualclone.data.prefs.SharedPrefsAppSettings
import com.mohitarora.dualclone.data.repository.AppOpsRepositoryImpl
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.DualMessengerSetup
import com.mohitarora.dualclone.data.repository.OrphanRepository
import com.mohitarora.dualclone.data.repository.ProfileRepositoryImpl
import com.mohitarora.dualclone.data.repository.Reconciler
import com.mohitarora.dualclone.data.repository.ThemeRepository
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.domain.repo.AppOpsPresetStore
import com.mohitarora.dualclone.domain.repo.AppOpsRepository
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import com.mohitarora.dualclone.domain.repo.Clock
import com.mohitarora.dualclone.domain.repo.CloneRepository
import com.mohitarora.dualclone.domain.repo.CloneStateStore
import com.mohitarora.dualclone.domain.repo.CommandAudit
import com.mohitarora.dualclone.domain.repo.HostPackages
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import com.mohitarora.dualclone.shizuku.ShizukuGatewayImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindGateway(impl: ShizukuGatewayImpl): ShizukuGateway
    @Binds abstract fun bindSettings(impl: SharedPrefsAppSettings): AppSettings
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }

    @Provides @Singleton
    fun provideHostIdentity(): HostIdentity = HostIdentity(BuildConfig.APPLICATION_ID)

    @Provides @Singleton
    fun provideCloneStateStore(dao: CloneStateDao): CloneStateStore = RoomCloneStateStore(dao)

    @Provides @Singleton
    fun provideCommandAudit(dao: CommandLogDao): CommandAudit = RoomCommandAudit(dao)

    @Provides @Singleton
    fun provideHostPackages(dao: AppDao): HostPackages = RoomHostPackages(dao)

    @Provides @Singleton
    fun provideAppOpsPresetStore(dao: AppOpsPresetDao): AppOpsPresetStore = RoomAppOpsPresetStore(dao)

    @Provides @Singleton
    fun provideProfileRepositoryImpl(
        gateway: ShizukuGateway,
        settings: AppSettings,
        @ApplicationScope scope: CoroutineScope
    ): ProfileRepositoryImpl = ProfileRepositoryImpl(gateway, settings, scope)

    @Provides @Singleton
    fun provideProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository = impl

    @Provides @Singleton
    fun provideCloneStateRepository(
        gateway: ShizukuGateway,
        store: CloneStateStore,
        audit: CommandAudit,
        hostPackages: HostPackages,
        profiles: ProfileRepository,
        host: HostIdentity,
        clock: Clock
    ): CloneStateRepository = CloneStateRepository(gateway, store, audit, hostPackages, profiles, host, clock)

    @Provides @Singleton
    fun provideCloneRepository(impl: CloneStateRepository): CloneRepository = impl

    @Provides @Singleton
    fun provideAppOpsRepositoryImpl(
        gateway: ShizukuGateway,
        audit: CommandAudit,
        presets: AppOpsPresetStore,
        profiles: ProfileRepository
    ): AppOpsRepositoryImpl = AppOpsRepositoryImpl(gateway, audit, presets, profiles)

    @Provides @Singleton
    fun provideAppOpsRepository(impl: AppOpsRepositoryImpl): AppOpsRepository = impl

    @Provides @Singleton
    fun provideCatalogRepository(
        @ApplicationContext context: Context,
        dao: AppDao,
        host: HostIdentity
    ): CatalogRepository = CatalogRepositoryImpl(context, dao, host)

    @Provides @Singleton
    fun provideReconciler(
        catalog: CatalogRepository,
        profiles: ProfileRepository,
        clones: CloneStateRepository,
        appOps: AppOpsRepository,
        presets: AppOpsPresetStore,
        settings: AppSettings,
        clock: Clock
    ): Reconciler = Reconciler(catalog, profiles, clones, appOps, presets, settings, clock)

    @Provides @Singleton
    fun provideDualMessengerSetup(gateway: ShizukuGateway, audit: CommandAudit, profiles: ProfileRepository): DualMessengerSetup =
        DualMessengerSetup(gateway, audit, profiles)

    @Provides @Singleton
    fun provideOrphanRepository(
        gateway: ShizukuGateway,
        store: CloneStateStore,
        profiles: ProfileRepository,
        clones: CloneRepository,
        host: HostIdentity
    ): OrphanRepository = OrphanRepository(gateway, store, profiles, clones, host)

    @Provides @Singleton
    fun provideBackupRepository(
        profiles: ProfileRepository,
        clones: CloneStateRepository,
        hostPackages: HostPackages,
        presets: AppOpsPresetStore,
        appOps: AppOpsRepositoryImpl,
        theme: ThemeRepository,
        clock: Clock,
        updates: com.mohitarora.dualclone.update.UpdateRepository
    ): BackupRepository = BackupRepository(
        profiles, clones, hostPackages, presets, appOps, theme, clock, BuildConfig.VERSION_NAME,
        dailyUpdateCheck = { updates.dailyCheckEnabled.value },
        setDailyUpdateCheck = updates::setDailyCheck
    )
}
