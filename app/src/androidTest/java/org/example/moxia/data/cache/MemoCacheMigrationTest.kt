package org.example.moxia.data.cache

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that upgrading an older database to the current schema
 * ([MEMO_CACHE_DB_VERSION]) preserves the durable cache/outbox tables and, for
 * the risky migrations, the rows inside them. Uses Room's [MigrationTestHelper],
 * which validates the post-migration schema against the exported JSON.
 */
@RunWith(AndroidJUnit4::class)
class MemoCacheMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MemoCacheDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2ToLatest() {
        helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 2).close()
        helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun migrate3ToLatest() {
        helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 3).close()
        helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun migrate4ToLatest() {
        helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 4).close()
        helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun migrate5ToLatest() {
        helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 5).close()
        helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )
    }

    @Test
    fun migrate6ToLatest() {
        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 6)
        insertCachedMemoV3Plus(db, name = "memos/1", accountId = "acc-1", listType = "INBOX")
        insertSyncAuditEntry(db, occurredAt = 1000L, accountHash = "hash-1")
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        // Pre-v7 rows survive; the audit log moves to a separate database and
        // its table is dropped (old audit rows are intentionally not carried over).
        assertEquals(1, countRows(migrated, "cached_memos"))
        assertFalse(tableExists(migrated, "sync_audit_log"))
        assertTrue(tableExists(migrated, "cached_attachment_meta"))
    }

    /**
     * v7 -> v8 adds the cached_attachment_meta table and the later versions
     * only touch indices, so pre-existing rows pass through untouched.
     */
    @Test
    fun migrate7ToLatestAddsAttachmentMetaTable() {
        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 7)
        insertCachedMemoV3Plus(db, name = "memos/1", accountId = "acc-1", listType = "INBOX")
        db.execSQL(
            "INSERT INTO cached_attachments (accountId, attachmentName, memoName, url, localPath, size, downloadedAt, lastAccessedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("acc-1", "attachments/9", "memos/1", "https://example.com/a.png", "/cache/a.png", 2048L, 200L, 300L)
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        assertEquals(1, countRows(migrated, "cached_memos"))
        assertEquals(1, countRows(migrated, "cached_attachments"))
        assertTrue(tableExists(migrated, "cached_attachment_meta"))
        assertEquals(0, countRows(migrated, "cached_attachment_meta"))
    }

    /**
     * v8 -> v9 only adds the outbox lookup indices, so the queue and the cache
     * must survive untouched and both indices must be present afterwards.
     */
    @Test
    fun migrate8ToLatestAddsPendingOpLookupIndices() {
        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 8)
        insertCachedMemoV3Plus(db, name = "memos/1", accountId = "acc-1", listType = "INBOX")
        db.execSQL(
            "INSERT INTO pending_ops (id, accountId, type, memoName, parentName, payloadJson, " +
                "updateMask, baseUpdateTime, createdAt, attemptCount, lastError, lastAttemptAt, permanentlyFailed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("op-1", "acc-1", "UPDATE", "memos/1", null, "{\"content\":\"x\"}", "content", null, 100L, 3L, "timeout", 200L, 1)
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        assertEquals(1, countRows(migrated, "cached_memos"))
        assertEquals(1, countRows(migrated, "pending_ops"))
        assertTrue(indexExists(migrated, "index_pending_ops_accountId_memoName"))
        assertTrue(indexExists(migrated, "index_pending_ops_accountId_parentName"))
        migrated.query(
            "SELECT attemptCount, permanentlyFailed, lastError FROM pending_ops WHERE id = ?",
            arrayOf<Any?>("op-1")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(3L, cursor.getLong(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals("timeout", cursor.getString(2))
        }
    }

    /**
     * v9 -> v10 adds the permanent-failure flag to the attachment upload queue. Every existing
     * row is a pending upload by definition, so it must survive with the flag cleared.
     */
    @Test
    fun migrate9ToLatestAddsAttachmentFailureFlag() {
        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 9)
        db.execSQL(
            "INSERT INTO attachment_uploads (id, accountId, clientId, localPath, filename, mimeType, " +
                "size, createdAt, attemptCount, lastError, lastAttemptAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "upload-1", "acc-1", "client-1", "/files/upload-1", "a.png", "image/png",
                42L, 100L, 2L, "timeout", 200L
            )
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        assertEquals(1, countRows(migrated, "attachment_uploads"))
        migrated.query(
            "SELECT attemptCount, lastError, permanentlyFailed FROM attachment_uploads WHERE id = ?",
            arrayOf<Any?>("upload-1")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2L, cursor.getLong(0))
            assertEquals("timeout", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
        }
    }

    /**
     * v2 -> v3 rebuilds cached_memos and backfills the indexed columns from
     * memoJson. Asserts the backfill landed and that a row with unparseable
     * memoJson is kept with default column values.
     */
    @Test
    fun migrate2ToLatestBackfillsIndexedMemoColumns() {
        val createTime = "2024-01-02T03:04:05Z"
        val updateTime = "2024-02-03T04:05:06Z"
        val memoJson = """
            {"name":"memos/1","state":"NORMAL","createTime":"$createTime",
             "updateTime":"$updateTime","content":"hello world",
             "visibility":"PUBLIC","tags":["alpha","beta"],"pinned":true}
        """.trimIndent().replace("\n", "")

        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 2)
        // v2 cached_memos: name, accountId, listType, memoJson, displayOrder, cachedAt
        db.execSQL(
            "INSERT INTO cached_memos (name, accountId, listType, memoJson, displayOrder, cachedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("memos/1", "acc-1", "INBOX", memoJson, 5L, 42L)
        )
        db.execSQL(
            "INSERT INTO cached_memos (name, accountId, listType, memoJson, displayOrder, cachedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("memos/broken", "acc-1", "INBOX", "{not valid json", 6L, 43L)
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        migrated.query(
            "SELECT content, createTime, updateTime, visibility, state, tags, pinned " +
                "FROM cached_memos WHERE name = ? AND accountId = ? AND listType = ?",
            arrayOf<Any?>("memos/1", "acc-1", "INBOX")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("hello world", cursor.getString(0))
            assertEquals(Instant.parse(createTime).toEpochMilliseconds(), cursor.getLong(1))
            assertEquals(Instant.parse(updateTime).toEpochMilliseconds(), cursor.getLong(2))
            assertEquals("PUBLIC", cursor.getString(3))
            assertEquals("NORMAL", cursor.getString(4))
            assertEquals("[\"alpha\",\"beta\"]", cursor.getString(5))
            assertEquals(1, cursor.getInt(6))
        }

        // Unparseable memoJson: row survives the rebuild, indexed columns keep defaults.
        migrated.query(
            "SELECT content, createTime, updateTime, visibility, state, tags, pinned " +
                "FROM cached_memos WHERE name = ?",
            arrayOf<Any?>("memos/broken")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertEquals(0L, cursor.getLong(1))
            assertEquals(0L, cursor.getLong(2))
            assertEquals("", cursor.getString(3))
            assertEquals("", cursor.getString(4))
            assertEquals("", cursor.getString(5))
            assertEquals(0, cursor.getInt(6))
        }
    }

    /**
     * v4 -> v5 rebuilds pending_ops (adding lastAttemptAt/permanentlyFailed)
     * and cached_attachments (account-scoped primary key). Asserts rows survive
     * with the new columns defaulting to 0 and attachment data intact.
     */
    @Test
    fun migrate4ToLatestPreservesPendingOpsAndAttachments() {
        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 4)
        // v4 pending_ops has no lastAttemptAt/permanentlyFailed columns.
        db.execSQL(
            "INSERT INTO pending_ops (id, accountId, type, memoName, parentName, payloadJson, " +
                "updateMask, baseUpdateTime, createdAt, attemptCount, lastError) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("op-1", "acc-1", "UPDATE_MEMO", "memos/1", null, "{\"content\":\"x\"}", "content", null, 100L, 2L, "timeout")
        )
        // v4 cached_attachments: single-column primary key on attachmentName.
        db.execSQL(
            "INSERT INTO cached_attachments (accountId, attachmentName, memoName, url, localPath, size, downloadedAt, lastAccessedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("acc-1", "attachments/9", "memos/1", "https://example.com/a.png", "/cache/a.png", 2048L, 200L, 300L)
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        migrated.query(
            "SELECT accountId, type, memoName, payloadJson, attemptCount, lastError, lastAttemptAt, permanentlyFailed " +
                "FROM pending_ops WHERE id = ?",
            arrayOf<Any?>("op-1")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("acc-1", cursor.getString(0))
            assertEquals("UPDATE_MEMO", cursor.getString(1))
            assertEquals("memos/1", cursor.getString(2))
            assertEquals("{\"content\":\"x\"}", cursor.getString(3))
            assertEquals(2L, cursor.getLong(4))
            assertEquals("timeout", cursor.getString(5))
            assertEquals(0L, cursor.getLong(6))
            assertEquals(0, cursor.getInt(7))
        }

        migrated.query(
            "SELECT accountId, memoName, url, localPath, size, downloadedAt, lastAccessedAt " +
                "FROM cached_attachments WHERE attachmentName = ?",
            arrayOf<Any?>("attachments/9")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("acc-1", cursor.getString(0))
            assertEquals("memos/1", cursor.getString(1))
            assertEquals("https://example.com/a.png", cursor.getString(2))
            assertEquals("/cache/a.png", cursor.getString(3))
            assertEquals(2048L, cursor.getLong(4))
            assertEquals(200L, cursor.getLong(5))
            assertEquals(300L, cursor.getLong(6))
        }
    }

    /**
     * v5 -> v6 only adds tables and v6 -> v7 only drops sync_audit_log, so
     * pre-existing cache/outbox rows must pass through untouched.
     */
    @Test
    fun migrate5ToLatestPreservesCacheOutboxAndDropsAuditLog() {
        val db = helper.createDatabase(MemoCacheDatabase.DATABASE_NAME, 5)
        insertCachedMemoV3Plus(db, name = "memos/1", accountId = "acc-1", listType = "INBOX")
        db.execSQL(
            "INSERT INTO pending_ops (id, accountId, type, memoName, parentName, payloadJson, " +
                "updateMask, baseUpdateTime, createdAt, attemptCount, lastError, lastAttemptAt, permanentlyFailed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("op-1", "acc-1", "DELETE_MEMO", "memos/1", null, null, null, null, 100L, 0L, null, 0L, 0)
        )
        db.execSQL(
            "INSERT INTO cached_attachments (accountId, attachmentName, memoName, url, localPath, size, downloadedAt, lastAccessedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("acc-1", "attachments/9", "memos/1", "https://example.com/a.png", "/cache/a.png", 2048L, 200L, 300L)
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            MemoCacheDatabase.DATABASE_NAME,
            MEMO_CACHE_DB_VERSION,
            true,
            *MemoCacheDatabase.ALL_MIGRATIONS
        )

        assertEquals(1, countRows(migrated, "cached_memos"))
        assertEquals(1, countRows(migrated, "pending_ops"))
        assertEquals(1, countRows(migrated, "cached_attachments"))
        assertFalse(tableExists(migrated, "sync_audit_log"))
    }

    /**
     * Smoke test: a freshly created database opens with the latest schema.
     * (Does not compare against the exported schema JSON; schema equality is
     * covered by the MigrationTestHelper validations above.)
     */
    @Test
    fun freshDatabaseOpensWithLatestSchema() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MemoCacheDatabase::class.java
        ).allowMainThreadQueries().build()
        db.openHelper.writableDatabase
        db.close()
    }

    /** Inserts a cached_memos row in the v3+ shape (composite PK, indexed columns present). */
    private fun insertCachedMemoV3Plus(
        db: SupportSQLiteDatabase,
        name: String,
        accountId: String,
        listType: String
    ) {
        db.execSQL(
            "INSERT INTO cached_memos (name, accountId, listType, memoJson, displayOrder, cachedAt, " +
                "content, createTime, updateTime, visibility, state, tags, pinned, parentName) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(name, accountId, listType, "{\"content\":\"hi\"}", 1L, 10L, "hi", 20L, 30L, "PRIVATE", "NORMAL", "[]", 0, null)
        )
    }

    /** Inserts a sync_audit_log row (table exists in schema v6). */
    private fun insertSyncAuditEntry(db: SupportSQLiteDatabase, occurredAt: Long, accountHash: String) {
        db.execSQL(
            "INSERT INTO sync_audit_log (occurredAt, accountHash, event, operation, outcome, targetHash, detailCode) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(occurredAt, accountHash, "SYNC", "REFRESH", "SUCCESS", null, null)
        )
    }

    private fun countRows(db: SupportSQLiteDatabase, table: String): Int {
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf<Any?>(table)
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0) > 0
        }
    }

    private fun indexExists(db: SupportSQLiteDatabase, index: String): Boolean {
        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = ?",
            arrayOf<Any?>(index)
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0) > 0
        }
    }
}
