package org.example.memosm.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.example.memosm.api.GsonProvider
import org.example.memosm.api.MemosApi
import org.example.memosm.data.DataStoreManager
import org.example.memosm.data.audit.SyncAuditDao
import org.example.memosm.data.audit.SyncAuditLogger
import org.example.memosm.data.cache.MemoCacheRepository
import org.example.memosm.data.cache.MemoDao
import org.example.memosm.data.sync.ConflictItem
import org.example.memosm.data.sync.ConflictResolution
import org.example.memosm.data.sync.PendingOp
import org.example.memosm.data.sync.PendingOpDao
import org.example.memosm.data.sync.PendingOpType
import org.example.memosm.data.sync.SyncManager
import org.example.memosm.data.sync.SyncRepository
import org.example.memosm.data.sync.SyncWorkScheduler
import org.example.memosm.model.Account
import org.example.memosm.model.Memo
import org.example.memosm.viewmodel.AccountSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/**
 * Resolving a conflict with KEEP_LOCAL/MERGE overwrites the server copy, so the server version is
 * re-read first: the dialog may have been open for minutes and another device may have edited the
 * memo again. Pushing the version snapshotted at detection time would silently drop that edit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConflictRereadTest {

    private val detectedAt = Instant.parse("2024-01-02T00:00:00Z")
    private val changedAgain = Instant.parse("2024-01-03T00:00:00Z")

    @Test
    fun `keep local re-surfaces the conflict instead of overwriting a newer server version`() = runTest {
        val fixture = Fixture(backgroundScope, detectedAt, changedAgain)

        fixture.manager.resolveConflict(fixture.conflict, ConflictResolution.KEEP_LOCAL)
        runCurrent()

        assertEquals(0, fixture.updateCalls)
        assertEquals(1, fixture.conflicts.size)
        // The dialog now shows the version that would have been overwritten.
        assertEquals(changedAgain, fixture.conflicts.single().serverMemo.updateTime)
        assertEquals(1, fixture.reads)
    }

    @Test
    fun `keep local still applies when the server version did not change`() = runTest {
        val fixture = Fixture(backgroundScope, detectedAt, detectedAt)

        fixture.manager.resolveConflict(fixture.conflict, ConflictResolution.KEEP_LOCAL)
        runCurrent()

        assertEquals(1, fixture.updateCalls)
        assertTrue(fixture.conflicts.isEmpty())
        assertEquals(1, fixture.synced)
    }

    @Test
    fun `keep server does not need to read or write the server`() = runTest {
        val fixture = Fixture(backgroundScope, detectedAt, changedAgain)

        fixture.manager.resolveConflict(fixture.conflict, ConflictResolution.KEEP_SERVER)
        runCurrent()

        assertEquals(0, fixture.reads)
        assertEquals(0, fixture.updateCalls)
        assertEquals(1, fixture.synced)
    }

    private class Fixture(
        scope: kotlinx.coroutines.CoroutineScope,
        private val detectedAt: Instant,
        serverUpdateTime: Instant
    ) {
        private val accountId = "conflict-account"
        private val memoName = "memos/1"
        var reads = 0
        var updateCalls = 0
        var synced = 0
        val conflicts = mutableListOf<ConflictItem>()

        private val local = Memo(name = memoName, content = "local text", updateTime = detectedAt)
        private val serverAtDetection = Memo(
            name = memoName, content = "server text", updateTime = detectedAt
        )
        private val freshServer = serverAtDetection.copy(
            content = "server text v2", updateTime = serverUpdateTime
        )

        private val api = stub<MemosApi> { name, args ->
            when (name) {
                "getMemo" -> { reads++; freshServer }
                "updateMemo" -> {
                    updateCalls++
                    // Echo the request back the way the server does.
                    (args[1] as Memo).copy(name = memoName, updateTime = serverUpdateTime)
                }
                else -> error(name)
            }
        }

        private val op = PendingOp.new(
            accountId = accountId,
            type = PendingOpType.UPDATE,
            memoName = memoName,
            payloadJson = GsonProvider.gson.toJson(local),
            updateMask = "content",
            baseUpdateTime = detectedAt.toString()
        )

        val conflict = ConflictItem(
            opId = op.id,
            accountId = accountId,
            memoName = memoName,
            localMemo = local,
            serverMemo = serverAtDetection
        )

        private val session = AccountSession(scope).apply {
            activate(Account(id = accountId), api, OkHttpClient())
        }

        private val repository = SyncRepository(stub<PendingOpDao> { name, _ ->
            when (name) {
                "getOp" -> op
                "getOps" -> emptyList<PendingOp>()
                "deleteOp" -> Unit
                else -> error(name)
            }
        })

        val manager = SyncManager(
            scope = scope,
            repository = repository,
            memoCacheRepository = MemoCacheRepository(stub<MemoDao> { _, _ -> Unit }),
            dataStoreManager = DataStoreManager(MemoryPreferences()),
            workScheduler = object : SyncWorkScheduler {
                override fun schedule(accountId: String) {}
                override fun cancel(accountId: String) {}
            },
            auditLogger = SyncAuditLogger(stub<SyncAuditDao> { name, _ ->
                if (name == "count") 0 else Unit
            }),
            accountSession = session,
            currentUserProvider = { null },
            onMemoSynced = { _, _ -> synced++ },
            onMemoDeleted = {},
            onCommentsRefresh = {},
            onConflict = { conflicts += it }
        )
    }
}
