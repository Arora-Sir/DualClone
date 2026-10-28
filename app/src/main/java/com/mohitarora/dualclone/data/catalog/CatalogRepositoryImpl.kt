package com.mohitarora.dualclone.data.catalog

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.mohitarora.dualclone.data.db.AppDao
import com.mohitarora.dualclone.data.db.AppEntity
import com.mohitarora.dualclone.domain.model.CatalogApp
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Host (User 0) app catalog. The DualClone package itself is never part of it. */
class CatalogRepositoryImpl(
    private val context: Context,
    private val dao: AppDao,
    private val host: HostIdentity
) : CatalogRepository {

    override fun observe(query: String): Flow<List<CatalogApp>> {
        val clean = query.trim()
        val source = if (clean.isEmpty()) dao.getAllAppsFlow() else dao.searchAppsLikeFlow(clean)
        return source.map { rows -> rows.filter { !host.isHost(it.packageName) }.map { it.toModel() } }
    }

    override val userAppCount: Flow<Int> get() = dao.getUserAppCountFlow()
    override val systemAppCount: Flow<Int> get() = dao.getSystemAppCountFlow()

    override suspend fun isEmpty(): Boolean = dao.getAppCount() == 0

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val entities = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { !host.isHost(it.packageName) }
                .map { toEntity(pm, it) }
            // NOTE: An empty system answer is not trusted, otherwise a transient failure would wipe the catalog.
            if (entities.isEmpty()) {
                Timber.tag("Catalog").w("PackageManager returned no applications, keeping previous catalog")
            } else {
                dao.replaceCatalog(entities)
                Timber.tag("Catalog").d("Catalog refreshed with %d apps", entities.size)
            }
        } catch (t: Throwable) {
            Timber.tag("Catalog").e(t, "Catalog refresh failed")
        }
    }

    override suspend fun onPackageAdded(packageName: String) = withContext(Dispatchers.IO) {
        if (host.isHost(packageName)) return@withContext
        try {
            val pm = context.packageManager
            dao.upsertApps(listOf(toEntity(pm, pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA))))
        } catch (t: Throwable) {
            Timber.tag("Catalog").w(t, "Could not add %s to the catalog", packageName)
        }
    }

    override suspend fun onPackageRemoved(packageName: String) = withContext(Dispatchers.IO) {
        dao.deleteByPackage(packageName)
    }

    private fun toEntity(pm: PackageManager, info: ApplicationInfo): AppEntity {
        val pkg = info.packageName
        val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        var installTime = 0L
        var updateTime = 0L
        try {
            val p = pm.getPackageInfo(pkg, 0)
            installTime = p.firstInstallTime
            updateTime = p.lastUpdateTime
        } catch (_: Throwable) {
            // Transient lookup failures leave the timestamps at zero.
        }
        return AppEntity(
            packageName = pkg,
            appName = pm.getApplicationLabel(info).toString(),
            isSystemApp = isSystem,
            isLaunchable = pm.getLaunchIntentForPackage(pkg) != null,
            installTime = installTime,
            lastUpdatedTime = updateTime
        )
    }

    private fun AppEntity.toModel() = CatalogApp(packageName, appName, isSystemApp, isLaunchable, installTime, lastUpdatedTime)
}
