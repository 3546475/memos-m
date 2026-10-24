package org.example.moxia.account

import android.content.ContextWrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.example.moxia.api.MemosApi
import org.example.moxia.data.DataStoreManager
import org.example.moxia.data.DraftManager
import org.example.moxia.data.audit.SyncAuditDao
import org.example.moxia.data.audit.SyncAuditLogger
import org.example.moxia.data.cache.CachedMemo
import org.example.moxia.data.cache.MemoCacheRepository
import org.example.moxia.data.cache.MemoDao
import org.example.moxia.data.sync.PendingOp
import org.example.moxia.data.sync.PendingOpDao
import org.example.moxia.data.sync.SyncManager
import org.example.moxia.data.sync.SyncRepository
import org.example.moxia.data.sync.SyncWorkScheduler
import org.example.moxia.model.Account
import org.example.moxia.model.Memo
import org.example.moxia.model.Visibility
import org.example.moxia.viewmodel.AccountSession
import org.example.moxia.viewmodel.MemosUiState
import org.example.moxia.viewmodel.delegates.DraftDelegate
import org.example.moxia.viewmodel.delegates.MemoActionDelegateImpl
import org.example.moxia.viewmodel.delegates.MemoListUpdater
import org.junit.Assert.*
import org.junit.Test

/**
 * The editor contract: closing the composer must never race the write that produced it. Creates are
 * local-first now (the card and the queued op are written before the server is asked), edits apply
 * optimistically and roll back when the server rejects them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoEditorSubmissionTest {
    private class Fixture(scope: CoroutineScope) {
        var reject = false
        val cacheWrite = CompletableDeferred<Unit>()
        val stored = mutableListOf<CachedMemo>()
        val ops = linkedMapOf<String, PendingOp>()
        val enqueued = mutableListOf<PendingOp>()
        val state = MutableStateFlow(MemosUiState(isOnline = true))
        val sessions = AccountSession(scope)
        val api = stub<MemosApi> { name, _ ->
            if (reject) error("Server rejected $name")
            when (name) {
                "createMemo", "updateMemo", "createMemoComment", "getMemo" ->
                    Memo(name = "memos/result", content = "Saved")
                else -> error(name)
            }
        }
        init { sessions.activate(Account(id = "editor-submission"), api, OkHttpClient()) }
        val cache = MemoCacheRepository(stub<MemoDao> { name, args ->
            when (name) {
                "insertMemo", "saveMemoState" -> {
                    cacheWrite.await()
                    stored += args[0] as CachedMemo
                    Unit
                }
                else -> Unit
            }
        })
        val sync = SyncManager(scope, SyncRepository(stub<PendingOpDao> { name, args ->
            when (name) {
                "insert" -> {
                    val op = args[0] as PendingOp
                    ops[op.id] = op
                    enqueued += op
                    Unit
                }
                "getOps" -> ops.values.toList()
                "getOp" -> ops[args[0] as String]
                "deleteOp" -> {
                    ops.remove(args[0] as String)
                    Unit
                }
                else -> Unit
            }
        }), cache, DataStoreManager(MemoryPreferences()), object : SyncWorkScheduler {
                override fun schedule(accountId: String) {}
                override fun cancel(accountId: String) {}
            }, SyncAuditLogger(stub<SyncAuditDao> { name, _ ->
                when (name) {
                    "insert" -> Unit
                    "count" -> 0
                    else -> Unit
                }
            }), sessions, { null },
            onMemoSynced = { _, _ -> }, onMemoDeleted = {}, onCommentsRefresh = {}, onConflict = {})
        val actions = MemoActionDelegateImpl(scope, state,
            stub<MemoListUpdater> { _, _ -> Unit }, stub<DraftDelegate> { _, _ -> Unit },
            { null }, { null }, sync, cache, sessions, DraftManager(ContextWrapper(null)), { true })
    }

    @Test fun `create writes the memo locally and queues it without waiting for the server`() = runTest {
        val fixture = Fixture(backgroundScope)
        var success = false
        fixture.actions.createMemo("Saved", Visibility.PRIVATE, onSuccess = { success = true })
        runCurrent()
        assertFalse("the editor waits for the local write", success)
        fixture.cacheWrite.complete(Unit)
        runCurrent()
        assertTrue(success)
        // One create is queued under a temporary identity, and the local card carries that name.
        val queued = fixture.enqueued.single()
        assertTrue(queued.memoName!!.startsWith("offline-"))
        assertEquals("Saved", fixture.stored.first().toMemo()!!.content)
        assertEquals(queued.memoName, fixture.stored.first().toMemo()!!.name)
    }

    @Test fun `update waits for the cache before the editor can finish`() = runTest {
        val fixture = Fixture(backgroundScope)
        var success = false
        fixture.actions.updateMemo(Memo(name = "memos/original", content = "Original"), "Saved",
            Visibility.PRIVATE, emptyList(), onSuccess = { success = true })
        runCurrent()
        assertFalse("the editor waits for the local write", success)
        fixture.cacheWrite.complete(Unit)
        runCurrent()
        assertTrue(success)
        assertTrue(fixture.stored.any { it.toMemo()!!.content == "Saved" })
    }

    @Test fun `comment success waits for cache before editor can finish`() = runTest {
        val fixture = Fixture(backgroundScope)
        var success = false
        fixture.actions.createComment(Memo(name = "memos/parent", content = "Parent"), "Saved",
            onSuccess = { success = true })
        runCurrent()
        assertFalse(success)
        fixture.cacheWrite.complete(Unit)
        runCurrent()
        assertTrue(success)
        assertEquals("memos/parent", fixture.stored.single().parentName)
    }

    @Test fun `rejected update reports failure and rolls the optimistic edit back`() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.cacheWrite.complete(Unit)
        fixture.reject = true
        var failed = false
        var success = false
        val memo = Memo(name = "memos/original", content = "Original")
        fixture.actions.updateMemo(memo, "Saved", Visibility.PRIVATE, emptyList(),
            onError = { failed = true }, onSuccess = { success = true })
        runCurrent()
        assertTrue(failed)
        assertFalse(success)
        assertNotNull(fixture.state.value.error)
        // Optimistic write first, then the rollback to the original content.
        assertEquals("Saved", fixture.stored.first().toMemo()!!.content)
        assertEquals("Original", fixture.stored.last().toMemo()!!.content)
    }

    @Test fun `rejected comment reports failure without success`() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.reject = true
        var failed = false
        var success = false
        fixture.actions.createComment(Memo(name = "memos/parent", content = "Parent"), "Saved",
            onError = { failed = true }, onSuccess = { success = true })
        runCurrent()
        assertTrue(failed)
        assertFalse(success)
        assertTrue(fixture.stored.isEmpty())
    }
}
