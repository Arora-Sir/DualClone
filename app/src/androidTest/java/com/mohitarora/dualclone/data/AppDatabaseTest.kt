package com.mohitarora.dualclone.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mohitarora.dualclone.data.db.AppDatabase
import com.mohitarora.dualclone.data.db.AppEntity
import com.mohitarora.dualclone.data.db.CloneStateEntity
import com.mohitarora.dualclone.data.db.CommandLogDao
import com.mohitarora.dualclone.data.db.CommandLogEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Data layer tests that need a real SQLite. Run with connectedDebugAndroidTest.
 * Schema JSON files in app/schemas must be on the androidTest asset path for MigrationTestHelper.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private lateinit var db: AppDatabase

    @Before
    fun open() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun close() = db.close()

    @Test
    fun commandLogIsCappedAtFiveHundredRowsOnInsert() = runBlocking {
        val dao = db.commandLogDao()
        repeat(CommandLogDao.MAX_ROWS + 25) { i ->
            dao.insert(CommandLogEntry(packageName = "com.example.app", command = "cmd $i", result = "", success = true, userId = 95))
        }
        assertEquals(CommandLogDao.MAX_ROWS, dao.count())
    }

    @Test
    fun cloneStateReplaceForUserSwapsOnlyThatUser() = runBlocking {
        val dao = db.cloneStateDao()
        dao.upsertAll(
            listOf(
                CloneStateEntity("com.a.a", 95, "INSTALLED"),
                CloneStateEntity("com.b.b", 10, "INSTALLED")
            )
        )
        dao.replaceForUser(95, listOf(CloneStateEntity("com.c.c", 95, "HIDDEN")))
        assertEquals(listOf("com.c.c"), dao.get(95).map { it.packageName })
        assertEquals(listOf("com.b.b"), dao.get(10).map { it.packageName })
    }

    @Test
    fun catalogReplaceIsAtomic() = runBlocking {
        val dao = db.appDao()
        dao.upsertApps(listOf(AppEntity("com.old.app", "Old", false, true)))
        dao.replaceCatalog(listOf(AppEntity("com.new.app", "New", false, true)))
        assertEquals(listOf("com.new.app"), dao.getPackageNames())
    }

    /** Baseline for every future migration test. Add one test per version step and keep this one. */
    @Test
    fun version1SchemaOpensWithTheCurrentDatabaseClass() {
        migrationHelper.createDatabase(MIGRATION_DB, 1).close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, MIGRATION_DB).build()
        try {
            reopened.openHelper.writableDatabase
        } finally {
            reopened.close()
            context.deleteDatabase(MIGRATION_DB)
        }
    }

    private companion object {
        const val MIGRATION_DB = "migration-test"
    }
}
